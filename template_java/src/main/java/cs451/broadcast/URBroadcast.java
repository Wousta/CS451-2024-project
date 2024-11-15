package cs451.broadcast;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class URBroadcast implements Broadcast {
    private Host selfHost;
    private List<Host> hosts;
    private Logger logger;
    private BEBroadcast beBroadcast;
    private HashMap<Integer, Boolean> delivered = new HashMap<>();
    private HashMap<Integer, MsgPacket> pending = new HashMap<>();
    private ConcurrentNavigableMap<Integer, boolean[]> acksMap = new ConcurrentSkipListMap<>();

    public URBroadcast(PerfectLink link, Host selfHost, List<Host> hosts, Logger logger) {
        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.beBroadcast = new BEBroadcast(link, hosts, logger);
        this.beBroadcast.setUrBroadcast(this);
    }
    

    public void broadcast(MsgPacket packet) {
        pending.put(packet.getPacketId(), packet);
        beBroadcast.broadcast(packet);
    }


      /**
     * Triggered by PerfectLink when it delivers a message
     * @param packet the packet delivered by perfect links
     */
    public void beBDeliver(MsgPacket packet) {
        int packetId = packet.getPacketId();
        boolean[] pendingAcks = acksMap.get(packetId);
        
        // Set to true that we have received this packet from host[i]
        if(pendingAcks == null) {
            boolean[] acks = new boolean[hosts.size()];
            acks[packet.getLastHopIndex()] = true;
            acksMap.put(packetId, acks);
        } else {
            pendingAcks[packet.getLastHopIndex()] = true;
        }

        // Add to pending messages if first time and relay message
        if(!pending.containsKey(packetId)) {
            pending.put(packetId, packet);
            packet.setLastHop(selfHost.getId());
            beBroadcast.broadcast(packet);
        }
        else if(canDeliver(packet) && !delivered.containsKey(packetId)) {
            delivered.put(packetId, true);
            deliver(packet);
        }
    }


    public void deliver(MsgPacket packet) {
        try {
            logger.logPacket(packet);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }


    private boolean canDeliver(MsgPacket packet) {
        int ackedHosts = 0;
        boolean[] pendingAcks = acksMap.get(packet.getPacketId());
        
        for(int i = 0; i < pendingAcks.length; i++) {
            if(pendingAcks[i]) {
                ++ackedHosts;
            }
        }

        return ackedHosts > hosts.size() / 2;
    }

}
