package g04.channel;

import java.io.IOException;

public class BackupChannel extends Channel {

    public BackupChannel(String address, int port) throws IOException {
        super(address, port);
    }
    
}
