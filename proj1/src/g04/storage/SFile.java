package g04.storage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import g04.Utils;

public class SFile implements Serializable {

    private static final long serialVersionUID = 2416806248376564181L;
    private String fileName;
    private String fileId;
    private int replicationDegree;
    private File file;
    private ConcurrentHashMap<Integer,ArrayList<String>> chunks; // Verificar se precisamos (já não temos na storage?)

    public SFile(String fileName, int replicationDegree) throws NoSuchAlgorithmException, IOException {
        this.fileName = fileName;
        this.replicationDegree = replicationDegree;
        this.fileId = Utils.generateHash(this.fileName);
        this.chunks = new ConcurrentHashMap<>();
        this.file = new File(fileName);
    }

    public ArrayList<Chunk> generateChunks() throws IOException{

        // Check file size
        if(this.file.length() >= Utils.MAX_FILE){
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

            this.chunks.put(i, new ArrayList<>());
        }

        // File Size is multiple of the chunk size 
        if(this.file.length() % Utils.CHUNK_SIZE == 0){
            Chunk chunk = new Chunk(chunksNum, this.fileId, new byte[0] ,this.replicationDegree);
            chunks.add(chunk);

            this.chunks.put(chunksNum, new ArrayList<>());
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

}
