package cs451.parsers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cs451.Host;
import cs451.packets.Packet;

public class Logger {
    public static AtomicInteger sentAcks = new AtomicInteger();
    public static AtomicInteger sentMsgs = new AtomicInteger();
    private static final BlockingQueue<String> outPutMsgs = new LinkedBlockingQueue<>();
    private BufferedWriter writer;
    private Host host;

    public Logger(String path, Host host){
        this.host = host;
        try {
            writer = new BufferedWriter(new FileWriter(path), 32768);
        } catch (IOException e) {
            System.out.println("Exception in logger constructor");
            e.printStackTrace();
        }
    }

    // TODO: concurrent writing and such
    public void addLine(String msg) {
        try {
            writer.write(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            //writer.write("sent ack packets: " + sentAcks.get());
            //writer.write("\ndelivered msgs: " + sentMsgs.get());
            // int deliveredCount = 0;
            // for(ConcurrentHashMap<Integer, Packet> m : host.getDelivered()) {
            //     deliveredCount += m.size();
            // }
            // writer.write("delivered size = " + deliveredCount);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Queue<String> getMessages(){
        return outPutMsgs;
    }

}
