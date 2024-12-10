package cs451.packet;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

public class MsgPacket extends Packet {

    public static final int MAX_MSGS = 8;
    private final Queue<Message> messages = new LinkedList<>();
    private final BitSet alreadyDelivered;
    private final int originalId;

    private byte lastHop;

    public MsgPacket(byte hostId, int originalId, BitSet alreadyDelivered) {
        super(hostId);
        this.lastHop = hostId;
        this.originalId = originalId;
        this.alreadyDelivered = alreadyDelivered;
    }

    public MsgPacket(MsgPacket packet, byte targetHostId) {
        super(packet.hostId, targetHostId);
        originalId = packet.getOriginalId();
        alreadyDelivered = packet.getAlreadyDelivered();
        for(Message m : packet.getMessages()) {
            messages.add(m);
        }
    }

    /**
     * Adds a message to the Packet's internal queue of messages.
     * @param msg the message to be added to the queue
     * @return true (as specified by Collection.add)
     */
    public boolean addMessage(Message msg) {
        return messages.add(msg);
    }

    ////////////////////// GETTERS & SETTERS //////////////////////

    public Queue<Message> getMessages() {
        return messages;
    }

    /**
     * The packet tracks the hosts that has already visited and have bebDelivered this packet,
     * to speed up the process of getting n/2 + 1 processes packet delivered.
     * @return A bitset with the bit of each host index set to 1 if it bebDelivered this packet or 0 if it did not.
     */
    public BitSet getAlreadyDelivered() {
        return alreadyDelivered;
    }

    public int getOriginalId() {
        return originalId;
    }

    public int getLastHop() {
        return lastHop;
    }

    public int getLastHopIndex() {
        return lastHop - 1;
    }

    public void setLastHop(byte lastHop) {
        this.lastHop = lastHop;
    }

    /////////////////////////////////////////////////////////////


    @Override
    public String toString() {
        String msgList = messages.stream().map(Message::toString)
                        .collect(Collectors.joining(", "));

        return "Id " + packetId + " hostId " + hostId + " [" + msgList + "]";
    }

    // equals and hashCode answers provided by grepper results
    @Override
    public boolean equals(Object o) { 
        if (o == this) { 
            return true; 
        } 

        if (!(o instanceof MsgPacket)) { 
            return false; 
        } 
        
        MsgPacket c = (MsgPacket) o; 
        
        // Compare the data members and return accordingly 
        return Long.compare(packetId, c.packetId) == 0
                && Integer.compare(hostId, c.hostId) == 0; 
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Long.hashCode(packetId);
        result = prime * result + Integer.hashCode(hostId);
        return result;
    }

}
