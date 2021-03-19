package g04.storage;

import java.io.Serializable;

public class Chunk implements Serializable {

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
		this.chunkKey = new ChunkKey(this.fileId, this.chunkNum);
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
		return buffer;
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

}
