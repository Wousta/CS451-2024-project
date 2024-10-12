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
        //logger.addLine("signal called");
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
    
        logger = new Logger(parser.output());
        String config = parser.config();
        Host thisHost = parser.hosts().get(parser.myIndex());
        Scheduler scheduler;
        int[] input;

        System.out.println("output: " + parser.output());

        initSignalHandlers();

        try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
            String[] parts = reader.readLine().trim().split("\\s+");
            input = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            scheduler = new Scheduler(parser, logger);
        }
        catch(Exception e){
            System.err.println("Bad initialization");
            e.printStackTrace();
            return;
        }

        /////////////////////////////////////////////////////////////////////////////////
        long pid = ProcessHandle.current().pid();
        System.out.println("conf: " + config + " output: " + parser.output() + " n hosts: " + parser.hosts().size() + "\n"
                        + "My PID: " + pid + "\n"
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

        switch (input.length) {
            case 1:
                scheduler.runFIFO(input);
                break;
            case 2:
                scheduler.runPerfect(input);
                break;
            case 3:
                scheduler.runLattice(input);
                break;
            default:
                System.out.println("Configuration mode not recognized");
                break;
        }

        //TODO: wait for threads to finish

        while (true) {
            System.out.println("Go sleep");
            // Sleep for 1 hour
            Thread.sleep(60L * 60 * 1000);
        }

    }
}
