package cs451.links;

import java.util.List;
import java.util.ArrayList;

import cs451.Host;
import cs451.Message;


public class StubbornLink {

    private List<Message> sent;
    private int timer; 
    private FairLossLink fll;



    public StubbornLink(Host linkOwnerHost){
        sent = new ArrayList<>();
        timer = 0; //TODO: thread to update counter
        fll = new FairLossLink(linkOwnerHost.getSocket());
    }

    public void send(Host h, Message m) {
        fll.send(h, m);
        sent.add(m);
    }


    public void deliver(Message m) {
        System.out.println("d " + m.getSenderId() + " " + m.getMsgId());
        // TODO: actual delivery to PL
    }

}
