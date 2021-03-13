package g04.channel;

import java.io.IOException;
import java.net.DatagramPacket;

import g04.storage.Chunk;

public class BackupChannel extends Channel {

    public BackupChannel(String address, int port) throws IOException {
        super(address, port);
    }
    
    public DatagramPacket putChunkPacket(String protocolVersion, int senderId, Chunk chunk){
        byte[] message = super.generateMessage(
            protocolVersion, 
            "PUTCHUNK", 
            senderId, 
            chunk.getFileId(), 
            new String[]{Integer.toString(chunk.getChunkNum()), Integer.toString(chunk.getReplicationDegree()[0])}, 
            chunk.getBuffer());

        return new DatagramPacket(message, message.length, this.address, this.port);
    }

    

}
