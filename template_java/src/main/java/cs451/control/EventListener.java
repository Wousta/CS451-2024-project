package cs451.control;

import cs451.broadcast.URBroadcast;
import cs451.link.PerfectLink;

public class EventListener {
    private URBroadcast urBroadcast;
    private PerfectLink link;

    public EventListener(URBroadcast urBroadcast, PerfectLink link) {
        this.urBroadcast = urBroadcast;
        this.link = link;
    }

    

}
