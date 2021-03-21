package g04.channel.handlers;

import java.io.IOException;
import java.util.TreeSet;

import g04.Peer;
import g04.storage.Chunk;

public class RestoreHandler implements Runnable {
    private Peer peer;
    private String fileId;
    private TreeSet<Chunk> chunks;

     public RestoreHandler(Peer peer, String fileId) {
        this.peer = peer;
        this.fileId = fileId;

        this.chunks = new TreeSet<Chunk>(
            this.peer.getRestoredChunks(this.fileId));
    }
    
    @Override
    public void run() {
        try {
            this.peer.removePendingRestore(this.fileId);
            System.out.println("Storing");    
            this.peer.getStorage().storeRestored(this.fileId, this.chunks);
        } catch (IOException e) {

            System.out.println(e.getMessage());
        }
    }
}
