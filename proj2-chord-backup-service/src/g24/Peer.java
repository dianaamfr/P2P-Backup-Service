package g24;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import g24.storage.*;
import g24.handler.BackupHandler;
import g24.handler.RestoreHandler;
import g24.handler.DeleteHandler;
import g24.handler.ReclaimHandler;
import g24.message.*;

public class Peer implements IRemote {

    private Chord chord;
    private MessageReceiver receiver;
    private Storage storage;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(250);

    // accessPoint ip port [successorIp successorPort]
    public static void main(String[] args) throws Exception {

        // Validate number of arguments
        if (args.length != 3 && args.length != 5) {
            System.err.println("Usage: java Peer <accessPoint> <peerIp> <peerPort> [successorIp successorPort]");
            System.err.println("<accessPoint> -> Access Point of the peer RMI");
            System.err.println("<peerIp> -> IP of the the peer to be initialized");
            System.err.println("<peerPort> -> Port of the the peer to be initialized");

            System.err.println("<successorIp> -> IP address of the successor of the new peer");
            System.err.println("<successorPort> -> Port address of the successor of the new peer");
            return;
        }

        Peer peer;
        Registry registry = null;

        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            registry = LocateRegistry.createRegistry(1099);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Parsed Arguments
        String peerAp = args[0];
        String peerIp = args[1];
        Utils.peerAp = peerAp;
        int peerPort = Integer.parseInt(args[2]);
        String successorIP;
        int successorPort;

        if(args.length == 5) {
            successorIP = args[3];
            successorPort = Integer.parseInt(args[4]);
            peer = new Peer(peerIp, peerPort, successorIP, successorPort);
        }
        else{
            peer = new Peer(peerIp, peerPort);
        }
        
        IRemote remote = (IRemote) UnicastRemoteObject.exportObject(peer, 0);
        registry.rebind(peerAp, remote);

        Utils.out("REGISTRY", "Peer registered with name " + peerAp);
    }

    public Peer(String ip, int port) {
        try {
            this.chord = new Chord(ip, port);
            initialize(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Peer(String ip, int port, String successorIp, int successorPort) {
        try {
            this.chord = new Chord(ip, port, successorIp, successorPort);
            initialize(port);
            this.executor.execute(new Thread(() -> this.chord.moveKeys(this.chord.getId().getSuccessor())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialize(int port) throws IOException{
            this.storage = new Storage(this.chord.getId(), this.executor);

            // Schedule the storage updater to execute with a fixed delay of 5s and before the peer process shuts down
            this.executor.scheduleWithFixedDelay(new AsyncStorageUpdater(this.storage), 5000, 5000, TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(new AsyncStorageUpdater(this.storage)));

            this.receiver = new MessageReceiver(port, this.executor, this.chord, this.storage);
            this.executor.execute(this.receiver);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> this.notifyLeaving()));
            this.executor.scheduleWithFixedDelay(new Thread(() -> this.chord.checkPredecessor()), 1000, 500, TimeUnit.MILLISECONDS);
            this.executor.scheduleWithFixedDelay(new Thread(() -> this.chord.fixFingers()), 1000, 500, TimeUnit.MILLISECONDS);
            this.executor.scheduleWithFixedDelay(new Thread(() -> this.chord.getSummary()), 1000, 500, TimeUnit.MILLISECONDS);
            this.executor.scheduleWithFixedDelay(new Thread(() -> this.chord.stabilize()), 1000, 500, TimeUnit.MILLISECONDS);
            this.executor.scheduleWithFixedDelay(new Thread(() -> this.chord.checkSuccessor()), 3000, 500, TimeUnit.MILLISECONDS);
    }

    public void notifyLeaving() {
        Identifier id = this.chord.getId();
        Identifier successor = id.getSuccessor();
        Identifier predecessor = id.getPredecessor();
        this.chord.sendMessage(successor.getIp(), successor.getPort(), 500, null, "NOTIFY", "L", predecessor.getIp(), Integer.toString(predecessor.getPort())); 
        this.chord.notifyPredecessor();

        ConcurrentHashMap<String, FileKey> storedFiles = this.storage.getStoredFiles();

        for(String key : storedFiles.keySet()) {
            FileData fileData;
            try {
                fileData = this.storage.read(key);
                new BackupHandler(this.chord, this.storage, fileData, successor, fileData.getReplicationDegree()).run();
                Utils.out("LEAVING BACKUP", key + " " + fileData.getReplicationDegree());
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void backup(String filename, int replicationDegree) throws RemoteException {
        this.executor.execute(new BackupHandler(this.chord, this.storage, filename, replicationDegree));
    }

    @Override
    public void restore(String filename) throws RemoteException {

        try {
            String fileID = "";
            fileID = Utils.generateFileHash(filename);
            // If the peer has the file in its storage
            if(this.storage.hasFileStored(fileID)) {
                FileData fileData = this.storage.read(fileID);
                fileData.setFilename(filename);
                this.storage.storeRestored(fileData);
            }
            else {
                this.executor.execute(new RestoreHandler(this.chord, new FileData(fileID, filename), this.storage));
            }
        } catch (Exception e) {
            System.err.println("File: " + filename + "could not be stored");
        }
    }

    @Override
    public void delete(String filename) throws RemoteException {

        try {
            String fileID = "";
            fileID = Utils.generateFileHash(filename);
            this.executor.execute(new DeleteHandler(this.chord, new FileData(fileID, filename), this.storage));
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("File: " + filename + "could not be deleted");
        }
    }

    @Override
    public void reclaim(long diskSpace) throws RemoteException {
         this.executor.execute(new ReclaimHandler(this.chord, this.storage, diskSpace));
    }

    @Override
    public String state() throws RemoteException {
        StringBuilder state = new StringBuilder();
        ConcurrentHashMap<String, FileKey> files = this.storage.getStoredFiles();

        state.append("\nStorage Summary:");

        state.append("\n\tTotal Space: ");
        state.append(this.storage.getTotalSpace()/1000.0);
        state.append(" Kb");
        state.append("\n\tOccupied Space: ");
        state.append(this.storage.getSpaceOccupied()/1000.0);
        state.append(" Kb");
        state.append("\n\tFree Space: ");
        state.append((this.storage.getTotalSpace() - this.storage.getSpaceOccupied())/1000.0);
        state.append(" Kb");

        state.append("\n-----------------------------------------");
        if (files.size() == 0) {
            state.append("\nNo Files Backed Up");
        }
        else {
            state.append("\nFiles Backed Up (" + files.size() + " files):\n");
            for (String fileId : files.keySet()) {
                FileKey key = files.get(fileId);

                state.append("\n\tId: ");
                state.append(key.getFileID());
                state.append("\n\tReplication Degree: ");
                state.append(key.getReplicationDegree());
                state.append("\n\tSize:");
                state.append(key.getSize());
                state.append("\n");
            }
        }
        
        return state.toString();
    }
}
