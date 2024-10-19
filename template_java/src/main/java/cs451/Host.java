package cs451;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.packets.AcksPacket;
import cs451.packets.Packet;

public class Host {

    private static final String IP_START_REGEX = "/";

    private int lastTimeStamp = 0; // Most recent timestamp received in an ack from this host
    private byte id;
    private String ip;
    private int port = -1;
    private String outputPath;
    private DatagramSocket socket;
    
    /**
     * Stores delivered messages from each sender. To create ack messages
     * get the keyset and put it in payload of ack message packet
     */
    private List<ConcurrentMap<Integer,Packet>> delivered;

    /**
     * List of indexes of the messages waiting for acks, it grows per new packet delivered
     */
    private List<BlockingQueue<Integer>> pendingAcks;

    /**
     * Queues of the ack packets ready to be sent. It grows each time the pendingAcks queue
     * of a host is dumped into an AcksPacket, when ack ok is received from sender that AcksPacket is removed.
     */
    private BlockingQueue<AcksPacket> ackPacketsQueue = new LinkedBlockingQueue<>(); // TODO: compare performance with hashmap

    /**
     * Stores the sent packets. It is a hashmap for fast lookup of packets when iterating
     * the queue of ack message indexes that specifies packets to be deleted.
     * The key is the PacketId.
     */
    private ConcurrentMap<Integer,Packet> sent = new ConcurrentHashMap<>(64, 0.75f, Constants.N_THREADS);

    public boolean populate(String idString, String ipString, String portString) {
        try {
            id = Byte.parseByte(idString); ////////////// WARNING TEMPLATE CODE CHANGE //////////////
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

    public void initLists(int nHosts) {
        delivered = new ArrayList<>(nHosts);
        pendingAcks = new ArrayList<>(nHosts);
        
        for (int i = 0; i < nHosts; i++) {
            delivered.add(new ConcurrentHashMap<>(32, 0.75f, Constants.N_THREADS));
            pendingAcks.add(new LinkedBlockingQueue<>());
        }
    }

    // GETTERS ================================================
    /**
     * Do not use to get index of Host in hosts list
     * @return Id of this host
     */
    public byte getId() {
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
    public ConcurrentMap<Integer,Packet> getSent() {
        return sent;
    }

    /**
     * Returns the queue of delivered messages of this host.
     * @return the ConcurrentLinkedQueue for concurrent access with the delivered messages
     */
    public List<ConcurrentMap<Integer,Packet>> getDelivered() {
        return delivered;
    }

    public String getOutputPath() {
        assert outputPath != null : "outPutPath should not be null when calling get";
        return outputPath;
    }

    /**
     * Used to know if the packet we received is older than last ack received from this host and
     * therefore should be ignored.
     * @return the most recent timestamp received in an ack from this host
     */
    public int getLastTimeStamp() {
        return lastTimeStamp;
    }

    public List<BlockingQueue<Integer>> getPendingAcks() {
        return pendingAcks;
    }

    public BlockingQueue<AcksPacket> getAckPacketsQueue() {
        return ackPacketsQueue;
    }

    // SETTERS ================================================
    public void setSocket(DatagramSocket s) {
        if(socket != null) socket.close();
        socket = s;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public void setLastTimeStamp(int lastAckTimestamp) {
        this.lastTimeStamp = lastAckTimestamp;
    }

}
