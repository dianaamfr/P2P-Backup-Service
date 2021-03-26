package g04.channel.handlers;

import java.io.IOException;
import java.net.DatagramPacket;

import g04.Peer;
import g04.Utils;
import g04.storage.Chunk;
import g04.storage.ChunkKey;

public class RemoveHandler implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;

    public RemoveHandler(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    @Override
    public void run() {

        // Check if no other peer has started the PUTCHUNK protocol for the removed
        // chunk
        if (!this.peer.hasRemovedChunk(this.chunkKey)) {
            return;
        }

        // Chunk chunk;
        // try {
        //     chunk = this.peer.getStorage().read(chunkKey.getFileId(), chunkKey.getChunkNum());

        //     DatagramPacket packet = this.peer.getBackupChannel().putChunkPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID,
        //             chunk);
        //     // Start PUTCHUNK protocol
        //     this.peer.getScheduler()
        //             .execute(new BackupHandler(this.peer, packet, this.chunkKey, this.chunkKey.getReplicationDegree()));
        // } catch (Exception e) {
        // }
    }
}
