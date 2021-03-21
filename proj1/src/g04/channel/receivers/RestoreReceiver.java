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

            
            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);
            
            HashMap<String, String> message = this.parseMessage(received);


            ChunkKey chunkKey = new ChunkKey(message.get("FileId"), Integer.parseInt(message.get("ChunkNo")));
        
            // Receive CHUNK for initiator-peer
            if (this.peer.isPendingRestore(chunkKey.getFileId())) {
                System.out.println("Initiator Peer received the requested chunk");
                this.peer.addPendingChunk(
                        new Chunk(chunkKey.getChunkNum(), chunkKey.getFileId(), message.get("Body").getBytes(), 0));

                // Restore file if all chunks have been restored
                if (this.peer.isReadyToRestore(chunkKey.getFileId())) {
                    System.out.println("All chunks of a file ready");
                    this.peer.getScheduler()
                            .execute(new RestoreHandler(this.peer, chunkKey.getFileId()));
                }
            }
            
            // Receive CHUNK for other peers
            if (!message.get("SenderId").equals(Integer.toString(Utils.PEER_ID))
                    && this.peer.getStorage().hasStoredChunk(chunkKey) && this.peer.hasRestoreRequest(chunkKey)) {
                System.out.println("Peer " + Utils.PEER_ID + " listened chunk from peer " + message.get("SenderId") + ". Remove chunk from restore requests!");        
                this.peer.removeRestoreRequest(chunkKey);     
            }
        }
    }
}
