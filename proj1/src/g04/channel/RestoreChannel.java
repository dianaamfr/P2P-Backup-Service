package g04.channel;

import java.io.IOException;
import java.net.DatagramPacket;

import g04.Peer;
import g04.channel.receivers.RestoreReceiver;
import g04.storage.Chunk;

public class RestoreChannel extends Channel{

    public RestoreChannel(String address, int port) throws IOException {
        super(address, port);
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
        peer.getScheduler().execute(this.messageReceiver);   
    }

}
