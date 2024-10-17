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
    private SLTimerTask slTask; 
    private FairLossLink fll;


    public StubbornLink(Host thisHost, List<Host> hosts, ScheduledExecutorService executor){
        slTask = new SLTimerTask(thisHost);
        fll = new FairLossLink(thisHost.getSocket());
        sent = thisHost.getSent();
        this.hosts = hosts;

        executor.scheduleWithFixedDelay(slTask, 200, 1000, TimeUnit.MILLISECONDS);
    }

    public void send(Host h, Packet p) {
        //System.out.println("sending message: " + m.getMsgId());
        slTask.setDestinationHost(h);
        fll.send(h, p);
        sent.put(p.getPacketId(), p);
    }

    public Packet deliver() {
        return fll.deliver();
    }

    /**
     * Timer service of StubbornLinks, it resends all messages
     * in intervals using TimerTask java library class spec.
     */
    private class SLTimerTask implements Runnable{
        private Host destinationHost;

        public SLTimerTask(Host h) {
            destinationHost = h;
        }

        @Override
        public void run() {
            System.out.println("Running timerTask StubbornLinks con sent size: " + sent.size());
            //logger.addLine("Running timerTask StubbornLinks con sent size: " + sent.size());
            sent.forEach((id, packet) -> fll.send(destinationHost, packet));
        }

        public void setDestinationHost(Host destinationHost) {
            this.destinationHost = destinationHost;
        }
    }
}
