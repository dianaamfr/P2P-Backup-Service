package g04.channel.handlers;

import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.RestoreChannel;
import g04.storage.Chunk;
import g04.storage.ChunkKey;
import g04.storage.Storage;

/**
 * Send CHUNK message using the Multicast Restore Channel, in version 1.0, or TCP, in version 2.0 
 */
public class GetChunkHandler implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;
    private int tcpPort = -1;
    private InetAddress address = null;

    /**
     * Constructor for version 1.0 (uses Multicast Restore Channel)
     * @param peer
     * @param chunkKey
     */
    public GetChunkHandler(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    /**
     * Constructor for version 2.0 (uses TCP)
     * @param peer
     * @param chunkKey
     * @param tcpPort
     * @param address
     */
    public GetChunkHandler(Peer peer, ChunkKey chunkKey, int tcpPort, InetAddress address){
        this(peer,chunkKey);
        this.address = address;
        this.tcpPort = tcpPort;
    }

    @Override
    public void run() {

        // Check if the chunk was already restored by other peer. Exit if it was
        if(!this.peer.hasRestoreRequest(this.chunkKey)){
            return;
        }
        
        Storage storage = this.peer.getStorage();
        Chunk chunk = new Chunk();
		try {
            // Read the chunk from non-volatile memory
			chunk = storage.read(chunkKey.getFileId(),chunkKey.getChunkNum());

            RestoreChannel restoreChannel = this.peer.getRestoreChannel();
            
            DatagramPacket packet = restoreChannel.chunkPacket(
                    Utils.PROTOCOL_VERSION, 
                    Utils.PEER_ID,
                    chunk);

            // Version 2.0: use TCP to send CHUNK
            if(Utils.PROTOCOL_VERSION.equals("2.0") && (this.address != null) && (this.tcpPort >= 0)){
                Socket socket = new Socket(this.address, this.tcpPort);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.write(packet.getData());
                out.close();
                socket.close();
            }
            // Version 1.0: use Restore Multicast Channel to send CHUNK
            else{
                restoreChannel.sendMessage(packet);
            }

            Utils.sendLog(Protocol.RESTORE, MessageType.CHUNK, "for chunk" + chunk.getChunkNum());

            // Remove the chunk from the ones that are waiting to be restored
            this.peer.removeRestoreRequest(chunkKey);

		} catch (Exception e) {
            Utils.protocolError(Protocol.RESTORE, MessageType.CHUNK, "for chunk" + chunk.getChunkNum());
		}

    }
}
