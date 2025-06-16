import java.io.Serializable;

public class Restaurant implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String nom;
    private String adresse;
    private double latitude;
    private double longitude;
    private double note;

    public Restaurant(int id, String nom, String adresse, double latitude, double longitude, double note) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.latitude = latitude;
        this.longitude = longitude;
        this.note = note;
    }

    // Getters 
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getAdresse() { return adresse; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getNote() { return note; }
}
