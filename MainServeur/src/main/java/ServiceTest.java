import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.json.JSONObject;

public class ServiceTest extends UnicastRemoteObject implements Service {

    protected ServiceTest() throws RemoteException {
        super();
    }

@Override
public String getMessage() throws RemoteException {
    return "Hello depuis Service RMI ";
}

    public static void main(String[] args) {
        try {
            // Connexion au registre du serveur principal
            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
            ServiceTest service = new ServiceTest();
            reg.rebind("service", service);
            System.out.println("Service enregistré dans le registre.");
            Service service1 = (Service) reg.lookup("ServiceIncidents");
            System.out.println(service1.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
