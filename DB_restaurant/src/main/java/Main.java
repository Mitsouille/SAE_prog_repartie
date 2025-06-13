import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        ServiceRestaurant service = new ServiceRestaurantImpl();
        ServiceRestaurant rd = (ServiceRestaurant) UnicastRemoteObject.exportObject(service, 0);
        registry.rebind("ServiceRestaurant", rd);
        System.out.println("Connecter sur le registre.");

    }
}
