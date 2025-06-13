import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceRestaurant extends Remote {
    String getTousLesRestaurantsJson()               throws RemoteException;
    String getTablesParRestaurantJson(String json)   throws RemoteException;
    String getPlacesDisponiblesJson(String json)     throws RemoteException;
    String getToutesLesReservationsJson()            throws RemoteException;
    String reserverTableJson(String json)            throws RemoteException;
    String annulerReservationJson(String json)       throws RemoteException;
}
