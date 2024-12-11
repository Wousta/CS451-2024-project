package cs451.packet;

import java.io.Serializable;

/**
 * Up to 8 can be stored in a single packet
 */
public class Message implements Serializable {
    // TODO: enforce message maximum size
    public static final int MSG_MAX_SIZE = 1000; 

    /**
     * The message content
     */
    private String data;


    public Message(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }


    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "M:" + data;
    }

}
