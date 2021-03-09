
HOSTNAME = 225.10.10.10
MC_PORT = 8010
MDB_PORT = 8011
MDR_PORT = 8012

all:
	javac ./*.java
	
clean:
	rm ./*.class && rm ./*/*.class

rmi:
	rmiregistry &
	sleep 0.5

kill:
	killall rmiregistry
	killall java

peers: all rmi
	java Peer 1.0 0 peer0 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java Peer 1.0 1 peer1 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java Peer 1.0 2 peer2 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	