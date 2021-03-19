package g04;

import java.io.IOException;
import java.net.DatagramPacket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import g04.channel.BackupChannel;
import g04.channel.ChannelAggregator;
import g04.channel.ControlChannel;
import g04.channel.handlers.BackupHandler;
import g04.storage.AsyncStorageUpdater;
import g04.storage.Chunk;
import g04.storage.SFile;
import g04.storage.Storage;

public class Peer implements IRemote {

    private ChannelAggregator channelAggregator;
    private Storage storage;
    private ScheduledThreadPoolExecutor scheduler;

    public Peer(ChannelAggregator aggregator) throws IOException {
        this.channelAggregator = aggregator;
        this.storage = new Storage();
        this.scheduler = new ScheduledThreadPoolExecutor(50);

        this.scheduler.scheduleWithFixedDelay(new AsyncStorageUpdater(this.storage),5000,5000,TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 9) {
            Utils.usage("Wrong number of arguments");
            System.exit(1);
        }

        Utils.PROTOCOL_VERSION = args[0];
        Utils.PEER_ID = Integer.parseInt(args[1]);
        String peerAp = args[2];

        String mcAddress = "", mdbAddress = "", mdrAddress = "";
        int mcPort = 0, mdbPort = 0, mdrPort = 0;

        ChannelAggregator channelAggregator = null;
        Peer peer;
        Registry registry = null;

        try {
            mcAddress = args[3];
            mcPort = Integer.parseInt(args[4]);

            mdbAddress = args[5];
            mdbPort = Integer.parseInt(args[6]);

            mdrAddress = args[7];
            mdrPort = Integer.parseInt(args[8]);

            channelAggregator = new ChannelAggregator(mcAddress, mcPort, mdbAddress, mdbPort, mdrAddress, mdrPort);

            registry = LocateRegistry.getRegistry();

        } catch (NumberFormatException e) {
            Utils.usage("Number Format Exception");
            System.exit(1);
        } catch (RemoteException e) {
            registry = LocateRegistry.createRegistry(1099);
        } catch (IOException e) {
            Utils.usage("IOException when joining Multicast Groups");
            System.exit(1);
        }

        peer = new Peer(channelAggregator);
        IRemote remote = (IRemote) UnicastRemoteObject.exportObject(peer, 0);
        registry.rebind(peerAp, remote);

        System.out.println("Peer with id " + Utils.PEER_ID + " registered to service with name " + peerAp);

        // Initate Channels
        channelAggregator.run(peer);

    }

    @Override
    public String backup(String fileName, int replicationDegree) throws RemoteException {

        try {
            SFile file = new SFile(fileName, replicationDegree);
            this.storage.store(file);

            ArrayList<Chunk> chunks = file.generateChunks();

            // Send putchunk message
            for (Chunk chunk : chunks) {
                DatagramPacket packet = getBackupChannel().putChunkPacket(Utils.PROTOCOL_VERSION, Utils.PEER_ID, chunk);      
                // Get confirmation messages or resend putchunk
                scheduler.execute(new BackupHandler(this, packet, chunk.getChunkKey(), replicationDegree));  
            }
            
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
            // Throw error message - file error
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String restore(String fileName) throws RemoteException {
        // TODO Auto-generated method stub

        return null;
    }

    @Override
    public String delete(String fileName) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String reclaim(int diskSpace) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String state() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }


    public BackupChannel getBackupChannel() {
        return this.channelAggregator.getBackupChannel();
    }

    public ControlChannel getControlChannel() {
        return this.channelAggregator.getControlChannel();
    }

    public Storage getStorage(){
        return this.storage;
    }

    public ScheduledThreadPoolExecutor getScheduler() {
        return scheduler;
    }
}
