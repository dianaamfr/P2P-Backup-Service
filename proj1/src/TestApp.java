import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

class TestApp {
    public static void main(String[] args){
        if(args.length < 2 || args.length > 4){
            usage();
            return;
        }

        String peer_ap = args[0];
        String sub_protocol = args[1];

        try {
            Registry registry = LocateRegistry.getRegistry();
            IRemote stub = (IRemote) registry.lookup(peer_ap);

            switch(sub_protocol){
                case "BACKUP":
                    if(args.length != 4){
                       usage();
                       return;
                    }
                    stub.backup(args[2], Integer.parseInt(args[3]));
                    break;
                case "RESTORE":
                    if(args.length != 3){
                        usage();
                        return;
                    }
                    stub.restore(args[2]);
                    break;
                case "DELETE":
                    if(args.length != 3){
                        usage();
                        return;
                    }
                    stub.restore(args[2]);
                    break;
                case "RECLAIM":
                    if(args.length != 3){
                        usage();
                        return;
                    }
                    stub.reclaim(Integer.parseInt(args[2]));
                    break;
                case "STATE":
                    if(args.length != 2){
                        usage();
                        return;
                    }
                    stub.state();
                    break;
                default:
                    System.err.println("Invalid sub protocol");
                    return;
            }
        
        } catch(NumberFormatException e){
            e.printStackTrace();
            System.err.println("Invalid operands");
            return;
        }
        catch(Exception e) {
            e.printStackTrace();
            System.err.println("Remote object error");
            return;
        }
        
    }

    public static void usage(){
        System.err.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
    }
}