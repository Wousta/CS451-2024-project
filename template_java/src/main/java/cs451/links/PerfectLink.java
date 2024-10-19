package cs451.links;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import cs451.Constants;
import cs451.Host;
import cs451.packets.*;
import cs451.parsers.Logger;

public class PerfectLink {

    private StubbornLink sl;
    private Logger logger;
    private List<Host> hosts;
    private List<ConcurrentMap<Integer,Packet>> delivered;
    private List<BlockingQueue<Integer>> pendingAcks;

    public PerfectLink(Host thisHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor){
        sl = new StubbornLink(thisHost, hosts, executor);
        delivered = thisHost.getDelivered();
        pendingAcks = thisHost.getPendingAcks();
        this.hosts = hosts;
        this.logger = logger;
    }

    public void send(Host h, Packet p) {
        sl.send(h, p);
    }

    public void deliver() {
        byte[] data = sl.deliver();

        // Data buffer received was too big, buffe size has been incremented for next try
        if(data == null) return;

        try {
            Object obj = Packet.deSerialize(data);
            // Acks packets only contain one message and are lighter when sending
            if(obj instanceof MsgPacket) handleMsgPacket(data);
            if(obj instanceof AcksPacket) handleAcksPacket(data);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public void handleMsgPacket(byte[] data) throws ClassNotFoundException, IOException {
        MsgPacket packet = (MsgPacket)Packet.deSerialize(data);
        int packetId = packet.getPacketId();
        int lastAck = hosts.get(packet.getHostIndex()).getLastAck();
        BlockingQueue<Integer> senderAcks = pendingAcks.get(packet.getHostIndex());
        ConcurrentMap<Integer,Packet> senderDelivered = delivered.get(packet.getHostIndex());

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && packetId > lastAck) {
            System.out.println("Recibido paquete: " + packet.toString());
            senderDelivered.put(packetId, packet);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            assert senderAcks.offer(packetId) : "senderAcks offer should not return false";

            for(Message m : packet.getMessages()) {
                System.out.println("d " + m.getHostId() + " " + (String)MsgPacket.deSerialize(m.getData()));
                logger.addLine("d " + m.getHostId() + " " + (String)MsgPacket.deSerialize(m.getData()));
            }
        }
        else System.out.println("Already delivered: " + packet);
    }

    public void handleAcksPacket(byte[] data) throws ClassNotFoundException, IOException {
        AcksPacket packet = (AcksPacket)Packet.deSerialize(data);
        System.out.println("What are we doing here, just to suffer");
    }
}