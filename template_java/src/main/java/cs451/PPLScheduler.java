package cs451;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.links.PerfectLink;
import cs451.packets.AcksPacket;
import cs451.packets.Message;
import cs451.packets.MsgPacket;
import cs451.parsers.Logger;
import cs451.parsers.Parser;

public class PPLScheduler {
    private AtomicInteger packetId = new AtomicInteger(1);
    private List<Host> hosts;
    private Host thisHost;
    private Logger logger;
    private ScheduledExecutorService executor;

    public PPLScheduler(Parser parser, Logger logger, ScheduledExecutorService executor) throws SocketException, UnknownHostException {
        this.hosts = parser.hosts();
        thisHost = hosts.get(parser.myIndex());

        thisHost.setSocket(new DatagramSocket(
            thisHost.getPort(), 
            InetAddress.getByName(thisHost.getIp())
        ));

        thisHost.setOutputPath(parser.output());
        thisHost.initLists(hosts.size());
        this.logger = logger;
        this.executor = executor;
    }

    protected void runPerfectSender(int[] params) {
        int msgsToSend = params[0];
        int receiverId = params[1];

        System.out.println("I am the sender with ID" + thisHost.getId() + ", sending messages...");
        PerfectLink link = new PerfectLink(
            thisHost, 
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
        
        executor.execute(sender);
    }

    protected void runPerfectReceiver() {
        System.out.println("I am the receiver with ID: " + thisHost.getId() + ", delivering messages...");
        PerfectLink link = new PerfectLink(
            thisHost, 
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

        executor.scheduleWithFixedDelay(
            ackBuildAndSend, 
            50, 
            100, 
            TimeUnit.MILLISECONDS
        );

        executor.scheduleWithFixedDelay( () -> {
            System.out.println("Sending acks");
            for(AcksPacket p : thisHost.getAckPacketsQueue()) {
                link.send(hosts.get(p.getHostIndex()), p);
            }
        }, 180, 200, TimeUnit.MILLISECONDS);
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
            byte thisHostId = thisHost.getId();
            MsgPacket packet = new MsgPacket(thisHostId, targetHost.getId(), packetId.getAndIncrement());

            for(int i = 0; i < msgsToAdd; i++) {
                // To string because payload can be any datatype and it only has to be logged, 
                // so parse to string to be able to cast to string when deserializing to log the message payload.
                byte[] payload = MsgPacket.serialize(Integer.toString(currentMsgId));

                packet.addMessage(new Message(thisHostId, currentMsgId, payload));
                System.out.println("b " + currentMsgId);
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
            System.out.println("Iters = " + iters + " lastIters = " + lastIters);
            for(int i = 0; i < iters; i++) {
                System.out.println("sendpacket================================");
                sendPacket(msgsPerPacket, msgId);
                msgId += 8;
            }
            // Send remaining messages
            sendPacket(lastIters, msgId);
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
            for(BlockingQueue<Integer> queue : thisHost.getPendingAcks()) {
                if(queue.isEmpty()) {
                    ++indexOfTargetHost;
                    continue;
                }
                Host targetHost = hosts.get(indexOfTargetHost);
                Queue<Integer> ackList = new LinkedList<>();

                queue.drainTo(ackList, 32);
                System.out.println("Sending acklist: " + ackList);

                AcksPacket packet = new AcksPacket(thisHost.getId(), targetHost.getId(), packetId.getAndIncrement(), ackList);
                assert thisHost.getAckPacketsQueue().offer(packet) : "Offer to getAckPacketsQueue failed";
                link.send(targetHost, packet);
                ++indexOfTargetHost;
            }
        }
        
    }

}
