package g04.channel.receivers;

import java.io.IOException;
import java.net.ServerSocket;

import g04.Peer;
import g04.Utils;
import g04.Utils.Protocol;
import g04.channel.handlers.TcpRestoreHandler;

public class TcpRestoreReceiver extends MessageReceiver {
    
    private ServerSocket serverSocket;
	
    public TcpRestoreReceiver(Peer peer) {
		super(peer);
        this.serverSocket = this.peer.getRestoreChannel().getTcpSocket();
	}

	/**
	 * Accept new TCP connections and start a thread for each one of them
	 */
	@Override
	public void run() { 
        while (true) {
            
			try {
				this.peer.getScheduler().execute(new TcpRestoreHandler(this.peer, this, serverSocket.accept()));
			} catch (IOException e) {
				Utils.protocolError(Protocol.RESTORE, null, " I/O error when waiting for a connection in TCP socket");
			}
			
        }
	}
}
