package cs451.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class Packet implements Serializable {
    public static final int MAX_PACKET_SIZE = 65504; // 64KiB - 32
    public static final int EXPECTED_SIZE = 529;

    protected final int hostId;
    protected final int packetId; // Serves as timestamp, since it is incremented by an atomic int each time a packet is created

    protected Packet(int hostId, int packetId) {
        this.hostId = hostId;
        this.packetId = packetId;
    }

    public int getHostId() {
        return hostId;
    }

    public int getPacketId() {
        return packetId;
    }

    public int getHostIndex() {
        return hostId-1;
    }

    // Code from: https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    public static byte[] serialize(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Returning null byte array, should not be happening"
        return new byte[]{};
    }

    public static Object deSerialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } 
    }

}
