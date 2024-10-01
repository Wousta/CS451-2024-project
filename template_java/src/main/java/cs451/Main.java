package cs451;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Stream;

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
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        
        //List<Host> hosts = parser.hosts();
        for (Host host: parser.hosts()) {
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
        //TODO: INICIALIZATION
        // Gets the name of the configuration in position 3 of [.. , example, configs, mode.config]
        String mode;
        try{
            mode = parser.config().trim().split("/")[3];
            System.out.println("MODE: " + mode);
        }
        catch(IndexOutOfBoundsException e){
            System.err.println("Bad path to config file, does not have correct length");
            e.printStackTrace();
            return;
        }

        if(mode.equals("perfect-links.config")){
            System.out.println("ENTERING PERFECT LINKS MODE");
            //TODO perf links
            // Contains the values of the config file
            int[] confVals;
            try (BufferedReader reader = new BufferedReader(new FileReader(parser.config()))) {
                // Read the first (and only) line from the config file
                String line = reader.readLine();
                
                if (line != null) {
                    // Split the line by spaces
                    String[] parts = line.trim().split("\\s+");
    
                    // Convert to an array of integers
                    confVals = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        confVals[i] = Integer.parseInt(parts[i]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
        else if(mode.equals("fifo-broadcast.config")){
            System.out.println("ENTERING FIFO BROADCAST MODE");
        }
        else if(mode.equals("lattice-agreement.config")){
            System.out.println("ENTERING LATTICE AGREEMENT MODE");
        }


        System.out.println("Broadcasting and delivering messages...\n");
        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            //TODO: HEYYYY aqui se para el process
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
