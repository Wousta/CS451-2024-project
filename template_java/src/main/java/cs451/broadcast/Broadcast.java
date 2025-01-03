package cs451.broadcast;

import cs451.packet.MsgPacket;

public interface Broadcast {

    void broadcast(MsgPacket packet);

    MsgPacket deliver();

}
