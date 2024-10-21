package cs451.links;

import java.io.IOException;
import java.util.Iterator;
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
    private AtomicInteger packetId;
    private List<Host> hosts;
    private List<ConcurrentMap<Integer,Packet>> delivered;
    private List<BlockingQueue<Integer>> pendingAcks;

    public PerfectLink(Host selfHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor, AtomicInteger packetId){
        sl = new StubbornLink(selfHost, hosts, executor);
        delivered = selfHost.getDelivered();
        pendingAcks = selfHost.getPendingAcks();
        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.packetId = packetId;
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
        if(data == null) return;

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
        ConcurrentMap<Integer,Packet> senderDelivered = delivered.get(senderIndex);

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && senderTimeStamp > lastTimeStamp) {
            //System.out.println("Recibido paquete: " + packet.toString());
            senderDelivered.put(packetId, packet);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            pendingAcks.get(senderIndex).offer(packetId);

            for(Message m : packet.getMessages()) {
                //System.out.println("d " + m.getHostId() + " " + (String)MsgPacket.deSerialize(m.getData()));
                logger.addLine("d " + m.getHostId() + " " + (String)MsgPacket.deSerialize(m.getData()));
            }
        }
        else {
            //System.out.println("Already delivered packet " + packetId);
        }
    }

    private void handleAcksPacket(byte[] data) throws ClassNotFoundException, IOException {
        AcksPacket packet = (AcksPacket)Packet.deSerialize(data);

        if(packet.getAckStep() == AcksPacket.ACK_RECEIVER) {
            //System.out.println("Received ack from receiver");

            // TODO: Thread clear sent messages
            Queue<Integer> acksQueue = packet.getAcks();
            ConcurrentMap<Integer,Packet> sent = selfHost.getSent();

            for(int packetId : acksQueue) {
                sent.remove(packetId);
            }
            //acksQueue.clear();
            // with this host id and the receiver packet id, so that receiver can search in sender's delivered map
            AcksPacket ackOk = new AcksPacket(
                selfHost.getId(),
                packet.getHostId(), 
                packet.getPacketId(), 
                acksQueue
            );
            ackOk.setAckStep(AcksPacket.ACK_SENDER);
            ackOk.setTimeStamp(packetId.getAndIncrement());

            // Send it back (ack ok) with the ACK_SENDER flag so that receiver can clear delivered queue
            //selfHost.getAckPacketsQueue().add(ackOk);
            sendAckOk(hosts.get(ackOk.getTargetHostIndex()), ackOk); // TODO: potential concurrency issue, executor?
        }

        if(packet.getAckStep() == AcksPacket.ACK_SENDER) {
            //System.out.println("Received ack from sender");

            int senderIndex = packet.getHostIndex();

            // Update last ack received from this host to ignore old messages
            hosts.get(senderIndex).setLastTimeStamp(packet.getTimeStamp());

            Queue<Integer> acksQueue = packet.getAcks();
            ConcurrentMap<Integer,Packet> delivered = selfHost.getDelivered().get(senderIndex);

            // Delete delivered messages of this sender, they will not be received anymore
            for(int packetId : acksQueue) {
                delivered.remove(packetId);
            }
            // TODO: clear ack
            selfHost.getSent().remove(packet.getPacketId());
            
            // Iterator<AcksPacket> it = selfHost.getAckPacketsQueue().iterator();
            // boolean found = false;
            // while(it.hasNext() && !found) {
            //     if(it.next().getPacketId() == packet.getPacketId()) {
            //         it.remove();
            //         found = true;
            //     }
            // }

        }
        
        //System.out.println("What are we doing here, just to suffer");
    }

    private class ClearSentMessages implements Runnable {



        @Override
        public void run() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'run'");
        }
        
    }
}