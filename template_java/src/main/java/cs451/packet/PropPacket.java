package cs451.packet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PropPacket extends Packet {
    
    public static final int MAX_MSGS = 8;

    private int shot;
    private List<String> proposals = new ArrayList<>(MAX_MSGS);
    private final BitSet isNack = new BitSet(MAX_MSGS);
    

    public PropPacket(byte hostId, int shot, List<String> proposals) {
        super(hostId);
        this.shot = shot;
        this.proposals = proposals;
    }

    
    public void setShot(int shot) {
        this.shot = shot;
    }

    public int getShot() {
        return shot;
    }

    public BitSet getIsNack() {
        return isNack;
    }

    public List<String> getProposals() {
        return proposals;
    }

    public void setProposals(List<String> proposals) {
        this.proposals = proposals;
    }


    public String getProp(int index) {
        return proposals.get(index);
    }

    public void setProp(int index, String proposal) {
        proposals.set(index, proposal);
    }

}
