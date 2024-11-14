package cs451.broadcast;

import java.io.IOException;
import java.util.List;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.Message;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;
import cs451.parser.Logger;

public class BEBroadcast implements Broadcast {

    private PerfectLink link;
    private URBroadcast urBroadcast;
    private List<Host> hosts;
    private Logger logger;

    public BEBroadcast(PerfectLink link, List<Host> hosts, Logger logger) {
        this.link = link;
        this.hosts = hosts;
        this.logger = logger;
        this.link.setBEBroadcast(this);
    }

    public void setUrBroadcast(URBroadcast urBroadcast) {
        this.urBroadcast = urBroadcast;
    }

    @Override
    public void broadcast(MsgPacket basePacket) {
        for(Host host : hosts) {
            MsgPacket packet = new MsgPacket(basePacket, host.getId());
            link.send(host, packet);
        }
    }

    /**
     * Triggered by PerfectLink when it delivers a message
     * @param p the packet delivered by perfect links
     */
    @Override
    public void deliver(MsgPacket p) {
        if(urBroadcast != null) urBroadcast.deliver(p);
        else try {
                logger.logPacket(p);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
    }
}