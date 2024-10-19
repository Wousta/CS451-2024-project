package cs451.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MsgPacket extends Packet {

    public static final int MAX_MSGS = 8;
    //private boolean isAck = false;
    private final List<Message> messages = new ArrayList<>(8);

    public MsgPacket(byte hostId, byte destinationHostId, int packetId) {
        super(hostId, destinationHostId, packetId);
    }

    public boolean addMessage(Message msg) {
        return messages.add(msg);
    }

    ////////////////////// GETTERS & SETTERS //////////////////////

    public List<Message> getMessages() {
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
