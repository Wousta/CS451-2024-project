package cs451.agreement;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

    /*
     * For cleaning of acceptedValues. 
     */
    private Map<Integer, BitSet> ongoingShots = new HashMap<>();
    private int shotToClean = 1;


    public Acceptor(PerfectLink link, Scheduler scheduler){
        this.link = link;
        this.hosts = scheduler.getHosts();
        this.selfHost = scheduler.getSelfHost();

        scheduler.getLogger().setAcceptedValues(acceptedValues);
    }


    public void processProposal(MsgPacket proposal) {
        
        //System.out.println("*****Processing proposal from host " + proposal.getHostId() + " shot:" + proposal.getShot());
        MsgPacket ack = new MsgPacket(selfHost.getId(), proposal.getHostId(), proposal);
        List<String> acceptedValsList = acceptedValues.get(proposal.getShot());
        
        // First proposal packet received, it will be an ack for every proposal
        if(acceptedValsList == null) {
            acceptedValsList = proposal.getMessages();
            
            for(int i = 0; i < acceptedValsList.size(); i++) {
                ack.addMessage("");
            }
            
            acceptedValues.put(proposal.getShot(), acceptedValsList);

        } else {
            List<String> newAcceptedValsList = new LinkedList<>();

            for(int i = 0; i < acceptedValsList.size(); i++) {
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

            acceptedValues.put(proposal.getShot(), newAcceptedValsList);
        }

        ack.setProposal(false);

        // Try acceptedValues cleanup
        cleanAcceptedValues(proposal);
        link.send(hosts.get(ack.getTargetHostIndex()), ack);
    }

    private void cleanAcceptedValues(MsgPacket proposal) {
        int proposalShot = proposal.getShot();

        if(proposalShot <= shotToClean) {
            return;
        }

        if(!ongoingShots.containsKey(proposalShot)) {
            BitSet set = new BitSet(hosts.size());
            set.set(proposal.getHostIndex());
            ongoingShots.put(proposalShot, set);

            return;
        }

        BitSet hostsInShot = ongoingShots.get(proposalShot);
        hostsInShot.set(proposal.getHostIndex());

        if(hostsInShot.cardinality() == hosts.size()) {

            for(int i = 1; i < proposalShot; i++) {
                acceptedValues.remove(i);
                ongoingShots.remove(i);
            }

            shotToClean = proposalShot;
        }
    }


    private String merge(String str1, String str2) {
        Set<String> uniqueWords = new HashSet<>();

        Collections.addAll(uniqueWords, str1.split("\\s+"));
        Collections.addAll(uniqueWords, str2.split("\\s+"));
        
        // Join words back together
        return String.join(" ", uniqueWords);
    }

}