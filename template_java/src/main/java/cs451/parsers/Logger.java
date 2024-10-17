package cs451.parsers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class Logger {
    private static final BlockingQueue<String> outPutMsgs = new LinkedBlockingQueue<>();
    private BufferedWriter writer;

    public Logger(String path){
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

    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Queue<String> getMessages(){
        return outPutMsgs;
    }

}
