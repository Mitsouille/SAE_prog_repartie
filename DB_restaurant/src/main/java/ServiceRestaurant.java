import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceRestaurant extends Remote {
    List<Restaurant> getTousLesRestaurants() throws RemoteException;
    ReservationResponse reserverTable(ReservationRequest req) throws RemoteException;

    List<Reservation> getToutesLesReservations() throws RemoteException;

    boolean annulerReservation(int idReservation) throws RemoteException;

    List<ReservationResponse> getReservationsParTable(int idTable) throws RemoteException;

    List<Table> getTablesParRestaurant(int idRestaurant) throws RemoteException;

    int getPlacesDisponibles(int idRestaurant, LocalDateTime debut, LocalDateTime fin) throws RemoteException;
}
