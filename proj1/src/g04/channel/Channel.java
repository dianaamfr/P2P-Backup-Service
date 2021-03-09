package g04.channel;

import g04.Utils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel {

    private final InetAddress address;
    private final int port;

    protected final MulticastSocket socket;

    public Channel(String address, int port) throws IOException {

        this.address = InetAddress.getByName(address);
        this.port = port;

        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.address);

        // byte[] sbuf = (Utils.PEER_ID).getBytes();
        // DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, this.address, this.port);
        // this.socket.send(packet);
    }

}
