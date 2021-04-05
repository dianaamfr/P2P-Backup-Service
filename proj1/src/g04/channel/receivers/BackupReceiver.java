package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.channel.handlers.PutChunkHandler;
import g04.storage.ChunkKey;

public class BackupReceiver extends MessageReceiver {

    public BackupReceiver(Peer peer) {
        super(peer);
    }

    @Override
    /**
     * Listens to PUTCHUNK messages sent to the Backup Multicast Channel
     */
    public void run() {

        while (true) {

            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            Message message = new Message();

            // TODO: decide how to handle this exceptions
            try {
                this.peer.getBackupChannel().getSocket().receive(packet);
                message = this.parseMessage(packet.getData(), packet.getLength());
            } catch (IOException e) {
                Utils.error("I/O exception when receiving messages in the MDB");
			} catch (Exception e) {
				Utils.error(e.getMessage());
			}
            
            // Receive PUTCHUNK from other peers - don't store his own chunks
            if (message.getMessageType().equals("PUTCHUNK") && (message.getSenderId() != Utils.PEER_ID)
                    && !this.peer.getStorage().hasFile(message.getFileId())) {

                ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());
                
                // Other peer has started a PUTCHUNK protocol for a removed chunk and this peer 
                // was also going to start one
                if (this.peer.hasRemovedChunk(chunkKey)) {
                    // Avoid starting yet another backup subprotocol
                    this.peer.deleteRemovedChunk(chunkKey);
                }

                // Wait a random delay before sending the STORED confirmation
                this.peer.getScheduler().schedule(new PutChunkHandler(this.peer, message), Utils.getRandomDelay(),
                        TimeUnit.MILLISECONDS);
            }
        }
    }

}
