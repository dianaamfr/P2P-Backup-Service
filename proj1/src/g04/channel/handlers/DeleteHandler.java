package g04.channel.handlers;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import g04.Peer;
import g04.Utils;
import g04.storage.ChunkKey;
import g04.storage.Storage;

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

        if(this.senderId != Utils.PEER_ID){

            for (ChunkKey key : storage.getStoredChunks().keySet()) {
                if (key.getFileId().equals(fileId)) {
                    storage.getStoredChunks().remove(key);
                    storage.getConfirmedChunks().remove(key);
                    storage.decreaseCapacity(key.getSize());
                }
            }
    
            File fileFolder = new File(storage.getPath() + "/backup/file-" + fileId);
            
            if(fileFolder.exists()) {
                String[] entries = fileFolder.list();
                for (String s : entries) {
                    File currentFile = new File(fileFolder.getPath(), s);
                    currentFile.delete();
                }
                fileFolder.delete();
            }		
        }
        else{
            File file = new File(storage.getPath() + "/restored/" + storage.getBackupFiles().get(this.fileId).getFileName());

            storage.getBackupFiles().remove(this.fileId);
            
            if(file.exists()) {
                file.delete();
            }
        }
	}
}
