package cs451.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class Packet implements Serializable {
    public static final int MAX_PACKET_SIZE = 65504; // 64KiB - 32
    public static final int EXPECTED_SIZE = 2356;

    protected final byte hostId;
    protected final long packetId; // Serves as timestamp, since it is incremented by an atomic int each time a packet is created
    protected long timeStamp;
    protected byte lastHop;  // Last host Id that received the message
    protected byte targetHostId; // Used by stubbornLink to know to which host each packet has to be sent

    protected Packet(byte hostId, byte targetHostId, Long packetId) {
        this.hostId = hostId;
        this.targetHostId = targetHostId;
        this.packetId = packetId;
        this.timeStamp = packetId;
    }

    public byte getHostId() {
        return hostId;
    }

    public long getPacketId() {
        return packetId;
    }

    public int getHostIndex() {
        return hostId-1;
    }

    public int getLastHop() {
        return lastHop;
    }

    public void setLastHop(byte lastHop) {
        this.lastHop = lastHop;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte getTargetHostId() {
        return targetHostId;
    }

    public byte getTargetHostIndex() {
        return (byte) (targetHostId - 1);
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
