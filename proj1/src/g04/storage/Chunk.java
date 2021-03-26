package g04.storage;

import java.io.Serializable;
import java.util.Arrays;

public class Chunk implements Serializable, Comparable<Chunk> {

	private static final long serialVersionUID = 328214701342162801L;
    private byte[] buffer;
	private ChunkKey chunkKey;
    
	public Chunk(){
		this.buffer = new byte[0];
		this.chunkKey = new ChunkKey("",-1,0,0);
	}

    public Chunk(int chunkNum, String fileId, byte[] buffer, int replicationDegree){
        this.buffer = buffer;
		this.chunkKey = new ChunkKey(fileId, chunkNum, this.buffer.length, replicationDegree);
    }

	public int getChunkNum() {
		return this.chunkKey.getChunkNum();
	}

	public String getFileId() {
		return this.chunkKey.getFileId();
	}

	public byte[] getBuffer() {
		return this.buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getReplicationDegree() {
		return this.chunkKey.getReplicationDegree();
	}

	@Override
	public String toString() {
		return "Chunk [chunkNum=" + this.getChunkNum() + ", fileId=" + this.getFileId() + ", replicationDegree="
				+ this.getReplicationDegree() + "]";
	}

	public ChunkKey getChunkKey() {
		return this.chunkKey;
	}

	@Override
	public int compareTo(Chunk chunk) {
		
		if(this.getChunkNum() < chunk.getChunkNum())
			return -1;
		
		if(this.getChunkNum() > chunk.getChunkNum())
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
		result = prime * result + this.getChunkNum();
		result = prime * result + ((this.getFileId() == null) ? 0 : this.getFileId().hashCode());
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
		if (this.getChunkNum() != other.getChunkNum())
			return false;
		if (this.getFileId() == null) {
			if (other.getFileId() != null)
				return false;
		} else if (!this.getFileId().equals(other.getFileId()))
			return false;
		return true;
	}

	
	
}
