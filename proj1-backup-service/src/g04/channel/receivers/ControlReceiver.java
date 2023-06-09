package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.handlers.DeleteHandler;
import g04.channel.handlers.GetChunkHandler;
import g04.channel.handlers.GetDeleteHandler;
import g04.channel.handlers.RemoveHandler;
import g04.storage.ChunkKey;
import g04.storage.Storage;

/**
 * Listens to messages sent to the Control Multicast Channel
 */
public class ControlReceiver extends MessageReceiver {

    public ControlReceiver(Peer peer) {
        super(peer);
    }

    @Override
    public void run() {

        while (true) {

            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            Message message = new Message();

            try {
                this.peer.getControlChannel().getSocket().receive(packet);
                message = this.parseMessage(packet.getData(), packet.getLength());
            } catch (IOException e) {
                Utils.error("I/O exception when receiving messages in the MC");
            } catch (Exception e) {
                Utils.error(e.getMessage());
            }

            Storage storage = this.peer.getStorage();

            ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());

            // Process received message
            switch (message.getMessageType()) {

                case "STORED":

                    Utils.receiveLog(Protocol.BACKUP, MessageType.STORED, message.getSenderId(), 
                        "for chunk" + chunkKey.getChunkNum());
                        
                    // Add peer confirmation for a chunk
                    storage.addStoredConfirmation(chunkKey, message.getSenderId());

                    break;

                case "GETCHUNK":
                    // Check if the peer has the requested chunk
                    if ((message.getSenderId() != Utils.PEER_ID) && storage.hasStoredChunk(chunkKey)) {
                        this.peer.addRestoreRequest(chunkKey);

                        // Version 2.0: Schedule task to send CHUNK using TCP (requires TCP port and address)
                        if(Utils.PROTOCOL_VERSION.equals("2.0") && message.getVersion().equals("2.0")){
                            this.peer.getScheduler().schedule(new GetChunkHandler(this.peer, chunkKey, message.getTcpPort(), packet.getAddress()), Utils.getRandomDelay(),
                            TimeUnit.MILLISECONDS);
                        }
                        // Version 1.0: Schedule task to send CHUNK using the Restore Multicast Channel
                        else{
                            this.peer.getScheduler().schedule(new GetChunkHandler(this.peer, chunkKey), Utils.getRandomDelay(),
                            TimeUnit.MILLISECONDS);
                        }

                    }
                    break;

                case "DELETE":

                    Utils.receiveLog(Protocol.DELETE, MessageType.DELETE, message.getSenderId(), 
                        "for file" + message.getFileId());

                    this.peer.getScheduler().execute(new DeleteHandler(this.peer, message.getFileId(), message.getSenderId()));
                    break;

                case "REMOVED":
                    if ((message.getSenderId() != Utils.PEER_ID)) {
                        // Decrease the perceived replication degree of the chunk
                        int confirmations = storage.removeStoredConfirmation(chunkKey, message.getSenderId());

                        if (storage.hasStoredChunk(chunkKey)) {
                            int desiredReplicationDegree = storage.getStoredChunks().get(chunkKey);

                            // Check if the perceived replication degree dropped below the desired
                            if (confirmations < desiredReplicationDegree) {
                                // To avoid to peers starting a PUTCHUNK protocol
                                this.peer.addRemovedChunk(chunkKey);

                                chunkKey.setReplicationDegree(desiredReplicationDegree);

                                // Start PUTCHUNK protocol to ensure the desired replication degree
                                this.peer.getScheduler().schedule(new RemoveHandler(this.peer, chunkKey),
                                        Utils.getRandomDelay(), TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                    break;

                case "DELETED":

                    // Version 2.0: Listen to DELETED confirmations
                    if (Utils.PROTOCOL_VERSION.equals("2.0") && message.getVersion().equals("2.0") &&
                    (message.getSenderId() != Utils.PEER_ID)) {

                        Utils.receiveLog(Protocol.DELETE, MessageType.DELETED, message.getSenderId(), "for file " + message.getFileId());

                        // Confirm deletion of a file from a peer
                        storage.removePendingDeletion(message.getFileId(), message.getSenderId());
                    }
                    break;

                case "GETDELETE":
                    if (Utils.PROTOCOL_VERSION.equals("2.0") && message.getVersion().equals("2.0")){
                        Utils.receiveLog(Protocol.DELETE, MessageType.GETDELETE, message.getSenderId(),"");
                        this.peer.getScheduler().execute(new GetDeleteHandler(this.peer, message.getSenderId()));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
