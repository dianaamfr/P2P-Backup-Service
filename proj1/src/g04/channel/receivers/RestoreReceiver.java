import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import g04.Peer;
import g04.Utils;
import g04.storage.Chunk;
import g04.storage.ChunkKey;

public class RestoreReceiver extends MessageReceiver{

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

            // Receive Chunk
            if(this.peer.isPendingRestore(chunkKey.getFileId())){
                this.peer.addPendingChunk(new Chunk(chunkKey.getChunkNum(),chunkKey.getFileId(), message.get("Body").getBytes(), 0));
            }
        }
    }
}
