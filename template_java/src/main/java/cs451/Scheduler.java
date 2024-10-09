package cs451;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cs451.links.FairLossLink;
import cs451.links.StubbornLink;

public class Scheduler {

    /**
     * Concurrent queue of sent messages
     */
    private Queue<Message> sent;
    private List<Host> hosts;

    /**
     * Id of this host
     */
    private int myId;
    private Host thisHost;
    

    public Scheduler(int myId, List<Host> hosts) throws SocketException, UnknownHostException {
        sent = new ConcurrentLinkedQueue<>();
        this.hosts = hosts;
        this.myId = myId;
        thisHost = hosts.get(myId);

        thisHost.setSocket(new DatagramSocket(thisHost.getPort(), InetAddress.getByName(thisHost.getIp())));
    }
    
    public Queue<Message> getSent() {
        return sent;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    protected void run(String config) {
        System.out.println("ENTERING PERFECT LINKS MODE");
        //TODO perf links
        // Contains the values of the config file, first value m is number of messages to send, second value is receiver index
        int msgsToSend;
        int receiverId;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(config))) {

            String[] parts = reader.readLine().trim().split("\\s+");
            msgsToSend = Integer.parseInt(parts[0]);
            receiverId = Integer.parseInt(parts[1]);

        } catch (Exception e) {

            System.err.println("Error while reading perfect links config");
            e.printStackTrace();
            thisHost.getSocket().close();
            return;

        }

        if(receiverId == myId){

            System.out.println("I am the receiver with ID: " + myId + ", delivering messages...");
            StubbornLink link = new StubbornLink(thisHost, hosts);
            link.deliver();

        }
        else {
            // Sender
            StubbornLink link = new StubbornLink(thisHost, hosts);
            for(int i = 0; i < msgsToSend; i++) {

                link.send(hosts.get(receiverId-1), new Message(thisHost.getId(), i));
                System.out.println("b " + i);

            }
        }

        /**
         * Close resources
         */
        //thisHost.getSocket().close();
    }
}
