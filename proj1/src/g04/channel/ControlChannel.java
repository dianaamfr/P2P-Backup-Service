package g04.channel;

import java.io.IOException;

public class ControlChannel extends Channel {

    public ControlChannel(String address, int port) throws IOException {
        super(address, port);
    }
    
}
