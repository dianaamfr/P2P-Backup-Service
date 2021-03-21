package g04.channel.handlers;

import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.storage.ChunkKey;

public class BackupHandler implements Runnable {

    private Peer peer; 
    private DatagramPacket packet;
    private ChunkKey chunkKey;
    private int replicationDegree;
    
    private int tries;
    private int time;

    public BackupHandler(Peer peer, DatagramPacket packet, ChunkKey chunkKey, int replicationDegree, int tries, int time) {
        this.peer = peer;
        this.packet = packet;
        this.replicationDegree = replicationDegree;
        this.chunkKey = chunkKey;
        
        this.tries = tries;
        this.time = time;
    }

    public BackupHandler(Peer peer, DatagramPacket packet, ChunkKey chunkKey, int replicationDegree){
        this(peer, packet, chunkKey, replicationDegree, 0, Utils.WAIT_TIME);
    }

    @Override
    public void run() {
        
        if(this.peer.getStorage().getConfirmedBackups(chunkKey) < replicationDegree && this.tries < Utils.MAX_TRIES) {
    
            // Send PutChunk message
            System.out.println("PUTCHUNK " + chunkKey.getChunkNum() + " try " + this.tries);
            this.peer.getBackupChannel().sendMessage(packet);

            // Wait for confirmation
			this.peer.getScheduler().schedule(new BackupHandler(this.peer, this.packet, this.chunkKey, 
                this.replicationDegree, this.tries + 1, this.time * 2), this.time, TimeUnit.MILLISECONDS);
		}
    }
}
