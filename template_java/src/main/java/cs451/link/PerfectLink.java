package cs451.link;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packet.AcksPacket;
import cs451.packet.Message;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;
import cs451.parser.Logger;

public class PerfectLink {

    private StubbornLink sl;
    private Logger logger;
    private Host selfHost;
    private AtomicInteger packetIdAtomic;
    private List<Host> hosts;
    private List<BlockingQueue<Integer>> pendingAcks;
    private final Object sentLock = new Object(); 

    public PerfectLink(Host selfHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor, AtomicInteger packetId){
        sl = new StubbornLink(selfHost, hosts, executor, packetId, sentLock, this);
        pendingAcks = selfHost.getPendingAcks();
        AckBuildAndSend ackBuildAndSend = new AckBuildAndSend(pendingAcks);
        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.packetIdAtomic = packetId;

        if(selfHost.getId()  == 1) {
            executor.scheduleAtFixedRate(
                ackBuildAndSend, 
                50, 
                50, 
                TimeUnit.MILLISECONDS
            );
        }
        
    }

    

    public StubbornLink getStubbornLink() {
        return sl;
    }



    public void send(Host h, Packet p) {
        sl.send(h, p);
    }

    public void sendAckOk(Host h, Packet p) {
        sl.sendAckOk(h, p);
    }

    public void deliver(byte[] data) {
        try {
    
            synchronized(sentLock) {
                Object obj = Packet.deSerialize(data);
                if(obj instanceof MsgPacket) {
                    handleMsgPacket(data);
                }
    
                if(obj instanceof AcksPacket) {
                    handleAcksPacket(data);
                }
            }

        } catch (Exception e) {
            //System.out.println("Size of buffer was too small ===========================");
            // Data buffer received was too big, buffe size is incremented for next try
            sl.fllIncreaseBufSize();
        } 
    }

    private void handleMsgPacket(byte[] data) throws ClassNotFoundException, IOException {
        MsgPacket packet = (MsgPacket)Packet.deSerialize(data);
        int packetId = packet.getPacketId();
        int senderTimeStamp = packet.getTimeStamp();
        int senderIndex = packet.getHostIndex();
        int lastTimeStamp = hosts.get(senderIndex).getLastTimeStamp();
        ConcurrentMap<Integer,Boolean> senderDelivered = selfHost.getDelivered().get(senderIndex);

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && senderTimeStamp > lastTimeStamp) {
            senderDelivered.put(packetId, false);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            if(!pendingAcks.get(senderIndex).offer(packetId)) {
                System.err.println("Offer of new package failed");
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
            handleAckFromReceiver(packet);
        }

        else if(packet.getAckStep() == AcksPacket.ACK_SENDER) {
            handleAckFromSender(packet);
        }

    }

    private void handleAckFromReceiver(AcksPacket packet) {
        BlockingQueue<Integer> acksQueue = packet.getAcks();
        ConcurrentMap<Integer,Packet> sent = selfHost.getSent();

        boolean isNewAck = true;

            for(int packetId : acksQueue) {
                if(sent.remove(packetId) == null) {
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

            // Only send the acks Queue if this is a new ack, to avoid null checks
            ackOk.setAckStep(AcksPacket.ACK_SENDER);
            // if(isNewAck) {
            //     ackOk.setTimeStamp(packetIdAtomic.getAndIncrement());
            //     ackOk.setAcks(acksQueue);
            // }
            ackOk.setTimeStamp(packetIdAtomic.getAndIncrement());
            ackOk.setAcks(acksQueue);

            sendAckOk(hosts.get(ackOk.getTargetHostIndex()), ackOk);
        
    }

    private void handleAckFromSender(AcksPacket packet) {
        int senderIndex = packet.getHostIndex();
        int packetTimestamp = packet.getTimeStamp();
        Host host = hosts.get(senderIndex);
        Queue<Integer> acksQueue = packet.getAcks();
        ConcurrentMap<Integer,Boolean> delivered = selfHost.getDelivered().get(senderIndex);


            boolean isNewAck = delivered.containsKey(acksQueue.peek());

            // Only update timestamp if ack is newer
            if(host.getLastTimeStamp() < packetTimestamp) {
                host.setLastTimeStamp(packetTimestamp);
            }

            for(int packetId : acksQueue) {
                if(delivered.remove(packetId) == null) {
                    //isNewAck = false;
                    break;
                }
            }
            
            selfHost.getSent().remove(packet.getPacketId());
        
    }


    private class AckBuildAndSend implements Runnable {
        private List<BlockingQueue<Integer>> pendingAcksList;

        public AckBuildAndSend(List<BlockingQueue<Integer>> pendingAcks) {
            this.pendingAcksList = pendingAcks;
        }

        // Extracts from waiting acks queue and puts them into a new acks queue ready to be sent.
        private BlockingQueue<Integer> buildAckQueue(BlockingQueue<Integer> pendingAcksQueue) {
            int count = 0;
            int acksToAdd = 256;//loadBalancer.getAcksToAdd();
            BlockingQueue<Integer> ackQueueToSend = new LinkedBlockingDeque<>();

            while(!pendingAcksQueue.isEmpty() && count < acksToAdd) {
                try {
                    ackQueueToSend.add(pendingAcksQueue.poll(100000, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                ++count;
            }

            return ackQueueToSend;
        }

        @Override
        public void run() {
            int indexOfTargetHost = 0;

            // A list of ack queues, one queue per host, iterate over the list.
            for(BlockingQueue<Integer> pendingAcksQueue : pendingAcksList) {
                if(pendingAcksQueue.isEmpty()) {
                    ++indexOfTargetHost;
                    continue;
                }
                synchronized(sentLock) {
                    Host targetHost = hosts.get(indexOfTargetHost);
                    ++indexOfTargetHost;
    
                    // Build ack queue to send from pending acks that came from TargetHost
                    BlockingQueue<Integer> ackQueueToSend = buildAckQueue(pendingAcksQueue);
    
                    AcksPacket packet = new AcksPacket(
                        selfHost.getId(), 
                        targetHost.getId(), 
                        packetIdAtomic.getAndIncrement(), 
                        ackQueueToSend
                    );
    
                    send(targetHost, packet);
                }
                
            }
        }
        
    }
}