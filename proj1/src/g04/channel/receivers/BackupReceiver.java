package g04.channel.receivers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import g04.Peer;
import g04.Utils;
import g04.channel.handlers.PutChunkHandler;

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

            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);

            HashMap<String, String> message = this.parseMessage(received);

            // Receive Putchunk - don't store his own chunks
            if (message.get("MessageType").equals("PUTCHUNK")
                    && !message.get("SenderId").equals(Integer.toString(Utils.PEER_ID))) {
                    this.peer.getScheduler().schedule(new PutChunkHandler(this.peer, message), Utils.getRandomDelay(), TimeUnit.MILLISECONDS);              
            }
        }
    }

}
