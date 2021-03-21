package g04.channel.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import g04.Peer;
import g04.Utils;
import g04.channel.ControlChannel;
import g04.storage.Chunk;
import g04.storage.Storage;

public class PutChunkHandler implements Runnable {

    private Peer peer; 
    private HashMap<String,String> message;

    public PutChunkHandler(Peer peer, HashMap<String,String> message) {
        this.peer = peer;
        this.message = message;
    }
    
    @Override
    public void run() {
        
        Chunk chunk = new Chunk(Integer.parseInt(message.get("ChunkNo")), message.get("FileId"),
        message.get("Body").getBytes(StandardCharsets.US_ASCII), Integer.parseInt(message.get("ReplicationDeg")));

        Storage storage = this.peer.getStorage();

        // If it hasn't stored the chunk yet
        if (!storage.hasStoredChunk(chunk.getChunkKey())) {
            try {
                // Store the chunk

                System.out.println("LEEEEEEEEEEEEEEEEEEEEEENGHT " + message.get("Body").length() + "  " + message.get("Body").getBytes(StandardCharsets.US_ASCII).length);

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
