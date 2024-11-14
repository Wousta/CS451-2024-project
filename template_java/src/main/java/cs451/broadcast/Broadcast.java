package cs451.broadcast;

import cs451.packet.MsgPacket;
import cs451.packet.Packet;

public interface Broadcast {

    void broadcast(MsgPacket packet);

    void deliver(MsgPacket packet);

}
