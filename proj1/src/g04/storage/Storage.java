package g04.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.Future;

import g04.Utils;
import g04.Utils.Protocol;

public class Storage implements Serializable {

    private static final long serialVersionUID = -3297985980735829122L;
    // Path to peer storage directory
    private String path;
    // Stored Chunks
    private ConcurrentHashMap<ChunkKey, Integer> storedChunks;
    // To store the chunks confirmations
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> confirmedChunks;
    // To retrieve information about files which the peer has initiated a backup for (initiator peer)
    private ConcurrentHashMap<String, SFile> backupFiles;
    // For each deleted file, keeps the peers that did not confirm the deletion
    private ConcurrentHashMap<String, HashSet<Integer>> deletedFiles;
    // Storage maximum capacity
    private long capacity;
    // Storage used capacity
    private long capacityUsed;

    /**
     * Generates a new storage for a peer if it is not in memory. Otherwise recover previous storage state 
     * from memory.
     * @throws IOException
     */
    public Storage() throws IOException {
        this.path = "g04/output/peer" + Utils.PEER_ID;
        this.storedChunks = new ConcurrentHashMap<>();
        this.confirmedChunks = new ConcurrentHashMap<>();
        this.backupFiles = new ConcurrentHashMap<>();
        this.deletedFiles = new ConcurrentHashMap<>();
        this.capacity = Utils.MAX_CAPACITY;
        this.capacityUsed = 0;

        try {
            File storage = new File(this.path + "/storage.ser");
            if (storage.exists()) {
                this.deserializeStorage(storage);
            }
            Files.createDirectories(Paths.get(this.path));
        } catch (Exception e) {
            Utils.error(e.getMessage());
        }
    }

    /**
     * Recovers the storage previous state that was saved in memory.
     * @param storage
     * @throws IOException, ClassNotFoundException
     */
    private void deserializeStorage(File storage) throws IOException, ClassNotFoundException {
        FileInputStream fi = new FileInputStream(storage);
        ObjectInputStream oi = new ObjectInputStream(fi);
        Storage s = (Storage) oi.readObject();

        this.path = s.getPath();
        this.storedChunks = s.getStoredChunks();
        this.confirmedChunks = s.getConfirmedChunks();
        this.backupFiles = s.getBackupFiles();
        this.capacity = s.getCapacity();
        this.capacityUsed = s.getCapacityUsed();
        this.deletedFiles = s.getDeletedFiles();

        oi.close();
        fi.close();
    }


    // Backup

    // Initiator-peer

    /**
     * Used by the initiator-peer to save information about a file he initiated a backup for.
     * @param file
     * @throws IOException
     */
    public void store(SFile file) throws IOException {
        this.backupFiles.putIfAbsent(file.getFileId(), file);
    }

    /**
     * Used by a peer to check if he was the one who initiated the backup of a file.
     * @param fileId
     * @return true if the peer was the initiator of a backup for the file, false otherwise
     */
    public boolean hasFile(String fileId) {
        return this.backupFiles.containsKey(fileId);
    }

    /**
     * Used by a peer to check if he was the one who initiated the backup of a file.
     * @param file
     * @return true if the peer was the initiator of a backup for the file, false otherwise
     */
    public boolean hasFile(SFile file) {
        return this.backupFiles.contains(file);
    }

    /**
     * Used by the initiator-peer to get a file that he has backed up by its name.
     * @param fileName
     * @return the file with the given fileName or null if the peer did not initiate any backup for the file.
     */
    public SFile getFileByFileName(String fileName) {

        ArrayList<SFile> files = new ArrayList<>();

        for (Object file : this.backupFiles.values().toArray()) {
            files.add(((SFile) file));
        }

        Collections.sort(files, (a,b) -> a.compare(b));

        for (SFile file : files) {
            if (file.getFileName().equals(fileName))
                return file;
        }

        return null;
    }

