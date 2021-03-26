package g04.storage;

public class ReplicationDegreeComparable {

    private ChunkKey chunkKey;
    private int perceivedRepDegree;

    public ReplicationDegreeComparable(ChunkKey chunkKey, int perceivedRepDegree){
        this.chunkKey = chunkKey;
        this.perceivedRepDegree = perceivedRepDegree;
    }

    public int compare(ReplicationDegreeComparable other){
        
        int myDiff = this.perceivedRepDegree - this.chunkKey.getReplicationDegree();
        int otherDiff = other.perceivedRepDegree - other.chunkKey.getReplicationDegree();
        
        return myDiff < otherDiff ? -1 : myDiff == otherDiff ? 0 : 1;
    }

    public ChunkKey getChunkKey() {
        return this.chunkKey;
    }

    public int getPerceivedRepDegree() {
        return this.perceivedRepDegree;
    }
}