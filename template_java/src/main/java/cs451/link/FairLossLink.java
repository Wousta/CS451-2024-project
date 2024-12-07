package cs451.link;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packet.Packet;

/**
 * Can send and deliver Messages using UDP packets.
 * Use it inside Stubbornlink to guarantee Fair-loss, finite duplication and no creation properties
 */
public class FairLossLink {

    private final DatagramSocket socketReceive;
    private final DatagramSocket socketSend;
    private final DatagramSocket socketSendAckOk;
    // The size will be incremented if some packet exceeds the expected size
    
    private AtomicInteger timesTamp = new AtomicInteger(1);
    private AtomicInteger bufSize = new AtomicInteger(Packet.EXPECTED_SIZE);
    private PerfectLink perfectLink;
    ScheduledExecutorService executor;

    public FairLossLink(DatagramSocket socketReceive,  ScheduledExecutorService executor, PerfectLink perfectLink) throws SocketException {
        this.socketReceive = socketReceive;
        this.socketSend = new DatagramSocket();
        this.socketSendAckOk = new DatagramSocket();
        this.executor = executor;
        this.perfectLink = perfectLink;
    }

    /**
     * Serializes the message and sends it to specified host through a DatagramSocket
     * @param host target host that receives the message
     * @param msg data to send
     */
    public synchronized void send(Host host, Packet packet) {
        try {
            packet.setTimeStamp(timesTamp.getAndIncrement());
            byte[] buf = Packet.serialize(packet);
            if(buf.length > bufSize.get()) {
                bufSize.set(buf.length);
            }

            socketSend.send(new DatagramPacket(
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
     * When sending ack ok the packet should not be put in sent messages for resending.
     * This method is equivalent to send() except it does not store the message in sent map.
     * The reason is that if this ack ok does not arrive, the ack message will be resent from the receiver,
     * But if this ack ok is kept in sent messages, it would need to be cleaned and therefore needs another
     * ack message for the ack ok message, starting an infinite loop of acks that need to be acked.
     * @param host the target host
     * @param packet the ack ok packet to be sent
     */
    public synchronized void sendAckOk(Host host, Packet packet) {
        try {
            packet.setTimeStamp(timesTamp.getAndIncrement());
            byte[] buf = Packet.serialize(packet);
            if(buf.length > bufSize.get()) {
                bufSize.set(buf.length);
            }

            socketSendAckOk.send(new DatagramPacket(
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
    public void deliver() {
        int size = bufSize.get();
        DatagramPacket packet = new DatagramPacket(new byte[size], size);
        try {
            socketReceive.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        perfectLink.deliver(packet.getData());
    }

    public void adjustBufSize() {
        int newSize = bufSize.get()*2;

        if(newSize > Packet.MAX_PACKET_SIZE) bufSize.set(Packet.MAX_PACKET_SIZE);
        else bufSize.set(newSize);
    }

}
