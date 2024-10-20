package cs451.links;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packets.Packet;

/**
 * Can send and deliver Messages using UDP packets.
 * Use it inside Stubbornlink to guarantee Fair-loss, finite duplication and no creation properties
 */
public class FairLossLink {

    private final DatagramSocket socket;
    // The size will be incremented if some packet exceeds the expected size
    private AtomicInteger bufSize = new AtomicInteger(Packet.EXPECTED_SIZE);

    public FairLossLink(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Serializes the message and sends it to specified host through a DatagramSocket
     * @param host target host that receives the message
     * @param msg data to send
     */
    public void send(Host host, Packet packet) {
        try {
            byte[] buf = Packet.serialize(packet);
            //System.out.println("Buf length = " + buf.length);
            if(buf.length > bufSize.get()) {
                bufSize.set(buf.length);
            }
            socket.send(new DatagramPacket(
                buf, 
                buf.length, 
                host.getInetAddress(), 
                host.getPort()
            ));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Waits for a DatagramPacket and returns it deserialized into Message.
     * It is synchronized because only one port for receiving messages is allowed,
     * so threads have to wait for each other and read one port one message at a time.
     * @return the deserialized Message
     */
    // TODO: a thread has to be used to keep listening and appending received messages, while another thread processes them
    public byte[] deliver() {
        DatagramPacket packet;
        int size = bufSize.get();
        packet = new DatagramPacket(new byte[size], size);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = packet.getData();
        System.out.println("Bytes length = " + data.length);

        return data;
    }

    public void adjustBufSize() {
        int newSize = bufSize.get()*2;

        if(newSize > Packet.MAX_PACKET_SIZE) bufSize.set(Packet.MAX_PACKET_SIZE);
        else bufSize.set(newSize);
    }

}
