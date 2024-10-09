package cs451.links;

import cs451.Host;
import cs451.Message;

import java.util.Timer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.TimerTask;

public class StubbornLink {

    /**
     * Timer service of StubbornLinks, it resends all messages
     * in intervals using TimerTask java library class spec.
     */
    public class SLTimerTask extends TimerTask{

        @Override
        public void run() {
            System.out.println("Running timerTask StubbornLinks");
            sent.forEach( m -> fll.send(hosts.get(m.getSenderId()), m));
        }
    
    }

    private static Queue<Message> sent = new ConcurrentLinkedQueue<>();
    private List<Host> hosts; 
    private Timer timer;  
    private FairLossLink fll;

    public StubbornLink(Host thisHost, List<Host> hosts){
        timer = new Timer(); // Add parameter true to run as Daemon: https://www.digitalocean.com/community/tutorials/java-timer-timertask-example
        timer.scheduleAtFixedRate(
            new SLTimerTask(),
            1000,
            4000);

        fll = new FairLossLink(thisHost.getSocket());
        this.hosts = hosts;
    }

    public static Queue<Message> getSent() {
        return sent;
    }

    public void send(Host h, Message m) {
        fll.send(h, m);
        sent.add(m);
    }

    public void deliver() {
        Message m = fll.deliver();
        System.out.println("d " + m.getSenderId() + " " + m.getMsgId());
        // TODO: actual delivery to PL
    }

}
