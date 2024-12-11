package cs451.agreement;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import cs451.broadcast.BEBroadcast;
import cs451.packet.PropPacket;

public class Proposer {
    
    private int currShot = 0;
    private int ackCount = 0;
    private int nackCount = 0;
    private int activePropNum = 0;
    private PropPacket propValues;

    private BEBroadcast beBroadcast;
    private BufferedReader reader;

    public Proposer(BEBroadcast beBroadcast, String config) {
        this.beBroadcast = beBroadcast;
        
        try {
            reader = new BufferedReader(new FileReader(config));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }




}
