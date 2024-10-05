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
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
        Parser parser = new Parser(args);
        parser.parse();
        int myId = parser.myId();
        String config = parser.config();

        initSignalHandlers();
        List<Host> hosts = parser.hosts();
        Host thisHost = hosts.get(myId);    // Host of this process
        Logger logger = new Logger(parser, thisHost);
        
        //TODO: INICIALIZATION
        // Create socket on my port
        DatagramSocket sock;
        DatagramPacket packet;

        logger.printLayout();
                  
        String mode; // Name of the configuration to be run (perfect links, fifo broadcast, lattice agreement)

        // Initialize mode and UDP socket and packet
        try{
            sock = new DatagramSocket(thisHost.getPort(), InetAddress.getByName(thisHost.getIp()));
            packet = new DatagramPacket(new byte[1], 1);
            mode = config.trim().split("/")[3];
            System.out.println("MODE: " + mode);
        }
        catch(Exception e){
            System.err.println("Bad initialization");
            e.printStackTrace();
            return;
        }

        if(mode.equals("perfect-links.config")){
            System.out.println("ENTERING PERFECT LINKS MODE");
            //TODO perf links
            // Contains the values of the config file, first value m is number of messages to send, second value is receiver index
            int[] confVals = new int[2];
            try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
                // Read the first (and only) line from the config file
                String line = reader.readLine();

                // Split the line by spaces
                String[] parts = line.trim().split("\\s+");

                // Convert to an array of integers for later use to compare
                for (int i = 0; i < parts.length; i++) {
                    confVals[i] = Integer.parseInt(parts[i]);
                }

            } catch (Exception e) {
                System.err.println("Error while reading perfect links config");
                e.printStackTrace();
                sock.close();
                return;
            }

            if(confVals[1] == myId){
                System.out.println("I am the receiver with ID: " + confVals[1] + ", delivering messages...");
                // After a process finishes broadcasting,
                // it waits forever for the delivery of messages.
                int leidoCount = 0;
                while (leidoCount < 16) {
                    //TODO: receive packet and process
                    sock.receive(packet);
                    ByteArrayInputStream bin =  new ByteArrayInputStream(packet.getData());
                    DataInputStream din = new DataInputStream (bin);
                    int val = din.read();
                    System.out.println("Leido: " + val);
                    leidoCount++;
                }
                System.out.println("leidos = " + leidoCount);
            }
            else {
                System.out.println("I am a sender with ID: " + myId + ", Broadcasting messages...");
                Host receiver = hosts.get(confVals[1]);
                packet.setAddress(InetAddress.getByName(receiver.getIp()));
                packet.setPort(receiver.getPort());
                System.out.println("Sender enters while loop to send");
                while (true) {
                    byte data[] = {(byte) thisHost.getId()};
                    packet.setData(data);
                    sock.send(packet);
                }
            }

        }
        else if(mode.equals("fifo-broadcast.config")){
            System.out.println("ENTERING FIFO BROADCAST MODE");
        }
        else if(mode.equals("lattice-agreement.config")){
            System.out.println("ENTERING LATTICE AGREEMENT MODE");
        }

        // TODO: Close resources, refactor later
        sock.close();

    }
}
