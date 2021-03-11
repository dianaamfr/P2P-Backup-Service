package g04.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import g04.Utils;

public class SFile {
    private String fileName;
    private String fileId;
    private int replicationDegree;
    private ConcurrentHashMap<Integer,ArrayList<String>> chunks;

    public SFile(String fileName, int replicationDegree) throws NoSuchAlgorithmException, IOException {
        this.fileName = fileName;
        this.replicationDegree = replicationDegree;
        this.fileId = Utils.generateHash(this.fileName);
        this.chunks = new ConcurrentHashMap<>();
    }

    public void generateChunks() throws IOException{
        System.out.println("Generating chunks");

        File file = new File(fileName);
        
        // Check file size
        if(file.length() >= Utils.MAX_FILE){
            throw new IOException("Max File Size Exception");
        }
        
        int chunksNum = (int) Math.ceil((double) file.length() / Utils.CHUNK_SIZE);

        // Read chunks
        ArrayList<Chunk> chunks = new ArrayList();
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        
        for(int i = 0; i < chunksNum ; i++) { 
            int bufLength = fileBytes.length - i * Utils.CHUNK_SIZE > Utils.CHUNK_SIZE ? Utils.CHUNK_SIZE : fileBytes.length - Utils.CHUNK_SIZE * i;

            byte[] buf = new byte[bufLength];
            System.arraycopy(fileBytes, i * Utils.CHUNK_SIZE, buf, 0, buf.length);

            Chunk chunk = new Chunk(i, this.fileId, buf, this.replicationDegree);
            chunks.add(chunk);
        }

        // File Size is multiple of the chunk size 
        if(file.length() % Utils.CHUNK_SIZE == 0){
            Chunk chunk = new Chunk(chunksNum, this.fileId, new byte[0] ,this.replicationDegree);
            chunks.add(chunk);
        }

    }

}
