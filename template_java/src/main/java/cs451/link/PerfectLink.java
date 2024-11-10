package cs451.link;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import cs451.Host;
import cs451.packet.AcksPacket;
import cs451.packet.Message;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;
import cs451.parser.Logger;

public class PerfectLink {


    private FairLossLink fll;
    private Logger logger;
    private Host selfHost;
    private AtomicLong packetIdAtomic;
    private List<Host> hosts;
    private ConcurrentMap<Long,Packet> sent;
    private final Object sentLock = new Object(); 

    public PerfectLink(Host selfHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor, AtomicLong packetId){
        try {
            fll = new FairLossLink(selfHost.getSocketReceive(), executor, this);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        sent = selfHost.getSent();

        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.packetIdAtomic = packetId;

        AckBuildAndSend ackBuildAndSend = new AckBuildAndSend();
        if(selfHost.getId()  == 1) {
            executor.scheduleAtFixedRate(
                ackBuildAndSend, 
                50, 
                50, 
                TimeUnit.MILLISECONDS
            );
        }

        // Resend operation of stubbornLink that guarantees eventual delivery between correct processes.
        executor.scheduleWithFixedDelay(() -> {
            synchronized(this.sentLock) {
                sent.forEach((id, packet) -> {
                    packet.setTimeStamp(packetIdAtomic.getAndIncrement());
                    fll.send(hosts.get(packet.getTargetHostIndex()), packet);
                });
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
        
    }

    

    public FairLossLink getFairLossLink() {
        return fll;
    }



    public void send(Host h, Packet p) {
        fll.send(h, p);

        synchronized(sentLock) {
            try {
                sent.put(p.getPacketId(), p); // Gets blocked here
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * When sending ack ok the packet should not be put in sent messages for resending.
     * This method is equivalent to send() except it does not store the message in sent map.
     * The reason is that if this ack ok does not arrive, the ack message will be resent from the receiver,
     * But if this ack ok is kept in sent messages, it would need to be cleaned and therefore needs another
     * ack message for the ack ok message, starting an infinite loop of acks that need to be acked.
     * @param h the target host
     * @param p the ack ok packet to be sent
     */
    public void sendAckOk(Host h, Packet p) {
        fll.send(h, p);
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
            fll.adjustBufSize();
        } 
    }

    private void handleMsgPacket(byte[] data) throws ClassNotFoundException, IOException {
        MsgPacket packet = (MsgPacket)Packet.deSerialize(data);
        long packetId = packet.getPacketId();
        long senderTimeStamp = packet.getTimeStamp();
        int senderIndex = packet.getHostIndex();

        Host sender = hosts.get(senderIndex);
        long lastTimeStamp = sender.getLastTimeStamp();
        ConcurrentMap<Long,Boolean> senderDelivered = sender.getDelivered();

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && senderTimeStamp > lastTimeStamp) {
            senderDelivered.put(packetId, false);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            if(!sender.getPendingAcks().offer(packetId)) {
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
        BlockingQueue<Long> acksQueue = packet.getAcks();

        boolean isNewAck = true;

            for(long packetId : acksQueue) {
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
        long packetTimestamp = packet.getTimeStamp();
        Host host = hosts.get(senderIndex);
        Queue<Long> acksQueue = packet.getAcks();
        ConcurrentMap<Long,Boolean> delivered = hosts.get(senderIndex).getDelivered();


        boolean isNewAck = delivered.containsKey(acksQueue.peek());
        // Only update timestamp if ack is newer
        if(host.getLastTimeStamp() < packetTimestamp) {
            host.setLastTimeStamp(packetTimestamp);
        }

        for(long packetId : acksQueue) {
            if(delivered.remove(packetId) == null) {
                //isNewAck = false;
                break;
            }
        }
        
        selfHost.getSent().remove(packet.getPacketId());
        
    }


    private class AckBuildAndSend implements Runnable {

        // Extracts from waiting acks queue and puts them into a new acks queue ready to be sent.
        private BlockingQueue<Long> buildAckQueue(BlockingQueue<Long> pendingAcks) {
            int count = 0;
            int acksToAdd = 256;//loadBalancer.getAcksToAdd();
            BlockingQueue<Long> ackQueueToSend = new LinkedBlockingDeque<>();

            while(!pendingAcks.isEmpty() && count < acksToAdd) {
                try {
                    ackQueueToSend.add(pendingAcks.poll(100000, TimeUnit.MILLISECONDS));
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

            // A list of ack queues, one queue per host, iterate over the list.
            for(Host host : hosts) {
                BlockingQueue<Long> pendingAcks = host.getPendingAcks();

                if(pendingAcks.isEmpty()) {
                    continue;
                }

                synchronized(sentLock) {
                    // Build ack queue to send from pending acks that came from TargetHost
                    BlockingQueue<Long> ackQueueToSend = buildAckQueue(pendingAcks);
    
                    AcksPacket packet = new AcksPacket(
                        selfHost.getId(), 
                        host.getId(), 
                        packetIdAtomic.getAndIncrement(), 
                        ackQueueToSend
                    );
    
                    send(host, packet);
                }
                
            }
        }
        
    }
}