package cs451.links;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packets.Packet;

/**
 * Use it inside PerfectLink to guarantee Stubborn delivery.
 */
public class StubbornLink {

    private ConcurrentHashMap<Integer,Packet> sent;
    private FairLossLink fll;
    private final Object sentLock;

    public StubbornLink(Host thisHost, List<Host> hosts, ScheduledExecutorService executor, AtomicInteger packetId, Object sentLock){
        try {
            fll = new FairLossLink(thisHost.getSocketReceive());
        } catch (SocketException e) {
            e.printStackTrace();
        }

        sent = thisHost.getSent();
        this.sentLock = sentLock;
        executor.scheduleWithFixedDelay(() -> {
            System.out.println("\nTimerTask StubbornLinks con sent size: " + sent.size());
            int deliveredCount = 0;
            for(ConcurrentHashMap m : thisHost.getDelivered()) {
                deliveredCount += m.size();
            }
            System.out.println("Tamaño de delivered = " + deliveredCount);
            //System.out.println("sent: " + sent.keySet());
            //logger.addLine("Running timerTask StubbornLinks con sent size: " + sent.size());
            // synchronized(this.sentLock) {
            //     sent.forEach((id, packet) -> {
            //         packet.setTimeStamp(packetId.getAndIncrement());
            //         fll.send(hosts.get(packet.getTargetHostIndex()), packet);
            //     });
            // }
            sent.forEach((id, packet) -> {
                packet.setTimeStamp(packetId.getAndIncrement());
                fll.send(hosts.get(packet.getTargetHostIndex()), packet);
            });
        }, 300, 300, TimeUnit.MILLISECONDS);
    }

    public void send(Host h, Packet p) {
        fll.send(h, p);
        try {
            sent.put(p.getPacketId(), p); // Gets blocked here
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * When sending ack ok the packet should not be put in sent messages for resending.
     * This method is equivalent to send() except it does not store the message in sent map.
     * The reason is that if this ack ok does not arrive, the ack message will be resent from the receiver,
     * But if this ack ok is kept in sent messages, it would need to be cleaned and therefore needs another
     * ack message for the ack ok message, starting an infinite loop of acks.
     * @param h the target host
     * @param p the ack ok packet to be sent
     */
    public void sendAckOk(Host h, Packet p) {
        fll.send(h, p); 
    }

    public byte[] deliver() {
        return fll.deliver();
    }

    // Called by perfectLink to tell FairlossLink to increase buffer size
    public void fllIncreaseBufSize() {
        fll.adjustBufSize();
    }

}
