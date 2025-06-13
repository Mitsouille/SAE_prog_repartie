import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceRestaurantImpl extends UnicastRemoteObject implements ServiceRestaurant {
    private final Connection conn;

    public ServiceRestaurantImpl() throws RemoteException {
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@charlemagne.iutnc.univ-lorraine.fr:1521:infodb",
                "e0460u", "mdpdemesreves"
            );
        } catch (SQLException e) {
            throw new RemoteException("Connexion à Oracle échouée", e);
        }
    }

    @Override
    public String getTousLesRestaurants() throws RemoteException {
        List<Restaurant> liste = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
               "SELECT id_restaurant, nom, adresse, latitude, longitude, note_moyenne FROM restaurants")) {
            while (rs.next()) {
                liste.add(new Restaurant(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getDouble(5),
                    rs.getDouble(6)
                ));
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        StringBuilder sb = new StringBuilder();
        for (Restaurant r : liste) {
            sb.append("[")
              .append(r.getId())
              .append("] ")
              .append(r.getNom())
              .append(" | ")
              .append(r.getAdresse())
              .append(" | note:")
              .append(r.getNote())
              .append("\n");
        }
        return sb.toString();
    }

    @Override
    public String getTablesParRestaurant(int idRestaurant) throws RemoteException {
        List<Table> liste = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
               "SELECT id_table, numero_table, capacite, exterieur FROM tables_restaurant WHERE id_restaurant = ?")) {
            ps.setInt(1, idRestaurant);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new Table(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getInt(3),
                        "Y".equals(rs.getString(4))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        StringBuilder sb = new StringBuilder();
        for (Table t : liste) {
            sb.append("Table ")
              .append(t.getNumero())
              .append(" (cap:")
              .append(t.getCapacite())
              .append(", ext=")
              .append(t.isExterieur())
              .append(")\n");
        }
        return sb.toString();
    }

    @Override
    public String getPlacesDisponibles(int idRestaurant, LocalDateTime debut, LocalDateTime fin) throws RemoteException {
        int total = 0;
        try (PreparedStatement ps1 = conn.prepareStatement(
               "SELECT NVL(SUM(capacite),0) FROM tables_restaurant WHERE id_restaurant = ?")) {
            ps1.setInt(1, idRestaurant);
            try (ResultSet rs = ps1.executeQuery()) {
                if (rs.next()) total = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        int reserved = 0;
        try (PreparedStatement ps2 = conn.prepareStatement(
               "SELECT NVL(SUM(r.nombre_convives),0) " +
               "FROM reservations r JOIN tables_restaurant t ON r.id_table = t.id_table " +
               "WHERE t.id_restaurant = ? AND r.statut <> 'annulee' " +
               "AND ? < r.fin_reservation AND ? > r.debut_reservation")) {
            ps2.setInt(1, idRestaurant);
            ps2.setTimestamp(2, Timestamp.valueOf(debut));
            ps2.setTimestamp(3, Timestamp.valueOf(fin));
            try (ResultSet rs = ps2.executeQuery()) {
                if (rs.next()) reserved = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        int dispo = Math.max(total - reserved, 0);
        return "Places disponibles: " + dispo;
    }

    @Override
    public String getToutesLesReservations() throws RemoteException {
        List<Reservation> liste = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
               "SELECT id_reservation, prenom_client, nom_client, nombre_convives, statut, debut_reservation, fin_reservation FROM reservations")) {
            while (rs.next()) {
                liste.add(new Reservation(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4),
                    rs.getString(5),
                    rs.getTimestamp(6).toLocalDateTime(),
                    rs.getTimestamp(7).toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        StringBuilder sb = new StringBuilder();
        for (Reservation r : liste) {
            sb.append(r.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String reserverTable(ReservationRequest req) throws RemoteException {
        try (PreparedStatement ps = conn.prepareStatement(
               "SELECT COUNT(*) FROM reservations WHERE id_table = ? AND statut <> 'annulee' " +
               "AND ? < fin_reservation AND ? > debut_reservation")) {
            ps.setInt(1, req.idTable);
            ps.setTimestamp(2, Timestamp.valueOf(req.debut));
            ps.setTimestamp(3, Timestamp.valueOf(req.fin));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "FAIL: créneau indisponible";
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 20001) {
                return "FAIL: créneau indisponible";
            }
            throw new RemoteException(e.getMessage(), e);
        }
        try (PreparedStatement ps = conn.prepareStatement(
               "INSERT INTO reservations (id_table, prenom_client, nom_client, nombre_convives, " +
               "telephone_client, debut_reservation, fin_reservation, statut) VALUES (?, ?, ?, ?, ?, ?, ?, 'en_attente')")) {
            ps.setInt(1, req.idTable);
            ps.setString(2, req.prenom);
            ps.setString(3, req.nom);
            ps.setInt(4, req.nbConvives);
            ps.setString(5, req.tel);
            ps.setTimestamp(6, Timestamp.valueOf(req.debut));
            ps.setTimestamp(7, Timestamp.valueOf(req.fin));
            if (ps.executeUpdate() > 0) {
                return "SUCCESS: réservation réussie";
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        return "FAIL: échec de l'insertion";
    }

    @Override
    public String annulerReservation(int idReservation) throws RemoteException {
        try (PreparedStatement ps = conn.prepareStatement(
               "DELETE FROM reservations WHERE id_reservation = ?")) {
            ps.setInt(1, idReservation);
            if (ps.executeUpdate() > 0) {
                return "SUCCESS: réservation " + idReservation + " annulée";
            }
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
        return "FAIL: impossible d'annuler " + idReservation;
    }
}
