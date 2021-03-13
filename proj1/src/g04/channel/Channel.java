package g04.channel;

import g04.Peer;
import g04.Utils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class Channel {

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
            String[] optional, byte[] body) {
        StringBuilder builder = new StringBuilder();

        builder.append(protocolVersion);
        builder.append(" ");
        builder.append(operation);
        builder.append(" ");
        builder.append(senderId);
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

        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);

        return message;
    }

    public void sendMessage(DatagramPacket packet) {
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(Peer peer){
        this.messageReceiver = new MessageReceiver(peer);
        peer.getScheduler().execute(this.messageReceiver);
    }

}
