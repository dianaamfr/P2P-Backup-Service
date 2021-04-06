package g04.channel.handlers;

import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.Utils.Protocol;
import g04.storage.ChunkKey;

/**
 * Checks if the backup of a file is complete
 */
public class BackupNotifier implements Runnable {

    private Peer peer;
    private int tries;
    private int time;
    private String fileId;
    int replicationDegree;

    public BackupNotifier(Peer peer, int tries, int time, String fileId, int replicationDegree) {
        this.peer = peer;
        this.tries = tries;
        this.time = time;
        this.fileId = fileId;
        this.replicationDegree = replicationDegree;
    }

    public BackupNotifier(Peer peer, String fileId, int replicationDegree) {
        this.peer = peer;
        this.tries = 0;
        this.time = Utils.WAIT_TIME;
        this.fileId = fileId;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public void run() {

        boolean retry = false;

        for (ChunkKey key : this.peer.getStorage().getConfirmedChunks().keySet()) {
            if (key.getFileId().equals(this.fileId)
                    && this.peer.getStorage().getConfirmedChunks(key) < replicationDegree) {

                if (this.tries < Utils.MAX_TRIES) {
                    // Wait for confirmation
                    retry = true;
                    break;
                } else {
                    Utils.protocolError(Protocol.BACKUP, null, ": backup failed");
                    break;
                }
            }
        }

        if (retry) {
            this.peer.getScheduler().schedule(
                new BackupNotifier(this.peer, 
                    this.tries + 1, 
                    this.time * 2, 
                    this.fileId, 
                    this.replicationDegree), 
                    this.time,
                    TimeUnit.MILLISECONDS);
        } else {
            // Backup succeeded
            Utils.protocolLog(Protocol.BACKUP, ": backup succeeded for file " + fileId);
        }
    }

}
