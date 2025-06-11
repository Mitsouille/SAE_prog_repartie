import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceRestaurantImpl extends UnicastRemoteObject implements ServiceRestaurant {
    private Connection conn;

    public ServiceRestaurantImpl() throws RemoteException {
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@charlemagne.iutnc.univ-lorraine.fr:1521:infodb",
                    "e0460u",
                    "mdpdemesreves"
            );
            System.out.println("📦 Connexion Oracle établie !");
        } catch (SQLException e) {
            throw new RemoteException("❌ Connexion à Oracle échouée", e);
        }
    }

    @Override
    public List<Restaurant> getTousLesRestaurants() throws RemoteException {
        List<Restaurant> liste = new ArrayList<>();
        String sql = "SELECT id_restaurant, nom, adresse, latitude, longitude, note_moyenne FROM restaurants";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(new Restaurant(
                        rs.getInt("id_restaurant"),
                        rs.getString("nom"),
                        rs.getString("adresse"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getDouble("note_moyenne")
                ));
            }
            System.out.println("📤 [SERVER] getTousLesRestaurants retourne " + liste.size() + " restaurants.");
        } catch (SQLException e) {
            throw new RemoteException("❌ Erreur SQL lors de la lecture des restaurants", e);
        }
        return liste;
    }

    @Override
    public List<Reservation> getToutesLesReservations() throws RemoteException {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT id_reservation, prenom_client, nom_client, nombre_convives, statut, debut_reservation, fin_reservation FROM reservations";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(new Reservation(
                        rs.getInt("id_reservation"),
                        rs.getString("prenom_client"),
                        rs.getString("nom_client"),
                        rs.getInt("nombre_convives"),
                        rs.getString("statut"),
                        rs.getTimestamp("debut_reservation").toLocalDateTime(),
                        rs.getTimestamp("fin_reservation").toLocalDateTime()
                ));
            }
            System.out.println("📤 [SERVER] getToutesLesReservations retourne " + liste.size() + " réservations.");
        } catch (SQLException e) {
            throw new RemoteException("❌ Erreur SQL lors de la lecture des réservations", e);
        }
        return liste;
    }

    @Override
    public boolean annulerReservation(int idReservation) throws RemoteException {
        String sql = "DELETE FROM reservations WHERE id_reservation = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RemoteException("❌ Erreur SQL lors de l’annulation", e);
        }
    }

    @Override
    public ReservationResponse reserverTable(ReservationRequest req) throws RemoteException {
        try {
            // 1️⃣ Vérifier les chevauchements AVANT insert
            String checkSql = """
            SELECT COUNT(*) 
              FROM reservations r
             WHERE r.id_table = ?
               AND r.statut <> 'annulee'
               AND ? < r.fin_reservation
               AND ? > r.debut_reservation
        """;
            try (PreparedStatement checkSt = conn.prepareStatement(checkSql)) {
                checkSt.setInt(1, req.idTable);
                // ⚠️ On passe d'abord req.debut, puis req.fin
                checkSt.setTimestamp(2, Timestamp.valueOf(req.debut));
                checkSt.setTimestamp(3, Timestamp.valueOf(req.fin));
                try (ResultSet rs = checkSt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return new ReservationResponse(false,
                                "⚠️ Créneau indisponible : table déjà réservée sur ce créneau");
                    }
                }
            }

            // 2️⃣ Si pas de conflit, on insère
            String insertSql = """
            INSERT INTO reservations 
              (id_table, prenom_client, nom_client, nombre_convives, telephone_client, 
               debut_reservation, fin_reservation, statut) 
            VALUES (?, ?, ?, ?, ?, ?, ?, 'en_attente')
        """;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, req.idTable);
                ps.setString(2, req.prenom);
                ps.setString(3, req.nom);
                ps.setInt(4, req.nbConvives);
                ps.setString(5, req.tel);
                ps.setTimestamp(6, Timestamp.valueOf(req.debut));
                ps.setTimestamp(7, Timestamp.valueOf(req.fin));
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return new ReservationResponse(true, "✅ Réservation réussie !");
                } else {
                    return new ReservationResponse(false, "⚠️ Aucune réservation insérée.");
                }
            }
        } catch (SQLException e) {
            // En cas d'ORA-20001 levé par trigger, on le traduit aussi en message d'indisponibilité
            if (e.getErrorCode() == 20001) {
                return new ReservationResponse(false,
                        "⚠️ Créneau indisponible : table déjà réservée sur ce créneau");
            }
            return new ReservationResponse(false, "❌ Erreur SQL : " + e.getMessage());
        }
    }

    @Override
    public List<ReservationResponse> getReservationsParTable(int idTable) throws RemoteException {
        // TODO : implémenter si nécessaire
        throw new UnsupportedOperationException("❌ Non implémenté : getReservationsParTable");
    }

    @Override
    public List<Table> getTablesParRestaurant(int idRestaurant) throws RemoteException {
        List<Table> tables = new ArrayList<>();
        String sql = """
        SELECT id_table, numero_table, capacite, exterieur
          FROM tables_restaurant
         WHERE id_restaurant = ?
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRestaurant);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(new Table(
                            rs.getInt("id_table"),
                            rs.getString("numero_table"),
                            rs.getInt("capacite"),
                            "Y".equals(rs.getString("exterieur"))
                    ));
                }
            }
            System.out.println("📤 [SERVER] getTablesParRestaurant(" + idRestaurant + ") → " + tables.size() + " tables");
        } catch (SQLException e) {
            throw new RemoteException("❌ Erreur SQL getTablesParRestaurant", e);
        }
        return tables;
    }


    @Override
    public int getPlacesDisponibles(int idRestaurant, LocalDateTime debut, LocalDateTime fin) throws RemoteException {
        try {
            // Capacité totale du resto
            String capSql = "SELECT NVL(SUM(capacite),0) FROM tables_restaurant WHERE id_restaurant = ?";
            int capaciteTotale;
            try (PreparedStatement ps = conn.prepareStatement(capSql)) {
                ps.setInt(1, idRestaurant);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    capaciteTotale = rs.getInt(1);
                }
            }

            // Nombre de convives déjà réservés sur le créneau
            String resSql = """
            SELECT NVL(SUM(r.nombre_convives),0)
              FROM reservations r
              JOIN tables_restaurant t ON r.id_table = t.id_table
             WHERE t.id_restaurant = ?
               AND r.statut <> 'annulee'
               AND ? < r.fin_reservation
               AND ? > r.debut_reservation
        """;
            int convivesReserves;
            try (PreparedStatement ps2 = conn.prepareStatement(resSql)) {
                ps2.setInt(1, idRestaurant);
                ps2.setTimestamp(2, Timestamp.valueOf(debut));
                ps2.setTimestamp(3, Timestamp.valueOf(fin));
                try (ResultSet rs2 = ps2.executeQuery()) {
                    rs2.next();
                    convivesReserves = rs2.getInt(1);
                }
            }

            int dispo = capaciteTotale - convivesReserves;
            System.out.println("📤 [SERVER] getPlacesDisponibles(" + idRestaurant + ", " + debut + "→" + fin + ") = " + dispo);
            return Math.max(dispo, 0);
        } catch (SQLException e) {
            throw new RemoteException("❌ Erreur SQL getPlacesDisponibles", e);
        }
    }

}
