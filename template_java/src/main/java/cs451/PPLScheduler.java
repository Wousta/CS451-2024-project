package cs451;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cs451.links.PerfectLink;
import cs451.packets.*;
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

        thisHost.setSocket(new DatagramSocket(thisHost.getPort(), InetAddress.getByName(thisHost.getIp())));
        thisHost.setOutputPath(parser.output());
        thisHost.initLists(hosts.size());
        this.logger = logger;
        this.executor = executor;
    }

    protected void runPerfectSender(int[] params) {
        int msgsToSend = params[0];
        int receiverId = params[1];

        System.out.println("I am the sender with ID" + thisHost.getId() + ", sending messages...");
        PerfectLink link = new PerfectLink(thisHost, hosts, logger, executor);
        MessageSender sender = new MessageSender(msgsToSend, receiverId, link);
        
        executor.execute(sender);
    }

    protected void runPerfectReceiver() {
        System.out.println("I am the receiver with ID: " + thisHost.getId() + ", delivering messages...");
        PerfectLink link = new PerfectLink(thisHost, hosts, logger, executor);

        
        // One thread permanently receives packets
        executor.execute( () -> {
            while(true) link.deliver();
        });


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
            MsgPacket packet = new MsgPacket(thisHost.getId(), packetId.getAndIncrement());
            for(int i = 0; i < msgsToAdd; i++) {
                // To string because payload can be any datatype and it only has to be logged, 
                // so parse to string to be able to cast to string when deserializing to log the message payload.
                byte[] payload = MsgPacket.serialize(Integer.toString(currentMsgId));

                packet.addMessage(new Message(thisHost.getId(), currentMsgId, payload));
                System.out.println("b " + currentMsgId);
                logger.addLine("b " + currentMsgId);
                ++currentMsgId;
            }
            link.send(hosts.get(receiverId-1), packet);
        }

        // public void sendPacket(int msgsToAdd, int currentMsgId) {
        //     byte[] payload = Packet.serialize(Integer.toString(currentMsgId));
        //     AcksPacket packet = new AcksPacket(thisHost.getId(), packetId.getAndIncrement(), new Message(thisHost.getId(), currentMsgId, payload));
        //     System.out.println("b " + currentMsgId);
        //     link.send(hosts.get(receiverId-1), packet);
        // }

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

    private class SendAck implements Runnable {
        private List<BlockingQueue<Integer>> pendingAcks;
        private PerfectLink link;

        public SendAck(List<BlockingQueue<Integer>> pendingAcks, PerfectLink link) {
            this.pendingAcks = pendingAcks;
            this.link = link;
        }

        @Override
        public void run() {
            for(BlockingQueue<Integer> queue : pendingAcks) {
                List<Integer> ackList = new ArrayList<>(32);
                queue.drainTo(ackList, 32);

                Message message = new Message(thisHost.getId(), packetId.getAndIncrement(), MsgPacket.serialize(ackList));

            }
        }
        
    }

}
