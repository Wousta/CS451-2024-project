package cs451.links;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cs451.Host;
import cs451.Message;

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
    public void send(Host host, Message msg) {
        try {
            byte[] buf = serialize(msg);
            //System.out.println("Enviando msg a dir: " + host.getIp() + " " + host.getInetAddress().toString() + " port: " + host.getPort());
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
    public synchronized Message deliver() {
        DatagramPacket packet;
        packet = new DatagramPacket(new byte[Message.MSG_MAX_SIZE], Message.MSG_MAX_SIZE);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (Message)deSerialize(packet.getData());
    }

    
    // Code from: https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    private byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } 
    }

    private Object deSerialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("Error deserializing");
            e.printStackTrace();
        }

        System.out.println("Returning null message, should not be happening");
        return null;
    }
}
