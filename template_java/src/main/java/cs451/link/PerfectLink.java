package cs451.link;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import cs451.Host;
import cs451.control.Scheduler;
import cs451.packet.AcksPacket;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;

public class PerfectLink {

    // Dependencies
    private Scheduler scheduler;
    private FairLossLink fll;
    private Host selfHost;
    private List<Host> hosts;
    private List<ReentrantLock> sentMapsLocks;

    // Flow control
    private AtomicInteger ackIdCounter = new AtomicInteger(1);
    private AtomicInteger packetIdCounter = new AtomicInteger(1);
    private ConcurrentMap<Integer,Packet> acks = new ConcurrentHashMap<>();

    /**
     * Delivered packets ready to be consummed by the upper layer
     */
    private BlockingQueue<MsgPacket> packetsToConsume = new LinkedBlockingQueue<>(32);


    public PerfectLink(ScheduledExecutorService executor, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.selfHost = scheduler.getSelfHost();
        this.hosts = scheduler.getHosts();

        try {
            fll = new FairLossLink(selfHost.getSocketReceive());
        } catch (SocketException e) {
            e.printStackTrace();
        }

        sentMapsLocks = new ArrayList<>(hosts.size());
        for(int i = 0; i < hosts.size(); i++) {
            sentMapsLocks.add(new ReentrantLock());
        }

        executor.scheduleAtFixedRate(new AckBuildAndSend(), 100, 250, TimeUnit.MILLISECONDS);

        // Resend operation of stubbornLink that guarantees eventual delivery between correct processes.
        if(scheduler.isLatticeMode()) {
            executor.scheduleAtFixedRate(new StubbornSendLattice(), 150, 300, TimeUnit.MILLISECONDS);
        } else {
            executor.scheduleAtFixedRate(new StubbornSend(), 150, 300, TimeUnit.MILLISECONDS);
        }
        
    }


    public synchronized void send(Host host, MsgPacket packet) {
        packet.setPacketId(packetIdCounter.getAndIncrement());
        packet.setLastHop(selfHost.getId());
        
        // Do not send over network packets to ourself
        if(host.isSelfHost()) {
            addPacketToConsume(packet);
            
        } else {
            try {
                host.getSent().put(packet.getPacketId(), packet);
                host.getSentSize().getAndIncrement();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Awaits for the reception of one packet, and adds it to the {@link PerfectLink#packetsToConsume} queue if it
     * can be delivered and consumed by the upper layer with {@link PerfectLink#pollDeliveredPacket()}
     */
    public void deliver() {

        try {
            byte[] data = fll.deliver();
            Object obj = Packet.deSerialize(data);

            if(obj instanceof MsgPacket) {
                handleMsgPacket((MsgPacket) obj);

            } else if(obj instanceof AcksPacket) {
                handleAcksPacket((AcksPacket) obj);
            }
            
        } catch (Exception e) {
            // Data buffer received was too big, buffe size is incremented for next try
            System.out.println("Buf size adjusted");
            fll.adjustBufSize();
        }

    }


    /**
     * Polls a packet from the {@link PerfectLink#packetsToConsume} and returns it.
     * @return the packet retrieved or NULL if the queue was empty.
    * @throws InterruptedException 
    */
    public MsgPacket pollDeliveredPacket(long timeOut, TimeUnit unit) throws InterruptedException {
        return packetsToConsume.poll(timeOut, unit);
    }

    private void addPacketToConsume(MsgPacket packet) {
        try {
            packetsToConsume.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Adds the packet to the pollDeliveredPacket queue to be used by the upper layer if indeed can be delivered.
     * @param packet the packet to check if it can be delivered
     */
    private void handleMsgPacket(MsgPacket packet) {
        int packetId = packet.getPacketId();
        int senderTimeStamp = packet.getTimeStamp();
        int senderIndex = packet.getLastHopIndex();

        Host sender = hosts.get(senderIndex);
        int lastTimeStamp = sender.getLastTimeStamp();
        ConcurrentMap<Integer,Boolean> senderDelivered = sender.getDelivered();

        // Test if packet already delivered and id is not older than last ack
        if(senderTimeStamp > lastTimeStamp && !senderDelivered.containsKey(packetId) ) {
            senderDelivered.put(packetId, false);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            sender.getPendingAcks().add(packetId);

            addPacketToConsume(packet);
        }

    }

    private void handleAcksPacket(AcksPacket packet) {
        if(packet.getAckStep() == AcksPacket.ACK_RECEIVER) {
            handleAckFromReceiver(packet);
        }

        else if(packet.getAckStep() == AcksPacket.ACK_SENDER) {
            handleAckFromSender(packet);
        }
    }

    private void handleAckFromReceiver(AcksPacket packet) {
        BlockingQueue<Integer> acksQueue = packet.getAcks();
        Host receiver = hosts.get(packet.getHostIndex());
        ConcurrentSkipListMap<Integer,Packet> receiverSent = receiver.getSent();
        AcksPacket ackOk = new AcksPacket(selfHost.getId(), packet.getHostId());

        ReentrantLock lock = sentMapsLocks.get(receiver.getIndex());
        lock.lock();
        for(int packetId : packet.getAcks()) {
            if(receiverSent.remove(packetId) == null) {
                break;
            }
            
            receiver.getSentSize().getAndDecrement();
        }
        lock.unlock();

        ackOk.setAcks(acksQueue);
        ackOk.setPacketId(packet.getPacketId());
        ackOk.setAckStep(AcksPacket.ACK_SENDER);

        fll.sendAckOk(hosts.get(ackOk.getTargetHostIndex()), ackOk);

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
        
        acks.remove(packet.getPacketId());
    }


    private class StubbornSendLattice implements Runnable {

        @Override
        public void run() {
            for(Host host : hosts) {
                ReentrantLock lock = sentMapsLocks.get(host.getIndex());

                if(lock.tryLock()) {
                    host.getSent().forEach((id, packet) -> fll.send(hosts.get(packet.getTargetHostIndex()), packet));
                    lock.unlock();
                }        

                acks.forEach((id, packet) -> fll.send(hosts.get(packet.getTargetHostIndex()), packet));

            }
        }
        
    }


    private class StubbornSend implements Runnable {

        @Override
        public void run() {
            boolean activateSend = false;

            for(Host host : hosts) {
                ReentrantLock lock = sentMapsLocks.get(host.getIndex());

                if(!activateSend && host.getSentSize().get() < 8) {
                    activateSend = true;
                }

                if(lock.tryLock()) {
                    host.getSent().forEach((id, packet) -> fll.send(hosts.get(packet.getTargetHostIndex()), packet));
                    lock.unlock();
                }        

            }

            acks.forEach((id, packet) -> fll.send(hosts.get(packet.getTargetHostIndex()), packet));

            if(activateSend) {
                scheduler.tryActivateSend();
            }
        }
        
    }


    private class AckBuildAndSend implements Runnable {

        private void sendAck(AcksPacket packet) {
            packet.setPacketId(ackIdCounter.getAndIncrement());
    
            try {
                acks.put(packet.getPacketId(), packet);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        // Extracts from waiting acks queue and puts them into a new acks queue ready to be sent.
        private BlockingQueue<Integer> buildAckQueue(BlockingQueue<Integer> pendingAcks) {
        
            BlockingQueue<Integer> ackQueueToSend = new LinkedBlockingDeque<>();
            int count = 0;
            while(!pendingAcks.isEmpty() && count < 128) {
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

                sendAck(packet);
            }
        }
        
    }
}