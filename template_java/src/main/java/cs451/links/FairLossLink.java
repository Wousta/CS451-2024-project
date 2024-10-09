package cs451.links;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import cs451.Host;
import cs451.Message;

public class FairLossLink {

    private final DatagramSocket socket;

    public FairLossLink(DatagramSocket socket) {
        this.socket = socket;
    }

    public void send(Host host, Message msg) {
        byte[] buf = msg.serialize();
        try {
            System.out.println("Enviando msg a dir: " + host.getIp() + " " + host.getInetAddress().toString() + " port: " + host.getPort());
            socket.send(new DatagramPacket(buf, buf.length, host.getInetAddress(), host.getPort()));
        } catch (Exception e) {
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
        // TODO: actual delivery
        return Message.deSerialize(p.getData());
    }

}
