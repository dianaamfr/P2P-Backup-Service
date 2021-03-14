
HOSTNAME = 225.10.10.10
MC_PORT = 8010
MDB_PORT = 8011
MDR_PORT = 8012

all:
	javac g04/*.java
	
clean:
	rm g04/*.class && rm g04/*/*.class

rmi:
	rmiregistry &
	sleep 0.5

kill:
	killall rmiregistry
	killall java

peers: all rmi
	java g04.Peer 1.0 0 peer0 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 1.0 1 peer1 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 1.0 2 peer2 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &

backup: 
	java g04.TestApp peer0 BACKUP Makefile 3