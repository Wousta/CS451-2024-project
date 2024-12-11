package cs451.broadcast;

import java.io.IOException;
import java.util.List;

import cs451.Host;
import cs451.control.Scheduler;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class BEBroadcast implements Broadcast {

    private PerfectLink link;
    private List<Host> hosts;
    private Logger logger;

    
    public BEBroadcast(PerfectLink link, Scheduler scheduler) {
        this.link = link;
        this.hosts = scheduler.getHosts();
        this.logger = scheduler.getLogger();
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
        return link.deliver();
    }

}
