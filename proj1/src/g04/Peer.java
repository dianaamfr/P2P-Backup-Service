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

import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.BackupChannel;
import g04.channel.ChannelAggregator;
import g04.channel.ControlChannel;
import g04.channel.RestoreChannel;
import g04.channel.handlers.BackupHandler;
import g04.channel.handlers.ReclaimHandler;
import g04.storage.AsyncDeleteUpdater;
import g04.storage.AsyncStorageUpdater;
import g04.storage.Chunk;
import g04.storage.ChunkKey;
import g04.storage.SFile;
import g04.storage.Storage;

public class Peer implements IRemote {

    private ChannelAggregator channelAggregator; /** Control, Backup and Restore multicast channels */
    private Storage storage; /** Stores data that must persist between executions */
    private ScheduledThreadPoolExecutor scheduler;

    // Auxiliar data structures for RESTORE
    private ConcurrentHashMap<String, HashSet<Chunk>> pendingRestoreFiles; /*
                                                                            * For each file pending restore, keeps the
                                                                            * chunks already restored (initiator-peer)
                                                                            */
    private ConcurrentHashMap<ChunkKey, Integer> restoreRequests; /** Keeps track of restore requests (non-initiator peers) */

    // Auxiliar data structures for REMOVED
    private ConcurrentHashMap<ChunkKey, Integer> removedChunks; /** Keeps track of removed chunks */

    public Peer(ChannelAggregator aggregator) throws IOException {
        this.channelAggregator = aggregator;
        this.storage = new Storage();
        this.scheduler = new ScheduledThreadPoolExecutor(50);

        this.scheduler.scheduleWithFixedDelay(new AsyncStorageUpdater(this.storage), 5000, 5000, TimeUnit.MILLISECONDS);

        if (Utils.PROTOCOL_VERSION.equals("2.0")) {
            this.scheduler.scheduleWithFixedDelay(new AsyncDeleteUpdater(this), 5000, 5000, TimeUnit.MILLISECONDS);
        }

        this.pendingRestoreFiles = new ConcurrentHashMap<>();
        this.restoreRequests = new ConcurrentHashMap<>();
        this.removedChunks = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) throws IOException {
        // Validate number of arguments
        if (args.length != 9) {
            Utils.usage("Wrong number of arguments");
            System.exit(1);
        }

        // Parse peer version, id and access point
        Utils.PROTOCOL_VERSION = args[0];
        Utils.PEER_ID = Integer.parseInt(args[1]);
        String peerAp = args[2];

        // Parse the addresses and ports of the multicast channels
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

            // Create an aggregator for the channels
            channelAggregator = new ChannelAggregator(mcAddress, mcPort, mdbAddress, mdbPort, mdrAddress, mdrPort);

            registry = LocateRegistry.getRegistry();

        } catch (NumberFormatException e) {
            Utils.usage("The ports of the multicast channels must be integers");
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

        System.out.println("REGISTRY :: Peer" + Utils.PEER_ID + " registered with name " + peerAp);

        // Initiate Channels
        channelAggregator.run(peer);

    }

