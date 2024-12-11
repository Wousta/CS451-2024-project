package cs451.broadcast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cs451.Host;
import cs451.control.Scheduler;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class URBroadcast implements Broadcast {

    private final int hostsSize;
    private BEBroadcast broadcast;
    private Host selfHost;
    private List<Host> hosts;
    private Logger logger;
    private FifoURBroadcast fifoURBroadcast;

    private List<Map<Integer, Boolean>> deliveredList;
    private List<ConcurrentHashMap<TupleKey, Boolean>> pendingList;
    private List<ConcurrentHashMap<Integer, BitSet>> acksMapList;

    public URBroadcast(BEBroadcast broadcast, Scheduler scheduler) {
        this.selfHost = scheduler.getSelfHost();
        this.hosts = scheduler.getHosts();
        this.logger = scheduler.getLogger();
        this.broadcast = broadcast;
        this.logger.setUrBroadcast(this);
        this.hostsSize = hosts.size();

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

        broadcast.broadcast(basePacket);
    }

    public MsgPacket deliver() {

        MsgPacket result = null;

        int ogPacketId;
        int ogSenderIndex;
        Map<Integer, Boolean> delivered;
        BitSet pendingAcks;

        while(result == null) {
            MsgPacket packet = broadcast.deliver();

            ogPacketId = packet.getOriginalId();
            ogSenderIndex = packet.getHostIndex();
            delivered = deliveredList.get(ogSenderIndex);
    
            pendingAcks = acksMapList.get(ogSenderIndex).get(ogPacketId);
            
            // Add to the bitSet the last hop host that bebDelivered this packet
            packet.getFlags().set(packet.getLastHopIndex());
    
            // If received for the first time, create the bitset from the packet bitset and add it to acks map.
            if(pendingAcks == null) {
                BitSet acks = new BitSet(hostsSize);
                acks.or(packet.getFlags());
                acksMapList.get(ogSenderIndex).put(ogPacketId, acks);
            } 
            else {
                // If already received before just OR the hosts that have bebDelivered this packet using the BitSet
                pendingAcks.or(packet.getFlags());
            }
    
    
            TupleKey key = new TupleKey(packet.getHostId(), ogPacketId);
            Map<TupleKey, Boolean> pending = pendingList.get(ogSenderIndex);
    
            // Relay message if applicable, check delivered as well because we clean pending
            if(!pending.containsKey(key) && !delivered.containsKey(ogPacketId)) {
                pending.put(key, true);
                broadcast.broadcast(packet);
            }
            // Check if it can be URB delivered and remove from pending and acksMap
            else if(canDeliver(packet) && !delivered.containsKey(ogPacketId)) {
                delivered.put(ogPacketId, true);
                acksMapList.get(ogSenderIndex).remove(ogPacketId);
                pending.remove(key);
    
                result = packet;
            }
        }

        return result;
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
