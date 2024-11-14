package cs451.broadcast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;
import cs451.parser.Logger;

public class URBroadcast implements Broadcast {
    private PerfectLink link;
    private List<Host> hosts;
    private Logger logger;
    private BEBroadcast beBroadcast;
    private BlockingQueue<Packet> pending = new LinkedBlockingQueue<>();
    private BlockingQueue<Packet> delivered = new LinkedBlockingQueue<>();
    private ConcurrentNavigableMap<Integer, Packet> acks = new ConcurrentSkipListMap<>();

    public URBroadcast(PerfectLink link, List<Host> hosts, Logger logger) {
        this.link = link;
        this.hosts = hosts;
        this.logger = logger;
        this.beBroadcast = new BEBroadcast(link, hosts, logger);
        this.beBroadcast.setUrBroadcast(this);
    }

    public void broadcast(MsgPacket p) {
        pending.add(p);
        beBroadcast.broadcast(p);
    }

    /**
     * Triggered by PerfectLink when it delivers a message
     * @param p the packet delivered by perfect links
     */
    public void deliver(MsgPacket p) {
        try {
            logger.logPacket(p);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }
}
