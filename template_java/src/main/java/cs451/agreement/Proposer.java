package cs451.agreement;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cs451.Constants;
import cs451.Host;
import cs451.broadcast.BEBroadcast;
import cs451.control.Scheduler;
import cs451.packet.MsgPacket;
import cs451.parser.Logger;

public class Proposer {
    
    private boolean eof = false;
    private AtomicBoolean finished = new AtomicBoolean(false);
    private int currShot = 0;
    private int[] ackCount;
    private int[] nackCount;
    private int[] activePropNum;
    private BitSet inActive;
    private int quorum;
    private LinkedList<String> proposedValues = new LinkedList<>();

    private int[] config = new int[Constants.LATTICE];
    private List<Host> hosts;
    private Host selfHost;
    private BEBroadcast beBroadcast;
    private BufferedReader reader;
    private Logger logger;

    public Proposer(BEBroadcast beBroadcast, BufferedReader reader, Scheduler scheduler) {
        this.hosts = scheduler.getHosts();
        this.selfHost = scheduler.getSelfHost();
        this.logger = scheduler.getLogger();
        this.beBroadcast = beBroadcast;
        this.reader = reader;
        this.quorum = hosts.size() / 2 + 1;

        this.ackCount = new int[MsgPacket.MAX_MSGS];
        this.nackCount = new int[MsgPacket.MAX_MSGS];
        this.activePropNum = new int[MsgPacket.MAX_MSGS];
        this.inActive = new BitSet(MsgPacket.MAX_MSGS);

        String[] parts;
        try {
            parts = reader.readLine().trim().split("\\s+");
            config = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * It will read the BufferedReader and propose a set of proposals. 
     */
    public void propose() throws IOException {

        for(int i = 0; i < MsgPacket.MAX_MSGS && !eof; i++) {
            String proposal = reader.readLine();
            if(proposal == null) {
                eof = true;
                //System.out.println("EOF reached");
                reader.close();
            } else {
                proposedValues.add(proposal);
            }
        }

        ++currShot;
        
        MsgPacket packet = new MsgPacket(
            selfHost.getId(), 
            activePropNum, 
            currShot, 
            new BitSet(MsgPacket.MAX_MSGS)
            );

        // Total_proposals % MAX_MSGS == 0 so we stop, as we are about to send an empty packet
        if(!proposedValues.isEmpty()) {
            //System.out.println("NEW PROPOSAL SHOT:" + currShot + " proposal: " + proposedValues);
            packet.setProposal(true);
            packet.setMessages(proposedValues);
            beBroadcast.broadcast(packet);
        }
        
    }

    public void processAck(MsgPacket packet) {

        if(packet.getShot() != currShot) {
            // System.out.println("WRONG SHOT host-"+ packet.getHostId());
            return;
        }

        //System.out.println("ACK from shot:" + packet.getShot() + " host:" + packet.getHostId());
        //System.out.println("    proposals: " + packet.getMessages() + " activeProps: " + proposedValues);

        boolean doRefine = false;
        List<String> proposals = packet.getMessages();
        for(int i = 0; i < proposals.size(); i++) {
            
            if(packet.getPropNumber()[i] == activePropNum[i]) {

                // This means it is a nack, it needs to be refined
                if(packet.getFlags().get(i)) {
                    String mergedProposals = merge(proposals.get(i), proposedValues.get(i));
                    proposedValues.set(i, mergedProposals);
                    ++nackCount[i];
                } else {
                    ++ackCount[i];
                }
                
            }


            if(nackCount[i] > 0 
                    && nackCount[i] + ackCount[i] >= quorum 
                    && !inActive.get(i)) {

                ackCount[i] = 0;
                nackCount[i] = 0; 
                activePropNum[i]++;
                doRefine = true; 
                // System.out.println("Must be refined to: " + proposedValues.get(i));

            } else if(ackCount[i] >= quorum && !inActive.get(i)) {
                // System.out.println("    Deciding prop "+ i + ": ackCount=" + ackCount[i] + " quorum=" + quorum + " currShot=" + currShot);
                inActive.set(i);
            }

        }

        // System.out.println("        inactive: " + inActive + " ackCount:" + Arrays.toString(ackCount) + " nackCount:" +  Arrays.toString(nackCount));
        // System.out.println("            packet Shot:" + packet.getShot() + " currShot:" + currShot);
        // System.out.println("processed ack from host " + packet.getHostId() + ": " + Arrays.toString(packet.getPropNumber()));

        if(doRefine) {
            MsgPacket proposal = new MsgPacket(
                selfHost.getId(), 
                activePropNum, 
                currShot, 
                new BitSet(proposedValues.size())
            );
    
            proposal.setMessages(proposedValues);
            proposal.setProposal(true);
            beBroadcast.broadcast(proposal); 

        } else if(inActive.cardinality() == proposedValues.size()) {
            //System.out.println("Deciding " + currShot + " ==================================");
            decide();
        }
        
    }


    private void decide() {

        //logger.addLine("DECIDING " + currShot + "  ==================================");
        for(String proposal : proposedValues) {
            logger.addLine(proposal);
        }

        // Prepare state for next shot
        Arrays.fill(ackCount, 0);
        Arrays.fill(nackCount, 0);
        Arrays.fill(activePropNum, 0);
        inActive.clear();
        proposedValues.clear();

        if(!eof) {
            try {
                propose();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            finished.set(true);
        }
    }


    public boolean hasFinished() {
        return finished.get();
    }


    private String merge(String str1, String str2) {
        Set<String> uniqueWords = new HashSet<>();

        Collections.addAll(uniqueWords, str1.split("\\s+"));
        Collections.addAll(uniqueWords, str2.split("\\s+"));
        
        // Join words back together
        return String.join(" ", uniqueWords);
    }

}
