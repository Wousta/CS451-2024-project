package cs451;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

public class Logger {
    private long pid;
    private Parser parser;
    private Host host;
    private PrintWriter out;
    String path;

    public Logger(Parser parser, Host host){
        pid = ProcessHandle.current().pid();
        out = new PrintWriter(new OutputStreamWriter(System.out));

        this.path = path;
        this.parser = parser;
        this.host = host;
    }

    /**
     * Print initial configuration of the hosts IPs, paths, etc
     */
    public void printLayout(){
        int myId = parser.myId();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + myId + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        
        List<Host> hosts = parser.hosts();
        for (Host host: hosts) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");
    }


}
