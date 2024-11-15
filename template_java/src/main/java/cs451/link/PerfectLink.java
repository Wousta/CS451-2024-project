package cs451.link;

import java.io.IOException;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cs451.Host;
import cs451.broadcast.BEBroadcast;
import cs451.packet.AcksPacket;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;
import cs451.parser.Logger;

public class PerfectLink {

    private FairLossLink fll;
    private BEBroadcast beBroadcast;
    private Logger logger;
    private Host selfHost;
    private List<Host> hosts;
    private ConcurrentMap<Integer,Packet> sent; 
    private Queue<AcksPacket> acksFromReceiver = new LinkedList<>();
    private AtomicInteger idCounter = new AtomicInteger(1);
    private AtomicInteger timesTamp = new AtomicInteger(1);
    private Lock lock = new ReentrantLock();

    public PerfectLink(Host selfHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor){
        try {
            fll = new FairLossLink(selfHost.getSocketReceive(), executor, this);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        sent = selfHost.getSent();

        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;

        AckBuildAndSend ackBuildAndSend = new AckBuildAndSend();
        //if(selfHost.getId() == 1) // TODO:  remember to comment when testing broadcast
            executor.scheduleWithFixedDelay(ackBuildAndSend, 50, 100, TimeUnit.MILLISECONDS);

        // Resend operation of stubbornLink that guarantees eventual delivery between correct processes.
        executor.scheduleWithFixedDelay(() -> {
            try {
                if(lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                    sent.forEach((id, packet) -> {
                        packet.setTimeStamp(timesTamp.getAndIncrement());
                        fll.send(hosts.get(packet.getTargetHostIndex()), packet);
                    });

                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 150, 100, TimeUnit.MILLISECONDS);
        
    }


    public FairLossLink getFairLossLink() {
        return fll;
    }


    public BEBroadcast getBEBroadcast() {
        return beBroadcast;
    }


    public void setBEBroadcast(BEBroadcast beBroadcast) {
        this.beBroadcast = beBroadcast;
    }


    public void send(Host h, Packet p) {
        p.setPacketId(idCounter.getAndIncrement());
        p.setTimeStamp(timesTamp.getAndIncrement());
        fll.send(h, p);

        try {
            sent.put(p.getPacketId(), p);
        } catch(Exception e) {
            e.printStackTrace();
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
            Object obj = Packet.deSerialize(data);
            if(obj instanceof MsgPacket) {
                handleMsgPacket(data);
            }

            if(obj instanceof AcksPacket) {
                handleAcksPacket(data);
            }
        } catch (Exception e) {
            // Data buffer received was too big, buffe size is incremented for next try
            fll.adjustBufSize();
        } 
    }

    private void handleMsgPacket(byte[] data) throws ClassNotFoundException, IOException {
        MsgPacket packet = (MsgPacket)Packet.deSerialize(data);
        int packetId = packet.getPacketId();
        int senderTimeStamp = packet.getTimeStamp();
        int senderIndex = packet.getHostIndex();

        Host sender = hosts.get(senderIndex);
        int lastTimeStamp = sender.getLastTimeStamp();
        ConcurrentMap<Integer,Boolean> senderDelivered = sender.getDelivered();

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && senderTimeStamp > lastTimeStamp) {
            senderDelivered.put(packetId, false);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            sender.getPendingAcks().add(packetId);

            if(beBroadcast != null) {
                beBroadcast.deliver(packet);
            } else {
                logger.logPacket(packet);
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
        AcksPacket ackOk = new AcksPacket(selfHost.getId(), packet.getHostId());

        for(int packetId : packet.getAcks()) {
            if(sent.remove(packetId) == null) {
                break;
            }
        }

        ackOk.setPacketId(packet.getPacketId());
        ackOk.setAckStep(AcksPacket.ACK_SENDER);
        ackOk.setAcks(acksQueue);
        acksFromReceiver.add(ackOk);
        
        try {
            if(lock.tryLock(200, TimeUnit.MILLISECONDS)) {

                while(!acksFromReceiver.isEmpty()) {
                    AcksPacket acksPacket = acksFromReceiver.poll();

                    acksPacket.setTimeStamp(timesTamp.getAndIncrement());
                    sendAckOk(hosts.get(acksPacket.getTargetHostIndex()), acksPacket);
                }

                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleAckFromSender(AcksPacket packet) {
        int senderIndex = packet.getHostIndex();
        int packetTimestamp = packet.getTimeStamp();
        Host host = hosts.get(senderIndex);
        Queue<Integer> acksQueue = packet.getAcks();
        ConcurrentMap<Integer,Boolean> delivered = hosts.get(senderIndex).getDelivered();

        // Only update timestamp if ack is newer
        if(host.getLastTimeStamp() < packetTimestamp) {
            host.setLastTimeStamp(packetTimestamp);
        }

        for(int packId : acksQueue) {
            if(delivered.remove(packId) == null) {
                break;
            }
        }
        
        sent.remove(packet.getPacketId());
    }


    private class AckBuildAndSend implements Runnable {

        // Extracts from waiting acks queue and puts them into a new acks queue ready to be sent.
        private BlockingQueue<Integer> buildAckQueue(BlockingQueue<Integer> pendingAcks) {
            int count = 0;
            int acksToAdd = 128;//loadBalancer.getAcksToAdd();
            BlockingQueue<Integer> ackQueueToSend = new LinkedBlockingDeque<>();

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
                BlockingQueue<Integer> pendingAcks = host.getPendingAcks();

                if(pendingAcks.isEmpty()) {
                    continue;
                }

                // Build ack queue to send from pending acks that came from TargetHost
                BlockingQueue<Integer> ackQueueToSend = buildAckQueue(pendingAcks);

                AcksPacket packet = new AcksPacket(
                    selfHost.getId(), 
                    host.getId(), 
                    ackQueueToSend
                );

                send(host, packet);
            }
        }
        
    }
}