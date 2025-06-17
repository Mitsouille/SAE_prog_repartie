import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;


public class Main {

    private static int RMI_PORT;
    private static String RMI_HOST;
    private static int PROXY_PORT;
    private static String PROXY_URL;
    private static String PROXY_HOST_NAME;
    private static String URL_API_INCIDENT;

    public static void main(String[] args) throws IOException, NotBoundException {
        loadConfig();
        Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
        Service service = new AccidentService(PROXY_PORT, PROXY_URL, PROXY_HOST_NAME, URL_API_INCIDENT);
        Service servIncident = (Service) UnicastRemoteObject.exportObject(service, 0);
        IServeur mainServ = (IServeur) registry.lookup("MainServeur");
        mainServ.enregistrerServIncident(servIncident);
        System.out.println("Connecter sur le registre.");

    }


    private static void loadConfig() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("HTTPService/config.properties")) {
            props.load(fis);
        }
        PROXY_HOST_NAME = props.getProperty("PROXY_HOST_NAME");
        PROXY_URL = props.getProperty("PROXY_URL");
        PROXY_PORT = Integer.parseInt(props.getProperty("PROXY_PORT"));
        URL_API_INCIDENT = props.getProperty("INCIDENT_URL_API");
        RMI_PORT = Integer.parseInt(props.getProperty("RMI_PORT"));
        RMI_HOST = props.getProperty("RMI_HOST");

    }
}