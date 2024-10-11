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

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
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
        Logger logger = new Logger(parser, thisHost);

        logger.printLayout();

        initSignalHandlers();

        Scheduler scheduler;
        String mode; // Name of the configuration to be run (perfect links, fifo broadcast, lattice agreement)

        try{

            scheduler = new Scheduler(thisHost, parser.hosts());
            mode = config.trim().split("/")[3];

        }
        catch(Exception e){

            System.err.println("Bad initialization");
            e.printStackTrace();
            return;
            
        }

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
            // Sleep for 1 hour
            Thread.sleep(60L * 60 * 1000);
        }

    }
}
