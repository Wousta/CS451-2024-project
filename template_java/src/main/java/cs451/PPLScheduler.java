package cs451;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.links.PerfectLink;
import cs451.packets.AcksPacket;
import cs451.packets.Message;
import cs451.packets.MsgPacket;
import cs451.packets.Packet;
import cs451.parsers.Logger;
import cs451.parsers.Parser;

public class PPLScheduler {
    private AtomicInteger packetId = new AtomicInteger(1);
    private List<Host> hosts;
    private Host selfHost;
    private Logger logger;
    private ScheduledExecutorService executor;

    public PPLScheduler(Parser parser, Logger logger, ScheduledExecutorService executor) throws SocketException, UnknownHostException {
        this.hosts = parser.hosts();
        selfHost = hosts.get(parser.myIndex());

        selfHost.setSocketReceive(new DatagramSocket(
            selfHost.getPort(), 
            InetAddress.getByName(selfHost.getIp())
        ));

        selfHost.setOutputPath(parser.output());
        selfHost.initLists(hosts.size());
        this.logger = logger;
        this.executor = executor;
    }

    protected void runPerfectSender(int[] params) {
        int msgsToSend = params[0];
        int receiverId = params[1];

        PerfectLink link = new PerfectLink(
            selfHost, 
            hosts, 
            logger, 
            executor, 
            packetId
        );

        MessageSender sender = new MessageSender(
            msgsToSend, 
            receiverId, 
            link
        );

        executor.execute( () -> {
            while(true) link.deliver();
        });

        executor.execute(sender);
    }

    protected void runPerfectReceiver() {
        PerfectLink link = new PerfectLink(
            selfHost, 
            hosts, 
            logger, 
            executor, 
            packetId
        );
        AckBuildAndSend ackBuildAndSend = new AckBuildAndSend(link);
        
        // One thread permanently receives packets
        executor.execute( () -> {
            while(true) link.deliver();
        });

        executor.scheduleAtFixedRate(
            ackBuildAndSend, 
            50, 
            50, 
            TimeUnit.MILLISECONDS
        );
    }

    public void sendPacket(int msgsToAdd, int currentMsgId, int receiverId, PerfectLink link) {
        Host targetHost = hosts.get(receiverId-1);
        byte thisHostId = selfHost.getId();
        MsgPacket packet = new MsgPacket(thisHostId, targetHost.getId(), packetId.getAndIncrement());

        for(int i = 0; i < msgsToAdd; i++) {
            // To string because payload can be any datatype and it only has to be logged, 
            // so parse to string to be able to cast to string when deserializing to log the message payload.
            byte[] payload = Packet.serialize(Integer.toString(currentMsgId));

            packet.addMessage(new Message(thisHostId, currentMsgId, payload));
            //System.out.println("b " + currentMsgId);
            logger.addLine("b " + currentMsgId);
            ++currentMsgId;
        }
        //logger.addLine("MESSAGES IN PACKET = " + packet.getMessages().size());
        link.send(targetHost, packet);
    }

    private class MessageSender implements Runnable {
        private int msgsToSend;
        int receiverId;
        PerfectLink link;

        public MessageSender(int msgsToSend, int receiverId, PerfectLink link){
            this.msgsToSend = msgsToSend;
            this.link = link;
            this.receiverId = receiverId;
        }

        public void sendPacket(int msgsToAdd, int currentMsgId) {
            Host targetHost = hosts.get(receiverId-1);
            byte thisHostId = selfHost.getId();
            MsgPacket packet = new MsgPacket(thisHostId, targetHost.getId(), packetId.getAndIncrement());

            for(int i = 0; i < msgsToAdd; i++) {
                // To string because payload can be any datatype and it only has to be logged, 
                // so parse to string to be able to cast to string when deserializing to log the message payload.
                byte[] payload = Packet.serialize(Integer.toString(currentMsgId));

                packet.addMessage(new Message(thisHostId, currentMsgId, payload));
                //System.out.println("b " + currentMsgId);
                logger.addLine("b " + currentMsgId);
                ++currentMsgId;
            }

            link.send(targetHost, packet);
        }

        @Override
        public void run() {
            int msgId = 1;
            int msgsPerPacket = MsgPacket.MAX_MSGS;
            int iters = msgsToSend/msgsPerPacket; // Each packet can store up to 8 messages
            int lastIters = msgsToSend % msgsPerPacket;
            //System.out.println("Iters = " + iters + " lastIters = " + lastIters);
            for(int i = 0; i < iters; i++) {
                while(selfHost.getSent().size() > 132) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.err.println("EXPECTED EXCEPTION Mensajes enviados: " + msgId + " Tid: " + Thread.currentThread().getId());
                        //e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                } 
                sendPacket(msgsPerPacket, msgId);
                msgId += 8;
            }
            // Send remaining messages
            if(lastIters > 0) {
                sendPacket(lastIters, msgId);
            }
        }
    }

    private class AckBuildAndSend implements Runnable {
        private PerfectLink link;

        public AckBuildAndSend(PerfectLink link) {
            this.link = link;
        }

        @Override
        public void run() {
            int indexOfTargetHost = 0;
            for(BlockingQueue<Integer> pendingAcksQueue : selfHost.getPendingAcks()) {
                if(pendingAcksQueue.isEmpty()) {
                    ++indexOfTargetHost;
                    continue;
                }
                Host targetHost = hosts.get(indexOfTargetHost);
                ++indexOfTargetHost;
                BlockingQueue<Integer> ackQueue = new LinkedBlockingDeque<>();

                //queue.drainTo(ackQueue, 32);
                int count = 0;
                while(!pendingAcksQueue.isEmpty() && count < 256) {
                    try {
                        ackQueue.add(pendingAcksQueue.poll(100000, TimeUnit.MILLISECONDS));
                    } catch (InterruptedException e) {
                        System.err.println("Sleeping thread got interrupted while waiting for an ack to be polled");
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    ++count;
                }
                
                //System.out.println("Sending acklist ACKbuild and send: " + ackList);
                AcksPacket packet = new AcksPacket(
                    selfHost.getId(), 
                    targetHost.getId(), 
                    packetId.getAndIncrement(), 
                    ackQueue
                );
                //System.out.println("Sending acks packet id: " + packet.getPacketId());
                link.send(targetHost, packet);
            }
        }
        
    }

}
