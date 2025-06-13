
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;


public class Serveur {
    private HttpsServer serveur;
    private final int portHttps;
    private final String hostRmi;
    private final int portRmi;

    public Serveur(int portHttps, String hostRmi, int portRmi) {
        this.portHttps = portHttps;
        this.hostRmi = hostRmi;
        this.portRmi = portRmi;
    }

    public void demarrer() throws Exception {
        // Charger le keystore
        char[] password = "azerty".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), password);

        // Initialiser le KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // Créer le contexte SSL
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        // Créer le serveur HTTPS
        serveur = HttpsServer.create(new InetSocketAddress(portHttps), 0);
        serveur.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                SSLContext c = getSSLContext();
                SSLEngine engine = c.createSSLEngine();
                params.setNeedClientAuth(false);
                params.setCipherSuites(engine.getEnabledCipherSuites());
                params.setProtocols(engine.getEnabledProtocols());
                params.setSSLParameters(c.getDefaultSSLParameters());
            }
        });

        // Ajout des routes
        serveur.createContext("/data", new HandlerRestaurantRMI(hostRmi, portRmi));
        serveur.createContext("/incidents", new HandlerRMI(hostRmi, portRmi));

        serveur.setExecutor(null);
        serveur.start();
        System.out.println("Serveur HTTPS démarré sur le port " + portHttps);
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
                System.out.println(query);

                if (path.endsWith("/restaurants")) {
                    jsonResponse = service.getTousLesRestaurantsJson();

                } else if (path.endsWith("/reservations")) {
                    jsonResponse = service.getToutesLesReservationsJson();

                } else if (path.endsWith("/tables")) {
                    String jsonParams = queryToJsonString(query);
                    jsonResponse = service.getTablesParRestaurantJson(jsonParams);

                } else if (path.endsWith("/placesDisponibles")) {
                    String jsonParams = queryToJsonString(query);
                    jsonResponse = service.getPlacesDisponiblesJson(jsonParams);

                } else if (path.endsWith("/reserver") && "POST".equalsIgnoreCase(method)) {
                    String body = new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    jsonResponse = service.reserverTableJson(body);

                } else if (path.endsWith("/annuler")) {
                    String jsonParams = queryToJsonString(query);
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

        public static String queryToJsonString(String query) {
            if (query == null || query.isEmpty()) return "{}";

            Map<String, String> params = new HashMap<>();
            for (String param : query.split("&")) {
                int idx = param.indexOf('=');
                if (idx > 0) {
                    String key = URLDecoder.decode(param.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(param.substring(idx + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }

                String key = entry.getKey();
                String value = entry.getValue();

                // Si la valeur est un nombre (int), on l'insère sans guillemets, sinon avec guillemets
                if (value.matches("-?\\d+")) {
                    sb.append("\"").append(key).append("\":").append(value);
                } else {
                    sb.append("\"").append(key).append("\":\"").append(value).append("\"");
                }
            }

            sb.append("}");
            return sb.toString();
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
                JSONObject reponseJson = service.getMessage();  // On suppose que ça retourne un JSON

                byte[] bytes = reponseJson.toString().getBytes(StandardCharsets.UTF_8);
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
