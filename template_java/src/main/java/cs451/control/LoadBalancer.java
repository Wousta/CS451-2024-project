package cs451.control;

public class LoadBalancer {

    private int sentMaxSize;
    private int acksToAdd;

    public LoadBalancer(int nProcesses) {

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
