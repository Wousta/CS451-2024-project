package cs451.links;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Queue;

import cs451.Host;
import cs451.Message;
import cs451.Logger;

public class PerfectLink {

    private Queue<Message> delivered;
    private StubbornLink sl;
    private Logger logger;

    public PerfectLink(Host thisHost, List<Host> hosts, Logger logger){
        sl = new StubbornLink(thisHost, hosts, logger);
        delivered = thisHost.getDelivered();
        this.logger = logger;
    }

    // Thread to send
    public void send(Host h, Message m) {
        sl.send(h, m);
    }

    // Thread to deliver
    public void deliver() {
        Message msg = sl.deliver();

        if(!delivered.contains(msg)){
            delivered.offer(msg);
            
            // TODO: ack of message to not send it anymore
            logger.addLine("d " + msg.getSenderId() + " " + (String)deSerialize(msg.getData()));
            System.out.println("d " + msg.getSenderId() + " " + msg.getMsgId());
        }

        else System.out.println("message already delivered id: " + msg.getMsgId() + " sender: " + msg.getSenderId());
    }

    private Object deSerialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception e) {
            System.out.println("Error deserializing");
            e.printStackTrace();
        }

        // Returning null message, should not be happening"
        return null;
    }
}