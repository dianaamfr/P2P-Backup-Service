package g04.channel.handlers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.ControlChannel;
import g04.storage.Storage;

/**
 * To ensure that, if a peer that backs up some chunks of the file is not running at the time 
 * the initiator send a DELETE message, the space used by the chunks will be reclaimed.
 * 
 * Sends DELETE messages if there are still pending confirmations regarding the 
 * deletion of a file.
 */
public class GetDeleteHandler implements Runnable {
    private Peer peer;
    private int senderId;

    public GetDeleteHandler(Peer peer, int senderId) {
        this.peer = peer;
        this.senderId = senderId;
    }

    @Override
    public void run() {
        Storage storage = peer.getStorage();
        ConcurrentHashMap<String, HashSet<Integer>> deletedFiles = storage.getDeletedFiles();

        ControlChannel controlChannel = this.peer.getControlChannel();

        for(String file : deletedFiles.keySet()){
            try {
                
                if(deletedFiles.get(file).contains(this.senderId)){
                    // Send DELETE message for each chunk of the file
                    DatagramPacket packet = controlChannel.deletePacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, file);
                    controlChannel.sendMessage(packet);
                    Utils.sendLog(Protocol.DELETE, MessageType.DELETE, "for the file " + file);
                }

			} catch (IOException e) {
                // Failed to send DELETE
				Utils.protocolError(Protocol.DELETE, MessageType.DELETE, "for the file " + file);
			}
        }
    }
}