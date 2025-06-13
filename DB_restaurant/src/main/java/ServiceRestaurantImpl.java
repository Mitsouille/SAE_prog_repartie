
import java.rmi.RemoteException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ServiceRestaurantImpl implements ServiceRestaurant {
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
    public String getTousLesRestaurantsJson() throws RemoteException {
        StringBuilder sb = new StringBuilder("{\"restaurants\":[");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
               "SELECT id_restaurant, nom, adresse, latitude, longitude, note_moyenne FROM restaurants")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"id\":").append(rs.getInt(1)).append(",")
                  .append("\"nom\":").append(jsonEsc(rs.getString(2))).append(",")
                  .append("\"adresse\":").append(jsonEsc(rs.getString(3))).append(",")
                  .append("\"latitude\":").append(rs.getDouble(4)).append(",")
                  .append("\"longitude\":").append(rs.getDouble(5)).append(",")
                  .append("\"note\":").append(rs.getDouble(6))
                  .append("}");
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public String getTablesParRestaurantJson(String requestJson) throws RemoteException {
        Map<String,String> m = parseJsonToMap(requestJson);
        int idRestaurant = Integer.parseInt(m.get("idRestaurant"));
        StringBuilder sb = new StringBuilder("{\"tables\":[");
        try (PreparedStatement ps = conn.prepareStatement(
               "SELECT id_table, numero_table, capacite, exterieur FROM tables_restaurant WHERE id_restaurant=?")) {
            ps.setInt(1, idRestaurant);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{")
                      .append("\"id\":").append(rs.getInt(1)).append(",")
                      .append("\"numero\":").append(jsonEsc(rs.getString(2))).append(",")
                      .append("\"capacite\":").append(rs.getInt(3)).append(",")
                      .append("\"exterieur\":").append(rs.getString(4).equals("Y"))
                      .append("}");
                }
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public String getPlacesDisponiblesJson(String requestJson) throws RemoteException {
        Map<String,String> m = parseJsonToMap(requestJson);
        int idRestaurant = Integer.parseInt(m.get("idRestaurant"));
        LocalDateTime debut = LocalDateTime.parse(m.get("debut"));
        LocalDateTime fin   = LocalDateTime.parse(m.get("fin"));

        int total = 0, reserved = 0;
        try (PreparedStatement ps1 = conn.prepareStatement(
               "SELECT NVL(SUM(capacite),0) FROM tables_restaurant WHERE id_restaurant=?")) {
            ps1.setInt(1, idRestaurant);
            try (ResultSet rs = ps1.executeQuery()) {
                if (rs.next()) total = rs.getInt(1);
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
        try (PreparedStatement ps2 = conn.prepareStatement(
               "SELECT NVL(SUM(r.nombre_convives),0) " +
               "FROM reservations r JOIN tables_restaurant t ON r.id_table=t.id_table " +
               "WHERE t.id_restaurant=? AND r.statut<>'annulee' AND ?<r.fin_reservation AND ?>r.debut_reservation")) {
            ps2.setInt(1, idRestaurant);
            ps2.setTimestamp(2, Timestamp.valueOf(debut));
            ps2.setTimestamp(3, Timestamp.valueOf(fin));
            try (ResultSet rs = ps2.executeQuery()) {
                if (rs.next()) reserved = rs.getInt(1);
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
        int dispo = Math.max(total - reserved, 0);
        return "{\"placesDisponibles\":" + dispo + "}";
    }

    @Override
    public String getToutesLesReservationsJson() throws RemoteException {
        StringBuilder sb = new StringBuilder("{\"reservations\":[");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
               "SELECT id_reservation, prenom_client, nom_client, nombre_convives, statut, debut_reservation, fin_reservation FROM reservations")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"id\":").append(rs.getInt(1)).append(",")
                  .append("\"prenom\":").append(jsonEsc(rs.getString(2))).append(",")
                  .append("\"nom\":").append(jsonEsc(rs.getString(3))).append(",")
                  .append("\"convives\":").append(rs.getInt(4)).append(",")
                  .append("\"statut\":").append(jsonEsc(rs.getString(5))).append(",")
                  .append("\"debut\":").append(jsonEsc(rs.getTimestamp(6).toLocalDateTime().toString())).append(",")
                  .append("\"fin\":").append(jsonEsc(rs.getTimestamp(7).toLocalDateTime().toString()))
                  .append("}");
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public String reserverTableJson(String requestJson) throws RemoteException {
        Map<String,String> m = parseJsonToMap(requestJson);
        ReservationRequest req = new ReservationRequest(
            Integer.parseInt(m.get("idTable")),
            m.get("prenom"), m.get("nom"),
            Integer.parseInt(m.get("nbConvives")),
            m.get("tel"),
            LocalDateTime.parse(m.get("debut")),
            LocalDateTime.parse(m.get("fin"))
        );

        // Vérif chevauchement
        try (PreparedStatement ps = conn.prepareStatement(
               "SELECT COUNT(*) FROM reservations WHERE id_table=? AND statut<>'annulee' AND ?<fin_reservation AND ?>debut_reservation")) {
            ps.setInt(1, req.idTable);
            ps.setTimestamp(2, Timestamp.valueOf(req.debut));
            ps.setTimestamp(3, Timestamp.valueOf(req.fin));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "{\"success\":false,\"message\":\"créneau indisponible\"}";
                }
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }

        // Insertion
        try (PreparedStatement ps = conn.prepareStatement(
               "INSERT INTO reservations(id_table,prenom_client,nom_client,nombre_convives,telephone_client,debut_reservation,fin_reservation,statut) VALUES(?,?,?,?,?,?,?,'en_attente')")) {
            ps.setInt(1, req.idTable);
            ps.setString(2, req.prenom);
            ps.setString(3, req.nom);
            ps.setInt(4, req.nbConvives);
            ps.setString(5, req.tel);
            ps.setTimestamp(6, Timestamp.valueOf(req.debut));
            ps.setTimestamp(7, Timestamp.valueOf(req.fin));
            if (ps.executeUpdate() > 0) {
                return "{\"success\":true,\"message\":\"réservation réussie\"}";
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
        return "{\"success\":false,\"message\":\"échec insertion\"}";
    }
    
    private String jsonEsc(String s) {
        return "\"" + s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            + "\"";
    }

    @Override
    public String annulerReservationJson(String requestJson) throws RemoteException {
        // 1) Récupération des paramètres
        Map<String,String> m = parseJsonToMap(requestJson);
        String tel   = m.get("telephone");
        LocalDateTime debut = LocalDateTime.parse(m.get("debut"));

        // 2) Nouvelle requête : comparaison sur un Timestamp
        String sql = "DELETE FROM reservations "
                + "WHERE telephone_client = ? "
                + "  AND debut_reservation = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tel);
            ps.setTimestamp(2, Timestamp.valueOf(debut));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                return "{\"success\":true,\"deleted\":" + rows + "}";
            } else {
                return "{\"success\":false,\"message\":\"Aucune réservation trouvée pour ce téléphone/créneau\"}";
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
    }

    


    private Map<String,String> parseJsonToMap(String json) {
        Map<String,String> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0,json.length()-1);
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":",2);
            if (kv.length==2) {
                String k = kv[0].trim().replaceAll("^\"|\"$","");
                String v = kv[1].trim().replaceAll("^\"|\"$","");
                map.put(k,v);
            }
        }
        return map;
    }

    
}
