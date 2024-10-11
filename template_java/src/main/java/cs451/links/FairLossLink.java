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

public class FairLossLink {

    private final DatagramSocket socket;

    public FairLossLink(DatagramSocket socket) {
        this.socket = socket;
    }

    // Super mega ugly Java code
    public void send(Host host, Message msg) {
        try {
            byte[] buf = serialize(msg);
            System.out.println("Enviando msg a dir: " + host.getIp() + " " + host.getInetAddress().toString() + " port: " + host.getPort());
            socket.send(new DatagramPacket(buf, buf.length, host.getInetAddress(), host.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Message deliver() {
        DatagramPacket p;
        p = new DatagramPacket(new byte[Message.MSG_MAX_SIZE], Message.MSG_MAX_SIZE);
        System.out.println("Esperando nuevo paquete");
        try {
            socket.receive(p);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return deSerialize(p.getData());
    }

    
    // Code from: https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    private byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } 
    }

    private Message deSerialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return (Message) in.readObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.err.println("Returning null message, should not be happening");
        return null;
    }
}
