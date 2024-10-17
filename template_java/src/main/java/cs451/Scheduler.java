package cs451;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import cs451.packets.Message;
import cs451.packets.Packet;
import cs451.parsers.Logger;
import cs451.parsers.Parser;

public class Scheduler {
    private AtomicInteger packetId = new AtomicInteger();
    private List<Host> hosts;
    private Host thisHost;
    private Logger logger;
    private ScheduledExecutorService executor;

    public Scheduler(Parser parser, Logger logger, ScheduledExecutorService executor) throws SocketException, UnknownHostException {
        this.hosts = parser.hosts();
        thisHost = hosts.get(parser.myIndex());

        thisHost.setSocket(new DatagramSocket(thisHost.getPort(), InetAddress.getByName(thisHost.getIp())));
        thisHost.setOutputPath(parser.output());
        this.logger = logger;
        this.executor = executor;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    protected void runPerfectSender(int[] params) {
        int msgsToSend = params[0];
        int receiverId = params[1];

        System.out.println("I am the sender with ID" + thisHost.getId() + ", sending messages...");
        PerfectLink link = new PerfectLink(thisHost, hosts, logger, executor);
        Sender sender = new Sender(msgsToSend, receiverId, link);
        
        executor.execute(sender);
    }

    protected void runPerfectReceiver() {
        System.out.println("I am the receiver with ID: " + thisHost.getId() + ", delivering messages...");
        PerfectLink link = new PerfectLink(thisHost, hosts, logger, executor);
        while(true){
            link.deliver();
        }
    }

    // Code from: https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    private byte[] serialize(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Returning null byte array, should not be happening"
        return new byte[]{};
    }

    private class Sender implements Runnable {
        private int msgsToSend;
        int receiverId;
        PerfectLink link;

        public Sender(int msgsToSend, int receiverId, PerfectLink link){
            this.msgsToSend = msgsToSend;
            this.link = link;
            this.receiverId = receiverId;
        }

        @Override
        public void run() {
            Packet packet = new Packet(thisHost.getId(), packetId.getAndIncrement());
            for(int i = 1; i <= msgsToSend; i++) {
                byte[] payload = serialize(Integer.toString(i));
                Message m = new Message(thisHost.getId(), i, payload);
                assert payload.length != 0 : "Payload is empty";
                packet.addMessage(m);

                if(packet.getMessages().size() == 8) {
                    // TODO
                }

                //link.send(hosts.get(receiverId-1), m);
                logger.addLine("b " + i);
            }
        }
    }


}
