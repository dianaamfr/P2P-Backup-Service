package g04.channel.handlers;

import java.io.IOException;
import java.util.HashMap;

import g04.Peer;
import g04.Utils;
import g04.channel.ControlChannel;
import g04.channel.RestoreChannel;
import g04.storage.Chunk;
import g04.storage.Storage;

public class GetChunkHandler implements Runnable {

    private Peer peer;
    private HashMap<String, String> message;

    public GetChunkHandler(Peer peer, HashMap<String, String> message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {

        // if(ver se peer ja tem o chunk)
            // se tiver aborta
        
        Storage storage = peer.getStorage();

        Chunk chunk;
		try {
			chunk = storage.read(message.get("FileId"), Integer.parseInt(message.get("ChunkNo")));
            
            // Send CHUNK message
            RestoreChannel restoreChannel = peer.getRestoreChannel();
    
            restoreChannel.sendMessage(restoreChannel.chunkPacket(
                Utils.PROTOCOL_VERSION, 
                Utils.PEER_ID,
                chunk));
		} catch (Exception e) {
		}        
    }
}
