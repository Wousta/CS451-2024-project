package cs451.links;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packets.AcksPacket;
import cs451.packets.Message;
import cs451.packets.MsgPacket;
import cs451.packets.Packet;
import cs451.parsers.Logger;

public class PerfectLink {

    private StubbornLink sl;
    private Logger logger;
    private Host selfHost;
    private AtomicInteger packetIdAtomic;
    private List<Host> hosts;
    private List<BlockingQueue<Integer>> pendingAcks;

    public PerfectLink(Host selfHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor, AtomicInteger packetId){
        sl = new StubbornLink(selfHost, hosts, executor, packetId);
        pendingAcks = selfHost.getPendingAcks();
        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.packetIdAtomic = packetId;
    }

    public void send(Host h, Packet p) {
        sl.send(h, p);
    }

    public void sendAckOk(Host h, Packet p) {
        sl.sendAckOk(h, p);
    }

    public void deliver() {
        byte[] data = sl.deliver();

        // Data buffer received was too big, buffe size has been incremented for next try
        //if(data == null) return;

        try {
            Object obj = Packet.deSerialize(data);
            // Acks packets only contain one message and are lighter when sending
            if(obj instanceof MsgPacket) {
                handleMsgPacket(data);
            }

            if(obj instanceof AcksPacket) {
                handleAcksPacket(data);
            }

        } catch (Exception e) {
            //System.out.println("Size of buffer was too small ===========================");
            sl.fllIncreaseBufSize();
        } 
    }

    private void handleMsgPacket(byte[] data) throws ClassNotFoundException, IOException {
        MsgPacket packet = (MsgPacket)Packet.deSerialize(data);
        int packetId = packet.getPacketId();
        int senderTimeStamp = packet.getTimeStamp();
        int senderIndex = packet.getHostIndex();
        int lastTimeStamp = hosts.get(senderIndex).getLastTimeStamp();
        ConcurrentMap<Integer,Packet> senderDelivered = selfHost.getDelivered().get(senderIndex);

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && senderTimeStamp > lastTimeStamp) {
            senderDelivered.put(packetId, packet);
            //logger.addLine("messages in packet = " + packet.getMessages().size());
            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            if(!pendingAcks.get(senderIndex).offer(packetId)) {
                System.out.println("Offer of new package failed");
            }

            for(Message m : packet.getMessages()) {
                //System.out.println("d " + m.getHostId() + " " + (String)MsgPacket.deSerialize(m.getData()));
                logger.addLine("d " + m.getHostId() + " " + (String)Packet.deSerialize(m.getData()));
            }
        }
    }

    private void handleAcksPacket(byte[] data) throws ClassNotFoundException, IOException {
        AcksPacket packet = (AcksPacket)Packet.deSerialize(data);

        if(packet.getAckStep() == AcksPacket.ACK_RECEIVER) {
            //System.out.println("Received ack from receiver");
            handleAckFromReceiver(packet);
        }

        else if(packet.getAckStep() == AcksPacket.ACK_SENDER) {
            handleAckFromSender(packet);
        }

    }

    private void handleAckFromReceiver(AcksPacket packet) {
        BlockingQueue<Integer> acksQueue = packet.getAcks();
        ConcurrentMap<Integer,Packet> sent = selfHost.getSent();

        //System.out.println("Received ack from receiver: " + packet.getPacketId());
        //System.out.println("Messages to remove: " + packet.getAcks());

        boolean isNewAck = true;
        for(int packetId : acksQueue) {
            if(sent.remove(packetId) == null) {
                //System.out.println("NULL REMOVE EN MAP SENDER");
                isNewAck = false;
                break;
            }
        }
        
        // with this host id and the receiver packet id, so that receiver can search in sender's delivered map
        AcksPacket ackOk = new AcksPacket(
            selfHost.getId(),
            packet.getHostId(), 
            packet.getPacketId()
        );

        ackOk.setAckStep(AcksPacket.ACK_SENDER);
        if(isNewAck) {
            ackOk.setTimeStamp(packetIdAtomic.getAndIncrement());
            ackOk.setAcks(acksQueue);
        }

        sendAckOk(hosts.get(ackOk.getTargetHostIndex()), ackOk);
    }

    private void handleAckFromSender(AcksPacket packet) {
        //System.out.println("Received ack from sender: " + packet.getPacketId());
        //System.out.println("Messages to remove: " + packet.getAcks());
        int senderIndex = packet.getHostIndex();
        int packetTimestamp = packet.getTimeStamp();
        Host host = hosts.get(senderIndex);
        Queue<Integer> acksQueue = packet.getAcks();
        ConcurrentMap<Integer,Packet> delivered = selfHost.getDelivered().get(senderIndex);

        for(int packetId : acksQueue) {
            delivered.remove(packetId);
        }

        // Only update timestamp if ack is newer
        if(host.getLastTimeStamp() < packetTimestamp) {
            host.setLastTimeStamp(packetTimestamp);
            return;
        }

        selfHost.getSent().remove(packet.getPacketId());
    }

}