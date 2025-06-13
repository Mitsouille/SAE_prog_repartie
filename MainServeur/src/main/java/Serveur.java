import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Serveur {
    private HttpServer serveur;
    private final int portHttp;
    private final String hostRmi;
    private final int portRmi;

    public Serveur(int portHttp, String hostRmi, int portRmi) {
        this.portHttp = portHttp;
        this.hostRmi = hostRmi;
        this.portRmi = portRmi;
    }

    public void demarrer() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("0.0.0.0", portHttp), 0);

        // Création de routes avec nom du service RMI différent
        serveur.createContext("/data", new HandlerRestaurantRMI(hostRmi, portRmi));
        serveur.createContext("/incidents", new HandlerRMI(hostRmi, portRmi));

        serveur.setExecutor(null);
        serveur.start();
        System.out.println("Serveur HTTP démarré sur le port " + portHttp);
    }

    static class HandlerRestaurantRMI implements HttpHandler {
        private final String host;
        private final int port;

        public HandlerRestaurantRMI(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void handle(HttpExchange echange) throws IOException {
            String path = echange.getRequestURI().getPath();
            String query = echange.getRequestURI().getQuery();
            String method = echange.getRequestMethod();

            // CORS
            String origin = echange.getRequestHeaders().getFirst("Origin");
            if (origin != null) {
                echange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            }
            echange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            echange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                echange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Registry reg = LocateRegistry.getRegistry(host, port);
                ServiceRestaurant service = (ServiceRestaurant) reg.lookup("ServiceRestaurant");

                String jsonResponse;

                if (path.endsWith("/restaurants")) {
                    jsonResponse = service.getTousLesRestaurantsJson();

                } else if (path.endsWith("/reservations")) {
                    jsonResponse = service.getToutesLesReservationsJson();

                } else if (path.endsWith("/tables")) {
                    int idRestaurant = getQueryParamInt(query, "idRestaurant");
                    String jsonParams = "{"
                        + "\"idRestaurant\":" + idRestaurant + "}";

                    jsonResponse = service.getTablesParRestaurantJson(jsonParams);

                } else if (path.endsWith("/placesDisponibles")) {
                    int idRestaurant = getQueryParamInt(query, "idRestaurant");
                    LocalDateTime debut = getQueryParamDateTime(query, "debut");
                    LocalDateTime fin = getQueryParamDateTime(query, "fin");
                    String jsonParams = "{"
                        + "\"idRestaurant\":" + idRestaurant + ","
                        + "\"debut\":\"" + debut.toString() + "\","
                        + "\"fin\":\"" + fin.toString() + "\""
                        + "}";

                    jsonResponse = service.getPlacesDisponiblesJson(jsonParams);

                } else if (path.endsWith("/reserver") && "POST".equalsIgnoreCase(method)) {
                    String body = new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    jsonResponse = service.reserverTableJson(body);

                } else if (path.endsWith("/annuler")) {
                    int idReservation = getQueryParamInt(query, "idReservation");
                    String jsonParams = "{"
                        + "\"idReservation\":" + idReservation + "}";

                    jsonResponse = service.annulerReservationJson(jsonParams);

                } else {
                    throw new IllegalArgumentException("Chemin inconnu : " + path);
                }

                sendResponse(echange, 200, jsonResponse);

            } catch (Exception e) {
                sendResponse(echange, 500, "{\"erreur\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            }
        }

        private void sendResponse(HttpExchange echange, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            echange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = echange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private int getQueryParamInt(String query, String key) {
            if (query == null) return -1;
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2 && parts[0].equals(key)) {
                    return Integer.parseInt(parts[1]);
                }
            }
            return -1;
        }

        private LocalDateTime getQueryParamDateTime(String query, String key) {
            if (query == null) return null;
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2 && parts[0].equals(key)) {
                    return LocalDateTime.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            }
            return null;
        }
    }


    static class HandlerRMI implements HttpHandler {
        private final String host;
        private final int port;
        public HandlerRMI(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void handle(HttpExchange echange) throws IOException {
            String origin = echange.getRequestHeaders().getFirst("Origin");
            if (origin != null) {
                echange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            }

            echange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            echange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");

            if ("OPTIONS".equalsIgnoreCase(echange.getRequestMethod())) {
                echange.sendResponseHeaders(204, -1); // No Content
                return;
            }

            try {
                Registry reg = LocateRegistry.getRegistry(host, port);
                Service service = (Service) reg.lookup("ServiceIncidents");

                // Appel du service
                String reponseJson = service.getMessage();  // On suppose que ça retourne un JSON

                byte[] bytes = reponseJson.getBytes(StandardCharsets.UTF_8);
                echange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                echange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = echange.getResponseBody()) {
                    os.write(bytes);
                }

            } catch (Exception e) {
                String erreur = "{\"erreur\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
                byte[] erreurBytes = erreur.getBytes(StandardCharsets.UTF_8);
                echange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                echange.sendResponseHeaders(500, erreurBytes.length);

                try (OutputStream os = echange.getResponseBody()) {
                    os.write(erreurBytes);
                }
            }
        }
    }
}
