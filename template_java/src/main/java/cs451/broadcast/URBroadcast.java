package cs451.broadcast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cs451.Host;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class URBroadcast implements Broadcast {

    private final int hostsSize;
    private PerfectLink link;
    private Host selfHost;
    private List<Host> hosts;
    private Logger logger;
    private FifoURBroadcast fifoURBroadcast;

    private List<Map<Integer, Boolean>> deliveredList;
    private List<ConcurrentHashMap<TupleKey, Boolean>> pendingList;
    private List<ConcurrentHashMap<Integer, BitSet>> acksMapList;

    public URBroadcast(PerfectLink link, Host selfHost, List<Host> hosts, Logger logger) {
        this.hostsSize = hosts.size();
        this.link = link;
        this.link.setURBroadcast(this);
        this.selfHost = selfHost;
        this.hosts = hosts;
        this.logger = logger;
        this.logger.setUrBroadcast(this);

        deliveredList = new ArrayList<>(hostsSize);
        pendingList = new ArrayList<>(hostsSize);
        acksMapList = new ArrayList<>(hostsSize);

        for(int i = 0; i < hostsSize; i++) {
            deliveredList.add(new HashMap<>());
            pendingList.add(new ConcurrentHashMap<>());
            acksMapList.add(new ConcurrentHashMap<>());
        }
    }


    public void setFifoURBroadcast(FifoURBroadcast fifoURBroadcast) {
        this.fifoURBroadcast = fifoURBroadcast;
    }


    public List<Map<Integer, Boolean>> getDeliveredList() {
        return deliveredList;
    }


    public List<ConcurrentHashMap<TupleKey, Boolean>> getPendingList() {
        return pendingList;
    }


    public List<ConcurrentHashMap<Integer, BitSet>> getAcksMapList() {
        return acksMapList;
    }


    public void broadcast(MsgPacket basePacket) {
        TupleKey origin = new TupleKey(selfHost.getId(), basePacket.getOriginalId());
        pendingList.get(selfHost.getIndex()).put(origin, true);

        beBroadcast(basePacket);
    }

    public void beBroadcast(MsgPacket basePacket) {
        for(Host host : hosts) {
            MsgPacket packet = new MsgPacket(basePacket, host.getId());
            link.send(host, packet);
        }
    }


    /**
     * Triggered by PerfectLink when it delivers a message
     * @param packet the packet delivered by perfect links
     */
    public void beBDeliver(MsgPacket packet) {

        int ogPacketId = packet.getOriginalId();
        int ogSenderIndex = packet.getHostIndex();
        int lastHopIndex = packet.getLastHopIndex();
        Map<Integer, Boolean> delivered = deliveredList.get(ogSenderIndex);

        Map<Integer, BitSet> acksMap = acksMapList.get(ogSenderIndex);
        BitSet pendingAcks = acksMap.get(ogPacketId);
        
        packet.getAlreadyDelivered().set(lastHopIndex);
        if(pendingAcks == null) {
            BitSet acks = new BitSet(hostsSize);
            acks.or(packet.getAlreadyDelivered());
            acksMap.put(ogPacketId, acks);
        } 
        else {
            pendingAcks.or(packet.getAlreadyDelivered());
        }

        TupleKey key = new TupleKey(packet.getHostId(), ogPacketId);
        Map<TupleKey, Boolean> pending = pendingList.get(ogSenderIndex);
        if(!pending.containsKey(key)) {
            pending.put(key, true);
            beBroadcast(packet);
        }
        else if(canDeliver(packet) && !delivered.containsKey(ogPacketId)) {
            delivered.put(ogPacketId, true);
            acksMap.remove(ogPacketId);

            try {
                deliver(packet);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }

        }
    }


    public void deliver(MsgPacket packet) throws ClassNotFoundException, IOException {
        if(fifoURBroadcast != null) {
            fifoURBroadcast.deliver(packet);
        } 
        else {
            logger.logPacket(packet);
        }
    }


    private boolean canDeliver(MsgPacket packet) {
        int ackedHosts = 0;
        BitSet pendingAcks = acksMapList.get(packet.getHostIndex()).get(packet.getOriginalId());

        for(int i = 0; i < pendingAcks.size(); i++) {
            if(pendingAcks.get(i)) {
                ++ackedHosts;
            }
        }

        return ackedHosts > hostsSize / 2;
    }

}
