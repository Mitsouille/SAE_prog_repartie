package HTTPService;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {

    private static int PROXY_PORT;
    private static String PROXY_URL;
    private static String PROXY_HOST_NAME;

    private static String URL_API_INCIDENT;

    public static void main(String[] args) throws IOException {

        Registry registry = LocateRegistry.getRegistry();
        AccidentService service = new AccidentService();
        IHttpService rd = (IHttpService) UnicastRemoteObject.exportObject(service);
        registry.rebind("ServiceIncidents", rd);

    }



}