package g04;

import java.io.File;
import java.nio.charset.StandardCharsets;
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

    public final static String generate_hash(String path) {

        File file = new File(path);

        StringBuilder builder = new StringBuilder();

        builder.append(file.getName());
        builder.append(file.lastModified());
        builder.append(file.getParent());

        MessageDigest digest;
        byte[] hash = null;
        
        try {
            digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No Such Hash Algorithm");
            return null;
        }

        return hash.toString();
    }
    
    public static void usage(String message) {
        System.err.println(message);
        System.err.println(
                "Usage: java Peer <protocol-version> <peer-id> <service-ap> <mc-address> <mc-port> <mdb-address> <mdb-port> <mdr-address> <mdr-port>");
    }
}
