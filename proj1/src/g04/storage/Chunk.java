package g04.storage;

import java.io.Serializable;
import java.util.Arrays;

public class Chunk implements Serializable, Comparable<Chunk> {

	private static final long serialVersionUID = 328214701342162801L;
	private int chunkNum;
    private String fileId;
    private byte[] buffer;
    private int replicationDegree;
	private ChunkKey chunkKey;
    
    public Chunk(int chunkNum, String fileId, byte[] buffer, int replicationDegree){
        this.chunkNum = chunkNum;
        this.fileId = fileId;
        this.buffer = buffer;
        this.replicationDegree = replicationDegree;
		this.chunkKey = new ChunkKey(this.fileId, this.chunkNum, buffer.length);
    }

	public int getChunkNum() {
		return chunkNum;
	}

	public void setChunkNum(int chunkNum) {
		this.chunkNum = chunkNum;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public byte[] getBuffer() {
		return this.buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getReplicationDegree() {
		return this.replicationDegree;
	}

	@Override
	public String toString() {
		return "Chunk [chunkNum=" + chunkNum + ", fileId=" + fileId + ", replicationDegree="
				+ this.replicationDegree + "]";
	}

	public ChunkKey getChunkKey() {
		return this.chunkKey;
	}

	@Override
	public int compareTo(Chunk chunk) {
		
		if(this.chunkNum < chunk.getChunkNum())
			return -1;
		
		if(this.chunkNum > chunk.getChunkNum())
			return 1;

		return 0;
	}
	
	// Check hashCode and equals - auto generated to use Chunk HashSet
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(buffer);
		result = prime * result + ((chunkKey == null) ? 0 : chunkKey.hashCode());
		result = prime * result + chunkNum;
		result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Chunk other = (Chunk) obj;
		if (!Arrays.equals(buffer, other.buffer))
			return false;
		if (chunkKey == null) {
			if (other.chunkKey != null)
				return false;
		} else if (!chunkKey.equals(other.chunkKey))
			return false;
		if (chunkNum != other.chunkNum)
			return false;
		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;
		return true;
	}

	
	
}
