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
    protected int packetId; 
    protected int timeStamp;
    protected byte targetHostId;

    protected Packet(byte hostId, byte targetHostId) {
        this.hostId = hostId;
        this.targetHostId = targetHostId;
    }

    protected Packet(byte hostId) {
        this.hostId = hostId;
    }


    public byte getHostId() {
        return hostId;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public void initIdAndTimesTamp(int initValue) {
        this.packetId = initValue;
        this.timeStamp = initValue;

    }

    public int getHostIndex() {
        return hostId-1;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte getTargetHostId() {
        return targetHostId;
    }

    public void setTargetHostId(byte targetHostId) {
        this.targetHostId = targetHostId;
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
