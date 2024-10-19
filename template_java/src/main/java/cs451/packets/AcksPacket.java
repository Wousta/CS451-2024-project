package cs451.packets;

import java.util.Queue;

public class AcksPacket extends Packet {
    /**
     * Ack sent by receiver to clear sent messages and send the ack ok message to receiver
     */
    public static final boolean ACK_RECEIVER = true;

    /**
     * Ack sent by sender to clear delivered msgs and the ack, this would be the ack ok message
     */
    public static final boolean ACK_SENDER = false;

    // Initialized to ACK_RECEIVER since the sender of first ack message in the exchange is the receiver
    private boolean ackStep = ACK_RECEIVER;
    private Queue<Integer> acks;

    public AcksPacket(byte hostId, byte destinationHostId, int packetId, Queue<Integer> acks) {
        super(hostId, destinationHostId, packetId);
        this.acks = acks;
    }

    public boolean getAckStep() {
        return ackStep;
    }

    public Queue<Integer> getAcks() {
        return acks;
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
