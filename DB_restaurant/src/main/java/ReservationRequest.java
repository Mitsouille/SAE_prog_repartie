import java.io.Serializable;
import java.time.LocalDateTime;

public class ReservationRequest implements Serializable {
    public int idTable;             // <-- C'était ça qu’il manquait
    public String prenom;
    public String nom;
    public int nbConvives;
    public String tel;
    public LocalDateTime debut;
    public LocalDateTime fin;

    public ReservationRequest(int idTable, String prenom, String nom, int nbConvives, String tel, LocalDateTime debut, LocalDateTime fin) {
        this.idTable = idTable;
        this.prenom = prenom;
        this.nom = nom;
        this.nbConvives = nbConvives;
        this.tel = tel;
        this.debut = debut;
        this.fin = fin;
    }
}