    /**
     * Used by the initiator-peer to get the number of Chunks of a File
     * @param fileId
     * @return number of chunks of the file
     */
    public int getFileNumChunks(String fileId) {
        return this.backupFiles.get(fileId).getNumberOfChunks();
    }

    // Other peers
    /**
     * Used by a peer to store a chunk in non-volatile memory.
     * @param chunk
     * @throws IOException
     */
    public void store(Chunk chunk) throws IOException {
        String fileDir = this.path + "/backup/file-" + chunk.getFileId();
        Files.createDirectories(Paths.get(fileDir));

        Path path = Paths.get(fileDir + "/chunk-" + chunk.getChunkNum() + ".ser");
        this.capacityUsed += chunk.getBuffer().length;

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(chunk);
        oos.flush();

        ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
        Future<Integer> operation = channel.write(buffer, 0);
        while (!operation.isDone()) {
        }

        channel.close();
        oos.close();
        baos.close();
    }

    /**
     * Used by a peer to keep track of a chunk that he has stored.
     * @param chunkKey
     */
    public void addChunk(ChunkKey chunkKey) {
        this.storedChunks.put(chunkKey, chunkKey.getReplicationDegree());
    }

    /**
     * Used by a peer to check if he has a chunk stored in memory.
     * @param chunkKey
     * @return true if the peer has stored that chunk, false otherwise
     */
    public boolean hasStoredChunk(ChunkKey chunkKey) {
        return this.storedChunks.containsKey(chunkKey);
    }


    // All peers
    /**
     * Used by any peer to store all the STORED confirmations received for chunks, 
     * except his own confirmations.
     * @param chunkKey
     * @param serverId
     */
    public void addStoredConfirmation(ChunkKey chunkKey, int serverId) {

        if (serverId != Utils.PEER_ID) {

            HashSet<Integer> peers;

            if (!this.confirmedChunks.containsKey(chunkKey)) {
                peers = new HashSet<>();
            } else {
                peers = this.confirmedChunks.get(chunkKey);
            }

            peers.add(serverId);
            this.confirmedChunks.put(chunkKey, peers);
        }

    }

    /**
     * Used by the a peer to get the perceived replication degree of a chunk 
     * (number of STORED confirmations received for that chunk)
     * @param chunkKey
     * @return perceived replication degree of the chunk 
     * (number of STORED confirmations received for the chunk)
     */
    public int getConfirmedChunks(ChunkKey chunkKey) {
        if (this.confirmedChunks.containsKey(chunkKey)) {
            return this.hasStoredChunk(chunkKey) ? this.confirmedChunks.get(chunkKey).size() + 1 : this.confirmedChunks.get(chunkKey).size() ;
        }
        return 0;
    }


    // Restore
    /**
     * Used by the initiator-peer to restore a file, given all its chunks.
     * @param fileId
     * @param restoredChunks
     * @throws IOException
     */
    public void storeRestored(String fileId, TreeSet<Chunk> restoredChunks) throws IOException {

        String fileDir = this.path + "/restored";
        Files.createDirectories(Paths.get(fileDir));

        Path path = Paths.get(fileDir + "/" + this.backupFiles.get(fileId).getFileName());

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);

        ByteBuffer buffer = ByteBuffer.allocate((int) this.backupFiles.get(fileId).getFileSize());

        for (Chunk chunk : restoredChunks) {
            buffer.put(chunk.getBuffer());
        }

        buffer.flip();

        Future<Integer> operation = channel.write(buffer, 0);

        while (!operation.isDone()) {
        }

        channel.close();

