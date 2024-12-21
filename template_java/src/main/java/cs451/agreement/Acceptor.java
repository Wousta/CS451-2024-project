package cs451.agreement;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs451.Host;
import cs451.control.Scheduler;
import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;


public class Acceptor {

    private Map<Integer, List<String>> acceptedValues = new HashMap<>();

    private PerfectLink link;
    private List<Host> hosts;
    private Host selfHost;

    public Acceptor(PerfectLink link, Scheduler scheduler){
        this.link = link;
        this.hosts = scheduler.getHosts();
        this.selfHost = scheduler.getSelfHost();
    }

    public void processProposal(MsgPacket proposal) {
        
        System.out.println("*****Processing proposal from host " + proposal.getHostId() + " shot:" + proposal.getShot());
        MsgPacket ack = new MsgPacket(selfHost.getId(), proposal.getHostId(), proposal);
        List<String> newAcceptedValsList = new ArrayList<>(MsgPacket.MAX_MSGS);
        List<String> acceptedValsList = acceptedValues.get(proposal.getShot());
        
        // First proposal packet received, it will be an ack for every proposal
        if(acceptedValsList == null) {
            System.out.println("    NULL PROPOSAL shot:" + proposal.getShot());
            acceptedValsList = proposal.getMessages();
        }

        for(int i = 0; i < acceptedValsList.size(); i++) {
            String acceptedVal = acceptedValsList.get(i);
            String proposedVal = proposal.getMessages().get(i);
            String merged = merge(proposedVal, acceptedVal);

            if(proposedVal.length() != merged.length()) {
                // Setting the bit of this proposal to represent that is has been nacked (needs to be refined)
                System.out.println("    acceptedval:" + acceptedVal + " | proposedVal:" + proposedVal + " | merged: " + merged);
                ack.getFlags().set(i);
                ack.addMessage(merged);  

            } else {
                // If the proposal contained the accepted value, just ack that proposal
                ack.addMessage("");
            }

            newAcceptedValsList.add(merged);
        }

        acceptedValues.put(proposal.getShot(), newAcceptedValsList);
        ack.setProposal(false);
        System.out.println("    Sending ack to host " + ack.getTargetHostId());
        link.send(hosts.get(ack.getTargetHostIndex()), ack);
        
    }

    private String merge(String str1, String str2) {
        Set<String> uniqueWords = new HashSet<>();

        Collections.addAll(uniqueWords, str1.split("\\s+"));
        Collections.addAll(uniqueWords, str2.split("\\s+"));
        
        // Join words back together
        return String.join(" ", uniqueWords);
    }

}