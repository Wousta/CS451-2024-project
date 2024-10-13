package cs451;

import java.io.Serializable;

/**
 * Up to 8 can be stored in a single packet
 */
public class Message implements Serializable{

    /**
     * The time when the message was created
     */
    private final long timeStampMs;

    /**
     * TODO: use actual length after testing
     * Maximum size of a message in UDP packet is 64KiB
     */
    public static final int MSG_MAX_SIZE = 4096;

    /**
     * The message content
     */
    private final byte[] data;

    /**
     * Sequence number of the message
     */
    private int msgId;

    /**
     * Id of the host that created the message
     */
    private int senderId;


    public Message(int senderId, int msgId, long timeStampMs, byte[] data) {
        this.senderId = senderId;
        this.msgId = msgId;
        this.timeStampMs = timeStampMs;
        this.data = data;
    }
    
    public int getMsgId(){
        return msgId;
    }

    public int getSenderId(){
        return senderId;
    }

    public long getTimeStampMs() {
        return timeStampMs;
    }

    public byte[] getData() {
        return data;
    }

    // equals and hashCode answers provided by grepper results
    @Override
	public boolean equals(Object o) { 

		// If the object is compared with itself then return true 
		if (o == this) { 
			return true; 
		} 

		/* Check if o is an instance of Complex or not 
		"null instanceof [type]" also returns false */
		if (!(o instanceof Message)) { 
			return false; 
		} 
		
		// typecast o to Complex so that we can compare data members 
		Message c = (Message) o; 
		
		// Compare the data members and return accordingly 
		return Integer.compare(msgId, c.msgId) == 0
				&& Integer.compare(senderId, c.senderId) == 0; 
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Integer.hashCode(msgId);
        result = prime * result + Integer.hashCode(senderId);
        return result;
    }
}
