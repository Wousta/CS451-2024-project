package cs451;

public class Constants {
    public static final int ARG_LIMIT_CONFIG = 7;

    // indexes for id
    public static final int ID_KEY = 0;
    public static final int ID_VALUE = 1;

    // indexes for hosts
    public static final int HOSTS_KEY = 2;
    public static final int HOSTS_VALUE = 3;

    // indexes for output
    public static final int OUTPUT_KEY = 4;
    public static final int OUTPUT_VALUE = 5;

    // indexes for config
    public static final int CONFIG_VALUE = 6;

    // A message with id -1 is an ack message
    public static final int ID_ACK_MSG = -1;

    // Number of threads used in the thread pool
    public static final int N_THREADS = 8;

    // The three execution modes available
    public static final int FIFO = 1;
    public static final int PERFECT_LINK = 2;
    public static final int LATTICE = 3;

}