        Utils.protocolLog(Protocol.RESTORE, "restored file " + this.backupFiles.get(fileId).getFileName());
    }

    /**
     * Used by a peer to read a stored chunk from memory.
     * 
     * AsynchronousFileChannel resources:
     * http://tutorials.jenkov.com/java-nio/asynchronousfilechannel.html
     * https://www.baeldung.com/java-nio2-async-file-channel
     * @param fileId
     * @param chunkNum
     * @return the Chunk with the requested fileId and chunkNum
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Chunk read(String fileId, int chunkNum) throws IOException, ClassNotFoundException {

        Path path = Paths.get(this.path + "/backup/file-" + fileId + "/chunk-" + chunkNum + ".ser");

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

        ByteBuffer buffer = ByteBuffer.allocate(Utils.CHUNK_SIZE * 2);

        Future<Integer> result = channel.read(buffer, 0);

        while (!result.isDone()) {
        }

        buffer.flip();

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        Chunk c = (Chunk) ois.readObject();

        return c;
    }

    
    // Reclaim

    /**
     * Used by a peer to decrease the perceived replication degree of a chunk when 
     * he receives a REMOVED message. Removes the STORED confirmation that was stored for the given 
     * server because that server has discarded the chunk.
     * @param chunkKey
     * @param serverId
     * @return the perceived replication degree of the chunk
     */
    public Integer removeStoredConfirmation(ChunkKey chunkKey, int serverId) {

        // All peers 
        if (this.confirmedChunks.containsKey(chunkKey)) {
            HashSet<Integer> peers = this.confirmedChunks.get(chunkKey);
            peers.remove(serverId);
            this.confirmedChunks.put(chunkKey, peers);

            // If the peer has stored the chunk, he must count his own confirmation when calculating 
            // the perceived replication degree
            return (this.hasStoredChunk(chunkKey) ? peers.size() + 1 : peers.size());
        }

        return 0;
    }

    // Delete
    /**
     * In the version 2.0 of the Delete Protocol, it is used by the Initiator-peer to add a file that is being deleted
     * and associates it with the peers that must send DELETED confirmations.
     * @param fileId
     */
    public void addDeletedFile(String fileId){
        HashSet<Integer> peers = new HashSet<>();

        for (ChunkKey key : this.confirmedChunks.keySet()) {
            if(key.getFileId().equals(fileId)){
                peers.addAll(this.confirmedChunks.get(key));
            }
        }
        
        this.deletedFiles.put(fileId, peers);
    }

    /**
     * In the version 2.0 of the Delete Protocol, it is used when a DELETED confirmation is received by the Initiator-peer,
     * removing the pending confirmation. If all the peers that had chunks of the file have already confirmed the deletion,
     * the file is removed from the files pending deletion.
     * @param fileId
     * @param senderId
     */
    public void removePendingDeletion(String fileId, int senderId){
        if(this.deletedFiles.containsKey(fileId)){
    
            HashSet<Integer> peers = this.deletedFiles.get(fileId);
            peers.remove(senderId);

            if(peers.isEmpty()){
                this.deletedFiles.remove(fileId);
            }
            else{
                this.deletedFiles.put(fileId, peers);
            }
        }
    }

    // Capacity

    public void decreaseCapacity(int amount) {
        this.capacityUsed -= amount;
    }

    public boolean hasCapacity(int size) {
        return this.capacityUsed + size <= this.capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public boolean isFull() {
        return this.capacity < this.capacityUsed;
    }

    public long getFreeCapacity() {
        return this.capacity - this.capacityUsed;
    }

    // Getters

    public String getPath() {
        return this.path;
    }

    public ConcurrentHashMap<ChunkKey, Integer> getStoredChunks() {
        return this.storedChunks;
    }

    public ConcurrentHashMap<String, SFile> getBackupFiles() {
        return this.backupFiles;
    }

    public ConcurrentHashMap<ChunkKey, HashSet<Integer>> getConfirmedChunks() {
        return this.confirmedChunks;
    }

    public long getCapacity() {
        return this.capacity;
    }

    public long getCapacityUsed() {
        return this.capacityUsed; 
    }

    public ConcurrentHashMap<String, HashSet<Integer>> getDeletedFiles() {
        return this.deletedFiles;
    }
}
