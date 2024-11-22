package cs451;

public class TupleKey {
    private final byte hostId;
    private final int ogPacketId;

    public TupleKey(byte hostId, int ogPacketId) {
        this.hostId = hostId;
        this.ogPacketId = ogPacketId;
    }

    public byte getHostId() {
        return hostId;
    }

    public int getOgPacketId() {
        return ogPacketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TupleKey that = (TupleKey) o;
        return hostId == that.hostId && ogPacketId == that.ogPacketId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Integer.hashCode(hostId);
        result = prime * result + Integer.hashCode(ogPacketId);
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "hId=" + hostId +
                ", ogPId=" + ogPacketId +
                '}';
    }
    
}
