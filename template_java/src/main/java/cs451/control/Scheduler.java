package cs451.control;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import cs451.Constants;
import cs451.Host;
import cs451.broadcast.BEBroadcast;
import cs451.broadcast.Broadcast;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;
import cs451.parser.Parser;

public class Scheduler {
    
    private static final int MSGS_TO_SEND_INDEX = 0;
    private static final int RECEIVER_ID_INDEX = 1;
    private static final int PACKETS_PER_SEND_EXECUTION = 32;

    private List<Host> hosts;
    private Host selfHost;
    private Logger logger;
    private ScheduledExecutorService executor;
    private int[] input;
    private LoadBalancer loadBalancer;

    // The id counter for messages and the number of send executions to send all messages
    private final int sendIters;
    private final int messagesToSendRemainder;

    // Runnable task for sending the messages
    private MessageSender sender;
    private boolean senderExecutionFinished = false;
    private boolean allPacketsSent = false;

    public Scheduler(Parser parser, Logger logger, ScheduledExecutorService executor, int[] input) throws SocketException, UnknownHostException {
        this.hosts = parser.hosts();
        this.selfHost = hosts.get(parser.myIndex());
        this.logger = logger;
        this.executor = executor;
        this.input = input;

        sendIters = input[MSGS_TO_SEND_INDEX]/MsgPacket.MAX_MSGS;
        messagesToSendRemainder = input[MSGS_TO_SEND_INDEX] % MsgPacket.MAX_MSGS; 

        // Only one socket for receiving allowed
        selfHost.setSocketReceive(new DatagramSocket(selfHost.getPort(), InetAddress.getByName(selfHost.getIp())));
        selfHost.setOutputPath(parser.output());

        loadBalancer = new LoadBalancer(hosts.size());
    }


    public List<Host> getHosts() {
        return hosts;
    }


    public Host getSelfHost() {
        return selfHost;
    }


    public Logger getLogger() {
        return logger;
    }


    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }


    // Sends messages to one host, receives acks from that host, sends back ack ok.
    public void runPerfectLinks() {
        int receiverId = input[RECEIVER_ID_INDEX];
        Host targeHost = hosts.get(receiverId - 1);
        PerfectLink link = new PerfectLink(executor, this);

        sender = new MessageSender(targeHost, link);
        if(selfHost.getId() != receiverId) {
            executor.execute(sender);
        }
        
        executor.execute(() -> {
            while(true) {
                link.deliver();
            }
        });
    } 


    public void runFIFOBroadcast() {
        PerfectLink link = new PerfectLink(executor, this);
        BEBroadcast beBroadcast = new BEBroadcast(link, this);
        BEBroadcast broadcast = new BEBroadcast(link, this);
        //Broadcast broadcast = new FifoURBroadcast(new URBroadcast(beBroadcast, this), this);

        sender = new MessageSender(broadcast);  
        executor.execute(sender);

        executor.execute(() -> {
            while(true) {
                logger.logPacket(broadcast.deliver());
            }
        });

    }


    public void runLatticeAgreement(String config) throws IOException {
        PerfectLink link = new PerfectLink(executor, this);
        Broadcast broadcast = new BEBroadcast(link, this);

        BufferedReader reader = new BufferedReader(new FileReader(config));



        reader.close();
        
    }


    public void tryActivateSend() {
        // Used boolean because this function is always called sequentially by perfect link stubbornSend task
        if(senderExecutionFinished && !allPacketsSent) {
            senderExecutionFinished = false;
            executor.execute(sender); // Will set boolean to true after finishing, avoids multiple calls to this line
        }
    }

    
    private class MessageSender implements Runnable {
        private Host targetHost;
        private PerfectLink link;
        private Broadcast broadcast;
        private int originalId = 0;
        private int currentIter = 0;
        private int msgId = 1;

        public MessageSender(Host targetHost, PerfectLink link){
            this.link = link;
            this.targetHost = targetHost;
        }

        public MessageSender(Broadcast broadcast){
            this.broadcast = broadcast;
        }

        // Adds up to 8 messages to a new packet and sends it to the receiver Host.
        public void sendPacket(int msgsToAdd, int currentMsgId) {
            byte thisHostId = selfHost.getId();
            MsgPacket packet = new MsgPacket(thisHostId, ++originalId, new BitSet(hosts.size()));

            for(int i = 0; i < msgsToAdd; i++) {
                // To string because payload can be any datatype and it only has to be logged, 
                // so parse to string to be able to cast to string when deserializing to log the message payload.

                packet.addMessage(Integer.toString(currentMsgId));
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

            // Send remaining messages, we do not set finished to true because all messages are sent
            if(currentIter == sendIters) {
                if(messagesToSendRemainder > 0) {
                    sendPacket(messagesToSendRemainder, msgId);
                }

                allPacketsSent = true;
                return;
            }

            for(int i = 0; i < PACKETS_PER_SEND_EXECUTION; i++) {
                sendPacket(MsgPacket.MAX_MSGS, msgId);
                msgId += 8;
                currentIter++;
                if(currentIter == sendIters) {
                    break;
                }
            }

            senderExecutionFinished = true;

        }
    }

}
