package g24;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.BufferedWriter;
import java.io.FileWriter;


public class Utils {

    public static final int m = 4;
    public static final byte CR = 0xD, LF = 0xA;  // ASCII codes for <CRLF>
    public static final String CRLF = "\r\n";
    public static final int FILE_SIZE = 8*1024*1024; // File Maximum Size: 8 Mb
    public static final long MAX_STORAGE = 2000000000l;; // Default storage size: 2 GB 
    public static String peerAp;

    public static final String[] CYPHER_SUITES =  new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA"};

    public static final int generateHash(String key) throws NoSuchAlgorithmException {
        
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.reset();
        md.update(key.getBytes(StandardCharsets.UTF_8));

        String sha1 = String.format("%040x", new BigInteger(1, md.digest()));

        int hashCode = sha1.hashCode();

        if(hashCode < 0)
            hashCode = -hashCode;
        return hashCode % ((int) Math.pow(2, Utils.m));
    }

    public final static String generateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        StringBuilder builder = new StringBuilder();
        builder.append(file.getName());
        MessageDigest digest;
        byte[] hash = null;
        
        digest = MessageDigest.getInstance("SHA-256");
        hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));

        return bytesToHex(hash);
    }

    public final static String generateFileHash(String filename) throws IOException, NoSuchAlgorithmException {
        StringBuilder builder = new StringBuilder();
        builder.append(filename);
        MessageDigest digest;
        byte[] hash = null;
        
        digest = MessageDigest.getInstance("SHA-256");
        hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));

        return bytesToHex(hash);
    }

    public static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

    public static synchronized void log(String head, String body) {
        try {
            File oFile = new File(Utils.peerAp + ".log");
            if (!oFile.exists()) {
                oFile.createNewFile();
            }
            if (oFile.canWrite()) {
                BufferedWriter oWriter = new BufferedWriter(new FileWriter(Utils.peerAp + ".log", true));
                oWriter.write(String.format("%-20s | %-40s\n", head, body));
                oWriter.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void out(String head, String body) {
        System.out.printf("%-20s | %-40s\n", head, body);
    }

}
