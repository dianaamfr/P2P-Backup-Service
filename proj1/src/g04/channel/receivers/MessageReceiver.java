package g04.channel.receivers;

import java.util.HashMap;

import g04.Peer;
import g04.Utils;

public abstract class MessageReceiver implements Runnable {
    
    protected Peer peer;

    public MessageReceiver(Peer peer) {
        this.peer = peer;
    }

    @Override
    public abstract void run();

    public HashMap<String,String> parseMessage(String message){
        
        HashMap<String,String> parsed = new HashMap<>();
        String[] args = message.split(" ");
        int crlf = message.indexOf(Utils.CRLF + Utils.CRLF, 0);
        String body = message.substring(crlf);


        parsed.put("Version", args[0]);
        parsed.put("MessageType", args[1]);
        parsed.put("SenderId", args[2]);
        parsed.put("FileId", args[3]);
        
        if(!args[1].equals("DELETE")){
            parsed.put("ChunkNo", args[4]);
            if(args[1].equals("PUTCHUNK")){
                parsed.put("ReplicationDeg", args[5]);
                body = body.replaceFirst(Utils.CRLF + Utils.CRLF,"");
            }
            else if(args[1].equals("CHUNK")){
                body = body.replaceFirst(Utils.CRLF + Utils.CRLF,"");
            }

            parsed.put("Body", body);
        }

        return parsed;
    }

    public void printParsedMessage(HashMap<String,String> map){
        System.out.println(map.toString());
    }
}