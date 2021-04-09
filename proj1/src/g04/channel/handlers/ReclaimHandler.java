package g04.channel.handlers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Collections;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.storage.ChunkKey;
import g04.storage.ReplicationDegreeComparable;
import g04.storage.Storage;

/**
 * Used as a result of a RECLAIM protocol, when it is necessary to remove chunks to ensure the
 * capacity used doesn't surpass the one that was imposed.
 * A REMOVED message is sent for each chunk that is removed.
 */
public class ReclaimHandler implements Runnable {

    private Peer peer;
    private Storage storage;

    public ReclaimHandler(Peer peer) {
        this.peer = peer;
        this.storage = peer.getStorage();
    }

    @Override
    public void run() {

        ArrayList<ReplicationDegreeComparable> chunks = new ArrayList<>();

        // Order the stored chunks so that so that the ones whose perceived replication degree is higher than the desired 
        // are removed first
        for(ChunkKey key: this.storage.getStoredChunks().keySet()){
            chunks.add(new ReplicationDegreeComparable(key, this.storage.getConfirmedChunks(key)));
        }

        Collections.sort(chunks, (a,b) -> a.compare(b));

        // Remove chunks until the capacity used doesn't exceed the maximum available
        while(this.storage.isFull()){
            try {
                // Send REMOVED message
                DatagramPacket packet = this.peer.getControlChannel().removedPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, chunks.get(0).getChunkKey());
                this.peer.getControlChannel().sendMessage(packet);
                
                ChunkKey key = chunks.get(0).getChunkKey();

                // Decrease used capacity and remove chunk from storage
                this.storage.decreaseCapacity(key.getSize());
                this.storage.getStoredChunks().remove(key);
                
                File file = new File(storage.getPath() + "/backup/file-" + key.getFileId() + "/chunk-" + key.getChunkNum() + ".ser");
            
                if(file.exists()) {
                    file.delete();
                }
                
                Utils.sendLog(Protocol.RECLAIM, MessageType.REMOVED, "chunk" +  key.getChunkNum() + " freeing up " + key.getSize()/1000 + "KB");
                
                chunks.remove(0);

            } catch (IOException e) {
                // Failed to send REMOVED message
                ChunkKey key = chunks.get(0).getChunkKey();
                Utils.protocolError(Protocol.RECLAIM, MessageType.REMOVED, "for chunk" + key.getChunkNum());
            }
        } 
        
    }
}
