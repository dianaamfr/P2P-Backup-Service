package g04.storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.ControlChannel;

/**
 * To ensure that, if a peer that backs up some chunks of the file is not running at the time 
 * the initiator send a DELETE message, the space used by the chunks will be reclaimed.
 * 
 * Sends DELETE messages periodically if there are still pending confirmations regarding the 
 * deletion of a file.
 */
public class AsyncDeleteUpdater implements Runnable {
    private Peer peer;

    public AsyncDeleteUpdater(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        Storage storage = peer.getStorage();
        ConcurrentHashMap<String, HashSet<Integer>> deletedFiles = storage.getDeletedFiles();

        ControlChannel controlChannel = this.peer.getControlChannel();

        for(String file : deletedFiles.keySet()){
            try {
                // Send DELETE message for each chunk of the file
                DatagramPacket packet = controlChannel.getDeletePacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, file);
				controlChannel.sendMessage(packet);
                Utils.sendLog(Protocol.DELETE, MessageType.DELETE, "for the file " + file);
                
			} catch (IOException e) {
                // Failed to send DELETE
				Utils.protocolError(Protocol.DELETE, MessageType.DELETE, "for the file " + file);
			}
        }
    }
}