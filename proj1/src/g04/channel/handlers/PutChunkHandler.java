package g04.channel.handlers;

import java.io.IOException;

import g04.Peer;
import g04.Utils;
import g04.channel.ControlChannel;
import g04.channel.receivers.Message;
import g04.storage.Chunk;
import g04.storage.Storage;

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

        // If it hasn't stored the chunk yet
        if (!storage.hasStoredChunk(chunk.getChunkKey())) {
            try {
                if (storage.hasCapacity(chunk.getChunkKey().getSize())) { // ver capacidade e rep degree
                    // Store the chunk
                    storage.store(chunk);
                    storage.addChunk(chunk.getChunkKey());
                } else {
                    stored = false;
                    System.out.println("NO CAPACITY TO STORE CHUNK " + chunk.getChunkNum());
                }

            } catch (IOException e) {
                // Failed at storing the chunk
                e.printStackTrace();
            }
        }

        if (stored) {
            // Send STORED message
            ControlChannel controlChannel = peer.getControlChannel();

            try {
                controlChannel.sendMessage(
                        controlChannel.storedPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, chunk.getChunkKey()));
            } catch (IOException e) {
                System.err.println("Failed to send STORED " + chunk.getChunkNum());
            }
        }

    }
}
