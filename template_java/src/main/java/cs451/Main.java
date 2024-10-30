package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cs451.parsers.Logger;
import cs451.parsers.Parser;

public class Main {
    private static Logger logger;
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        executor.shutdown();
        try {
            // Wait a bit for threads to finish and stop all of them after
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            } 
        } catch (InterruptedException e) {
            System.out.println("Interrupt Executor");
            executor.shutdownNow();
        }

        //write/flush output file if necessary
        System.out.println("Writing output.");
        logger.close();
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
        logger = new Logger(parser.output(), thisHost);

        System.out.println("output: " + parser.output());
        initSignalHandlers();

        PPLScheduler scheduler;
        int[] input;
        try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
            String[] parts = reader.readLine().trim().split("\\s+");
            input = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            scheduler = new PPLScheduler(parser, logger, executor);
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
                // Run fifo
                break;
            case 2:
                if(input[1] == thisHost.getId()) scheduler.runPerfectReceiver();
                else scheduler.runPerfectSender(input);
                break;
            case 3:
                // Run Lattice
                break;
            default:
                System.out.println("Configuration mode not recognized");
                break;
        }

        while (true) {
            System.out.println("Go sleep");
            // Sleep for 1 hour
            Thread.sleep(60L * 60 * 1000);
        }

    }
}
