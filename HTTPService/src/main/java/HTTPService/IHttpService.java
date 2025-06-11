package HTTPService;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IHttpService extends Remote {
    String getIncidents() throws RemoteException;
}