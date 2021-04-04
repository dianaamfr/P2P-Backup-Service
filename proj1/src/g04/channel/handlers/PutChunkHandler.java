package g04.channel.handlers;

import java.io.IOException;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.ControlChannel;
import g04.channel.receivers.Message;
import g04.storage.Chunk;
import g04.storage.Storage;

/**
 * Stores the chunk if it isn't already stored and there is space for it (version 1.0). 
 * In version 2.0 it also checks if the desired replication degree of the file was already achieved, and
 * if it was, it does not store it.
 * If the file was stored now or previously, the peer sends a STORED confirmation in the Control Channel
 */
public class PutChunkHandler implements Runnable {

    private Peer peer;
    private Message message;

    public PutChunkHandler(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {

        Chunk chunk = new Chunk(this.message.getChunkNo(), this.message.getFileId(), this.message.getBody(),
                this.message.getReplicationDegree());

        Storage storage = this.peer.getStorage();

        boolean stored = true;

        // Check if the chunk was already stored
        if (!storage.hasStoredChunk(chunk.getChunkKey())) {
            try {
                // Check if there is free capacity
                if (storage.hasCapacity(chunk.getChunkKey().getSize())) {
                    // In version 2.0, check if the chunk has the desired replication degree
                    if(this.message.getVersion().equals("2.0") && (storage.getConfirmedChunks(chunk.getChunkKey()) >= chunk.getReplicationDegree())){
                        return;
                    }

                    // Store the chunk
                    storage.store(chunk);
                    storage.addChunk(chunk.getChunkKey());
                } else {
                    stored = false;
                    Utils.protocolError(Protocol.BACKUP, MessageType.PUTCHUNK, "no capacity to store chunk" + chunk.getChunkNum());
                }

            } catch (IOException e) {
                stored = false;
                Utils.protocolError(Protocol.BACKUP, null, "failed to store chunk" + chunk.getChunkNum());
            }
        }

        if (stored) {
            // Send STORED message
            ControlChannel controlChannel = peer.getControlChannel();

            try {
                controlChannel.sendMessage(
                        controlChannel.storedPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, chunk.getChunkKey()));
            } catch (IOException e) {
                Utils.protocolError(Protocol.BACKUP, MessageType.STORED, "for chunk" + chunk.getChunkNum());
            }
        }

    }
}
