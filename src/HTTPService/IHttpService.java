package HTTPService;

import org.json.simple.JSONObject;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IHttpService extends Remote {
    JSONObject getIncidents() throws RemoteException;
}
