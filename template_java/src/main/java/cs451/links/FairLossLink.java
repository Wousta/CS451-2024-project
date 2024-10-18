package cs451.links;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cs451.Host;
import cs451.packets.Message;
import cs451.packets.Packet;

/**
 * Can send and deliver Messages using UDP packets.
 * Use it inside Stubbornlink to guarantee Fair-loss, finite duplication and no creation properties
 */
public class FairLossLink {

    private final DatagramSocket socket;

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
            socket.send(new DatagramPacket(buf, buf.length, host.getInetAddress(), host.getPort()));
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
    public Packet deliver() {
        DatagramPacket packet;
        packet = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (Packet)Packet.deSerialize(packet.getData());
    }
}
