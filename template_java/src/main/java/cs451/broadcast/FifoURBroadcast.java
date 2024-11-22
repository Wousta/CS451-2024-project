package cs451.broadcast;

import java.util.List;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class FifoURBroadcast implements Broadcast {

    private final int hostsSize;
    private PerfectLink link;
    private Host selfHost;
    private List<Host> hosts;
    private Logger logger;

    public FifoURBroadcast(PerfectLink link, Host selfHost, List<Host> hosts, Logger logger) {
        this.hostsSize = hosts.size();
        this.link = link;
    }

    @Override
    public void broadcast(MsgPacket packet) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'broadcast'");
    }

    @Override
    public void deliver(MsgPacket packet) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deliver'");
    }

}
