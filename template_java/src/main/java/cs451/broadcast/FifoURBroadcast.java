package cs451.broadcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import cs451.control.Scheduler;
import cs451.packet.MsgPacket;

public class FifoURBroadcast implements Broadcast {

    private final int hostsSize;
    private URBroadcast urBroadcast;
    private int[] next;
    private List<PriorityBlockingQueue<MsgPacket>> pendingList;

    public FifoURBroadcast(URBroadcast urBroadcast, Scheduler scheduler) {
        this.urBroadcast = urBroadcast;
        this.hostsSize = scheduler.getHosts().size();

        this.next = new int[hostsSize];
        Arrays.fill(this.next, 1);
        
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
    public MsgPacket deliver() {

        MsgPacket result = null;

        while(result == null) {
            MsgPacket packet = urBroadcast.deliver();
            pendingList.get(packet.getHostIndex()).add(packet);
    
            int i = 0;
            for(BlockingQueue<MsgPacket> pending : pendingList) {
                if(!pending.isEmpty() && pending.peek().getOriginalId() == next[i]) {
                    ++next[i];
                    result = pending.poll();
                    break;
                }
    
                ++i;
            }
        }

        return result;
        
    }

}
