package cs451;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cs451.links.PerfectLink;
import cs451.parsers.Parser;

public class Scheduler {

    /**
     * Concurrent queue of sent messages
     */
    private Queue<Message> sent;
    private List<Host> hosts;
    private Host thisHost;
    private Logger logger;

    public Scheduler(Parser parser, Logger logger) throws SocketException, UnknownHostException {
        sent = new ConcurrentLinkedQueue<>();
        this.hosts = parser.hosts();
        thisHost = hosts.get(parser.myIndex());

        thisHost.setSocket(new DatagramSocket(thisHost.getPort(), InetAddress.getByName(thisHost.getIp())));
        thisHost.setOutputPath(parser.output());
        this.logger = logger;
    }
    
    public Queue<Message> getSent() {
        return sent;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    protected void runPerfect(int[] params) {
        System.out.println("ENTERING PERFECT LINKS MODE");
        //TODO perf links
        // Contains the values of the config file, first value m is number of messages to send, second value is receiver index
        int msgsToSend = params[0];
        int receiverId = params[1];

        if(receiverId == thisHost.getId()) {
            System.out.println("I am the receiver with ID: " + thisHost.getId() + ", delivering messages...");
            PerfectLink link = new PerfectLink(thisHost, hosts, logger);
            while(true){
                link.deliver();
            }
        }
        else {
            // Sender
            System.out.println("I am the sender with ID" + thisHost.getId() + ", sending messages...");
            PerfectLink link = new PerfectLink(thisHost, hosts, logger);
            for(int i = 1; i <= msgsToSend; i++) {
                Message m = new Message(thisHost.getId(), i, System.currentTimeMillis(), null);
                // We will probably need a blocking queue to not mess up sending concurrently
                link.send(hosts.get(receiverId-1), m);
                String line = "b " + i;
                logger.addLine(line);
            }
            // int i = 1;
            // while(true) {
            //     Message m = new Message(thisHost.getId(), i, System.currentTimeMillis(), null);
            //         // We will probably need a blocking queue to not mess up sending concurrently
            //     link.send(hosts.get(receiverId-1), m);
            //     logger.addLine("b " + i);
            //     //System.out.println("b " + i);
            //     i++;
            // }
        }

        /**
         * Close resources
         */
        //thisHost.getSocket().close();
    }

    protected void runFIFO(int[] params) {
        System.out.println("FIFO not yet implemented");
    }

    protected void runLattice(int[] params) {
        System.out.println("Lattice agreement not yet implemented");
    }
}
