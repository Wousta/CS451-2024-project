package cs451;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.broadcast.Broadcast;
import cs451.broadcast.FifoURBroadcast;
import cs451.broadcast.URBroadcast;
import cs451.link.PerfectLink;
import cs451.packet.Message;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;
import cs451.parser.Logger;
import cs451.parser.Parser;

public class Scheduler {
    
    private static final int MSGS_TO_SEND_INDEX = 0;
    private static final int RECEIVER_ID_INDEX = 1;

    private List<Host> hosts;
    private Host selfHost;
    private Logger logger;
    private ScheduledExecutorService executor;
    private int[] input;
    private LoadBalancer loadBalancer;

    public Scheduler(Parser parser, Logger logger, ScheduledExecutorService executor, int[] input) throws SocketException, UnknownHostException {
        this.hosts = parser.hosts();
        this.selfHost = hosts.get(parser.myIndex());
        this.logger = logger;
        this.executor = executor;
        this.input = input;

        // Only one socket for receiving allowed
        selfHost.setSocketReceive(new DatagramSocket(selfHost.getPort(), InetAddress.getByName(selfHost.getIp())));
        selfHost.setOutputPath(parser.output());

        loadBalancer = new LoadBalancer(hosts.size(), input[MSGS_TO_SEND_INDEX]);
    }


    // Sends messages to one host, receives acks from that host, sends back ack ok.
    protected void runPerfectLinks() {
        int msgsToSend = input[MSGS_TO_SEND_INDEX];
        int receiverId = input[RECEIVER_ID_INDEX];
        Host targeHost = hosts.get(receiverId - 1);
        PerfectLink link = new PerfectLink(selfHost, hosts, logger, executor);
        MessageSender sender = new MessageSender(msgsToSend, targeHost, link);
        
        if(selfHost.getId() != receiverId) {
            executor.execute(sender);
        }
        
        executor.execute(() -> {
            while(true) {
                link.getFairLossLink().deliver();
            }
        });
    }


    protected void runFIFOBroadcast() {
        int msgsToSend = input[MSGS_TO_SEND_INDEX];
        PerfectLink link = new PerfectLink(selfHost, hosts, logger, executor);
        //Broadcast broadcast = new URBroadcast(link, selfHost, hosts, logger);
        Broadcast broadcast = new FifoURBroadcast(link, selfHost, hosts, logger);
        MessageSender sender = new MessageSender(msgsToSend, broadcast);
        
        executor.execute(sender);

        executor.execute(() -> {
            while(true) {
                link.getFairLossLink().deliver();
            }
        });

    }

    
    private class MessageSender implements Runnable {
        private int msgsToSend;
        private Host targetHost;
        private PerfectLink link;
        private Broadcast broadcast;
        private int originalId = 0;

        public MessageSender(int msgsToSend, Host targetHost, PerfectLink link){
            this.msgsToSend = msgsToSend;
            this.link = link;
            this.targetHost = targetHost;
        }

        public MessageSender(int msgsToSend, Broadcast broadcast){
            this.msgsToSend = msgsToSend;
            this.broadcast = broadcast;
        }

        // Adds up to 8 messages to a new packet and sends it to the receiver Host.
        public void sendPacket(int msgsToAdd, int currentMsgId) {
            byte thisHostId = selfHost.getId();
            MsgPacket packet = new MsgPacket(thisHostId, ++originalId, new BitSet(hosts.size()));

            for(int i = 0; i < msgsToAdd; i++) {
                // To string because payload can be any datatype and it only has to be logged, 
                // so parse to string to be able to cast to string when deserializing to log the message payload.
                byte[] payload = Packet.serialize(Integer.toString(currentMsgId));

                packet.addMessage(new Message(thisHostId, currentMsgId, payload));
                logger.addLine("b " + currentMsgId);
                ++currentMsgId;
            }

            if(input.length == Constants.FIFO) {
                broadcast.broadcast(packet);
            }
            else if(input.length == Constants.PERFECT_LINK) {
                packet.setTargetHostId(targetHost.getId());
                link.send(targetHost, packet);
            }    
        }

        @Override
        public void run() {
            int msgId = 1;
            int msgsPerPacket = MsgPacket.MAX_MSGS;
            int iters = msgsToSend/msgsPerPacket; // Each packet can store up to 8 messages
            int lastIters = msgsToSend % msgsPerPacket; // Remaining messages
            int maxSentSize = 32;//loadBalancer.getSentMaxSize();//132; // Maximum size of the sent messages data structure

            for(int i = 0; i < iters; i++) {
                // It waits before sending messages if sent size gets to a limit, to avoid consuming all memory.
                while(selfHost.getSent().size() >= maxSentSize) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        System.err.println("Thread stopped while waiting to send more packets, this is expected if program is stopped mid execution");
                        e.printStackTrace();
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

}
