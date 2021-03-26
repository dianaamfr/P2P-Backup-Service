package g04.channel.receivers;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import g04.Peer;
import g04.Utils;

public abstract class MessageReceiver implements Runnable {

    protected Peer peer;

    public MessageReceiver(Peer peer) {
        this.peer = peer;
    }

    @Override
    public abstract void run();

    public Message parseMessage(DatagramPacket packet) {
        
        // Parse Header

        // Find the end of the first header line
        int crlf = -1;
        for (int i = 0; i < packet.getLength() - 1; i++) {
            if (packet.getData()[i] == Utils.CR && packet.getData()[i + 1] == Utils.LF) {
                crlf = i;
                break;
            }
        }

        if (crlf == -1) {
            // TODO: Throw error message if crlf was not found
            System.err.println("Error reading header");
        }

        // Find the end of the header
        int lastCrlf = -1;
        for (int i = crlf; i < packet.getLength() - 3; i++) {
            if (packet.getData()[i] == Utils.CR && packet.getData()[i + 1] == Utils.LF
                    && packet.getData()[i + 2] == Utils.CR && packet.getData()[i + 3] == Utils.LF) {
                lastCrlf = i + 2;
                break;
            }
        }

        if (lastCrlf == -1) {
            // TODO: Throw error message
            System.err.println("Error parsing header");
        }

        String header = new String(packet.getData(), 0, crlf, StandardCharsets.US_ASCII);
        String[] args = header.trim().split("\\s+"); // Handle multiple whitespaces

        Message message = new Message(args[0], args[1], Integer.parseInt(args[2]), args[3]);

        if (!message.getMessageType().equals("DELETE")) {
            message.setChunkNo(Integer.parseInt(args[4]));

            if (message.getMessageType().equals("PUTCHUNK")) {
         
                message.setReplicationDegree(Integer.parseInt(args[5]));

                if (lastCrlf + 2 >= packet.getLength()) {
                    // TODO: Throw error message
                    System.err.println("Missing Body");
                }
                message.setBody(Arrays.copyOfRange(packet.getData(), lastCrlf + 2, packet.getLength()));

            } else if (message.getMessageType().equals("CHUNK")) {
                
                if (lastCrlf + 2 >= packet.getLength()) {
                    // TODO: Throw error message
                    System.err.println("Missing Body");
                }
                message.setBody(Arrays.copyOfRange(packet.getData(), lastCrlf + 2, packet.getLength()));
            }

        }

        return message;
    }

}