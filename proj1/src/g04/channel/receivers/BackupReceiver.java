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
    public void run() {

        while (true) {

            byte[] messageBytes = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);

            try {
                this.peer.getBackupChannel().getSocket().receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message message = this.parseMessage(packet);

            // Receive Putchunk - don't store his own chunks
            if (message.getMessageType().equals("PUTCHUNK") && (message.getSenderId() != Utils.PEER_ID)
                    && !this.peer.getStorage().hasFile(message.getFileId())) {

                ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNo());

                if (this.peer.hasRemovedChunk(chunkKey)) {
                    this.peer.deleteRemovedChunk(chunkKey);
                }

                this.peer.getScheduler().schedule(new PutChunkHandler(this.peer, message), Utils.getRandomDelay(),
                        TimeUnit.MILLISECONDS);
            }
        }
    }

}
