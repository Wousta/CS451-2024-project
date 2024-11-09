package cs451.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import cs451.Host;
import cs451.packet.Packet;

public class Logger {
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
            for(ConcurrentHashMap<Integer, Boolean> m : host.getDelivered()) {
                deliveredCount += m.size();
            }
            writer.write("delivered size = " + deliveredCount);
            writer.write("\nsent size = " + host.getSent().size());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Queue<String> getMessages(){
        return outPutMsgs;
    }

}
