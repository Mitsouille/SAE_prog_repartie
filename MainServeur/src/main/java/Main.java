import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

public class Main {

    private static int REGISTRY_PORT;
    private static int PORT_HTTP;
    private static String RMI_HOST;

    public static void main(String[] args) throws IOException{
        loadConfig();
        try {
            // Crée le registre RMI (sans enregistrer de service ici)
            LocateRegistry.createRegistry(REGISTRY_PORT);
            System.out.println("Registry RMI démarré sur le port " + REGISTRY_PORT);

            // Lancer le serveur HTTP
            Serveur serveur = new Serveur(PORT_HTTP, RMI_HOST, REGISTRY_PORT);
            serveur.demarrer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("MainServeur/config.properties")) {
            props.load(fis);
        }

        REGISTRY_PORT = Integer.parseInt(props.getProperty("RMI_PORT"));
        PORT_HTTP = Integer.parseInt(props.getProperty("SERVEUR_PORT"));
        RMI_HOST = props.getProperty("RMI_HOST");
    }
}
