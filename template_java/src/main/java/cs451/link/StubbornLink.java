package cs451.link;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packet.Packet;

/**
 * Use it inside PerfectLink to guarantee Stubborn delivery.
 */
public class StubbornLink {
    
    private ConcurrentMap<Integer,Packet> sent;
    private FairLossLink fll;
    private PerfectLink ppl;
    private Object sentLock;

    public StubbornLink(Host thisHost, List<Host> hosts, ScheduledExecutorService executor, AtomicInteger packetId, Object sentLock, PerfectLink ppl){
        try {
            fll = new FairLossLink(thisHost.getSocketReceive(), executor, sentLock, this);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        sent = thisHost.getSent();
        this.ppl = ppl;
        this.sentLock = sentLock;

        // Resend operation of stubbornLink that guarantees eventual delivery between correct processes.
        executor.scheduleWithFixedDelay(() -> {
            synchronized(this.sentLock) {
                sent.forEach((id, packet) -> {
                    packet.setTimeStamp(packetId.getAndIncrement());
                    fll.send(hosts.get(packet.getTargetHostIndex()), packet);
                });
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    

    public FairLossLink getFairLossLink() {
        return fll;
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
     * ack message for the ack ok message, starting an infinite loop of acks that need to be acked.
     * @param h the target host
     * @param p the ack ok packet to be sent
     */
    public void sendAckOk(Host h, Packet p) {
        fll.send(h, p); 
    }

    public void deliver(byte[] data) {
        ppl.deliver(data);
    }

    // Called by perfectLink to tell FairlossLink to increase buffer size
    public void fllIncreaseBufSize() {
        fll.adjustBufSize();
    }

}
