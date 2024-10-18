package cs451.links;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import cs451.Constants;
import cs451.Host;
import cs451.packets.Message;
import cs451.packets.Packet;
import cs451.parsers.Logger;

public class PerfectLink {

    private List<ConcurrentMap<Integer,Packet>> delivered;
    private StubbornLink sl;
    private Logger logger;

    public PerfectLink(Host thisHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor){
        sl = new StubbornLink(thisHost, hosts, executor);
        delivered = thisHost.getDelivered();
        this.logger = logger;
    }

    public void send(Host h, Packet p) {
        sl.send(h, p);
    }

    public void deliver() {
        Packet packet = sl.deliver();
        int packetId = packet.getPacketId();
        ConcurrentMap<Integer,Packet> senderDelivered = delivered.get(packet.getHostIndex());

        if(!senderDelivered.containsKey(packetId)) {
            System.out.println("Recibido paquete: " + packet.toString());
            senderDelivered.put(packetId, packet);
            for(Message m : packet.getMessages()) {
                System.out.println("d " + m.getHostId() + " " + (String)Packet.deSerialize(m.getData()));
                logger.addLine("d " + m.getHostId() + " " + (String)Packet.deSerialize(m.getData()));
            }
        }
        else System.out.println("Already delivered: " + packet);
    }
}