    // Protocols
    @Override
    public void backup(String fileName, int replicationDegree) throws RemoteException {

        try {
            SFile file = new SFile(fileName, replicationDegree);
            this.storage.store(file);

            ArrayList<Chunk> chunks = file.generateChunks();

            // Send PUTCHUNK message for each chunk of the file
            for (Chunk chunk : chunks) {
                DatagramPacket packet = this.getBackupChannel().putChunkPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID,
                        chunk);
                // Get confirmation messages or resend PUTCHUNK
                scheduler.execute(new BackupHandler(this, packet, chunk.getChunkKey(), replicationDegree));
            }

        } catch (Exception e) {
            Utils.protocolError(Protocol.BACKUP,null,"failed to process the file " + fileName);
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
                for (ChunkKey key : this.storage.getConfirmedChunks().keySet()) {
                    if (key.getFileId().equals(file.getFileId())) {
                        DatagramPacket packet;

                        // Version 2.0: GETCHUNK packet with TCP port
                        if (Utils.PROTOCOL_VERSION.equals("2.0")) {
                            packet = this.getControlChannel().getChunkEnhancedPacket(Utils.PROTOCOL_VERSION,
                                    Utils.PEER_ID, this.getRestoreChannel().getTcpPort(), key);
                        } 
                        // Version 1.0
                        else {
                            packet = this.getControlChannel().getChunkPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID,
                                    key);
                        }

                        this.getControlChannel().sendMessage(packet);
                        Utils.sendLog(Protocol.RESTORE, MessageType.GETCHUNK, "for chunk" + key.getChunkNum());
                    }
                }
            } else {
                // The peer did not initiate a backup for the file or the file was deleted
                Utils.protocolError(Protocol.RESTORE, null, "doesn't recognize the file " + fileName);
            }
        } catch (IOException e) {
            // Failed to send GETCHUNK
            Utils.protocolError(Protocol.RESTORE, MessageType.GETCHUNK, "for the file " + fileName);
        }
    }

    @Override
    public void delete(String fileName) throws RemoteException {

        try {
            SFile file;

            // Verify if the file was backed up by this peer
            if ((file = storage.getFileByFileName(fileName)) != null) {

                // Version 2.0: Add file to deleted files
                if (Utils.PROTOCOL_VERSION.equals("2.0")) {
                    this.storage.addDeletedFile(file.getFileId());
                }

                DatagramPacket packet = this.getControlChannel().getDeletePacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID,
                        file.getFileId());

                // Send DELETE message
                this.getControlChannel().sendMessage(packet);
                Utils.sendLog(Protocol.DELETE, MessageType.DELETE, "for the file " + fileName);

            } else {
                // The peer did not initiate a backup for the file or the file was already deleted
                Utils.protocolError(Protocol.DELETE, null, "doesn't recognize the file " + fileName);
            }
        } catch (IOException e) {
            // Failed to send DELETE
            Utils.protocolError(Protocol.DELETE, MessageType.DELETE, "for the file " + fileName);
        }

    }

    @Override
    public void reclaim(int diskSpace) {
        this.storage.setCapacity(diskSpace);
        this.scheduler.execute(new ReclaimHandler(this));
    }

    @Override
    public void state() throws RemoteException {

        /*
         * For each file whose backup it has initiated: 
         * the file pathname, the backup service id of the file and the desired replication degree. 
         * For each chunk of the file: its id and its perceived replication degree.
         */
        System.out.println("\nSTATE :: Peer: " + Utils.PEER_ID);
        System.out.println("\nStored Files:");
        ConcurrentHashMap<String, SFile> backupFiles = this.storage.getBackupFiles();
        for (String fileId : backupFiles.keySet()) {
            SFile file = backupFiles.get(fileId);

            System.out.println("\tPathname: " + file.getFileName());
            System.out.println("\tFileID: " + file.getFileId());
            System.out.println("\tReplication Degree: " + file.getReplicationDegree());

            System.out.println("\tChunks: ");

            for (ChunkKey key : this.storage.getConfirmedChunks().keySet()) {
                if (key.getFileId().equals(file.getFileId())) {
                    System.out.println("\t\tChunk No: " + key.getChunkNum());
                    System.out.println("\t\tPerceived Replication Degree: " + this.storage.getConfirmedChunks(key));
                    System.out.println("\t\t----------------------------------------------------");
                }
            }
        }

        /*
         * For each chunk it stores: its id, its size (in KBytes), the desired replication
         * degree and the perceived replication degree
         */
        System.out.println("\nStored chunks:");

        for (ChunkKey key : storage.getStoredChunks().keySet()) {

            System.out.println("\tFileId: " + key.getFileId());
            System.out.println("\tChunkNo: " + key.getChunkNum());
            System.out.println("\tSize: " + key.getSize() / 1000 + " KBytes");
            System.out.println("\tDesired Replication Degree: " + this.storage.getStoredChunks().get(key));
            System.out.println("\tPerceived Replication Degree: " + this.storage.getConfirmedChunks(key));
            System.out.println("\t----------------------------------------------------");
        }

        /*
         * The peer's storage capacity, i.e. the maximum amount of disk space that can be used 
         * to store chunks, and the amount of storage (both in KBytes) used to backup the chunks.
         */
        System.out.println("\nStorage capacity:");
        System.out.println("\tMaximum capacity: " + this.storage.getCapacity() / 1000 + " KBytes");
        System.out.println("\tUsed capacity: " + this.storage.getCapacityUsed() / 1000 + " KBytes");
        System.out.println("\tFree capacity: " + this.storage.getFreeCapacity() / 1000 + " KBytes");
    }

    
    // Methods for RESTORE

    // Initiator-peer

    /**
     * Used by the initiator-peer to check if a restore request was made for a file,
     * veryfing if he is waiting for CHUNKS of that file.
     * 
     * @param fileId
     * @return true if the peer has started a restore for the file, false otherwise
     */
    public boolean isPendingRestore(String fileId) {
        return this.pendingRestoreFiles.containsKey(fileId);
    }

    /**
     * Used by the initiator-peer to save a CHUNK for a file he is restoring.
     * 
     * @param chunk
     */
    public void addPendingChunk(Chunk chunk) {
        this.pendingRestoreFiles.get(chunk.getFileId()).add(chunk);
    }

    /**
     * Used by the initiator-peer to verify if all the CHUNKS were already received
     * for the file he is restoring (if the file is ready to actually be restored).
     * 
     * @param fileId
     * @return true if the file is ready to be restored (all the CHUNKS were
     *         received), false otherwise
     */
    public boolean isReadyToRestore(String fileId) {
        // System.out.println(this.pendingRestoreFiles.get(fileId).size());
        return this.pendingRestoreFiles.get(fileId).size() == this.storage.getFileNumChunks(fileId);
    }

    /**
     * Used by the initiator-peer to get all the CHUNKS of a file, in order to
     * restore it.
     * 
     * @param fileId
     * @return chunks of the file
     */
    public HashSet<Chunk> getRestoredChunks(String fileId) {
        return this.pendingRestoreFiles.get(fileId);
    }

    /**
     * Used by the initiator-peer when a restore was successfully completed.
     * 
     * @param fileId
     * @return 
     */
    public HashSet<Chunk> removePendingRestore(String fileId) {
        return this.pendingRestoreFiles.remove(fileId);
    }

    // Other peers

    /**
     * Used by peers who have stored a CHUNK and received a GETCHUNK message for a
     * chunk. It keeps track of GETCHUNK messages that were not yet met by any peer.
     * 
     * @param chunkKey
     * @return true if the peer received a GETCHUNK and no other peer has yet
     *         responded with the CHUNK
     */
    public boolean hasRestoreRequest(ChunkKey chunkKey) {
        return this.restoreRequests.containsKey(chunkKey);
    }

    /**
     * Used by a peer to register a received GETCHUNK message.
     * 
     * @param chunkKey
     */
    public void addRestoreRequest(ChunkKey chunkKey) {
        this.restoreRequests.put(chunkKey, 1);
    }

    /**
     * Used by a peer when the GETCHUNK request was fulfilled by himself or by other
     * peer, removing the request.
     * 
     * @param chunkKey
     */
    public void removeRestoreRequest(ChunkKey chunkKey) {
        this.restoreRequests.remove(chunkKey);
    }

    // Methods for REMOVED

    public void addRemovedChunk(ChunkKey chunkKey) {
        this.removedChunks.put(chunkKey, 1);
    }

    public boolean hasRemovedChunk(ChunkKey chunkKey) {
        return this.removedChunks.containsKey(chunkKey);
    }

    public void deleteRemovedChunk(ChunkKey chunkKey) {
        this.removedChunks.remove(chunkKey);
    }

    // Getters
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

}
