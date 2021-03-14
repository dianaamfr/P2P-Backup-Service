package g04.channel.message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import g04.Peer;
import g04.Utils;
import g04.storage.Chunk;
import g04.storage.Storage;

public class BackupReceiver extends MessageReceiver {

    public BackupReceiver(Peer peer) {
        super(peer);
    }

    @Override
    public void run() {
        System.out.println("Message Receiver of peer " + Utils.PEER_ID + " starting");

        while (true) {

            byte[] message = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(message, message.length);

            try {
                this.peer.getBackupChannel().getSocket().receive(packet);

            } catch (IOException e) {
                e.printStackTrace();
            }

            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);

            HashMap<String,String> parsed = this.parseMessage(received);

            if(parsed.get("MessageType").equals("PUTCHUNK") && !parsed.get("SenderId").equals(Integer.toString(Utils.PEER_ID))){
                Chunk chunk = new Chunk(
                    Integer.parseInt(parsed.get("ChunkNo")),
                    parsed.get("FileId"), 
                    parsed.get("Body").getBytes(),
                    Integer.parseInt(parsed.get("ReplicationDeg")));

                Storage storage = peer.getStorage();

                if(storage.getConfirmedStoredChunks(chunk.getChunkKey()) < 0){
                    try {
						storage.store(chunk);
                        storage.addChunk(chunk.getChunkKey());
					} catch (IOException e) {
                        // Failed at storing the chunk
					}
                }
            }
        }
    }

}
