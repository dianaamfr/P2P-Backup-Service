package g04;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    public static String PROTOCOL_VERSION;
    public static String PEER_ID;
    public static Peer PEER;

    public final static String CRLF = "\r\n";
    
    public final static int CHUNK_SIZE = 64000;
    public final static int MAX_CHUNKS = 1000000;
    public final static long MAX_FILE = 64000000000l;

    public final static int MAX_TRIES = 5;
    public final static int WAIT_TIME = 1000;
    public final static int MIN_DELAY = 0;
    public final static int MAX_DELAY = 401;

    public enum MESSAGE_TYPE {
        PUTCHUNK,
        STORED,
        GETCHUNK,
        CHUNK,
        DELETE,
        REMOVED
    }

    public final static int getRandomDelay(){
        return ThreadLocalRandom.current().nextInt(MIN_DELAY, MAX_DELAY);
    }

    public final static String generateHash(String fileName) throws IOException, NoSuchAlgorithmException {

        StringBuilder builder = new StringBuilder();
        File file = new File(fileName);
        
        // Get name, modification date and owner of the file
        builder.append(file.getName());
        builder.append(file.lastModified());
        builder.append(Files.getOwner(file.toPath()));

        MessageDigest digest;
        byte[] hash = null;
        
        digest = MessageDigest.getInstance("SHA-256");
        hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));

        return hash.toString();
    }
    
    public static void usage(String message) {
        System.err.println(message);
        System.err.println(
                "Usage: java Peer <protocol-version> <peer-id> <service-ap> <mc-address> <mc-port> <mdb-address> <mdb-port> <mdr-address> <mdr-port>");
    }
}
