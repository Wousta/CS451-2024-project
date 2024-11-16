package cs451.broadcast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class URBroadcast implements Broadcast {
    private final int hostsSize;
    private Host selfHost;
    private List<Host> hosts;
    private Logger logger;
    private BEBroadcast beBroadcast;
    private List<Map<Integer, Boolean>> urBdeliveredList;
    private List<Map<Integer, Boolean>> urBpendingList;
    private List<Map<Integer, boolean[]>> acksMapList;

    public URBroadcast(PerfectLink link, Host selfHost, List<Host> hosts, Logger logger) {
        this.hostsSize = hosts.size();
        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.beBroadcast = new BEBroadcast(link, hosts, logger);
        this.beBroadcast.setUrBroadcast(this);

        urBdeliveredList = new ArrayList<>(hostsSize);
        urBpendingList = new ArrayList<>(hostsSize);
        acksMapList = new ArrayList<>(hostsSize);

        for(int i = 0; i < hostsSize; i++) {
            urBdeliveredList.add(new HashMap<>());
            urBpendingList.add(new HashMap<>());
            acksMapList.add(new HashMap<>());
        }
    }


    public void broadcast(MsgPacket packet) {
        urBpendingList.get(packet.getHostIndex()).put(packet.getPacketId(), true);
        beBroadcast.broadcast(packet);
    }


      /**
     * Triggered by PerfectLink when it delivers a message
     * @param packet the packet delivered by perfect links
     */
    public void beBDeliver(MsgPacket packet) {

        int packetId = packet.getPacketId();
        int orignalSenderIndex = packet.getHostIndex();
        Map<Integer, Boolean> urBdelivered = urBdeliveredList.get(orignalSenderIndex);
        Map<Integer, Boolean> urBpending = urBpendingList.get(orignalSenderIndex);
        Map<Integer, boolean[]> acksMap = acksMapList.get(orignalSenderIndex);
        boolean[] pendingAcks = acksMap.get(packetId);
        
        // Set to true that we have received this packet from host[i]
        if(pendingAcks == null) {
            boolean[] acks = new boolean[hostsSize];
            acks[packet.getLastHopIndex()] = true;
            acksMap.put(packetId, acks);
        } else {
            pendingAcks[packet.getLastHopIndex()] = true;
        }

        // Add to pending messages if first time and relay message
        if(!urBpending.containsKey(packetId)) {
            urBpending.put(packetId, true);
            packet.setLastHop(selfHost.getId());
            System.out.println("urbpending of " + orignalSenderIndex + " = " + urBpending.keySet());
            beBroadcast.reBroadcast(packet);
        }
        else if(canDeliver(packet) && !urBdelivered.containsKey(packetId)) {
            logger.addLine("ayyyyy");
            urBdelivered.put(packetId, true);
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
        boolean[] pendingAcks = acksMapList.get(packet.getHostIndex()).get(packet.getPacketId());
        
        for(int i = 0; i < pendingAcks.length; i++) {
            if(pendingAcks[i]) {
                ++ackedHosts;
            }
        }

        boolean res = ackedHosts > hostsSize / 2;
        logger.addLine("checking candeliver " + res + " pendingAcks: " + Arrays.toString(pendingAcks));

        return res;
    }

}
