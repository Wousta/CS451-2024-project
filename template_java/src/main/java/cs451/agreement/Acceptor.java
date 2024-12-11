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

    private Map<Integer, MsgPacket> acceptedValues = new HashMap<>();

    private PerfectLink link;
    private List<Host> hosts;
    private Host selfHost;

    public Acceptor(PerfectLink link, Scheduler scheduler){
        this.link = link;
        this.hosts = scheduler.getHosts();
        this.selfHost = scheduler.getSelfHost();
    }

    public void receiveProp() {
        
        MsgPacket proposal = link.deliver();
        MsgPacket acceptedValPacket = acceptedValues.get(proposal.getShot());
        MsgPacket ack = new MsgPacket(
            selfHost.getId(), 
            proposal.getHostId(), 
            new BitSet(hosts.size())
        );

        List<String> acceptedValsList = acceptedValPacket.getMessages();
        List<String> newAcceptedValsList = new ArrayList<>(MsgPacket.MAX_MSGS);
        int size = acceptedValsList.size();         
        for(int i = 0; i < size; i++) {
            String acceptedVal = acceptedValsList.get(i);
            String proposedVal = proposal.getMessages().get(i);
            String merged = merge(proposedVal, acceptedVal);

            if(proposedVal.length() != merged.length()) {
                // Setting the bit of this proposal to represent that is has been nacked (needs to be refined)
                ack.getFlags().set(i);
                ack.addMessage(merged);  

            } else {
                // If the proposal contained the accepted value, just ack that proposal
                ack.addMessage("");
            }

            newAcceptedValsList.add(merged);
        }

        acceptedValPacket.setMessages(newAcceptedValsList);
        link.send(hosts.get(proposal.getHostIndex()), ack);
        
    }

    private String merge(String str1, String str2) {
        Set<String> uniqueWords = new HashSet<>();

        Collections.addAll(uniqueWords, str1.split("\\s+"));
        Collections.addAll(uniqueWords, str2.split("\\s+"));
        
        // Join words back together
        return String.join(" ", uniqueWords);
    }

}