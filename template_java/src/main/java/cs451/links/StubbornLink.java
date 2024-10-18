package cs451.links;

import cs451.Host;
import cs451.packets.Message;
import cs451.packets.Packet;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Use it inside PerfectLink to guarantee Stubborn delivery.
 */
public class StubbornLink {

    private ConcurrentMap<Integer,Packet> sent;
    private List<Host> hosts; 
    private SLMsgResender resend; 
    private FairLossLink fll;


    public StubbornLink(Host thisHost, List<Host> hosts, ScheduledExecutorService executor){
        resend = new SLMsgResender(thisHost);
        fll = new FairLossLink(thisHost.getSocket());
        sent = thisHost.getSent();
        this.hosts = hosts;

        executor.scheduleWithFixedDelay(resend, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public void send(Host h, Packet p) {
        resend.setDestinationHost(h);
        fll.send(h, p);
        try {
            sent.put(p.getPacketId(), p); // Gets blocked here
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Packet deliver() {
        return fll.deliver();
    }

    /**
     * It resends all messages in time intervals between each completed resend.
     */
    private class SLMsgResender implements Runnable{
        private Host destinationHost;

        public SLMsgResender(Host h) {
            destinationHost = h;
        }

        @Override
        public void run() {
            System.out.println("\nRunning timerTask StubbornLinks con sent size: " + sent.size());
            //logger.addLine("Running timerTask StubbornLinks con sent size: " + sent.size());
            System.out.println("Map: " + sent);
            sent.forEach((id, packet) -> fll.send(destinationHost, packet));
        }

        // StubbornLink does not know in constructor to what host it will send, so destination host has to be set in send().
        public void setDestinationHost(Host destinationHost) {
            this.destinationHost = destinationHost;
        }
    }
}
