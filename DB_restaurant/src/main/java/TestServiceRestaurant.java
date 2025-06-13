import java.time.LocalDateTime;

public class TestServiceRestaurant {
    public static void main(String[] args) throws Exception {
        ServiceRestaurantImpl service = new ServiceRestaurantImpl();

        // 1) Lister les restaurants
        String restosJson = service.getTousLesRestaurantsJson();
        System.out.println("1) Restaurants:\n" + restosJson + "\n");

        // 2) Lister les tables du restaurant n°1
        String reqTables = "{\"idRestaurant\":1}";
        String tablesJson = service.getTablesParRestaurantJson(reqTables);
        System.out.println("2) Tables (idRestaurant=1):\n" + tablesJson + "\n");

        // 3) Capacité dispo pour 20/06 19h→21h au resto 1
        String reqPlaces = String.format(
            "{\"idRestaurant\":1,\"debut\":\"%s\",\"fin\":\"%s\"}",
            LocalDateTime.of(2025,6,20,19,0),
            LocalDateTime.of(2025,6,20,21,0)
        );
        String placesJson = service.getPlacesDisponiblesJson(reqPlaces);
        System.out.println("3) Places dispo:\n" + placesJson + "\n");

        // 4) Lister les réservations existantes
        String resaJson = service.getToutesLesReservationsJson();
        System.out.println("4) Réservations:\n" + resaJson + "\n");

        // 5) Créer une réservation test
        String nouvelleResa = String.format(
          "{\"idTable\":1,\"prenom\":\"Jean\",\"nom\":\"Dupont\"," +
          "\"nbConvives\":2,\"tel\":\"0601020304\"," +
          "\"debut\":\"%s\",\"fin\":\"%s\"}",
          LocalDateTime.of(2025,6,25,18,0),
          LocalDateTime.of(2025,6,25,20,0)
        );
        String res1 = service.reserverTableJson(nouvelleResa);
        System.out.println("5) Réservation test:\n" + res1 + "\n");

        // 6) Tenter un doublon sur le même créneau
        String res2 = service.reserverTableJson(nouvelleResa);
        System.out.println("6) Réservation doublon:\n" + res2 + "\n");

        // 7) Vérifier la liste après insertion
        String resaAprès = service.getToutesLesReservationsJson();
        System.out.println("7) Réservations après insert:\n" + resaAprès + "\n");

        // 8) Annuler la réservation en fournissant prénom, nom, téléphone et début
        String reqAnnule = String.format(
          "{\"prenom\":\"Jean\",\"nom\":\"Dupont\",\"telephone\":\"0601020304\"," +
          "\"debut\":\"%s\"}",
          LocalDateTime.of(2025,6,25,18,0)
        );
        String annule = service.annulerReservationJson(reqAnnule);
        System.out.println("8) Annulation:\n" + annule + "\n");

        // 9) Liste finale
        String resaFinal = service.getToutesLesReservationsJson();
        System.out.println("9) Réservations finales:\n" + resaFinal);
    }
}
