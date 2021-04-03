package g04.channel.receivers;

public class Message {
    private String version;
    private String messageType;
    private int senderId;
    private String fileId;

    private int chunkNo;
    private int tcpPort;
    private int replicationDegree;
    private byte[] body;

    public Message(){
        
    }

    public Message(String version, String messageType, int senderId, String fileId){
        this.version = version;
        this.messageType = messageType;
        this.senderId = senderId;
        this.fileId = fileId;
        this.chunkNo = -1;
        this.body = new byte[0];
        this.tcpPort = -1;
        this.replicationDegree = -1;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int getSenderId() {
        return this.senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getFileId() {
        return this.fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    public void setChunkNo(int chunkNo) {
        this.chunkNo = chunkNo;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public void setReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

	@Override
	public String toString() {
		String str = "";

        str += "Version: " + this.version + "\n";
        str += "Message Type: " + this.messageType + "\n";
        str += "SenderId: " + this.senderId + "\n";
        str += "FileId: " + this.fileId + "\n";
        str += "ChunkNo: " + this.chunkNo + "\n";
        str += "Body size: " + this.body.length + "\n";

        return str;
	}
}
