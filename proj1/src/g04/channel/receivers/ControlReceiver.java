package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.channel.handlers.DeleteHandler;
import g04.channel.handlers.GetChunkHandler;
import g04.channel.handlers.RemoveHandler;
import g04.storage.ChunkKey;
import g04.storage.Storage;

public class ControlReceiver extends MessageReceiver {

    public ControlReceiver(Peer peer) {
        super(peer);
    }

    @Override
	public void run() {
        
        while (true) {
            
            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);

            try {
                this.peer.getControlChannel().getSocket().receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message message = this.parseMessage(packet);
            Storage storage = this.peer.getStorage();

            ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());

            switch (message.getMessageType()) {
                case "STORED":

                    // Add peer confirmation for a chunk
                    storage.addStoredConfirmation(chunkKey, message.getSenderId());

                    // System.out.println(message.getFileId());
                    // Add peer confirmation for a chunk of a file I backed up (initiator-peer)
                    if(storage.hasFile(message.getFileId())){
                        storage.addBackupConfirmation(chunkKey, message.getSenderId());
                        System.out.println("RECEIVED STORE CONFIRMATION FROM " + message.getSenderId() + " FOR CHUNK " + message.getChunkNo() + " BC " + storage.getConfirmedBackups(chunkKey));
                    }

                    break;
                    
                case "GETCHUNK":
                    if((message.getSenderId() != Utils.PEER_ID) && storage.hasStoredChunk(chunkKey)){
                        this.peer.addRestoreRequest(chunkKey);
                        this.peer.getScheduler().schedule(new GetChunkHandler(this.peer, chunkKey), Utils.getRandomDelay(), TimeUnit.MILLISECONDS);              
                    }
                    break;

                case "DELETE":
                    this.peer.getScheduler().execute(new DeleteHandler(this.peer, message.getFileId(), message.getSenderId()));
                    break;

                case "REMOVED":

                    int confirmations = storage.removeStoredConfirmation(chunkKey, message.getSenderId());

                    int replicationDegree  = storage.getStoredChunks().get(chunkKey);

                    if(storage.hasStoredChunk(chunkKey) && confirmations < replicationDegree){
                        this.peer.addRemovedChunk(chunkKey);

                        chunkKey.setReplicationDegree(replicationDegree);

                        // Start PUTCHUNK protocol to ensure the desired replication degree
                        this.peer.getScheduler().schedule(new RemoveHandler(this.peer, chunkKey), Utils.getRandomDelay(), TimeUnit.MILLISECONDS);    
                    }
                    
                    break;
                default:
                    break;
            }
            
        }
	}
    
}
