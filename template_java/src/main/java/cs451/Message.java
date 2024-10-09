package cs451;

/**
 * Up to 8 can be stored in a single packet
 */
public class Message {

    /**
     * Sequence number of the message
     */
    private int msgId;

    /**
     * Id of the host that created the message
     */
    private int senderId;

    /**
     * TODO: use actual length after testing
     * Maximum size of a message in UDP packet is 64KiB
     */
    public static final int MSG_MAX_SIZE = 16;


    public Message(int senderId, int msgId) {
        this.senderId = senderId;
        this.msgId = msgId;
    }
    
    public int getMsgId(){
        return msgId + 1;
    }

    public int getSenderId(){
        return senderId;
    }

    // TODO: use java serialize method, it is optimized
    public byte[] serialize(){
        // TODO actual serialization and proper declaration of buffer
        byte[] buf = new byte[MSG_MAX_SIZE];
        buf[0] = (byte) senderId;
        buf[1] = (byte) msgId;
        return buf;
    }

    public static Message deSerialize(byte[] buf) {
        return new Message(Integer.valueOf(buf[0]), Integer.valueOf(buf[1]));
    }
}
