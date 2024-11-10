package cs451.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

import cs451.Host;
import cs451.packet.Packet;

public class Logger {
    private static final BlockingQueue<String> outPutMsgs = new LinkedBlockingQueue<>();
    private BufferedWriter writer;
    private List<Host> hosts;
    private int myIndex;
    private AtomicLong packetId;

    public Logger(String path, List<Host> hosts, int myIndex){
        this.hosts = hosts;
        this.myIndex = myIndex;
        try {
            writer = new BufferedWriter(new FileWriter(path), 32768);
        } catch (IOException e) {
            System.out.println("Exception in logger constructor");
            e.printStackTrace();
        }
    }

    

    public void setPacketId(AtomicLong packetId) {
        this.packetId = packetId;
    }



    public void addLine(String msg) {
        try {
            writer.write(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            int deliveredCount = 0;
            for(Host h : hosts) {
                deliveredCount += h.getDelivered().size();
            }
            writer.write("delivered size = " + deliveredCount);
            writer.write("\nsent size = " + hosts.get(myIndex).getSent().size());
            writer.write("\natomic integer value reached = " + packetId.get());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Queue<String> getMessages(){
        return outPutMsgs;
    }

}
