package g04.storage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import g04.Utils;

public class SFile implements Serializable {

    private static final long serialVersionUID = 2416806248376564181L;
    private String fileName;
    private String fileId;
    private int replicationDegree;
    private File file;
    private long fileSize;
    private int numberOfChunks;

    public SFile(String fileName) {
        this.fileName = fileName;
    }

    public SFile(String fileName, int replicationDegree) throws NoSuchAlgorithmException, IOException {
        this.file = new File(fileName);
        this.fileName = this.file.getName();
        this.fileId = Utils.generateHash(this.file);
        this.fileSize = this.file.length();
        this.replicationDegree = replicationDegree;
        this.numberOfChunks = 0;
    }

    /**
     * Divides the file into chunks
     * @return an array of chunks
     * @throws IOException
     */
    public ArrayList<Chunk> generateChunks() throws IOException{

        // Check file size
        if(this.fileSize >= Utils.MAX_FILE){
            throw new IOException("Max File Size Exception");
        }
        
        this.numberOfChunks = (int) Math.ceil((double) this.file.length() / Utils.CHUNK_SIZE);

        // Read chunks
        ArrayList<Chunk> chunks = new ArrayList<>();
        byte[] fileBytes = Files.readAllBytes(this.file.toPath());
        
        for(int i = 0; i < this.numberOfChunks ; i++) { 
            int bufLength = fileBytes.length - i * Utils.CHUNK_SIZE > Utils.CHUNK_SIZE ? Utils.CHUNK_SIZE : fileBytes.length - Utils.CHUNK_SIZE * i;

            byte[] buf = new byte[bufLength];
            System.arraycopy(fileBytes, i * Utils.CHUNK_SIZE, buf, 0, buf.length);

            Chunk chunk = new Chunk(i, this.fileId, buf, this.replicationDegree);
            chunks.add(chunk);
        }

        // File Size is multiple of the chunk size 
        if(this.fileSize % Utils.CHUNK_SIZE == 0){
            this.numberOfChunks++;
            Chunk chunk = new Chunk(this.numberOfChunks, this.fileId, new byte[0] ,this.replicationDegree);
            chunks.add(chunk);
        }

        return chunks;
    }

    /** Compare two files by their names */
    @Override
    public boolean equals(Object obj) {
        return this.fileName.equals(((SFile) obj).getFileName());
    }
    
    // Getters
    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public long getFileSize(){
        return this.fileSize;
    }

    public int getNumberOfChunks(){
        return this.numberOfChunks;
    }
}