package MainServeur;

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
        serveur.createContext("/data", new MonHandler(hostRmi, portRmi));
        serveur.setExecutor(null);
        serveur.start();
        System.out.println("Serveur HTTP démarré sur le port " + portHttp);
    }

    static class MonHandler implements HttpHandler {
        private final String host;
        private final int port;

        public MonHandler(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void handle(HttpExchange echange) throws IOException {
            try {
                Registry reg = LocateRegistry.getRegistry(host, port);
                Service service = (Service) reg.lookup("service");
                String reponse = service.getMessage();

                byte[] bytes = reponse.getBytes(StandardCharsets.UTF_8);
                echange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                echange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = echange.getResponseBody()) {
                    os.write(bytes);
                }

            } catch (Exception e) {
                String erreur = "Erreur RMI : " + e.getMessage();
                byte[] erreurBytes = erreur.getBytes(StandardCharsets.UTF_8);
                echange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                echange.sendResponseHeaders(500, erreurBytes.length);

                try (OutputStream os = echange.getResponseBody()) {
                    os.write(erreurBytes);
                }
            }
        }

    }
}
