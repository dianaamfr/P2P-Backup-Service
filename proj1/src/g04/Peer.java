package g04;

import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;

import g04.channel.ChannelAggregator;
import g04.storage.SFile;

import java.rmi.registry.LocateRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;

public class Peer implements IRemote {

    private ChannelAggregator channelAggregator;

    public Peer(ChannelAggregator aggregator) throws RemoteException {
        this.channelAggregator = aggregator;
    }

    public static void main(String[] args) throws RemoteException {

        if (args.length != 9) {
            Utils.usage("Wrong number of arguments");
            System.exit(1);
        }

        String protocolVersion = args[0];
        String peerId = args[1];
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

        Utils.PROTOCOL_VERSION = protocolVersion;
        Utils.PEER_ID = peerId;
        Utils.PEER = peer;

        System.out.println("Peer with id " + peerId + " registered to service with name " + peerAp);
    }

    @Override
    public String backup(String fileName, int replicationDegree) throws RemoteException {
        
        try {
			SFile file = new SFile(fileName, replicationDegree);
            file.generateChunks();

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
}
