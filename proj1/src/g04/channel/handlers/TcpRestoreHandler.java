package g04.channel.handlers;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.receivers.Message;
import g04.channel.receivers.TcpRestoreReceiver;
import g04.storage.Chunk;
import g04.storage.ChunkKey;

public class TcpRestoreHandler implements Runnable {

    private Peer peer;
    private TcpRestoreReceiver receiver;
    private Socket socket;

    public TcpRestoreHandler(Peer peer, TcpRestoreReceiver receiver, Socket socket){
        this.peer = peer;
        this.receiver = receiver;
        this.socket = socket;
    }

	@Override
    public void run() {
        Message message = new Message();

        // Read CHUNK message from TCP socket
		try {
            DataInputStream in = new DataInputStream(this.socket.getInputStream());
			byte[] bytes = in.readAllBytes();
            message = this.receiver.parseMessage(bytes, bytes.length);
            in.close();
		} catch (IOException e) {
			Utils.error("I/O exception when receiving messages in the TCP socket");
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
                    //Utils.protocolLog(Protocol.RESTORE, "has file " + chunkKey.getFileId() + " ready to restore");
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
