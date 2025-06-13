import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class Main {

    private static String USER_DB;
    private static String URL_DB;
    private static String PASSWORD_DB;
    private static int RMI_PORT;
    private static String RMI_HOST;

    public static void main(String[] args) throws IOException {
        loadConfig();
        Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
        ServiceRestaurant service = new ServiceRestaurantImpl(URL_DB, USER_DB, PASSWORD_DB);
        ServiceRestaurant rd = (ServiceRestaurant) UnicastRemoteObject.exportObject(service, 0);
        registry.rebind("ServiceRestaurant", rd);
        System.out.println("Connecter sur le registre.");

    }

    private static void loadConfig() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("DB_restaurant/config.properties")) {
            props.load(fis);
        }

        URL_DB = props.getProperty("URL_DB");
        PASSWORD_DB = props.getProperty("PASSWORD_DB");
        USER_DB = props.getProperty("USER_DB");
        RMI_PORT = Integer.parseInt(props.getProperty("RMI_PORT"));
        RMI_HOST = props.getProperty("RMI_HOST");

    }

}
