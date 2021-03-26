package g04.channel.handlers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Collections;

import g04.Peer;
import g04.Utils;
import g04.storage.ChunkKey;
import g04.storage.ReplicationDegreeComparable;
import g04.storage.Storage;

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

        for(ChunkKey key: this.storage.getStoredChunks().keySet()){
            chunks.add(new ReplicationDegreeComparable(key, this.storage.getConfirmedChunks().get(key).size()));
        }

        Collections.sort(chunks, (a,b) -> a.compare(b));

        while(this.storage.isFull()){
            try {
                DatagramPacket packet = this.peer.getControlChannel().getRemovedPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, chunks.get(0).getChunkKey());
                this.peer.getControlChannel().sendMessage(packet);
                
                ChunkKey key = chunks.get(0).getChunkKey();
                this.storage.decreaseCapacity(key.getSize());
                this.storage.getStoredChunks().remove(key);
                
                File file = new File(storage.getPath() + "/backup/file-" + key.getFileId() + "/chunk-" + key.getChunkNum() + ".ser");
            
                if(file.exists()) {
                    file.delete();
                }
                
                System.out.println("REMOVED CHUNK " +  key.getChunkNum() + " SIZE " + key.getSize() + " CAPACITY " + this.storage.getCapacity());
                chunks.remove(0);
            } catch (IOException e) {
                ChunkKey key = chunks.get(0).getChunkKey();
                System.out.println("Failed to send REMOVED for chunk " + key.getChunkNum());
            }
        } 
        
    }
}
