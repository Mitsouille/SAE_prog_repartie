import java.rmi.Naming;
import java.time.LocalDateTime;

public class ClientTest {
    public static void main(String[] args) {
        try {
            ServiceRestaurant service = (ServiceRestaurant) Naming.lookup("rmi://localhost:1100/ServiceRestaurant");
            System.out.println("✅ Connexion RMI établie !");

            String restos = service.getTousLesRestaurants();
            System.out.println("\n📋 Liste des restaurants :\n" + restos);

            String tables = service.getTablesParRestaurant(1);
            System.out.println("🪑 Tables pour le restaurant 1 :\n" + tables);

            LocalDateTime debut = LocalDateTime.of(2025, 6, 20, 19, 0);
            LocalDateTime fin   = LocalDateTime.of(2025, 6, 20, 21, 0);
            String dispo = service.getPlacesDisponibles(1, debut, fin);
            System.out.println("📊 " + dispo);

            ReservationRequest req = new ReservationRequest(
                1, "Sophie", "Martin", 2, "0600001111",
                LocalDateTime.of(2025, 6, 13, 19, 0),
                LocalDateTime.of(2025, 6, 13, 20, 0)
            );
            //String r1 = service.reserverTable(req);
            //System.out.println("📞 Réservation libre → " + r1);
            //String r2 = service.reserverTable(req);
            //System.out.println("📞 Réservation en double → " + r2);

            String allResaAvant = service.getToutesLesReservations();
            System.out.println("\n📅 Réservations AVANT annulation :\n" + allResaAvant);

            int idToCancel = 6;
            String cancel = service.annulerReservation(idToCancel);
            System.out.println("❌ Annulation réservation n°" + idToCancel + " → " + cancel);

            String allResaApres = service.getToutesLesReservations();
            System.out.println("\n📅 Réservations APRÈS annulation :\n" + allResaApres);

        } catch (Exception e) {
            System.err.println("❌ Une erreur est survenue côté client :");
            e.printStackTrace();
        }
    }
}
