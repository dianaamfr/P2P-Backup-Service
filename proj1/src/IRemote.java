import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemote extends Remote {
    String backup(String file_name, int replication_degree) throws RemoteException;
    String restore(String file_name) throws RemoteException;
    String delete(String file_name) throws RemoteException;
    String reclaim(int disk_space) throws RemoteException;
    String state() throws RemoteException;
}