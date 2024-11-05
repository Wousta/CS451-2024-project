package cs451.packet;

import java.io.Serializable;

/**
 * Up to 8 can be stored in a single packet
 */
public class Message implements Serializable {
    // TODO: enforce message maximum size
    public static final int MSG_MAX_SIZE = 1000; 

    /**
     * The message content
     */
    private final byte[] data;

    /**
     * Sequence number of the message
     */
    private final int msgId;

    /**
     * Id of the host that created the message
     */
    private int hostId;


    public Message(int senderId, int msgId, byte[] data) {
        this.hostId = senderId;
        this.msgId = msgId;
        this.data = data;
    }
    
    public int getMsgId() {
        return msgId;
    }

    public int getHostId() {
        return hostId;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "H" + hostId + "M" + msgId;
    }

}
