# Distributed File Backup Service

## Instructions:

1. Compile all the sources from the `src` directory:
    Usage: `../scripts/compile.sh`

2. Initiate the rmi from inside the `build` directory:
    `rmiregistry &`

3. Run the peers from inside the `build` directory:
    Usage: `../../scripts/peer.sh <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>`

4. Run the test app with any protocol:
    Usage: `../../scripts/test.sh <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]]`

> IMP: 
> The BACKUP protocol can receive any path from the filesystem, being it relative or absolute.
> The RESTORE and DELETE protocols should only receive the file name.

5. To clean the peers' storage output directory:
    Usage: `../../scripts/cleanup.sh [<peer_id>]`
    
6. To kill all the peers processes:
   `killall java`

## Collaborators
1. [Diana Freitas](https://github.com/dianaamfr)
2. [Eduardo Brito](https://github.com/edurbrito)