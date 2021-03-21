package g04.channel.handlers;

import java.io.IOException;

import g04.Peer;
import g04.Utils;
import g04.channel.ControlChannel;
import g04.channel.receivers.Message;
import g04.storage.Chunk;
import g04.storage.Storage;

public class PutChunkHandler implements Runnable {

    private Peer peer; 
    private Message message;

    public PutChunkHandler(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }
    
    @Override
    public void run() {
        
        Chunk chunk = new Chunk(this.message.getChunkNo(), this.message.getFileId(),
        this.message.getBody(), this.message.getReplicationDegree());

        Storage storage = this.peer.getStorage();

        // If it hasn't stored the chunk yet
        if (!storage.hasStoredChunk(chunk.getChunkKey())) {
            try {
                // Store the chunk
                storage.store(chunk);
                storage.addChunk(chunk.getChunkKey());
            } catch (IOException e) {
                // Failed at storing the chunk
                e.printStackTrace();
            }
        }
        
        // Send STORED message
        ControlChannel controlChannel = peer.getControlChannel();

        controlChannel.sendMessage(controlChannel.storedPacket(
            Utils.PROTOCOL_VERSION, 
            Utils.PEER_ID,
            chunk.getChunkKey()));
        
    }
}
