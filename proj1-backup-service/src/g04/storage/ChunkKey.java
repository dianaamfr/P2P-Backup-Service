package g04.storage;

import java.io.Serializable;

public class ChunkKey implements Serializable {

	private static final long serialVersionUID = -8070875167232670060L;
	private String fileId;
    private int chunkNum;
    private int size;
    private int replicationDegree;

    public ChunkKey(String fileId, int chunkNum) {
        this.fileId = fileId;
        this.chunkNum = chunkNum;
        this.size = 0;
        this.replicationDegree = 0;
    } 

    public ChunkKey(String fileId, int chunkNum, int size, int replicationDegree) {
        this(fileId, chunkNum);
        this.size = size;
        this.replicationDegree = replicationDegree;
    } 

    public boolean equals(Object o) {
      
        if (!(o instanceof ChunkKey)) { 
            return false; 
        } 

        ChunkKey ck = (ChunkKey) o; 
        return this.fileId.equals(ck.getFileId()) && this.chunkNum == ck.getChunkNum();
    }

    public String getFileId() {
        return this.fileId;
    }

    public int getChunkNum() {
        return this.chunkNum;
    }

    public int getSize() {
        return this.size;
    }

    public int getReplicationDegree() {
        return this.replicationDegree;
    }

    public void setReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
    }

    @Override
    public int hashCode() {
        return this.fileId.hashCode() + this.chunkNum;
    }
}
