package cs451.link;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import cs451.Host;
import cs451.broadcast.URBroadcast;
import cs451.control.Scheduler;
import cs451.packet.AcksPacket;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;

public class PerfectLink {

    private Scheduler scheduler;
    private FairLossLink fll;
    private URBroadcast urBroadcast;
    private Host selfHost;
    private List<Host> hosts;
    private List<ReentrantLock> sentMapsLocks;
    private AtomicInteger ackIdCounter = new AtomicInteger(1);
    private AtomicInteger idCounter = new AtomicInteger(1);
    private ConcurrentMap<Integer,Packet> acks = new ConcurrentHashMap<>();

    public PerfectLink(ScheduledExecutorService executor, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.selfHost = scheduler.getSelfHost();
        this.hosts = scheduler.getHosts();

        try {
            fll = new FairLossLink(selfHost.getSocketReceive(), executor, this);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        sentMapsLocks = new ArrayList<>(hosts.size());
        for(int i = 0; i < hosts.size(); i++) {
            sentMapsLocks.add(new ReentrantLock());
        }

        executor.scheduleAtFixedRate(new AckBuildAndSend(), 100, 150, TimeUnit.MILLISECONDS);

        // Resend operation of stubbornLink that guarantees eventual delivery between correct processes.
        executor.scheduleAtFixedRate(new StubbornSend(), 150, 100, TimeUnit.MILLISECONDS);
        
    }


    public FairLossLink getFairLossLink() {
        return fll;
    }


    public URBroadcast getBEBroadcast() {
        return urBroadcast;
    }


    public void setURBroadcast(URBroadcast beBroadcast) {
        this.urBroadcast = beBroadcast;
    }


    public void send(Host host, MsgPacket packet) {
        packet.setPacketId(idCounter.getAndIncrement());
        host.getSentSize().getAndIncrement();

        try {
            packet.setLastHop(selfHost.getId());
            host.getSent().put(packet.getPacketId(), packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
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
        int senderIndex = packet.getLastHopIndex();

        Host sender = hosts.get(senderIndex);
        int lastTimeStamp = sender.getLastTimeStamp();
        ConcurrentMap<Integer,Boolean> senderDelivered = sender.getDelivered();

        // Test if packet already delivered and id is not older than last ack
        if(!senderDelivered.containsKey(packetId) && senderTimeStamp > lastTimeStamp) {
            senderDelivered.put(packetId, false);

            // Add id of packet to pending packets to be acked, we only send Ids for acking.
            sender.getPendingAcks().add(packetId);

            if(urBroadcast != null) {
                urBroadcast.beBDeliver(packet);
            } else {
                scheduler.getLogger().logPacket(packet);
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

                sendAck(packet);
            }
        }
        
    }
}