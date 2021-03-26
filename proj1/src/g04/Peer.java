package g04;

import java.io.IOException;
import java.net.DatagramPacket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import g04.channel.BackupChannel;
import g04.channel.ChannelAggregator;
import g04.channel.ControlChannel;
import g04.channel.RestoreChannel;
import g04.channel.handlers.BackupHandler;
import g04.channel.handlers.ReclaimHandler;
import g04.storage.AsyncStorageUpdater;
import g04.storage.Chunk;
import g04.storage.ChunkKey;
import g04.storage.SFile;
import g04.storage.Storage;

public class Peer implements IRemote {

    private ChannelAggregator channelAggregator; // Aggregates the Control, Backup and Restore multicast channels
    private Storage storage; // Stores data that must persist between executions
    private ScheduledThreadPoolExecutor scheduler;

    // Restore
    private ConcurrentHashMap<String, HashSet<Chunk>> pendingRestoreFiles; // For each file pending restore, keeps the
                                                                           // restored chunks (initiator-peer)
    private ConcurrentHashMap<ChunkKey, Integer> restoreRequests; // Keeps track of restore requests (non-iniator peers)
    
    private ConcurrentHashMap<ChunkKey, Integer> removedChunks; // Keeps track of removed chunks


    public Peer(ChannelAggregator aggregator) throws IOException {
        this.channelAggregator = aggregator;
        this.storage = new Storage();
        this.scheduler = new ScheduledThreadPoolExecutor(50);

        this.scheduler.scheduleWithFixedDelay(new AsyncStorageUpdater(this.storage), 5000, 5000, TimeUnit.MILLISECONDS);

        this.pendingRestoreFiles = new ConcurrentHashMap<>();
        this.restoreRequests = new ConcurrentHashMap<>();
        this.removedChunks = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 9) {
            Utils.usage("Wrong number of arguments");
            System.exit(1);
        }

        Utils.PROTOCOL_VERSION = args[0];
        Utils.PEER_ID = Integer.parseInt(args[1]);
        String peerAp = args[2];

        String mcAddress = "", mdbAddress = "", mdrAddress = "";
        int mcPort = 0, mdbPort = 0, mdrPort = 0;

        ChannelAggregator channelAggregator = null;
        Peer peer;
        Registry registry = null;

        try {
            mcAddress = args[3];
            mcPort = Integer.parseInt(args[4]);

            mdbAddress = args[5];
            mdbPort = Integer.parseInt(args[6]);

            mdrAddress = args[7];
            mdrPort = Integer.parseInt(args[8]);

            channelAggregator = new ChannelAggregator(mcAddress, mcPort, mdbAddress, mdbPort, mdrAddress, mdrPort);

            registry = LocateRegistry.getRegistry();

        } catch (NumberFormatException e) {
            Utils.usage("Number Format Exception");
            System.exit(1);
        } catch (RemoteException e) {
            registry = LocateRegistry.createRegistry(1099);
        } catch (IOException e) {
            Utils.usage("IOException when joining Multicast Groups");
            System.exit(1);
        }

        peer = new Peer(channelAggregator);
        IRemote remote = (IRemote) UnicastRemoteObject.exportObject(peer, 0);
        registry.rebind(peerAp, remote);

        System.out.println("Peer with id " + Utils.PEER_ID + " registered to service with name " + peerAp);

