package g04.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Future;

import g04.Utils;

public class AsyncStorageUpdater implements Runnable {

    private Storage storage;

    public AsyncStorageUpdater(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(Paths.get(storage.getPath()));

            Path path = Paths.get(storage.getPath() + "/storage.ser");

            AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this.storage);
            oos.flush();

            ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
            Future<Integer> operation = channel.write(buffer, 0);
            while (!operation.isDone());

            channel.close();
            oos.close();
            baos.close();
            // System.out.println("Serializing Storage for peer " +  Utils.PEER_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}