package cs451.packet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

public class MsgPacket extends Packet {

    public static final int MAX_MSGS = 8;
    private final Queue<Message> messages = new LinkedList<>();

    public MsgPacket(byte hostId, byte targetHostId) {
        super(hostId, targetHostId);
    }

    public MsgPacket(byte hostId) {
        super(hostId, (byte) 0); // Assuming a default value for destinationHostId
    }

    public MsgPacket(MsgPacket packet, byte targetHostId) {
        super(packet.hostId, targetHostId);
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
