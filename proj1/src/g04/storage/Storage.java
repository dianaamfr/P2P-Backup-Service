package g04.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import g04.Utils;
 
public class Storage {
    private String path;

    public Storage() throws IOException{
        this.path = "g04/chunks/peer" + Utils.PEER_ID;
    }

    public void store(SFile file) throws IOException {
        String fileDir = this.path + "/file" + file.getFileId();
        Files.createDirectories(Paths.get(fileDir));

        FileOutputStream f = new FileOutputStream(new File(fileDir + "/file-" + file.getFileId() + ".ser"));
        ObjectOutputStream o = new ObjectOutputStream(f);

        o.writeObject(file);
        o.close();
        f.close();
    }

    public void store(Chunk chunk) throws IOException{
        String fileDir = this.path + "/file" + chunk.getFileId();
        Files.createDirectories(Paths.get(fileDir));

        FileOutputStream f = new FileOutputStream(new File(fileDir + "/chunk-" + chunk.getChunkNum() + ".ser"));
        ObjectOutputStream o = new ObjectOutputStream(f);

        o.writeObject(chunk);
        o.close();
        f.close();
    }

    public Chunk read(String fileId, int chunkNum) throws IOException, ClassNotFoundException {
        String chunkPath =  this.path + "/file" + fileId + "/chunk-" + chunkNum + ".ser";
        FileInputStream fi = new FileInputStream(new File(chunkPath));
        ObjectInputStream oi = new ObjectInputStream(fi);
    
        Chunk c = (Chunk) oi.readObject();
        System.out.println(c.toString());
        return (Chunk) oi.readObject();
    }
}
