package cs451.packets;

public class AcksPacket extends Packet {
    public static final boolean ACK_RECEIVER = true;            // Ack sent by receiver to clear sent messages
    public static final boolean ACK_SENDER = false;             // Ack sent by sender to clear delivered msgs and the ack
    /**
     * if true, this is an ack message and therefore contains the indexes of
     * the acked messages by a receiver process
     */
    //private boolean isAck = true;
    private boolean ackStep = ACK_RECEIVER;
    private Message msg;

    public AcksPacket(int hostId, int packetId, Message msg) {
        super(hostId, packetId);
        this.msg = msg;
    }

    public boolean getAckStep() {
        return ackStep;
    }

    public Message getMsg() {
        return msg;
    }

    public void setAckStep(boolean ackStep) {
        this.ackStep = ackStep;
    }

    // equals and hashCode answers provided by grepper results
    @Override
    public boolean equals(Object o) { 
        if (o == this) { 
            return true; 
        } 

        if (!(o instanceof AcksPacket)) { 
            return false; 
        } 
        
        AcksPacket c = (AcksPacket) o; 
        
        // Compare the data members and return accordingly 
        return Long.compare(packetId, c.packetId) == 0
                && Integer.compare(hostId, c.hostId) == 0
                //&& Boolean.compare(isAck, c.isAck()) == 0
                && Boolean.compare(ackStep, c.getAckStep()) == 0; 
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Long.hashCode(packetId);
        result = prime * result + Integer.hashCode(hostId);
        return result;
    }

    @Override
    public String toString() {
        return "Id " + packetId + " hostId " + hostId + " ["  + "]";
    }
}
