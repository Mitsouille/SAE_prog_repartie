import java.io.Serializable;

public class ReservationResponse implements Serializable {
    private boolean succes;
    private String message;

    public ReservationResponse(boolean succes, String message) {
        this.succes = succes;
        this.message = message;
    }

    public boolean isSucces() { return succes; }
    public String getMessage() { return message; }
}

