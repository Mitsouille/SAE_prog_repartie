import java.rmi.registry.LocateRegistry;

public class Main {
    public static void main(String[] args) {
        try {
            // Crée le registre RMI (sans enregistrer de service ici)
            LocateRegistry.createRegistry(1099);
            System.out.println("Registry RMI démarré sur le port 1099");

            // Lancer le serveur HTTP
            Serveur serveur = new Serveur(8080, "localhost", 1099);
            serveur.demarrer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
