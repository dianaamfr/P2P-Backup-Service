package g04.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.ServerSocket;

import g04.Peer;
import g04.Utils;
import g04.channel.receivers.RestoreReceiver;
import g04.channel.receivers.TcpRestoreReceiver;
import g04.storage.Chunk;

public class RestoreChannel extends Channel{

    private int tcpPort;
    private ServerSocket tcpSocket;
    TcpRestoreReceiver tcpRestoreReceiver;

    public RestoreChannel(String address, int port) throws IOException {
        super(address, port);

        if(Utils.PROTOCOL_VERSION.equals("2.0")){
            this.tcpSocket = new ServerSocket(0);
            this.tcpPort = tcpSocket.getLocalPort();
        }
    }

    public DatagramPacket chunkPacket(String protocolVersion, int senderId, Chunk chunk){
        byte[] message = super.generateMessage(
            protocolVersion, 
            "CHUNK", 
            senderId, 
            chunk.getFileId(), 
            new String[]{Integer.toString(chunk.getChunkNum())},
            chunk.getBuffer());

        return new DatagramPacket(message, message.length, this.address, this.port);
    }

    @Override
    public void run(Peer peer) {
        this.messageReceiver = new RestoreReceiver(peer);
        this.tcpRestoreReceiver = new TcpRestoreReceiver(peer);
        peer.getScheduler().execute(this.messageReceiver);
        peer.getScheduler().execute(this.tcpRestoreReceiver);
    }

	public int getTcpPort() {
		return tcpPort;
	}

	public ServerSocket getTcpSocket() {
		return tcpSocket;
	}
}
