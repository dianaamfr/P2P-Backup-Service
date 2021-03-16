package g04.channel;

import java.io.IOException;
import java.net.DatagramPacket;

import g04.Peer;
import g04.channel.receivers.ControlReceiver;
import g04.storage.ChunkKey;

public class ControlChannel extends Channel {

    public ControlChannel(String address, int port) throws IOException {
        super(address, port);
    }
      
    public DatagramPacket storedPacket(String protocolVersion, int senderId, ChunkKey chunkKey){
        byte[] message = super.generateMessage(
            protocolVersion, 
            "STORED", 
            senderId, 
            chunkKey.getFileId(), 
            new String[]{Integer.toString(chunkKey.getChunkNum())});

        return new DatagramPacket(message, message.length, this.address, this.port);
    }


    @Override
    public void run(Peer peer) {
        this.messageReceiver = new ControlReceiver(peer);
        peer.getScheduler().execute(this.messageReceiver);
    }
    
}
