package cs451.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import cs451.Host;
import cs451.broadcast.URBroadcast;
import cs451.packet.AcksPacket;
import cs451.packet.Message;
import cs451.packet.MsgPacket;
import cs451.packet.Packet;

public class Logger {
    private static final BlockingQueue<String> outPutMsgs = new LinkedBlockingQueue<>();
    private BufferedWriter writer;
    private List<Host> hosts;
    private int myIndex;
    private AtomicLong packetId;
    private int deliveredCount = 0;
    private URBroadcast urBroadcast;


    public Logger(String path, List<Host> hosts, int myIndex){
        this.hosts = hosts;
        this.myIndex = myIndex;
        try {
            writer = new BufferedWriter(new FileWriter(path), 32768);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setUrBroadcast(URBroadcast urBroadcast) {
        this.urBroadcast = urBroadcast;
    }


    public void setPacketId(AtomicLong packetId) {
        this.packetId = packetId;
    }

    public synchronized void logPacket(MsgPacket packet) {
        for(Message m : packet.getMessages()) {
            try {
                addLine("d " + m.getHostId() + " " + (String)Packet.deSerialize(m.getData()));
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            ++deliveredCount;
        }
    }

    public synchronized void addLine(String msg) {
        try {
            writer.write(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            int deliveredRemaining = 0;
            for(Host h : hosts) {
                deliveredRemaining += h.getDelivered().size();
            }
            AtomicInteger acksInSent = new AtomicInteger(0);
            AtomicInteger msgsInSent = new AtomicInteger(0);
            ConcurrentMap<Integer, Packet> sent = hosts.get(myIndex).getSent();
            sent.forEach((id, packet) -> {
                if(packet instanceof AcksPacket) {
                    acksInSent.incrementAndGet();
                }
                else if(packet instanceof MsgPacket) {
                    msgsInSent.incrementAndGet();
                }
            });
            writer.write("delivered size = " + deliveredRemaining);
            writer.write("\nsent size = " + sent.size() + " acks: " + acksInSent.get() + " msgs: " + msgsInSent.get());
            writer.write("\ntotal messages delivered = " + deliveredCount);


            if(urBroadcast != null) {
                writer.write("\n\nURB data=============");
                int delSize = 0;
                int pendingSize = 0;
                int acksSize = 0;
                for(int i = 0; i < hosts.size(); i++) {
                    delSize += urBroadcast.getDeliveredList().get(i).size();
                    pendingSize += urBroadcast.getPendingList().get(i).size();
                    acksSize += urBroadcast.getAcksMapList().get(i).size();
                }
                writer.write("\ndelivered size =  " + delSize);
                writer.write("\npending size = " + pendingSize);
                writer.write("\nacks size = " + acksSize);
            }
            
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Queue<String> getMessages(){
        return outPutMsgs;
    }

}
