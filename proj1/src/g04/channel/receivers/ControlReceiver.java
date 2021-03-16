package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import g04.Peer;
import g04.Utils;
import g04.storage.ChunkKey;
import g04.storage.Storage;

public class ControlReceiver extends MessageReceiver {

    public ControlReceiver(Peer peer) {
        super(peer);
    }

    @Override
	public void run() {
        
        while (true) {
            // Dividir em função comum
            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);

            try {
                this.peer.getControlChannel().getSocket().receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);

            HashMap<String, String> message = this.parseMessage(received);

            //printParsedMessage(message);

            ChunkKey chunkKey = new ChunkKey(message.get("FileId"),Integer.parseInt(message.get("ChunkNo")));

            // Add peer confirmation for a chunk I stored
            Storage storage = this.peer.getStorage();
            storage.addStoredConfirmation(chunkKey, Integer.parseInt(message.get("SenderId")));

            // Add peer confirmation for a chunk of a file I backed up
            if(storage.hasFile(message.get("FileId"))){
                storage.addBackupConfirmation(chunkKey, Integer.parseInt(message.get("SenderId")));
            }
            
        }
	}
    
}
