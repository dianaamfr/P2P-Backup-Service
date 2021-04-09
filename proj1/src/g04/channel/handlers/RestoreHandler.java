package g04.channel.handlers;

import java.util.TreeSet;

import g04.Peer;
import g04.Utils;
import g04.Utils.Protocol;
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
            if(this.peer.removePendingRestore(this.fileId) != null){
                this.peer.getStorage().storeRestored(this.fileId, this.chunks, this.peer.getScheduler());
            }
        } catch (Exception e) {
            Utils.protocolError(Protocol.RESTORE, null, "failed to store file " + this.fileId);
        }
    }
}
