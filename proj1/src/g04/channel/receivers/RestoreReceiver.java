package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.handlers.RestoreHandler;
import g04.storage.Chunk;
import g04.storage.ChunkKey;

/**
 * Listens to messages sent to the Restore Multicast Channel
 */
public class RestoreReceiver extends MessageReceiver {

    public RestoreReceiver(Peer peer) {
        super(peer);
    }

    @Override
    public void run() {
   
        while (true) {
 
            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            Message message = new Message();

            // TODO: decide how to handle this exceptions
            try {
                this.peer.getRestoreChannel().getSocket().receive(packet);
                message = this.parseMessage(packet.getData(),packet.getLength());

            } catch (IOException e) {
                Utils.error("I/O exception when receiving messages in the MDR");
            } catch (Exception e) {
                Utils.error(e.getMessage());
            }
 

            ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());

            if(message.getMessageType().equals("CHUNK")){
                // Initiator-peer: receive CHUNK
                if (this.peer.isPendingRestore(chunkKey.getFileId())) {
                    Utils.receiveLog(Protocol.RESTORE, MessageType.CHUNK, message.getSenderId(), Integer.toString(message.getChunkNo()));
                    
                    this.peer.addPendingChunk(new Chunk(chunkKey.getChunkNum(), chunkKey.getFileId(), message.getBody(), 0));

                    // Restore file if all chunks have been restored
                    if (this.peer.isReadyToRestore(chunkKey.getFileId())) {
                        Utils.protocolLog(Protocol.RESTORE, "has file " + chunkKey.getFileId() + " ready to restore");
                        this.peer.getScheduler().execute(new RestoreHandler(this.peer, chunkKey.getFileId()));
                    }
                }

                // Other peers: listen to CHUNK messages sent by other, to avoid flooding the host
                if ((message.getSenderId() != Utils.PEER_ID) && this.peer.getStorage().hasStoredChunk(chunkKey)
                        && this.peer.hasRestoreRequest(chunkKey)) {
                    this.peer.removeRestoreRequest(chunkKey);
                }
            }
        }
    }
}
