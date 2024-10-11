package cs451.links;

import cs451.Host;
import cs451.Message;

import java.util.Timer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.TimerTask;

public class StubbornLink {

    private static Queue<Message> sent = new ConcurrentLinkedQueue<>();
    private List<Host> hosts; 
    private Timer timer;  
    private FairLossLink fll;

    public StubbornLink(Host thisHost, List<Host> hosts){
        timer = new Timer(); // Add parameter true to run as Daemon: https://www.digitalocean.com/community/tutorials/java-timer-timertask-example
        timer.scheduleAtFixedRate(
            new SLTimerTask(),
            1000,
            5000);

        fll = new FairLossLink(thisHost.getSocket());
        this.hosts = hosts;
    }

    public void send(Host h, Message m) {
        fll.send(h, m);
        sent.offer(m);
    }

    public Message deliver() {
        return fll.deliver();
    }

    public static Queue<Message> getSent() {
        return sent;
    }

    /**
     * Timer service of StubbornLinks, it resends all messages
     * in intervals using TimerTask java library class spec.
     */
    public class SLTimerTask extends TimerTask{

        @Override
        public void run() {
            System.out.println("Running timerTask StubbornLinks con sent size: " + sent.size());
            sent.forEach( m -> fll.send(hosts.get(m.getSenderId()), m));
        }
    
    }
}
