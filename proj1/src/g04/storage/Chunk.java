package g04.storage;

public class Chunk {
    private int chunkNum;
    private String fileId;
    private byte[] buffer;
    private int[] replicationDegree = new int[2]; // [1] desired; [2] perceived
    
    public Chunk(int chunkNum, String fileId, byte[] buffer, int replicationDegree){
        this.chunkNum = chunkNum;
        this.fileId = fileId;
        this.buffer = buffer;
        this.replicationDegree[0] = replicationDegree;
        this.replicationDegree[1] = 0;
    }
}
