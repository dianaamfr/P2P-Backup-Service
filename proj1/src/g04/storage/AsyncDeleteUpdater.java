package g04.storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import g04.Peer;
import g04.Utils;
import g04.channel.ControlChannel;

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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}