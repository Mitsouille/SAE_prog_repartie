import java.rmi.Naming;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ClientTest {
    public static void main(String[] args) {
        try {
            // Lookup du service RMI
            ServiceRestaurant service = (ServiceRestaurant)
                Naming.lookup("rmi://localhost:1100/ServiceRestaurant");
            System.out.println("✅ Connecté au service RMI\n");

            // 1️⃣ Tous les restaurants
            String jsonRestos = service.getTousLesRestaurantsJson();
            System.out.println("→ Restaurants :\n" + jsonRestos + "\n");

            // 2️⃣ Tables du restaurant #1
            String reqTables = "{\"idRestaurant\":1}";
            String jsonTables = service.getTablesParRestaurantJson(reqTables);
            System.out.println("→ Tables pour idRestaurant=1 :\n" + jsonTables + "\n");

            // 3️⃣ Places disponibles pour le 20/06 19h→21h au resto #1
            String reqPlaces = String.format(
              "{\"idRestaurant\":1,\"debut\":\"%s\",\"fin\":\"%s\"}",
              LocalDateTime.of(2025,6,20,19,0),
              LocalDateTime.of(2025,6,20,21,0)
            );
            String jsonPlaces = service.getPlacesDisponiblesJson(reqPlaces);
            System.out.println("→ Places disponibles :\n" + jsonPlaces + "\n");

            // 4️⃣ Toutes les réservations
            String jsonResa = service.getToutesLesReservationsJson();
            System.out.println("→ Toutes les réservations :\n" + jsonResa + "\n");

            // 5️⃣ Faire une nouvelle réservation
            String nouvelleResa = String.format(
              "{\"idTable\":1,\"prenom\":\"Jean\",\"nom\":\"Dupont\"," +
              "\"nbConvives\":4,\"tel\":\"0601020304\"," +
              "\"debut\":\"%s\",\"fin\":\"%s\"}",
              LocalDateTime.of(2025,6,13,18,0),
              LocalDateTime.of(2025,6,13,20,0)
            );
            String jsonRes1 = service.reserverTableJson(nouvelleResa);
            System.out.println("→ Réservation (nouvelle) :\n" + jsonRes1 + "\n");

            // 6️⃣ Réessayer la même réservation pour tester le conflit
            String jsonRes2 = service.reserverTableJson(nouvelleResa);
            System.out.println("→ Réservation (doublon) :\n" + jsonRes2 + "\n");

<<<<<<< HEAD
            // 7️⃣ Récupérer à nouveau toutes les réservations
            String jsonResApres = service.getToutesLesReservationsJson();
            System.out.println("→ Réservations après insert :\n" + jsonResApres + "\n");

            // 8️⃣ Annuler la dernière réservation (supposons id=… à récupérer ou ici  – on prend 6)
            String reqAnnule = "{\"idReservation\":6}";
            String jsonAnnule = service.annulerReservationJson(reqAnnule);
            System.out.println("→ Annulation id=6 :\n" + jsonAnnule + "\n");
=======
            // 7️⃣ Re-liste des réservations après insertion
            String jsonResApres = service.getToutesLesReservationsJson();
            System.out.println("→ Réservations après insert :\n" + jsonResApres + "\n");

            // 8️⃣ Annulation de la réservation par téléphone + début
            String reqAnnule = "{\"telephone\":\"0601020304\",\"debut\":\"2025-06-13T18:00:00\"}";
            String jsonAnnule = service.annulerReservationJson(reqAnnule);
            System.out.println("→ Annulation :\n" + jsonAnnule + "\n");
>>>>>>> 26f519a (merge)

            // 9️⃣ Vérifier la liste finale
            String jsonFinal = service.getToutesLesReservationsJson();
            System.out.println("→ Réservations finales :\n" + jsonFinal);

        } catch (Exception e) {
            System.err.println("❌ Erreur côté client :");
            e.printStackTrace();
        }
    }
}
