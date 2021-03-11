package g04.channel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import g04.Utils;
import g04.storage.Chunk;

public class BackupChannel extends Channel {

    public BackupChannel(String address, int port) throws IOException {
        super(address, port);
    }
    
    public void putChunk(String protocolVersion, String senderId, Chunk chunk){
        byte[] message = super.generateMessage(
            protocolVersion, 
            "PUTCHUNK", 
            senderId, 
            chunk.getFileId(), 
            new String[]{Integer.toString(chunk.getChunkNum()), Integer.toString(chunk.getReplicationDegree()[0])}, 
            chunk.getBuffer());

        // System.out.println(new String(message, StandardCharsets.US_ASCII));
    }

}
