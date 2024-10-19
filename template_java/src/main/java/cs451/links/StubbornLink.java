package cs451.links;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cs451.Host;
import cs451.packets.Packet;

/**
 * Use it inside PerfectLink to guarantee Stubborn delivery.
 */
public class StubbornLink {

    private ConcurrentMap<Integer,Packet> sent;
    private FairLossLink fll;


    public StubbornLink(Host thisHost, List<Host> hosts, ScheduledExecutorService executor){
        fll = new FairLossLink(thisHost.getSocket());
        sent = thisHost.getSent();

        executor.scheduleWithFixedDelay(() -> {
            System.out.println("\nRunning timerTask StubbornLinks con sent size: " + sent.size());
            //logger.addLine("Running timerTask StubbornLinks con sent size: " + sent.size());
            sent.forEach((id, packet) -> fll.send(hosts.get(packet.getHostIndex()), packet));
        }, 500, 1000, TimeUnit.MILLISECONDS);
    }

    public void send(Host h, Packet p) {
        fll.send(h, p);
        try {
            sent.put(p.getPacketId(), p); // Gets blocked here
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] deliver() {
        return fll.deliver();
    }

}
