package g04;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemote extends Remote {

    void backup(String fileName, int replicationDegree) throws RemoteException;

    void restore(String fileName) throws RemoteException;

    void delete(String fileName) throws RemoteException;

    void reclaim(int diskSpace) throws RemoteException;

    void state() throws RemoteException;
}