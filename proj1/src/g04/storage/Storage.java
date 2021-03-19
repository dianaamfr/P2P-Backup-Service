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
import java.util.concurrent.*;
import java.util.concurrent.Future;

import g04.Utils;

public class Storage implements Serializable {

    private static final long serialVersionUID = -3297985980735829122L;
    private String path;
    private ConcurrentHashMap<ChunkKey, Integer> storedChunks;
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> confirmedChunks; // To store the chunks confirmations for stored
                                                                        // chunks
    private ConcurrentHashMap<String, SFile> storedFiles; // To retrieve information about files which the peer has
                                                          // initiated a backup

    public Storage() throws IOException {
        this.path = "g04/chunks/peer" + Utils.PEER_ID;
        this.storedChunks = new ConcurrentHashMap<>();
        this.confirmedChunks = new ConcurrentHashMap<>();
        this.storedFiles = new ConcurrentHashMap<>();
        
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
        this.storedFiles = s.storedFiles;
    }

    public void store(SFile file) throws IOException {
        // Confirm if it is the best alternative
        this.storedFiles.put(file.getFileId(), file);
    }

    public void store(Chunk chunk) throws IOException {
        String fileDir = this.path + "/file-" + chunk.getFileId();
        Files.createDirectories(Paths.get(fileDir));

        Path path = Paths.get(fileDir + "/chunk-" + chunk.getChunkNum() + ".ser");

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

    // http://tutorials.jenkov.com/java-nio/asynchronousfilechannel.html
    // https://www.baeldung.com/java-nio2-async-file-channel
    public Chunk read(String fileId, int chunkNum) throws IOException, ClassNotFoundException {

        Path path = Paths.get(this.path + "/file-" + fileId + "/chunk-" + chunkNum + ".ser");

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

        ByteBuffer buffer = ByteBuffer.allocate(Utils.CHUNK_SIZE * 2);

        Future<Integer> result = channel.read(buffer, 0);

        while (!result.isDone()){
        }

        buffer.flip();

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Chunk c = (Chunk) ois.readObject();

        return c;
    }

    public void addChunk(ChunkKey chunkKey) {
        this.storedChunks.put(chunkKey,1);
    }

    public void addStoredConfirmation(ChunkKey chunkKey, int serverId) {
        if (serverId != Utils.PEER_ID) {

            HashSet<Integer> peers;

            if(!this.confirmedChunks.containsKey(chunkKey)){
                peers = new HashSet<>();
            }
            else
                peers = this.confirmedChunks.get(chunkKey);
            
            peers.add(serverId);
            this.confirmedChunks.put(chunkKey, peers);
        }
    }

    public boolean getStoredChunk(ChunkKey chunkKey) {
        return this.storedChunks.containsKey(chunkKey);
    }

    public boolean hasFile(String fileId) {
        return this.storedFiles.containsKey(fileId);
    }

    public boolean hasFile(SFile file) {
        return this.storedFiles.contains(file);
    }

    public SFile getFile(String fileName) {
        
        for(Object file : this.storedFiles.values().toArray()){
            if(((SFile) file).getFileName().equals(fileName))
                return (SFile) file;
        }

        return null;
    }

    public ConcurrentHashMap<ChunkKey, HashSet<Integer>> getConfirmedChunks() {
        return this.confirmedChunks;
    }

    public int getConfirmedBackups(ChunkKey chunkKey) {
        return this.storedFiles.get(chunkKey.getFileId()).getConfirmedBackups(chunkKey);
    }

    public void addBackupConfirmation(ChunkKey chunkKey, int serverId) {
        this.storedFiles.get(chunkKey.getFileId()).addBackupConfirmation(chunkKey, serverId);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setConfirmedChunks(ConcurrentHashMap<ChunkKey, HashSet<Integer>> confirmedChunks) {
        this.confirmedChunks = confirmedChunks;
    }

    public ConcurrentHashMap<String, SFile> getStoredFiles() {
        return storedFiles;
    }

    public void setStoredFiles(ConcurrentHashMap<String, SFile> storedFiles) {
        this.storedFiles = storedFiles;
    }

}
