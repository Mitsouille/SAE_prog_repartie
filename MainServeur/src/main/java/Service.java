import java.rmi.Remote;
import java.rmi.RemoteException;

import org.json.JSONObject;

public interface Service extends Remote {
    JSONObject getMessage() throws RemoteException;
}