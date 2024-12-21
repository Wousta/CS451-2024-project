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
    private int[] propNumber;
    private int shot;
    private boolean isProposal;
    private List<String> messages = new LinkedList<>();


    public MsgPacket(byte hostId, int originalId, BitSet flags) {
        super(hostId);
        this.originalId = originalId;
        this.flags = flags;
    }

    public MsgPacket(byte hostId, int[] propNumber, int shot, BitSet flags) {
        super(hostId);
        this.propNumber = propNumber;
        this.shot = shot;
        this.originalId = 0;
        this.flags = flags;
    }

    public MsgPacket(MsgPacket packet, byte targetHostId) {
        super(packet.hostId, targetHostId);
        this.originalId = packet.originalId;
        this.propNumber = packet.propNumber;
        this.isProposal = packet.isProposal;
        this.shot = packet.shot;
        this.flags = packet.flags;

        for(String m : packet.getMessages()) {
            messages.add(m);
        }
    }

    public MsgPacket(byte hostId, byte targetHostId, MsgPacket packet) {
        super(hostId);
        this.targetHostId = targetHostId;
        this.propNumber = packet.getPropNumber();
        this.shot = packet.getShot();
        this.flags = new BitSet(MAX_MSGS);
        this.originalId = 0;
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

    public int[] getPropNumber() {
        return propNumber;
    }

    public void setPropNumber(int[] propNumber) {
        this.propNumber = propNumber;
    }

    public int getShot() {
        return shot;
    }

    public void setShot(int shot) {
        this.shot = shot;
    }

    public boolean isProposal() {
        return isProposal;
    }

    public void setProposal(boolean isProposal) {
        this.isProposal = isProposal;
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


}
