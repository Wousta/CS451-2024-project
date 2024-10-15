package cs451;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cs451.links.PerfectLink;
import cs451.parsers.Parser;

public class Scheduler {

    private List<Host> hosts;
    private Host thisHost;
    private Logger logger;

    public Scheduler(Parser parser, Logger logger) throws SocketException, UnknownHostException {
        this.hosts = parser.hosts();
        thisHost = hosts.get(parser.myIndex());

        thisHost.setSocket(new DatagramSocket(thisHost.getPort(), InetAddress.getByName(thisHost.getIp())));
        thisHost.setOutputPath(parser.output());
        this.logger = logger;
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
                byte[] payload = serialize(Integer.toString(i));
                Message m = new Message(thisHost.getId(), i, System.currentTimeMillis(), payload);
                assert payload.length != 0 : "Payload is empty";
                link.send(hosts.get(receiverId-1), m);
                logger.addLine("b " + i);
            }
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
}
