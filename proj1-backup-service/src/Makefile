
# Hostname and ports for the multicast channels
HOSTNAME = 225.10.10.10
MC_PORT = 8013
MDB_PORT = 8011
MDR_PORT = 8012

all:
	javac g04/*.java
	
clean:
	rm g04/*.class && rm g04/*/*.class && rm g04/*/*/*.class

rmi:
	rmiregistry &
	sleep 0.5

kill:
	killall rmiregistry
	killall java

delout:
	rm -r g04/output/*

killall: kill clean delout

# 4 Peers with version 1.0
peers: all rmi
	java g04.Peer 1.0 0 peer0 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 1.0 1 peer1 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 1.0 2 peer2 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 1.0 3 peer3 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &

# 4 Peers with version 2.0
peers2: all rmi
	java g04.Peer 2.0 0 peer0 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 2.0 1 peer1 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 2.0 2 peer2 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &
	java g04.Peer 2.0 3 peer3 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &

# 1 Peer
extra:
	java g04.Peer 2.0 3 peer3 $(HOSTNAME) $(MC_PORT) $(HOSTNAME) $(MDB_PORT) $(HOSTNAME) $(MDR_PORT) &

# Backup tests
backup: 
	java g04.TestApp peer0 BACKUP Makefile 3

backup1: 
	java g04.TestApp peer0 BACKUP g04/test/iart.pdf 3

backup2: 
	java g04.TestApp peer0 BACKUP g04/test/README.md 1

backup3: 
	java g04.TestApp peer0 BACKUP g04/test/sdis.pdf 2

backup4: 
	java g04.TestApp peer0 BACKUP g04/test/Texto_01.pdf 3

backup5: 
	java g04.TestApp peer0 BACKUP g04/test/login-tog.png 3

backup6: 
	java g04.TestApp peer1 BACKUP g04/test/test6.pdf 1

backup7: 
	java g04.TestApp peer1 BACKUP g04/test/test7.mp3 3

backup5mb: 
	java g04.TestApp peer1 BACKUP g04/test/image5mb.jpg 2

backup10mb: 
	java g04.TestApp peer1 BACKUP g04/test/test10mb.zip 2

backupbin:
	java g04.TestApp peer1 BACKUP g04/test/test128kb.bin 3

# Restore tests
restore: 
	java g04.TestApp peer0 RESTORE Makefile

restore1: 
	java g04.TestApp peer0 RESTORE iart.pdf

restore2: 
	java g04.TestApp peer0 RESTORE README.md

restore3: 
	java g04.TestApp peer0 RESTORE sdis.pdf

restore4: 
	java g04.TestApp peer0 RESTORE Texto_01.pdf

restore5: 
	java g04.TestApp peer0 RESTORE login-tog.png

restore6: 
	java g04.TestApp peer1 RESTORE test6.pdf

restore7: 
	java g04.TestApp peer1 RESTORE test7.mp3

restore5mb: 
	java g04.TestApp peer1 RESTORE image5mb.jpg

restore10mb: 
	java g04.TestApp peer1 RESTORE test10mb.zip

restorebin: 
	java g04.TestApp peer1 RESTORE test128kb.bin

# Delete tests
delete: 
	java g04.TestApp peer0 DELETE Makefile

delete1: 
	java g04.TestApp peer0 DELETE iart.pdf

delete2: 
	java g04.TestApp peer0 DELETE README.md

delete3: 
	java g04.TestApp peer0 DELETE sdis.pdf

delete4: 
	java g04.TestApp peer0 DELETE Texto_01.pdf

delete5: 
	java g04.TestApp peer0 DELETE login-tog.png

delete6: 
	java g04.TestApp peer1 DELETE test6.pdf

delete7: 
	java g04.TestApp peer1 DELETE test7.mp3

delete5mb: 
	java g04.TestApp peer1 DELETE image5mb.jpg

delete10mb: 
	java g04.TestApp peer1 DELETE test10mb.zip

deletebin: 
	java g04.TestApp peer1 DELETE test128kb.bin

# Reclaim tests
reclaim_decrease2:
	java g04.TestApp peer3 RECLAIM 0

reclaim_increase2:
	java g04.TestApp peer2 RECLAIM 500000

# State tests
state:
	java g04.TestApp peer0 STATE

state1:
	java g04.TestApp peer1 STATE

state2:
	java g04.TestApp peer2 STATE

state3:
	java g04.TestApp peer3 STATE
