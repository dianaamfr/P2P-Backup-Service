package g04.storage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncChunkUpdater implements Runnable {

    private Storage storage;

    public AsyncChunkUpdater(Storage storage) {
        this.storage = storage;
    }    

    @Override
    public void run() {
        ConcurrentHashMap<ChunkKey, HashSet<Integer>> storedChunks = storage.getStoredChunks();
        Iterator<ConcurrentHashMap.Entry<ChunkKey, HashSet<Integer>>> itr = storedChunks.entrySet().iterator();

        while (itr.hasNext()) { 
            ConcurrentHashMap.Entry<ChunkKey, HashSet<Integer>> entry = itr.next(); 
            ChunkKey key = entry.getKey();
            try {
                Chunk chunk = storage.read(key.getFileId(), key.getChunkNum());
                System.out.println(new String(chunk.getBuffer()));
                chunk.setPeers(entry.getValue());
                storage.store(chunk);
                System.err.println("JA SOMOS" + chunk.getFileId() +  " " + chunk.getChunkNum());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }

}