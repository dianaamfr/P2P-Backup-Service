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
            System.out.println("Peer " + Utils.PEER_ID + " canceled CHUNK");
            return;
        }
        
        System.out.println("Peer " + Utils.PEER_ID + " sending CHUNK");
        
        Storage storage = this.peer.getStorage();

		try {
			Chunk chunk = storage.read(chunkKey.getFileId(),chunkKey.getChunkNum());
            System.out.println("Read chunk");
            
            // Send CHUNK message
            RestoreChannel restoreChannel = this.peer.getRestoreChannel();
    
            restoreChannel.sendMessage(restoreChannel.chunkPacket(
                Utils.PROTOCOL_VERSION, 
                Utils.PEER_ID,
                chunk));

            System.out.println("Peer " + Utils.PEER_ID + " CHUNK sent");

		} catch (Exception e) {
		}        

    }
}