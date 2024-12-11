package cs451.agreement;

import java.util.HashMap;
import java.util.Map;

import cs451.link.PerfectLink;
import cs451.packet.MsgPacket;

public class Acceptor {

    private Map<Integer, MsgPacket> acceptedValues = new HashMap<>();

    private PerfectLink link;

    public Acceptor(PerfectLink link){
        this.link = link;
    }

    public void receiveProp() {
        
    }

}
