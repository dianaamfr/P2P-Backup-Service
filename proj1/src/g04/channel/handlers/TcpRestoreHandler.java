package g04.channel.handlers;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import g04.Peer;
import g04.Utils;
import g04.channel.receivers.Message;
import g04.channel.receivers.TcpRestoreReceiver;
import g04.storage.Chunk;
import g04.storage.ChunkKey;

public class TcpRestoreHandler implements Runnable {

    private Peer peer;
    private TcpRestoreReceiver receiver;
    private Socket socket;

    public TcpRestoreHandler(Peer peer, TcpRestoreReceiver receiver, Socket socket) throws IOException{
        this.peer = peer;
        this.receiver = receiver;
        this.socket = socket;
    }

	@Override
    public void run() {
        Message message = new Message();

        // Read CHUNK message
		try {
            DataInputStream in = new DataInputStream(this.socket.getInputStream());
			byte[] bytes = in.readAllBytes();
            message = this.receiver.parseMessage(bytes);
            in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());

        // Receive CHUNK for initiator-peer
        if (this.peer.isPendingRestore(chunkKey.getFileId())) {
            System.out.println("Initiator: received CHUNK " +  message.getChunkNo());
            this.peer.addPendingChunk(new Chunk(chunkKey.getChunkNum(), chunkKey.getFileId(), message.getBody(), 0));
            
            // Restore file if all chunks have been restored
            if (this.peer.isReadyToRestore(chunkKey.getFileId())) {
                System.out.println("All chunks of a file ready");
                this.peer.getScheduler().execute(new RestoreHandler(this.peer, chunkKey.getFileId()));
            }
        }

        // Receive CHUNK for other peers
        if ((message.getSenderId() != Utils.PEER_ID) && this.peer.getStorage().hasStoredChunk(chunkKey)
                && this.peer.hasRestoreRequest(chunkKey)) {
            this.peer.removeRestoreRequest(chunkKey);
        }
		
	}
    
}
