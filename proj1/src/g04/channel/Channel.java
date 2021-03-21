package g04.channel;

import g04.Peer;
import g04.Utils;
import g04.channel.receivers.MessageReceiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public abstract class Channel {

    protected final InetAddress address;
    protected final int port;

    protected final MulticastSocket socket;
    protected MessageReceiver messageReceiver;

    public Channel(String address, int port) throws IOException {

        this.address = InetAddress.getByName(address);
        this.port = port;

        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.address);

    }

    public byte[] generateMessage(String protocolVersion, String operation, int senderId, String fileId,
            String[] optional) {
        StringBuilder builder = new StringBuilder();

        builder.append(protocolVersion);
        builder.append(" ");
        builder.append(operation);
        builder.append(" ");
        builder.append(Integer.toString(senderId));
        builder.append(" ");
        builder.append(fileId);
        for (String op : optional) {
            builder.append(" ");
            builder.append(op);
        }
        builder.append(" ");
        builder.append(Utils.CRLF);
        builder.append(Utils.CRLF);

        byte[] header = builder.toString().getBytes(StandardCharsets.US_ASCII);

        return header;
    }

    public byte[] generateMessage(String protocolVersion, String operation, int senderId, String fileId,
            String[] optional, byte[] body) {
                
        byte[] header = this.generateMessage(protocolVersion, operation, senderId, fileId, optional);
        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);

        return message;
    }

    public void sendMessage(DatagramPacket packet) {
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            System.err.println("Failed to send packet");
        }
    }

    public abstract void run(Peer peer);

    public MulticastSocket getSocket(){
        return this.socket;
    }
}
