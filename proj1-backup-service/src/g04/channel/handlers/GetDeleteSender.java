package g04.channel.handlers;

import java.io.IOException;

import g04.Peer;
import g04.Utils;
import g04.Utils.MessageType;
import g04.Utils.Protocol;
import g04.channel.ControlChannel;

public class GetDeleteSender implements Runnable {
    
    Peer peer;
    
    public GetDeleteSender(Peer peer){
        this.peer = peer;
    }

    @Override
    public void run() {
        
        ControlChannel controlChannel = this.peer.getControlChannel();

        try {
            controlChannel.sendMessage(controlChannel.getDeletePacket(
            		    Utils.PROTOCOL_VERSION, 
            		    Utils.PEER_ID
            		));

            Utils.sendLog(Protocol.DELETE, MessageType.GETDELETE, "");
        } catch (IOException e) {
           Utils.protocolError(Protocol.DELETE, MessageType.GETDELETE, "");
        }
    }
}
