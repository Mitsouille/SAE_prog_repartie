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

        // Contextes REST pour le service Restaurant
        serveur.createContext("/restaurants", new HandlerResto(hostRmi, portRmi));
        serveur.createContext("/reservations", new HandlerResto(hostRmi, portRmi));
        serveur.createContext("/tables", new HandlerResto(hostRmi, portRmi));
        serveur.createContext("/placesDisponibles", new HandlerResto(hostRmi, portRmi));
        serveur.createContext("/reserver", new HandlerResto(hostRmi, portRmi));
        serveur.createContext("/annuler", new HandlerResto(hostRmi, portRmi));

        serveur.setExecutor(null);
        serveur.start();
        System.out.println("Serveur HTTP démarré sur le port " + portHttp);
    }

    static class HandlerResto implements HttpHandler {
        private final String host;
        private final int port;

        HandlerResto(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            // CORS
            String origin = ex.getRequestHeaders().getFirst("Origin");
            if (origin != null) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            }
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String path  = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();
            String jsonResponse;

            try {
                Registry reg = LocateRegistry.getRegistry(host, port);
                ServiceRestaurant service = (ServiceRestaurant) reg.lookup("ServiceRestaurant");

                if (path.endsWith("/restaurants")) {
                    jsonResponse = service.getTousLesRestaurantsJson();

                } else if (path.endsWith("/reservations")) {
                    jsonResponse = service.getToutesLesReservationsJson();

                } else if (path.endsWith("/tables")) {
                    int idR = getIntParam(query, "idRestaurant");
                    String req = "{\"idRestaurant\":" + idR + "}";
                    jsonResponse = service.getTablesParRestaurantJson(req);

                } else if (path.endsWith("/placesDisponibles")) {
                    int idR = getIntParam(query, "idRestaurant");
                    String debut = getParam(query, "debut");
                    String fin   = getParam(query, "fin");
                    String req = String.format(
                        "{\"idRestaurant\":%d,\"debut\":\"%s\",\"fin\":\"%s\"}",
                        idR, debut, fin
                    );
                    jsonResponse = service.getPlacesDisponiblesJson(req);

                } else if (path.endsWith("/reserver")) {
                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    jsonResponse = service.reserverTableJson(body);

                } else if (path.endsWith("/annuler")) {
                    int idRes = getIntParam(query, "idReservation");
                    String req = "{\"idReservation\":" + idRes + "}";
                    jsonResponse = service.annulerReservationJson(req);

                } else {
                    throw new IllegalArgumentException("Chemin inconnu : " + path);
                }

                sendJson(ex, 200, jsonResponse);

            } catch (Exception e) {
                sendJson(ex, 500,
                  "{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}"
                );
            }
        }

        private void sendJson(HttpExchange ex, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private int getIntParam(String query, String key) {
            if (query == null) return -1;
            for (String p : query.split("&")) {
                String[] kv = p.split("=",2);
                if (kv.length == 2 && kv[0].equals(key)) {
                    return Integer.parseInt(kv[1]);
                }
            }
            return -1;
        }

        private String getParam(String query, String key) {
            if (query == null) return "";
            for (String p : query.split("&")) {
                String[] kv = p.split("=",2);
                if (kv.length == 2 && kv[0].equals(key)) {
                    return kv[1];
                }
            }
            return "";
        }
    }
}
