package g04.channel.receivers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import g04.Peer;
import g04.Utils;

/**
 * An abstract class that represents a thread that receives the messages sent via a Multicast Channel.
 */
public abstract class MessageReceiver implements Runnable {

    protected Peer peer;

    public MessageReceiver(Peer peer) {
        this.peer = peer;
    }

    @Override
    public abstract void run();

    /**
     * Parses a protocol message
     * @param bytes the messages bytes
     * @param len the number of bytes of the message
     * @return the parsed message
     * @throws Exception if the message does not follow the correct format
     */
    public Message parseMessage(byte[] bytes, int len) throws Exception {
        
        // Parse Header

        // Find the end of the first header line
        int crlf = -1;
        for (int i = 0; i < len - 1; i++) {
            if (bytes[i] == Utils.CR && bytes[i + 1] == Utils.LF) {
                crlf = i;
                break;
            }
        }

        if (crlf == -1) {
            throw new Exception("Error parsing Message Header");
        }

        // Find the end of the header
        int lastCrlf = -1;
        for (int i = crlf; i < len - 3; i++) {
            if (bytes[i] == Utils.CR && bytes[i + 1] == Utils.LF
                    && bytes[i + 2] == Utils.CR && bytes[i + 3] == Utils.LF) {
                lastCrlf = i + 2;
                break;
            }
        }

        if (lastCrlf == -1) {
            throw new Exception("Error parsing Message Header");
        }

        String header = new String(bytes, 0, crlf, StandardCharsets.US_ASCII);
        String[] args = header.trim().split("\\s+"); // Handle multiple whitespaces
        
        if(args[1].equals("GETDELETE")){
            return new Message(args[0], args[1], Integer.parseInt(args[2]),"");
        }

        Message message = new Message(args[0], args[1], Integer.parseInt(args[2]), args[3]);

        // Parse message depending on its type
        if (!message.getMessageType().equals("DELETE")  && !message.getMessageType().equals("DELETED")) {
            message.setChunkNo(Integer.parseInt(args[4]));

            if (message.getMessageType().equals("PUTCHUNK")) {
         
                message.setReplicationDegree(Integer.parseInt(args[5]));
                
                message.setBody(Arrays.copyOfRange(bytes, lastCrlf + 2, len));

            } 
            else if (message.getMessageType().equals("CHUNK")) {
                
                message.setBody(Arrays.copyOfRange(bytes, lastCrlf + 2, len));
            
            } 
            else if (message.getMessageType().equals("GETCHUNK") && message.getVersion().equals("2.0")) {
                message.setTcpPort(Integer.parseInt(args[5]));
            }
        }

        return message;
    }

}