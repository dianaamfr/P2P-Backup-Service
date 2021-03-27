package g04.channel.handlers;

import java.io.IOException;
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

        System.out.println("PUTCHUNK " + chunkKey.getChunkNum() + " try " + this.tries);
        System.out.println("Peer " + Utils.PEER_ID + " confirmed = " + this.peer.getStorage().getConfirmedBackups(chunkKey) + " desired = " + replicationDegree);
        
        // TODO - check if we can can use this instead - 
        //System.out.println("Peer " + Utils.PEER_ID + " confirmed2 = " + this.peer.getStorage().getConfirmedChunks(chunkKey) + " desired = " + replicationDegree);
        
        // TODO - check if we can use: this.peer.getStorage().getConfirmedChunks(chunkKey) to get the perceived rep degree
        // If we can, that would allow peers to execute the PUTCHUNK protocol after receiving REMOVED messages
        if(this.peer.getStorage().getConfirmedBackups(chunkKey) < replicationDegree && this.tries < Utils.MAX_TRIES) {
    
            // Send PutChunk message
            try {
				this.peer.getBackupChannel().getSocket().send(packet);
			} catch (IOException e) {
                System.err.println("Failed to send PUTCHUNK " + chunkKey.getChunkNum());
			}

            // Wait for confirmation
			this.peer.getScheduler().schedule(new BackupHandler(this.peer, this.packet, this.chunkKey, 
                this.replicationDegree, this.tries + 1, this.time * 2), this.time, TimeUnit.MILLISECONDS);
		}
    }
}
