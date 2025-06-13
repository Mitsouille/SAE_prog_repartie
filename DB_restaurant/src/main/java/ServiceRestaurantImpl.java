
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ServiceRestaurantImpl implements ServiceRestaurant {
    private final Connection conn;
    
    public ServiceRestaurantImpl(String URL_DB, String USER_DB, String PASSWORD_DB) throws RemoteException, IOException {
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            conn = DriverManager.getConnection(
                URL_DB,
                USER_DB, PASSWORD_DB
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
        int idRestaurant = Integer.parseInt(m.get("idRestaurant"));
        int nbConvives   = Integer.parseInt(m.get("nbConvives"));
        String prenom    = m.get("prenom");
        String nom       = m.get("nom");
        String tel       = m.get("tel");
        LocalDateTime debut = LocalDateTime.parse(m.get("debut"));
        LocalDateTime fin   = LocalDateTime.parse(m.get("fin"));

        // 1️⃣ Vérifier la capacité dispo dans le resto
        String capSql = 
        "SELECT NVL(SUM(capacite),0) FROM tables_restaurant WHERE id_restaurant = ?";
        int capaciteTotale;
        try (PreparedStatement ps = conn.prepareStatement(capSql)) {
            ps.setInt(1, idRestaurant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                capaciteTotale = rs.getInt(1);
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }

        String resSql =
        "SELECT NVL(SUM(r.nombre_convives),0) " +
        "FROM reservations r " +
        "JOIN tables_restaurant t ON r.id_table = t.id_table " +
        "WHERE t.id_restaurant = ? " +
        "  AND r.statut <> 'annulee' " +
        "  AND ? < r.fin_reservation " +
        "  AND ? > r.debut_reservation";
        int convivesReserves;
        try (PreparedStatement ps = conn.prepareStatement(resSql)) {
            ps.setInt(1, idRestaurant);
            ps.setTimestamp(2, Timestamp.valueOf(debut));
            ps.setTimestamp(3, Timestamp.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                convivesReserves = rs.getInt(1);
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }

        if (convivesReserves + nbConvives > capaciteTotale) {
            return "{\"success\":false,\"message\":\"Capacité restaurant insuffisante pour ce créneau\"}";
        }

        // 2️⃣ Si ok, on choisit une table libre (plus simple : la première qui peut accueillir)
        String findTable = 
        "SELECT t.id_table FROM tables_restaurant t " +
        "WHERE t.id_restaurant = ? AND t.capacite >= ? " +
        "  AND NOT EXISTS ( " +
        "    SELECT 1 FROM reservations r " +
        "    WHERE r.id_table = t.id_table AND r.statut <> 'annulee' " +
        "      AND ? < r.fin_reservation AND ? > r.debut_reservation ) " +
        "ORDER BY t.capacite ASC FETCH FIRST 1 ROWS ONLY";
        Integer idTableChoisie = null;
        try (PreparedStatement ps = conn.prepareStatement(findTable)) {
            ps.setInt(1, idRestaurant);
            ps.setInt(2, nbConvives);
            ps.setTimestamp(3, Timestamp.valueOf(debut));
            ps.setTimestamp(4, Timestamp.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idTableChoisie = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }

        if (idTableChoisie == null) {
            return "{\"success\":false,\"message\":\"Aucune table disponible pour cette taille de groupe\"}";
        }

        // 3️⃣ Insérer la réservation sur la table choisie
        String insertSql =
        "INSERT INTO reservations " +
        "(id_table, prenom_client, nom_client, nombre_convives, telephone_client, " +
        " debut_reservation, fin_reservation, statut) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, 'en_attente')";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, idTableChoisie);
            ps.setString(2, prenom);
            ps.setString(3, nom);
            ps.setInt(4, nbConvives);
            ps.setString(5, tel);
            ps.setTimestamp(6, Timestamp.valueOf(debut));
            ps.setTimestamp(7, Timestamp.valueOf(fin));
            ps.executeUpdate();
            return "{\"success\":true,\"message\":\"Réservation enregistrée\",\"idTable\":" + idTableChoisie + "}";
        } catch (SQLException e) {
            return "{\"error\":" + jsonEsc(e.getMessage()) + "}";
        }
    }


    
    private String jsonEsc(String s) {
        return "\"" + s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            + "\"";
    }

    @Override
    public String annulerReservationJson(String requestJson) throws RemoteException {
        // Parse du JSON { "prenom": "...", "nom": "...", "telephone": "...", "debut": "YYYY-MM-DDTHH:MM:SS" }
        Map<String,String> m = parseJsonToMap(requestJson);
        String prenom    = m.get("prenom");
        String nom       = m.get("nom");
        String telephone = m.get("telephone");
        LocalDateTime debut;
        try {
            debut = LocalDateTime.parse(m.get("debut"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ex) {
            return "{\"success\":false,\"message\":\"format de date invalide pour 'debut'\"}";
        }

        String sql = """
        DELETE FROM reservations
        WHERE prenom_client    = ?
            AND nom_client       = ?
            AND telephone_client = ?
            AND debut_reservation = ?
            AND statut <> 'annulee'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prenom);
            ps.setString(2, nom);
            ps.setString(3, telephone);
            ps.setTimestamp(4, Timestamp.valueOf(debut));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                return "{\"success\":true,\"deleted\":" + rows + "}";
            } else {
                return "{\"success\":false,\"message\":\"Aucune réservation trouvée pour ces critères\"}";
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
