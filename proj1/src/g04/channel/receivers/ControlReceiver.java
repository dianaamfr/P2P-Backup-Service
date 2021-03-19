package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.channel.handlers.GetChunkHandler;
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

            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);

            HashMap<String, String> message = this.parseMessage(received);

            ChunkKey chunkKey = new ChunkKey(message.get("FileId"),Integer.parseInt(message.get("ChunkNo")));

            switch (message.get("MessageType")) {
                case "STORED":
                    
                    // Add peer confirmation for a chunk
                    Storage storage = this.peer.getStorage();
                    storage.addStoredConfirmation(chunkKey, Integer.parseInt(message.get("SenderId")));

                    // Add peer confirmation for a chunk of a file I backed up
                    if(storage.hasFile(message.get("FileId"))){
                        storage.addBackupConfirmation(chunkKey, Integer.parseInt(message.get("SenderId")));
                    }

                    break;
                    
                case "GETCHUNK":
                    
                    if(!message.get("SenderId").equals(Integer.toString(Utils.PEER_ID))){
                        this.peer.getScheduler().schedule(new GetChunkHandler(this.peer, message), Utils.getRandomDelay(), TimeUnit.MILLISECONDS);              
                    }

                default:
                    break;
            }
            
        }
	}
    
}
