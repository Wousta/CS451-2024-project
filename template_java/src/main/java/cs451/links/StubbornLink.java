package cs451.links;

import cs451.Host;
import cs451.Message;

import java.util.Timer;
import java.util.List;
import java.util.Queue;
import java.util.TimerTask;

public class StubbornLink {

    private Queue<Message> sent;
    private List<Host> hosts; 
    private Timer timer;  
    private SLTimerTask slTask; 
    private FairLossLink fll;

    public StubbornLink(Host thisHost, List<Host> hosts){
        slTask = new SLTimerTask(thisHost);
        timer = new Timer(); // Add parameter true to run as Daemon: https://www.digitalocean.com/community/tutorials/java-timer-timertask-example
        timer.scheduleAtFixedRate(slTask, 1500, 5000);
        fll = new FairLossLink(thisHost.getSocket());
        sent = thisHost.getSent();
        this.hosts = hosts;
    }

    public void send(Host h, Message m) {
        slTask.setDestinationHost(h);
        fll.send(h, m);
        sent.offer(m);
    }

    public Message deliver() {
        return fll.deliver();
    }

    public Queue<Message> getSent() {
        return sent;
    }

    /**
     * Timer service of StubbornLinks, it resends all messages
     * in intervals using TimerTask java library class spec.
     */
    private class SLTimerTask extends TimerTask{

        private Host destinationHost;

        public SLTimerTask(Host h) {
            destinationHost = h;
        }

        @Override
        public void run() {
            //System.out.println("Running timerTask StubbornLinks con sent size: " + sent.size());
            sent.forEach( m -> fll.send(destinationHost, m));
        }

        public void setDestinationHost(Host destinationHost) {
            this.destinationHost = destinationHost;
        }
    
    }
}
