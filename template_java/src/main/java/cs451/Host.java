package cs451;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.DatagramSocket;

public class Host {

    private static final String IP_START_REGEX = "/";

    private int id;
    private String ip;
    private int port = -1;
    private DatagramSocket socket;
    private Queue<Message> sent = new ConcurrentLinkedQueue<>();
    private Queue<Message> delivered = new ConcurrentLinkedQueue<>();

    public boolean populate(String idString, String ipString, String portString) {
        try {
            id = Integer.parseInt(idString);

            String ipTest = InetAddress.getByName(ipString).toString();
            if (ipTest.startsWith(IP_START_REGEX)) {
                ip = ipTest.substring(1);
            } else {
                ip = InetAddress.getByName(ipTest.split(IP_START_REGEX)[0]).getHostAddress();
            }

            port = Integer.parseInt(portString);
            if (port <= 0) {
                System.err.println("Port in the hosts file must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            if (port == -1) {
                System.err.println("Id in the hosts file must be a number!");
            } else {
                System.err.println("Port in the hosts file must be a number!");
            }
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return true;
    }

    // GETTERS ================================================
    /**
     * Do not use to get index of Host in hosts list
     * @return Id of this host
     */
    public int getId() {
        return id;
    }

    /**
     * Main purpose is to avoid having to remember to substract 1 to my id
     * each time we look for a host in the Hosts queue.
     * @return Index of this host in the Hosts list.
     */
    public int getIndex() {
        return id - 1;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        return InetAddress.getByName(getIp());
    }

    /**
     * Non defensive implementation, the socket must be setted first or will return null
     * @return The datagram socket of this host
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * Returns the queue of sent messages of this host.
     * @return the ConcurrentLinkedQueue for concurrent access with the sent messages
     */
    public Queue<Message> getSent() {
        return sent;
    }

    /**
     * Returns the queue of delivered messages of this host.
     * @return the ConcurrentLinkedQueue for concurrent access with the delivered messages
     */
    public Queue<Message> getDelivered() {
        return delivered;
    }

    // SETTERS ================================================
    public void setSocket(DatagramSocket s) {
        if(socket != null) socket.close();
        socket = s;
    }

}
