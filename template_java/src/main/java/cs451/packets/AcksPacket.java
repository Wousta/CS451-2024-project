package cs451.packets;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private BlockingQueue<Integer> acks = new LinkedBlockingQueue<>();

    public AcksPacket(byte hostId, byte destinationHostId, int packetId) {
        super(hostId, destinationHostId, packetId);
    }

    public AcksPacket(byte hostId, byte destinationHostId, int packetId, BlockingQueue<Integer> acks) {
        super(hostId, destinationHostId, packetId);
        this.acks = acks;
    }

    /**
     * Used to know if this ack packet comes from the host that delivered the message (receiver), or if it comes from
     * the host that sent the message (sender). If it comes from receiver it is an ack message, if it comes from sender
     * it is an ack ok message.
     * @return True if it is an ACK_RECEIVER packet, false if it is ACK_SENDER packet.
     */
    public boolean getAckStep() {
        return ackStep;
    }

    public BlockingQueue<Integer> getAcks() {
        return acks;
    }

    public void setAcks(BlockingQueue<Integer> acks) {
        this.acks = acks;
    }

    /**
     * Sets this ack packet to be a receiver ack (ACK_RECEIVER) or if it is an ack ok packet (ACK_SENDER).
     * It is boolean for size optimization.
     * @param ackStep can be ACK_RECEIVER (true) or ACK_SENDER (false).
     */
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
        return "IdAck " + packetId + " hostId " + hostId + " ["  + "]";
    }
}
