import java.io.Serializable;
import java.time.LocalDateTime;

public class Reservation implements Serializable {
    private int id;
    private String prenom;
    private String nom;
    private int nbConvives;
    private String statut;
    private LocalDateTime debut;
    private LocalDateTime fin;

    public Reservation(int id, String prenom, String nom, int nbConvives, String statut, LocalDateTime debut, LocalDateTime fin) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.nbConvives = nbConvives;
        this.statut = statut;
        this.debut = debut;
        this.fin = fin;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + prenom + " " + nom + " (" + nbConvives + " pers.) - " + statut +
                " de " + debut + " à " + fin;
    }
}
