import java.io.Serializable;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String numero;
    private int capacite;
    private boolean exterieur;

    public Table(int id, String numero, int capacite, boolean exterieur) {
        this.id = id;
        this.numero = numero;
        this.capacite = capacite;
        this.exterieur = exterieur;
    }

    public int getId() { return id; }
    public String getNumero() { return numero; }
    public int getCapacite() { return capacite; }
    public boolean isExterieur() { return exterieur; }
}
