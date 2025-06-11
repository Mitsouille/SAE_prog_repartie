import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Main {

    public static void main(String[] args) throws IOException {

        Registry registry = LocateRegistry.getRegistry();
        Service service = new AccidentService();
        Service rd = (Service) UnicastRemoteObject.exportObject(service, 0);
        registry.rebind("ServiceIncidents", rd);
        System.out.println("Connecter sur le registre.");

    }

}