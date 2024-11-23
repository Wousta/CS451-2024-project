package cs451.broadcast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class FifoURBroadcast implements Broadcast {

    private final int hostsSize;
    private URBroadcast urBroadcast;
    private Logger logger;
    private int[] next;
    List<PriorityBlockingQueue<MsgPacket>> pendingList;

    public FifoURBroadcast(PerfectLink link, Host selfHost, List<Host> hosts, Logger logger) {
        this.hostsSize = hosts.size();
        this.logger = logger;
        this.next = new int[hostsSize];
        Arrays.fill(this.next, 1);

        this.urBroadcast = new URBroadcast(link, selfHost, hosts, logger);
        this.urBroadcast.setFifoURBroadcast(this);

        pendingList = new ArrayList<>(hostsSize);
        for(int i = 0; i < hostsSize; i++) {
            pendingList.add(new PriorityBlockingQueue<>(
                10000, 
                Comparator.comparing(MsgPacket::getOriginalId)
            ));
        }
    }

    @Override
    public void broadcast(MsgPacket packet) {
        urBroadcast.broadcast(packet);
    }

    @Override
    public void deliver(MsgPacket packet) throws ClassNotFoundException, IOException {
        pendingList.get(packet.getHostIndex()).add(packet);

        int i = 0;
        for(BlockingQueue<MsgPacket> pending : pendingList) {
            while(!pending.isEmpty() && pending.peek().getOriginalId() == next[i]) {
                ++next[i];
                logger.logPacket(pending.poll());
            }

            ++i;
        }
    }

}
