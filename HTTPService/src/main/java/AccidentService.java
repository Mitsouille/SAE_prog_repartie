import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.RemoteException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class AccidentService implements Service {
    private int PROXY_PORT;
    private String PROXY_URL;
    private String PROXY_HOST_NAME;
    private String URL_API_INCIDENT;

    private HttpClient httpClient;

    public AccidentService() throws IOException {
        loadConfig();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

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

            return response.body();

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
    }
}