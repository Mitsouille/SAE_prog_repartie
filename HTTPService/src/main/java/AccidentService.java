import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.RemoteException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;

public class AccidentService implements Service {
    private int PROXY_PORT;
    private String PROXY_URL;
    private String PROXY_HOST_NAME;
    private String URL_API_INCIDENT;

    private HttpClient httpClient;

    public AccidentService() throws IOException {
        loadConfig();

        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20));

        InetSocketAddress proxyAddr = new InetSocketAddress(PROXY_HOST_NAME, PROXY_PORT);
        if (!proxyAddr.isUnresolved()) {
            System.out.println("[PROXY] Utilisation du proxy : " + PROXY_HOST_NAME + ":" + PROXY_PORT);
            builder.proxy(ProxySelector.of(proxyAddr));
        } else {
            System.out.println("[PROXY] Proxy non résolu, utilisation directe sans proxy.");
        }

        this.httpClient = builder.build();
    }

    /**
     * avec proxy forcé
     * 
     * public AccidentService() throws IOException {
     * loadConfig();
     * 
     * System.out.println("[PROXY] Utilisation (forcée) du proxy : " +
     * PROXY_HOST_NAME + ":" + PROXY_PORT);
     * InetSocketAddress proxyAddr = new InetSocketAddress(PROXY_HOST_NAME,
     * PROXY_PORT);
     * 
     * HttpClient.Builder builder = HttpClient.newBuilder()
     * .version(HttpClient.Version.HTTP_2)
     * .followRedirects(HttpClient.Redirect.NORMAL)
     * .connectTimeout(Duration.ofSeconds(20))
     * .proxy(ProxySelector.of(proxyAddr));
     * 
     * this.httpClient = builder.build();
     * }
     */

    @Override
    public String getMessage() throws RemoteException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_API_INCIDENT))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();

            if (response.statusCode() != 200) {
                throw new RemoteException("Erreur HTTP: " + response.statusCode());
            }
            System.out.println("[HTTP] Status code : " + response.statusCode());
            System.out.println("[HTTP] Body : " + response.body());

            JSONObject jsonRes = new JSONObject(response.body());

            return jsonRes.toString();

        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Erreur lors de l'appel HTTP: " + e.getMessage(), e);
        }
    }

    private void loadConfig() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("HTTPService/config.properties")) {
            props.load(fis);
        }

        PROXY_HOST_NAME = props.getProperty("PROXY_HOST_NAME");
        PROXY_URL = props.getProperty("PROXY_URL");
        PROXY_PORT = Integer.parseInt(props.getProperty("PROXY_PORT"));
        URL_API_INCIDENT = props.getProperty("INCIDENT_URL_API");

        System.out.println("[CONFIG] PROXY_HOST_NAME = " + PROXY_HOST_NAME);
        System.out.println("[CONFIG] PROXY_PORT = " + PROXY_PORT);
        System.out.println("[CONFIG] INCIDENT_URL_API = " + URL_API_INCIDENT);

        if (PROXY_HOST_NAME == null || URL_API_INCIDENT == null) {
            throw new IOException("Fichier de configuration incomplet ou introuvable.");
        }
    }

}