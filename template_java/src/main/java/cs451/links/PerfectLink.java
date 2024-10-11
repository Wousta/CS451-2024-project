package cs451.links;

import java.util.List;
import java.util.Queue;

import cs451.Host;
import cs451.Message;

public class PerfectLink {

    private Queue<Message> delivered;
    private StubbornLink sl;

    public PerfectLink(Host thisHost, List<Host> hosts){
        sl = new StubbornLink(thisHost, hosts);
        delivered = thisHost.getDelivered();
    }

    // Thread to send
    public void send(Host h, Message m) {
        sl.send(h, m);
    }

    // Thread to deliver
    public void deliver() {
        Message m = sl.deliver();

        if(!delivered.contains(m)){
            delivered.offer(m);
            
            // TODO: ack of message to not send it anymore

            // TODO: trigger actual ppl delivery and call logger
            System.out.println("d " + m.getSenderId() + " " + m.getMsgId() + " delivered size: " + delivered.size());
        }
        else System.out.println("mensaje ya se delivereo");
    }
}