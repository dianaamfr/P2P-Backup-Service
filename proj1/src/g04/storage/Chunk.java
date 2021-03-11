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

	public int[] getReplicationDegree() {
		return replicationDegree;
	}

	public void setReplicationDegree(int[] replicationDegree) {
		this.replicationDegree = replicationDegree;
	}
}
