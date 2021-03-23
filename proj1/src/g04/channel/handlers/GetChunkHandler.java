package g04.channel.handlers;

import g04.Peer;
import g04.Utils;
import g04.channel.RestoreChannel;
import g04.storage.Chunk;
import g04.storage.ChunkKey;
import g04.storage.Storage;

public class GetChunkHandler implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;

    public GetChunkHandler(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    @Override
    public void run() {

        // If the chunk was already restored exit
        if(!this.peer.hasRestoreRequest(this.chunkKey)){
            return;
        }
        
        Storage storage = this.peer.getStorage();
        Chunk chunk = new Chunk(-1,null,null,-1);
		try {
			chunk = storage.read(chunkKey.getFileId(),chunkKey.getChunkNum());
            
            // Send CHUNK message
            RestoreChannel restoreChannel = this.peer.getRestoreChannel();
    
            restoreChannel.sendMessage(restoreChannel.chunkPacket(
                Utils.PROTOCOL_VERSION, 
                Utils.PEER_ID,
                chunk));

            System.out.println("Peer " + Utils.PEER_ID + ": sent CHUNK " + chunk.getChunkNum());

		} catch (Exception e) {
            System.out.println("Failed to send CHUNK " + chunk.getChunkNum());
		}

    }
}
