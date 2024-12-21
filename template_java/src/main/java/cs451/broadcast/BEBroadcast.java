package cs451.broadcast;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cs451.Host;
import cs451.control.Scheduler;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;

public class BEBroadcast implements Broadcast {

    private PerfectLink link;
    private List<Host> hosts;

    
    public BEBroadcast(PerfectLink link, Scheduler scheduler) {
        this.link = link;
        this.hosts = scheduler.getHosts();
    }

    @Override
    public void broadcast(MsgPacket basePacket) {
        for(Host host : hosts) {
            MsgPacket packet = new MsgPacket(basePacket, host.getId());
            link.send(host, packet);
        }
    }

    @Override
    public MsgPacket deliver() {
        MsgPacket packet = null;

        while(packet == null) {
            try {
                packet = link.pollDeliveredPacket(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        
        return packet;
    }

}
