import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;

public interface ServiceRestaurant extends Remote {
    String getTousLesRestaurants() throws RemoteException;
    String getTablesParRestaurant(int idRestaurant) throws RemoteException;
    String getPlacesDisponibles(int idRestaurant, LocalDateTime debut, LocalDateTime fin) throws RemoteException;
    String getToutesLesReservations() throws RemoteException;
    String reserverTable(ReservationRequest req) throws RemoteException;
    String annulerReservation(int idReservation) throws RemoteException;
}