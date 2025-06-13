import java.time.LocalDateTime;

public class TestServiceRestaurant {
    public static void main(String[] args) throws Exception {
        // Instancie directement l'implémentation (connexion Oracle nécessaire !)
        ServiceRestaurantImpl service = new ServiceRestaurantImpl();

        // 1️⃣ Test récupération des restaurants
        String jsonRestos = service.getTousLesRestaurantsJson();
        System.out.println("→ JSON Restaurants :\n" + jsonRestos + "\n");

        // 2️⃣ Test récupération des tables pour le resto 1
        String reqTables = "{\"idRestaurant\":1}";
        String jsonTables = service.getTablesParRestaurantJson(reqTables);
        System.out.println("→ JSON Tables (idRestaurant=1) :\n" + jsonTables + "\n");

        // 3️⃣ Test places disponibles le 20/06 19h→21h
        String reqPlaces = String.format(
            "{\"idRestaurant\":1,\"debut\":\"%s\",\"fin\":\"%s\"}",
            LocalDateTime.of(2025,6,20,19,0),
            LocalDateTime.of(2025,6,20,21,0)
        );
        String jsonPlaces = service.getPlacesDisponiblesJson(reqPlaces);
        System.out.println("→ JSON Places Disponibles :\n" + jsonPlaces + "\n");

        // 4️⃣ Test récupération de toutes les réservations
        String jsonResa = service.getToutesLesReservationsJson();
        System.out.println("→ JSON Réservations :\n" + jsonResa + "\n");

        // 5️⃣ Test annulation d'une réservation (id=6 par exemple)
        String reqAnnule = "{\"idReservation\":6}";
        String jsonAnnule = service.annulerReservationJson(reqAnnule);
        System.out.println("→ JSON Annulation(id=6) :\n" + jsonAnnule + "\n");
    }
}
