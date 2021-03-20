package g04.channel.handlers;

import java.io.IOException;
import java.util.TreeSet;

import g04.Peer;
import g04.storage.Chunk;

public class RestoreHandler implements Runnable {
    private Peer peer;
    private String fileId;
    private TreeSet<Chunk> chunks;

     public RestoreHandler(Peer peer, String fileId, TreeSet<Chunk> chunks) {
        this.peer = peer;
        this.fileId = fileId;
        this.chunks = chunks;
    }
    

    @Override
    public void run() {
        try {
            System.out.println("Storing");
            this.peer.getStorage().storeRestored(this.fileId, chunks);
        } catch (IOException e) {
        }
    }
}
