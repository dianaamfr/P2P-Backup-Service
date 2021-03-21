package g04.storage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import g04.Utils;

public class SFile implements Serializable {

    private static final long serialVersionUID = 2416806248376564181L;
    private String fileName;
    private String fileId;
    private int replicationDegree;
    private File file;
    private long fileSize;
    private ConcurrentHashMap<ChunkKey, HashSet<Integer>> backupConfirmations; // To store the backup confirmations for backed up chunks of the file

    public SFile(String fileName) {
        this.fileName = fileName;
    }

    public SFile(String fileName, int replicationDegree) throws NoSuchAlgorithmException, IOException {
        this.file = new File(fileName);
        this.fileName = this.file.getName();
        this.fileId = Utils.generateHash(this.file);
        this.fileSize = this.file.length();
        this.replicationDegree = replicationDegree;
        this.backupConfirmations = new ConcurrentHashMap<>();
    }

    public ArrayList<Chunk> generateChunks() throws IOException{

        // Check file size
        if(this.fileSize >= Utils.MAX_FILE){
            throw new IOException("Max File Size Exception");
        }
        
        int chunksNum = (int) Math.ceil((double) this.file.length() / Utils.CHUNK_SIZE);

        // Read chunks
        ArrayList<Chunk> chunks = new ArrayList<>();
        byte[] fileBytes = Files.readAllBytes(this.file.toPath());
        
        for(int i = 0; i < chunksNum ; i++) { 
            int bufLength = fileBytes.length - i * Utils.CHUNK_SIZE > Utils.CHUNK_SIZE ? Utils.CHUNK_SIZE : fileBytes.length - Utils.CHUNK_SIZE * i;

            byte[] buf = new byte[bufLength];
            System.arraycopy(fileBytes, i * Utils.CHUNK_SIZE, buf, 0, buf.length);

            Chunk chunk = new Chunk(i, this.fileId, buf, this.replicationDegree);
            chunks.add(chunk);

            if (!this.backupConfirmations.containsKey(chunk.getChunkKey())){
                this.backupConfirmations.put(chunk.getChunkKey(), new HashSet<Integer>());
            }
        }

        // File Size is multiple of the chunk size 
        if(this.fileSize % Utils.CHUNK_SIZE == 0){
            Chunk chunk = new Chunk(chunksNum, this.fileId, new byte[0] ,this.replicationDegree);
            chunks.add(chunk);

            if (!this.backupConfirmations.containsKey(chunk.getChunkKey())){
                this.backupConfirmations.put(chunk.getChunkKey(), new HashSet<Integer>());
            }
        }

        return chunks;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public int getConfirmedBackups(ChunkKey chunkKey) {

        if (this.backupConfirmations.containsKey(chunkKey)) {
            return this.backupConfirmations.get(chunkKey).size();
        }

        return 0;
    }

    public long getFileSize(){
        return this.fileSize;
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

    @Override
    public boolean equals(Object obj) {
        return this.fileName.equals(((SFile) obj).getFileName());
    }

    public ConcurrentHashMap<ChunkKey, HashSet<Integer>> getBackupConfirmations(){
        return this.backupConfirmations;
    }
}