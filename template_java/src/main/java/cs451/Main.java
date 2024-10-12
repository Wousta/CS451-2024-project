package cs451;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import cs451.links.FairLossLink;
import cs451.links.PerfectLink;
import cs451.parsers.Parser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {
    private static Logger logger;

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
        logger.flush();
    }

    private static void initSignalHandlers() {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }
    public static void main(String[] args) throws InterruptedException, IOException {
        Parser parser = new Parser(args);
        parser.parse();
        
        String config = parser.config();
        Host thisHost = parser.hosts().get(parser.myIndex());
        logger = new Logger(parser.output());

        initSignalHandlers();

        Scheduler scheduler;
        String mode; // Name of the configuration to be run (perfect links, fifo broadcast, lattice agreement)

        try{

            scheduler = new Scheduler(parser, logger);
            mode = config.trim().split("/")[3];

        }
        catch(Exception e){

            System.err.println("Bad initialization");
            e.printStackTrace();
            return;
            
        }

        /////////////////////////////////////////////////////////////////////////////////
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n"
                        + "From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n"
                        + "My ID: " + parser.myId() + " My port: " + thisHost.getPort() + "\n"
                        + "List of resolved hosts is:\n"
                        + "==========================\n");
        
        List<Host> hosts = parser.hosts();
        for (Host host: hosts) {
            System.out.println(host.getId() + "\n"
                            + "Human-readable IP: " + host.getIp() + "\n"
                            + "Human-readable Port: " + host.getPort() + "\n\n");

        }

        System.out.println("\nPath to output:\n"
                        + "===============\n"
                        + parser.output() + "\n");

        System.out.println("Path to config:\n"
                        + "===============\n"
                        + parser.config() + "\n"
                        + "Doing some initialization\n");
        /////////////////////////////////////////////////////////////////////////////////

        if(mode.equals("perfect-links.config")){
            scheduler.run(config);
        }
        else if(mode.equals("fifo-broadcast.config")){
            System.out.println("ENTERING FIFO BROADCAST MODE");
        }
        else if(mode.equals("lattice-agreement.config")){
            System.out.println("ENTERING LATTICE AGREEMENT MODE");
        }

        //TODO: wait for threads to finish

        while (true) {
            System.out.println("Go sleep");
            // Sleep for 1 hour
            Thread.sleep(60L * 60 * 1000);
        }

    }
}
