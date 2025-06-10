package MainServeur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Test {
    public static void main(String[] args) {
        try {
            // Se connecter au registre RMI
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // Chercher le service "service1"
            Service service = (Service) registry.lookup("service");

            // Appeler la méthode distante
            String message = service.getMessage();

            System.out.println("Message reçu du service RMI : " + message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
