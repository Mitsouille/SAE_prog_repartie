import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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
        serveur.createContext("/data", new HandlerRMI(hostRmi, portRmi, "service"));
        serveur.createContext("/incidents", new HandlerRMI(hostRmi, portRmi, "ServiceIncidents"));

        serveur.setExecutor(null);
        serveur.start();
        System.out.println("Serveur HTTP démarré sur le port " + portHttp);
    }

    static class HandlerRMI implements HttpHandler {
        private final String host;
        private final int port;
        private final String nomService;

        public HandlerRMI(String host, int port, String nomService) {
            this.host = host;
            this.port = port;
            this.nomService = nomService;
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
                Service service = (Service) reg.lookup(nomService);

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
