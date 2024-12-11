package cs451.packet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class MsgPacket extends Packet {

    public static final int MAX_MSGS = 8;
    private final BitSet flags;          // General purpose use set of flags
    private final int originalId;
    private int propNumber;
    private int shot;
    private byte lastHop;
    private List<String> messages = new ArrayList<>(MAX_MSGS);


    public MsgPacket(byte hostId, int originalId, BitSet flags) {
        super(hostId);
        this.lastHop = hostId;
        this.originalId = originalId;
        this.flags = flags;
    }

    public MsgPacket(MsgPacket packet, byte targetHostId) {
        super(packet.hostId, targetHostId);
        originalId = packet.getOriginalId();
        flags = packet.getFlags();
        for(String m : packet.getMessages()) {
            messages.add(m);
        }
    }

    public MsgPacket(byte hostId, byte targetHostId, BitSet alreadyDelivered) {
        super(hostId);
        this.lastHop = hostId;
        this.targetHostId = targetHostId;
        this.originalId = 0;
        this.flags = alreadyDelivered;
    }


    /**
     * Adds a message to the Packet's internal queue of messages.
     * @param msg the message to be added to the queue
     * @return true (as specified by Collection.add)
     */
    public boolean addMessage(String msg) {
        return messages.add(msg);
    }

    ////////////////////// GETTERS & SETTERS //////////////////////

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public BitSet getFlags() {
        return flags;
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

    public int getPropNumber() {
        return propNumber;
    }

    public void setPropNumber(int propNumber) {
        this.propNumber = propNumber;
    }

    public int getShot() {
        return shot;
    }

    public void setShot(int shot) {
        this.shot = shot;
    }

    /////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        String msgList = String.join(", ", messages);

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

    public static int getMaxMsgs() {
        return MAX_MSGS;
    }

}
