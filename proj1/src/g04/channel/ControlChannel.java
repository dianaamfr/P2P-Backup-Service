package g04.channel;

import java.io.IOException;

import g04.Peer;

public class ControlChannel extends Channel {

    public ControlChannel(String address, int port) throws IOException {
        super(address, port);
    }

    @Override
    public void run(Peer peer) {
        // TODO Auto-generated method stub
        
    }
    
}
