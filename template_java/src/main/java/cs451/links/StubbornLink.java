package cs451.links;

import cs451.Host;
import cs451.Logger;
import cs451.Message;

import java.util.Timer;
import java.util.List;
import java.util.Queue;
import java.util.TimerTask;

/**
 * Use it inside PerfectLink to guarantee Stubborn delivery.
 */
public class StubbornLink {

    private Queue<Message> sent;
    private List<Host> hosts; 
    private Timer timer;  
    private SLTimerTask slTask; 
    private FairLossLink fll;
    private Logger logger;

    public StubbornLink(Host thisHost, List<Host> hosts, Logger logger){
        slTask = new SLTimerTask(thisHost, logger);
        timer = new Timer(); // Add parameter true to run as Daemon: https://www.digitalocean.com/community/tutorials/java-timer-timertask-example
        timer.scheduleAtFixedRate(slTask, 100, 2000);
        fll = new FairLossLink(thisHost.getSocket());
        sent = thisHost.getSent();
        this.hosts = hosts;
        this.logger = logger;
    }

    public void send(Host h, Message m) {
        //System.out.println("sending message: " + m.getMsgId());
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

        public SLTimerTask(Host h, Logger logger) {
            destinationHost = h;
        }

        @Override
        public void run() {
            //System.out.println("Running timerTask StubbornLinks con sent size: " + sent.size());
            //logger.addLine("Running timerTask StubbornLinks con sent size: " + sent.size());
            sent.forEach( m -> fll.send(destinationHost, m));
        }

        public void setDestinationHost(Host destinationHost) {
            this.destinationHost = destinationHost;
        }
    
    }
}
