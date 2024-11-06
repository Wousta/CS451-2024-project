package cs451;

public class LoadBalancer {
    private int nProcesses;
    private int nMessages;

    private int sentMaxSize;
    private int acksToAdd;

    public LoadBalancer(int nProcesses, int nMessages) {
        this.nProcesses = nProcesses;
        this.nMessages = nMessages;

        sentMaxSize = (int) (170 * Math.exp(-3 * (double)nProcesses/100));
        acksToAdd = sentMaxSize * 2;
    }

    public int getSentMaxSize() {
        return sentMaxSize;
    }

    public int getAcksToAdd() {
        return acksToAdd;
    }

}
