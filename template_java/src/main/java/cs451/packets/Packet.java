package cs451.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Packet implements Serializable {
    public static final int MAX_PACKET_SIZE = 65504; // 64KiB - 32
    public static final int MAX_MSGS = 8;
    private final int hostId;
    private final int packetId; // Serves as timestamp, since it is incremented by an atomic int each time a packet is created
    private final List<Message> messages = new ArrayList<>(8);
    /**
     * if true, this is an ack message and therefore contains the indexes of
     * the acked messages by a receiver process
     */
    private boolean isAck = false;

    public Packet(int hostId, int packetId) {
        this.hostId = hostId;
        this.packetId = packetId;
    }

    public Packet(int hostId, int packetId, boolean isAck) {
        this.hostId = hostId;
        this.packetId = packetId;
        this.isAck = isAck;
    }

    public boolean addMessage(Message msg) {
        return messages.add(msg);
    }

    /////////// GETTERS
    public int getHostId() {
        return hostId;
    }

    public int getPacketId() {
        return packetId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isAck() {
        return isAck;
    }

    public int getHostIndex() {
        return hostId-1;
    }
    ///////////

    public static byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } 
    }

    public static Object deSerialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception e) {
            System.out.println("Error deserializing");
            e.printStackTrace();
        }

        System.out.println("Returning null message, should not be happening");
        return null;
    }

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

		if (!(o instanceof Packet)) { 
			return false; 
		} 
		
		Packet c = (Packet) o; 
		
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
