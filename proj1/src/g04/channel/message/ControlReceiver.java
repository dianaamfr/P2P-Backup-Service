package g04.channel.message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import g04.Peer;
import g04.Utils;

public class ControlReceiver extends MessageReceiver {

    public ControlReceiver(Peer peer) {
        super(peer);
    }

    @Override
	public void run() {
        
        System.out.println("Control Receiver of peer " + Utils.PEER_ID + " starting");

        while (true) {

            byte[] message = new byte[Utils.PACKET_SIZE];

            DatagramPacket packet = new DatagramPacket(message, message.length);

            try {
                this.peer.getControlChannel().getSocket().receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);

            HashMap<String, String> parsed = this.parseMessage(received);

            // printParsedMessage(parsed);

        }
	}
    
}