        // Initate Channels
        channelAggregator.run(peer);

    }

    @Override
    public void backup(String fileName, int replicationDegree) throws RemoteException {

        try {
            SFile file = new SFile(fileName, replicationDegree);
            this.storage.store(file);

            ArrayList<Chunk> chunks = file.generateChunks();

            // Send putchunk message
            for (Chunk chunk : chunks) {
                DatagramPacket packet = this.getBackupChannel().putChunkPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID,
                        chunk);
                // Get confirmation messages or resend putchunk
                scheduler.execute(new BackupHandler(this, packet, chunk.getChunkKey(), replicationDegree));
            }

        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
            // Throw error message - file error
            e.printStackTrace();
        }
    }

    @Override
    public void restore(String fileName) throws RemoteException {

        try {
            SFile file;

            // Verify if the file was backed up by this peer
            if ((file = storage.getFileByFileName(fileName)) != null) {
                // Add the file to the pending restore requests
                this.pendingRestoreFiles.put(file.getFileId(), new HashSet<>());

                // Send GETCHUNK message for each chunk of the file
                for (ChunkKey key : file.getBackupConfirmations().keySet()) {
                    DatagramPacket packet = this.getControlChannel().getChunkPacket(Utils.PROTOCOL_VERSION,
                            Utils.PEER_ID, key);

                    System.out.println("GETCHUNK " + key.getChunkNum());
                    this.getControlChannel().sendMessage(packet);
                }
            } else {
                throw new Exception("SFile is null");
            }
        } catch (IOException e) {
            System.err.println("Failed to send GETCHUNK for file " + fileName);
        } catch (Exception e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileName) throws RemoteException {

        try {
            SFile file;

            // Verify if the file was backed up by this peer
            if ((file = storage.getFileByFileName(fileName)) != null) {

                // Send DELETE message for each chunk of the file
                DatagramPacket packet = this.getControlChannel().getDeletePacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID,
                        file.getFileId());

                this.getControlChannel().sendMessage(packet);
            } else {
                throw new Exception("SFile is null");
            }
        } catch (IOException e) {
            System.err.println("Failed to send DELETE for file " + fileName);
        } catch (Exception e) {
            System.err.println("File not found: " + e.getMessage());
        }

    }

    @Override
    public void reclaim(int diskSpace){
        this.storage.setCapacity(diskSpace);
        this.scheduler.execute(new ReclaimHandler(this));
    }

    @Override
    public void state() throws RemoteException {
        // TODO Auto-generated method stub
    }

    public BackupChannel getBackupChannel() {
        return this.channelAggregator.getBackupChannel();
    }

    public RestoreChannel getRestoreChannel() {
        return this.channelAggregator.getRestoreChannel();
    }

    public ControlChannel getControlChannel() {
        return this.channelAggregator.getControlChannel();
    }

    public Storage getStorage() {
        return this.storage;
    }

    public ScheduledThreadPoolExecutor getScheduler() {
        return scheduler;
    }


    // Restore

    public boolean isPendingRestore(String fileId) {
        return this.pendingRestoreFiles.containsKey(fileId);
    }

    public void addPendingChunk(Chunk chunk) {
        this.pendingRestoreFiles.get(chunk.getFileId()).add(chunk);
    }

    public boolean isReadyToRestore(String fileId) {
        System.out.println(this.pendingRestoreFiles.get(fileId).size());
        return this.pendingRestoreFiles.get(fileId).size() == this.storage.getFileNumChunks(fileId);
    }

    public HashSet<Chunk> getRestoredChunks(String fileId) {
        return this.pendingRestoreFiles.get(fileId);
    }

    public void removePendingRestore(String fileId) {
        this.pendingRestoreFiles.remove(fileId);
    }

    public boolean hasRestoreRequest(ChunkKey chunkKey) {
        return this.restoreRequests.containsKey(chunkKey);
    }

    public void addRestoreRequest(ChunkKey chunkKey) {
        this.restoreRequests.put(chunkKey, 1);
    }

    public void removeRestoreRequest(ChunkKey chunkKey) {
        this.restoreRequests.remove(chunkKey);
    }


    // Reclaim
    public void addRemovedChunk(ChunkKey chunkKey) {
        this.removedChunks.put(chunkKey, 1);
    }

    public boolean hasRemovedChunk(ChunkKey chunkKey) {
        return this.removedChunks.containsKey(chunkKey);
    }

    public void deleteRemovedChunk(ChunkKey chunkKey) {
        this.removedChunks.remove(chunkKey);
    }
}
