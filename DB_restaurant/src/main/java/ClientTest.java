import java.rmi.Naming;
import java.time.LocalDateTime;
import java.util.List;

public class ClientTest {
    public static void main(String[] args) {
        try {
            // Connexion au service RMI
            ServiceRestaurant service = (ServiceRestaurant) Naming.lookup("rmi://localhost:1100/ServiceRestaurant");
            System.out.println("✅ Connexion RMI établie !");

            // 1️⃣ Affichage des restaurants
            System.out.println("\n📋 Liste des restaurants :");
            List<Restaurant> restaurants = service.getTousLesRestaurants();
            restaurants.forEach(r -> System.out.println("🍽 " + r.getNom() + " | 📍 " + r.getAdresse()));

            // 2️⃣ Récupération des tables pour le restaurant 1
            System.out.println("\n🪑 Tables pour le restaurant 1 :");
            List<Table> tables = service.getTablesParRestaurant(1);
            tables.forEach(t -> System.out.println(
                    "– Table n°" + t.getNumero() +
                            " (capacité " + t.getCapacite() +
                            ", extérieur=" + t.isExterieur() + ")"
            ));

            // 3️⃣ Nombre de places disponibles pour le créneau du 20/06 19h→21h
            LocalDateTime début = LocalDateTime.of(2025, 6, 20, 19, 0);
            LocalDateTime fin   = LocalDateTime.of(2025, 6, 20, 21, 0);
            int places = service.getPlacesDisponibles(1, début, fin);
            System.out.println("\n📊 Places dispo resto 1 du 20/06 19h→21h : " + places);

            // 4️⃣ Test réservation libre et conflit sur le 13/06
            ReservationRequest reqLibre = new ReservationRequest(
                    1, "Sophie", "Martin", 2, "0600001111",
                    LocalDateTime.of(2025, 6, 13, 19, 0),
                    LocalDateTime.of(2025, 6, 13, 20, 0)
            );
            System.out.println("\n📞 Réservation libre → " + service.reserverTable(reqLibre).getMessage());
            System.out.println("📞 Réservation en double → " + service.reserverTable(reqLibre).getMessage());

            // 5️⃣ Liste des réservations avant annulation
            System.out.println("\n📅 Réservations AVANT annulation :");
            List<Reservation> reservations = service.getToutesLesReservations();
            reservations.forEach(r -> System.out.println("📌 " + r));

            // 6️⃣ Annulation de la réservation ID 5
            int idToCancel = 5;
            System.out.println("\n❌ Annulation de la réservation n°" + idToCancel + "...");
            boolean cancelled = service.annulerReservation(idToCancel);
            System.out.println("📬 Résultat annulation : " + (cancelled ? "✅ Réservation supprimée" : "⚠️ Aucune réservation supprimée"));

            // 7️⃣ Affichage des réservations après annulation
            System.out.println("\n📅 Réservations APRÈS annulation :");
            service.getToutesLesReservations()
                    .forEach(r -> System.out.println("📌 " + r));

        } catch (Exception e) {
            System.err.println("❌ Une erreur est survenue côté client :");
            e.printStackTrace();
        }
    }
}
