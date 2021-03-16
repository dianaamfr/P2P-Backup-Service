package g04.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import g04.Utils;

public class Storage {

    private String path;
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> storedChunks; // To store the chunks confirmations for stored
                                                                        // chunks
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> backupConfirmations; // To store the backup confirmations for
                                                                               // backed up chunks
    private ConcurrentHashMap<String, SFile> storedFiles; // To retrieve information about files which the peer has initiated a backup
                                    
    public Storage() throws IOException {
        this.path = "g04/chunks/peer" + Utils.PEER_ID;

        this.storedChunks = new ConcurrentHashMap<>();
        this.backupConfirmations = new ConcurrentHashMap<>();
        this.storedFiles = new ConcurrentHashMap<>();
    }

    public void store(SFile file) throws IOException {
        // Confirm if it is the best alternative
        this.storedFiles.put(file.getFileId(), file);

        // Serialize file - not sure if we need to do this
        /*
         * String fileDir = this.path + "/file" + file.getFileId();
         * Files.createDirectories(Paths.get(fileDir));
         * 
         * FileOutputStream f = new FileOutputStream(new File(fileDir + "/file-" +
         * file.getFileId() + ".ser")); ObjectOutputStream o = new
         * ObjectOutputStream(f);
         * 
         * o.writeObject(file); o.close(); f.close();
         */
    }

    public void store(Chunk chunk) throws IOException {
        String fileDir = this.path + "/file" + chunk.getFileId();
        Files.createDirectories(Paths.get(fileDir));

        Path path = Paths.get(fileDir + "/chunk-" + chunk.getChunkNum() + ".ser");

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(chunk);
        oos.flush();
        
        ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
        channel.write(buffer, 0);
        channel.close();
        oos.close();
        baos.close();
    }

    public Chunk read(String fileId, int chunkNum) throws IOException, ClassNotFoundException {

        Path path = Paths.get(this.path + "/file" + fileId + "/chunk-" + chunkNum + ".ser");

        AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
        
        ByteBuffer buffer = ByteBuffer.allocate(Utils.CHUNK_SIZE*2);
        
        channel.read(buffer,0);

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Chunk c = (Chunk) ois.readObject();

        return c;
    }

    public int getConfirmedBackups(ChunkKey chunkKey) {

        if (this.backupConfirmations.containsKey(chunkKey)) {
            return this.backupConfirmations.get(chunkKey).size();
        }

        return 0;
    }

    public void addBackupConfirmation(ChunkKey chunkKey, int serverId) {
        HashSet<Integer> chunkPeers;
        
        if (this.backupConfirmations.containsKey(chunkKey)) {
			chunkPeers = this.backupConfirmations.get(chunkKey);
		} else {
			chunkPeers = new HashSet<Integer>();
		}

        chunkPeers.add(serverId);
        this.backupConfirmations.put(chunkKey, chunkPeers);
    }

    public void addChunk(ChunkKey chunkKey) {
        HashSet<Integer> chunkPeers = new HashSet<Integer>();
        this.storedChunks.put(chunkKey, chunkPeers);
    }

    public void addStoredConfirmation(ChunkKey chunkKey, int serverId) {
        if (this.storedChunks.containsKey(chunkKey) && serverId != Utils.PEER_ID) {
            HashSet<Integer> chunkPeers = this.storedChunks.get(chunkKey);
            chunkPeers.add(serverId);
            this.storedChunks.put(chunkKey, chunkPeers);
        }
    }

    public int getConfirmedStoredChunks(ChunkKey chunkKey) {
        if (this.storedChunks.containsKey(chunkKey)) {
            return this.storedChunks.get(chunkKey).size();
        }
        return -1;
    }

    public boolean hasFile(String fileId){
        return this.storedFiles.containsKey(fileId);
    }

    public ConcurrentHashMap<ChunkKey, HashSet<Integer>> getStoredChunks(){
        return this.storedChunks;
    }
}
