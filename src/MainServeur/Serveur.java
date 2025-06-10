package MainServeur;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
                Service service = (Service) reg.lookup("service"); // on appelle "service1"
                String reponse = service.getMessage();

                echange.getResponseHeaders().set("Content-Type", "text/plain");
                echange.sendResponseHeaders(200, reponse.length());
                OutputStream os = echange.getResponseBody();
                os.write(reponse.getBytes());
                os.close();

            } catch (Exception e) {
                String erreur = "Erreur RMI : " + e.getMessage();
                echange.sendResponseHeaders(500, erreur.length());
                OutputStream os = echange.getResponseBody();
                os.write(erreur.getBytes());
                os.close();
            }
        }
    }
}
