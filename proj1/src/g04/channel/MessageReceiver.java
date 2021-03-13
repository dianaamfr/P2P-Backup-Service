package g04.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

import g04.Peer;
import g04.Utils;

public class MessageReceiver implements Runnable {
    
    Peer peer;

    public MessageReceiver(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {

        while (true) {

            byte[] message = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(message, message.length);

            try {
                this.peer.getBackupChannel().socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);
            System.out.println(received.substring(0, 128));
        }
    }
}