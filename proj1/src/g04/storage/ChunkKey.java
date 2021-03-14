package g04.storage;

import java.io.Serializable;

public class ChunkKey implements Serializable {

	private static final long serialVersionUID = -8070875167232670060L;
	private String fileId;
    private int chunkNum;

    public ChunkKey(String fileId, int chunkNum) {
        this.fileId = fileId;
        this.chunkNum = chunkNum;
    } 

    public boolean equals(Object o) {
      
        if (!(o instanceof ChunkKey)) { 
            return false; 
        } 

        ChunkKey ck = (ChunkKey) o; 
        return this.fileId.equals(ck.getFileId()) && this.chunkNum == ck.getChunkNum();
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    @Override
    public int hashCode() {
        return this.fileId.hashCode() + this.chunkNum;
    }
}
