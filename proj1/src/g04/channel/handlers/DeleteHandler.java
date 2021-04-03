package g04.channel.handlers;

import java.io.File;
import java.io.IOException;

import g04.Peer;
import g04.Utils;
import g04.channel.ControlChannel;
import g04.storage.ChunkKey;
import g04.storage.Storage;

/**
 * Used to handle DELETE requests:
 * - Peers that have stored chunks of the file will remove all confirmations for those chunks, remove them from memory
 * and update the capacity
 * -Initiator-peers will 
 */
public class DeleteHandler implements Runnable {

    private Peer peer;
    private String fileId;
    private Integer senderId;

	public DeleteHandler(Peer peer, String fileId, Integer senderId) {
        this.peer = peer;
        this.fileId = fileId;
        this.senderId = senderId;
	}

	@Override
	public void run() {

        Storage storage = this.peer.getStorage();

        // Peers that stored chunks of the file
        if(this.senderId != Utils.PEER_ID){

            boolean hasFile = false;
            // Delete all confirmations and data associated with chunks of the deleted file
            for (ChunkKey key : storage.getStoredChunks().keySet()) {
                // Peer has stored a chunk of the file
                if (key.getFileId().equals(fileId)) {
                    storage.getStoredChunks().remove(key);
                    storage.getConfirmedChunks().remove(key);
                    storage.decreaseCapacity(key.getSize());
                    hasFile = true;
                }
            }

            if(!hasFile){
                
                // Remove confirmations from peers that did not store chunks of the file
                for (ChunkKey key : storage.getConfirmedChunks().keySet()) {
                    if (key.getFileId().equals(fileId)) {
                        storage.getConfirmedChunks().remove(key);
                    }
                }

                return;
            }
            
            // Remove all stored chunks of the file from memory
            File fileFolder = new File(storage.getPath() + "/backup/file-" + fileId);
            
            if(fileFolder.exists()) {
                String[] entries = fileFolder.list();
                for (String s : entries) {
                    File currentFile = new File(fileFolder.getPath(), s);
                    currentFile.delete();
                }
                fileFolder.delete();
            }	

            if(!Utils.PROTOCOL_VERSION.equals("2.0")){
                return;
            }
            
            // Version 2.0 - send DELETED confirmation
            try {
                ControlChannel controlChannel = this.peer.getControlChannel();
				controlChannel.sendMessage(controlChannel.getDeletedPacket(
				    Utils.PROTOCOL_VERSION, 
				    Utils.PEER_ID,
				    this.fileId
				));
                System.out.println("Peer " + Utils.PEER_ID + ": sent DELETED");
			} catch (IOException e) {
                System.out.println("Failed to send DELETED for file " + this.fileId);
			}
        }

        // Initiator-peer
        else{

            // Remove confirmations for the deleted file
            for (ChunkKey key : storage.getConfirmedChunks().keySet()) {
                if (key.getFileId().equals(fileId)) {
                    storage.getConfirmedChunks().remove(key);
                }
            }

            // Remove file from restored files
            File file = new File(storage.getPath() + "/restored/" + storage.getBackupFiles().get(this.fileId).getFileName());
            if(file.exists()) {
                file.delete();
            }

            // Remove from backed up files
            storage.getBackupFiles().remove(this.fileId);
        }
	}
}
