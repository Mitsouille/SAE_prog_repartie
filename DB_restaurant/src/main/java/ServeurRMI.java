import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Naming;

public class ServeurRMI {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1100);
            ServiceRestaurant service = new ServiceRestaurantImpl();
            Naming.rebind("rmi://localhost:1100/ServiceRestaurant", service);
            System.out.println("✅ Serveur RMI prêt !");
        } catch (Exception e) {
            System.err.println("❌ Erreur serveur RMI :");
            e.printStackTrace();
        }
    }
}
