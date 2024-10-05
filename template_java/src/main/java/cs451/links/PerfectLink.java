package cs451.links;

import java.util.List;

import cs451.Host;

import java.util.ArrayList;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PerfectLink {

    private List<DatagramPacket> delivered;
    private DatagramPacket msg;
    private DatagramSocket dest;

    public PerfectLink(){
        delivered = new ArrayList<>();
    }

    public PerfectLink(DatagramSocket destination){
        delivered = new ArrayList<>();
        dest = destination;   
    }

    // Thread to send
    public void send(Host host, DatagramPacket msg) throws IOException{
        msg.setAddress(InetAddress.getByName(host.getIp()));
        msg.setPort(host.getPort());
        byte[] data = {(byte) host.getId()};
        msg.setData(data);
        //System.out.println("Sending msg to port: " + host.getPort());
        dest.send(msg);
    }

    // Thread to deliver
    public void deliver(DatagramSocket s){

    }

}
