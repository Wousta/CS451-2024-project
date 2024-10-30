package cs451.links;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packets.MsgPacket;
import cs451.packets.Packet;

/**
 * Can send and deliver Messages using UDP packets.
 * Use it inside Stubbornlink to guarantee Fair-loss, finite duplication and no creation properties
 */
public class FairLossLink {

    private final DatagramSocket socketReceive;
    private final DatagramSocket socketSend;
    private final DatagramSocket socketSendAck;
    private final Object sendLock = new Object(); // Lock for socketSend
    private final Object sendAckLock = new Object(); // Lock for socketSendAck
    // The size will be incremented if some packet exceeds the expected size
    private AtomicInteger bufSize = new AtomicInteger(Packet.EXPECTED_SIZE);

    public FairLossLink(DatagramSocket socketReceive) throws SocketException {
        this.socketReceive = socketReceive;
        this.socketSend = new DatagramSocket();
        this.socketSendAck = new DatagramSocket();
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
            if(packet instanceof MsgPacket) {
                synchronized (sendLock) {
                    socketSend.send(new DatagramPacket(
                        buf, 
                        buf.length, 
                        host.getInetAddress(), 
                        host.getPort()
                    ));
                }
            }
            else {
                synchronized (sendAckLock) {
                    socketSendAck.send(new DatagramPacket(
                        buf, 
                        buf.length, 
                        host.getInetAddress(), 
                        host.getPort()
                    ));
                }
            }
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
            socketReceive.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return packet.getData();
    }

    public void adjustBufSize() {
        int newSize = bufSize.get()*2;

        if(newSize > Packet.MAX_PACKET_SIZE) bufSize.set(Packet.MAX_PACKET_SIZE);
        else bufSize.set(newSize);
    }

}
