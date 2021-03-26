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
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.Future;

import g04.Utils;

public class Storage implements Serializable {

    private static final long serialVersionUID = -3297985980735829122L;
    private String path;
    private ConcurrentHashMap<ChunkKey, Integer> storedChunks; // Stored Chunks
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> confirmedChunks; // To store the chunks confirmations for
                                                                           // stored chunks
    private ConcurrentHashMap<String, SFile> backupFiles; // To retrieve information about files which the peer has
                                                          // initiated a backup for (initiator peer)
    private int capacity;
    private int capacityUsed;

    public Storage() throws IOException {
        this.path = "g04/output/peer" + Utils.PEER_ID;
        this.storedChunks = new ConcurrentHashMap<>();
        this.confirmedChunks = new ConcurrentHashMap<>();
        this.backupFiles = new ConcurrentHashMap<>();
        this.capacity = Utils.MAX_CAPACITY;
        this.capacityUsed = 0;

        try {
            File storage = new File(this.path + "/storage.ser");
            if (storage.exists()) {
                this.deserializeStorage(storage);
            }
            Files.createDirectories(Paths.get(this.path));
        } catch (Exception e) {

        }
    }

    private void deserializeStorage(File storage) throws IOException, ClassNotFoundException {
        FileInputStream fi = new FileInputStream(storage);
        ObjectInputStream oi = new ObjectInputStream(fi);
        Storage s = (Storage) oi.readObject();

        this.path = s.path;
        this.storedChunks = s.storedChunks;
        this.confirmedChunks = s.confirmedChunks;
        this.backupFiles = s.backupFiles;
        this.capacity = s.capacity;
        this.capacityUsed = s.capacityUsed;
    }

    public void store(SFile file) throws IOException {
        this.backupFiles.putIfAbsent(file.getFileId(), file);
    }

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

    public void storeRestored(String fileId, TreeSet<Chunk> restoredChunks) throws IOException {

        String fileDir = this.path + "/restored";
        Files.createDirectories(Paths.get(fileDir));

        Path path = Paths.get(fileDir + "/" + this.backupFiles.get(fileId).getFileName());

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);

        System.out.println(this.backupFiles.get(fileId).getFileSize());
        ByteBuffer buffer = ByteBuffer.allocate((int) this.backupFiles.get(fileId).getFileSize());

        for (Chunk chunk : restoredChunks) {
            buffer.put(chunk.getBuffer());
        }

        buffer.flip();

        Future<Integer> operation = channel.write(buffer, 0);

        while (!operation.isDone()) {
        }

        channel.close();

        System.out.println("File " + this.backupFiles.get(fileId).getFileName() + " restored");
    }

    // http://tutorials.jenkov.com/java-nio/asynchronousfilechannel.html
    // https://www.baeldung.com/java-nio2-async-file-channel
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

    public void addChunk(ChunkKey chunkKey) {
        this.storedChunks.put(chunkKey, chunkKey.getReplicationDegree());
    }

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

    public Integer removeStoredConfirmation(ChunkKey chunkKey, int serverId) {

        if (this.confirmedChunks.containsKey(chunkKey)) {
            HashSet<Integer> peers = this.confirmedChunks.get(chunkKey);
            peers.remove(serverId);
            this.confirmedChunks.put(chunkKey, peers);

            return peers.size();
        }

        return 0;
    }

    public boolean hasStoredChunk(ChunkKey chunkKey) {
        return this.storedChunks.containsKey(chunkKey);
    }

    public boolean hasFile(String fileId) {
        return this.backupFiles.containsKey(fileId);
    }

    public boolean hasFile(SFile file) {
        return this.backupFiles.contains(file);
    }

    public SFile getFileByFileName(String fileName) {
        for (Object file : this.backupFiles.values().toArray()) {
            if (((SFile) file).getFileName().equals(fileName))
                return (SFile) file;
        }

        return null;
    }

    public int getFileNumChunks(String fileId) {
        return this.backupFiles.get(fileId).getBackupConfirmations().size();
    }

    public ConcurrentHashMap<ChunkKey, Integer> getStoredChunks() {
        return this.storedChunks;
    }

    public ConcurrentHashMap<ChunkKey, HashSet<Integer>> getConfirmedChunks() {
        return this.confirmedChunks;
    }

    public int getConfirmedBackups(ChunkKey chunkKey) {
        return this.backupFiles.get(chunkKey.getFileId()).getConfirmedBackups(chunkKey);
    }

    public void addBackupConfirmation(ChunkKey chunkKey, int serverId) {
        this.backupFiles.get(chunkKey.getFileId()).addBackupConfirmation(chunkKey, serverId);
    }

    public Integer removeBackupConfirmation(ChunkKey chunkKey, int serverId) {
        return this.backupFiles.get(chunkKey.getFileId()).removeBackupConfirmation(chunkKey, serverId);
    }

    public String getPath() {
        return this.path;
    }

    public ConcurrentHashMap<String, SFile> getBackupFiles() {
        return this.backupFiles;
    }

    public void decreaseCapacity(int amount) {
        this.capacityUsed -= amount;
    }

    public boolean hasCapacity(int size) {
        return this.capacityUsed + size <= this.capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isFull() {
        return this.capacity < this.capacityUsed;
    }

    public int getCapacity() {
        return this.capacity;
    }
}
