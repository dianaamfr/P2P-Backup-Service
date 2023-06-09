package g04.channel;

import java.io.IOException;
import java.net.DatagramPacket;

import g04.Peer;
import g04.channel.receivers.BackupReceiver;
import g04.storage.Chunk;

public class BackupChannel extends Channel {

    public BackupChannel(String address, int port) throws IOException {
        super(address, port);
    }
    
    /**
     * Generate a PUTCHUNK DatagramPacket
     * @param protocolVersion
     * @param senderId
     * @param chunk
     * @return PUTCHUNK DatagramPacket
     */
    public DatagramPacket putChunkPacket(String protocolVersion, int senderId, Chunk chunk){
        byte[] message = super.generateMessage(
            protocolVersion, 
            "PUTCHUNK", 
            senderId, 
            chunk.getFileId(), 
            new String[]{Integer.toString(chunk.getChunkNum()), Integer.toString(chunk.getReplicationDegree())}, 
            chunk.getBuffer());

        return new DatagramPacket(message, message.length, this.address, this.port);
    }

    @Override
    public void run(Peer peer) {
        this.messageReceiver = new BackupReceiver(peer);
        peer.getScheduler().execute(this.messageReceiver);
    }
}
