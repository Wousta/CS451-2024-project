package cs451.packet;

import java.util.List;

public class PropPacket {
    
    private int shot;
    private List<String[]> proposals;

    public PropPacket(byte hostId, int shot, List<String[]> proposals) {
        this.shot = shot;
        this.proposals = proposals;
    }


    public List<String[]> getPropList() {
        return proposals;
    }

    public void setPropList(List<String[]> proposals) {
        this.proposals = proposals;
    }

    public String[] getProp(int index) {
        return proposals.get(index);
    }

    public void setProp(int index, String[] proposal) {
        proposals.set(index, proposal);
    }


    public int getShot() {
        return shot;
    }

    

}
