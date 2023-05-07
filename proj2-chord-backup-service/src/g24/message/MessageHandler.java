
package g24.message;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.net.ssl.SSLSocket;

import g24.protocol.*;
import g24.storage.Storage;
import g24.*;

import java.util.Arrays;

public class MessageHandler {

    private ScheduledThreadPoolExecutor scheduler;
    private Chord chord;
    private Storage storage;

    public MessageHandler(ScheduledThreadPoolExecutor scheduler, Chord chord, Storage storage) {
        this.scheduler = scheduler;
        this.chord = chord;
        this.storage = storage;
    }

    public void handle(SSLSocket socket) throws IOException {
        this.scheduler.execute(this.parse(socket));
    }
    
    private Handler parse(SSLSocket socket) throws IOException {

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] response = new byte[Utils.FILE_SIZE + 200];
        byte[] aux = new byte[Utils.FILE_SIZE + 200];
        int bytesRead = 0;
        int counter = 0;

        int total = in.readInt();
        while(counter != total) {
            bytesRead = in.read(response);
            System.arraycopy(response, 0, aux, counter, bytesRead);
            counter += bytesRead;
        }

        byte[] result = new byte[counter];
        System.arraycopy(aux, 0, result, 0, counter);
        
        Handler handler = this.prepare(result);
        handler.setSocket(socket, out, in);

        return handler;
    }

    // Message: <MessageType> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
    private Handler prepare(byte[] message) {

        // Parse Header
        int i;  // Breakpoint index for header
        for (i = 0; i < message.length; i++) {
            if (i + 3 > message.length)
                return null;

            if (message[i] == Utils.CR && message[i + 1] == Utils.LF && message[i + 2] == Utils.CR && message[i + 3] == Utils.LF) {
                break;
            }
        }

        // Get body from the message
        byte[] body = Arrays.copyOfRange(message, i+4, message.length);

        // Get header from the message
        String header = new String(Arrays.copyOfRange(message, 0, i));  // Get Header from the message
        String[] splitHeader = header.trim().split("\\s+"); // Remove extra spaces and separate header component

        // System.out.println("RECEIVED: " + header);
        // System.out.println("--------------------------------");
        
        // Call the respective handler
        switch(splitHeader[0]) {
            case "BACKUP":
                return new Backup(splitHeader[1], Integer.parseInt(splitHeader[2]), body, this.storage);
            case "DELETE":
                return new Delete(splitHeader[1], this.storage);
            case "RESTORE":
                return new Restore(splitHeader[1], this.storage);
            case "ONLINE":
                return new Online();
            case "NOTIFY":
                return new Notify(splitHeader[1], splitHeader[2], Integer.parseInt(splitHeader[3]), this.chord);
            case "FINDSUCCESSOR":
                return new FindSuccessor(Integer.parseInt(splitHeader[1]), this.chord);
            case "GETPREDECESSOR":
                return new GetPredecessor(this.chord);
            case "GETKEYS":
                return new GetKeys(this.chord, this.storage, splitHeader[1], Integer.parseInt(splitHeader[2]));
            case "HASFILE":
                return new HasFile(this.storage, splitHeader[1]);
            default:
                System.err.println("Message is not recognized by the parser");
                break;
        }

        System.err.println("Parse failed");

        return null;
    }
}
