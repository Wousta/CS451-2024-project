package cs451.link;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
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
    private AtomicInteger idCounter = new AtomicInteger(1);
    private List<ReentrantLock> sentMapsLocks;

    public PerfectLink(Host selfHost, List<Host> hosts, Logger logger, ScheduledExecutorService executor){
        try {
            fll = new FairLossLink(selfHost.getSocketReceive(), executor, this);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;

        sentMapsLocks = new ArrayList<>(hosts.size());
        for(int i = 0; i < hosts.size(); i++) {
            sentMapsLocks.add(new ReentrantLock());
        }

        AckBuildAndSend ackBuildAndSend = new AckBuildAndSend();
        //if(selfHost.getId() == 1) // TODO:  remember to comment when testing broadcast
        executor.scheduleAtFixedRate(ackBuildAndSend, 50, 150, TimeUnit.MILLISECONDS);

        // Resend operation of stubbornLink that guarantees eventual delivery between correct processes.
        executor.scheduleAtFixedRate(() -> {
            for(Host host : hosts) {
                ReentrantLock lock = sentMapsLocks.get(host.getIndex());

                if(lock.tryLock()) {
                    host.getSent().forEach((id, packet) -> fll.send(hosts.get(packet.getTargetHostIndex()), packet));
                    lock.unlock();
                }        
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


    public void send(Host host, Packet packet) {
        if(packet.getPacketId() == 0) {
            packet.setPacketId(idCounter.getAndIncrement());
        }

        try {
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
        Host receiver = hosts.get(packet.getHostIndex());
        ConcurrentSkipListMap<Integer,Packet> receiverSent = receiver.getSent();
        AcksPacket ackOk = new AcksPacket(selfHost.getId(), packet.getHostId());
        ReentrantLock lock = sentMapsLocks.get(receiver.getIndex());

        lock.lock();
        for(int packetId : packet.getAcks()) {
            if(receiverSent.remove(packetId) == null) {
                break;
            }
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
        
        host.getSent().remove(packet.getPacketId());
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