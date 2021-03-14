package g04.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import g04.Utils;

public class Storage {

    private String path;
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> storedChunks; // To store the chunks confirmations for stored
                                                                        // chunks
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> backupConfirmations; // To store the backup confirmations for
                                                                               // backed up chunks
    private ArrayList<SFile> storedFiles; // To retrieve information about files which the peer has initiated a backup
                                          // for

    public Storage() throws IOException {
        this.path = "g04/chunks/peer" + Utils.PEER_ID;

        this.storedChunks = new ConcurrentHashMap<>();
        this.backupConfirmations = new ConcurrentHashMap<>();
        this.storedFiles = new ArrayList<>();
    }

    public void store(SFile file) throws IOException {
        this.storedFiles.add(file);

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

        FileOutputStream f = new FileOutputStream(new File(fileDir + "/chunk-" + chunk.getChunkNum() + ".ser"));
        ObjectOutputStream o = new ObjectOutputStream(f);

        o.writeObject(chunk);
        o.close();
        f.close();
    }

    public Chunk read(String fileId, int chunkNum) throws IOException, ClassNotFoundException {
        String chunkPath = this.path + "/file" + fileId + "/chunk-" + chunkNum + ".ser";
        FileInputStream fi = new FileInputStream(new File(chunkPath));
        ObjectInputStream oi = new ObjectInputStream(fi);

        Chunk c = (Chunk) oi.readObject();
        System.out.println(c.toString());
        return (Chunk) oi.readObject();
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
}
