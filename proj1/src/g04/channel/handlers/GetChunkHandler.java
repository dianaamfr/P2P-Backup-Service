package g04.channel.handlers;

import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

import g04.Peer;
import g04.Utils;
import g04.channel.RestoreChannel;
import g04.storage.Chunk;
import g04.storage.ChunkKey;
import g04.storage.Storage;

public class GetChunkHandler implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;
    private int tcpPort = -1;
    private InetAddress address = null;

    public GetChunkHandler(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    public GetChunkHandler(Peer peer, ChunkKey chunkKey, int tcpPort, InetAddress address){
        this(peer,chunkKey);
        this.address = address;
        this.tcpPort = tcpPort;
    }

    @Override
    public void run() {

        // If the chunk was already restored exit
        if(!this.peer.hasRestoreRequest(this.chunkKey)){
            return;
        }
        
        Storage storage = this.peer.getStorage();
        Chunk chunk = new Chunk();
		try {
			chunk = storage.read(chunkKey.getFileId(),chunkKey.getChunkNum());

            // Send CHUNK message
            RestoreChannel restoreChannel = this.peer.getRestoreChannel();
            
            DatagramPacket packet = restoreChannel.chunkPacket(
                    Utils.PROTOCOL_VERSION, 
                    Utils.PEER_ID,
                    chunk);

            // Version 2.0: use TCP to send CHUNK
            if(Utils.PROTOCOL_VERSION.equals("2.0") && this.address != null && this.tcpPort >= 0){
                Socket clientSocket = new Socket(this.address, this.tcpPort);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(new String(packet.getData(), 0, packet.getLength() - 1));
                out.close();
                clientSocket.close();
            }
            // Version 1.0: use Restore Multicasr Channel to send CHUNK
            else{
                restoreChannel.sendMessage(packet);
            }

            this.peer.removeRestoreRequest(chunkKey);

            System.out.println("Peer " + Utils.PEER_ID + ": sent CHUNK " + chunk.getChunkNum());

		} catch (Exception e) {
            System.out.println("Failed to send CHUNK " + chunk.getChunkNum());
		}

    }
}
