package g04.channel.handlers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.storage.ChunkKey;

/**
 * Sends a PUTCHUNK message for a specific chunk, if the desired replication degree was
 * not reached and the maximum number of tries was not exceeded
 */
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

        // Send PUTCHUNK if the desired replication degree was not yet reached
        if (this.peer.getStorage().getConfirmedChunks(chunkKey) < replicationDegree) {

            if (this.tries < Utils.MAX_TRIES) {

                // Send PUTCHUNK message
                try {
                    this.peer.getBackupChannel().getSocket().send(packet);
                    Utils.sendLog(Protocol.BACKUP, MessageType.PUTCHUNK, chunkKey.getChunkNum() + " try " + this.tries);
                } catch (IOException e) {
                    Utils.protocolError(Protocol.BACKUP, MessageType.PUTCHUNK,"for chunk" + chunkKey.getChunkNum());
                }

                // Wait for confirmation
                this.peer.getScheduler().schedule(new BackupHandler(this.peer, this.packet, this.chunkKey,
                        this.replicationDegree, this.tries + 1, this.time * 2), this.time, TimeUnit.MILLISECONDS);
            } else {
                Utils.protocolError(Protocol.BACKUP, MessageType.PUTCHUNK, ": maximum tries exceeded");
            }
        }
    }
}
