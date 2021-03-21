package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.TreeSet;

import g04.Peer;
import g04.Utils;
import g04.channel.handlers.RestoreHandler;
import g04.storage.Chunk;
import g04.storage.ChunkKey;

public class RestoreReceiver extends MessageReceiver {

    public RestoreReceiver(Peer peer) {
        super(peer);
    }

    @Override
    public void run() {
   
        while (true) {
 
            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            
            try {
                this.peer.getRestoreChannel().getSocket().receive(packet);

            } catch (IOException e) {
                e.printStackTrace();
            }

            Message message = this.parseMessage(packet);

            ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());

            // Receive CHUNK for initiator-peer
            if (this.peer.isPendingRestore(chunkKey.getFileId())) {
                System.out.println("Initiator: received CHUNK " +  message.getChunkNo());
                this.peer
                        .addPendingChunk(new Chunk(chunkKey.getChunkNum(), chunkKey.getFileId(), message.getBody(), 0));

                // Restore file if all chunks have been restored
                if (this.peer.isReadyToRestore(chunkKey.getFileId())) {
                    System.out.println("All chunks of a file ready");
                    this.peer.getScheduler().execute(new RestoreHandler(this.peer, chunkKey.getFileId()));
                }
            }

            // Receive CHUNK for other peers
            if ((message.getSenderId() != Utils.PEER_ID) && this.peer.getStorage().hasStoredChunk(chunkKey)
                    && this.peer.hasRestoreRequest(chunkKey)) {
                this.peer.removeRestoreRequest(chunkKey);
            }
        }
    }
}
