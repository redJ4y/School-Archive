package application;

import tasks.TaskObserver;

// @author Jared Scholz
public class ProgressObserver implements TaskObserver<Integer> {

    private final ImageConnection interestedClient;

    public ProgressObserver(ImageConnection interestedClient) {
        this.interestedClient = interestedClient;
    }

    @Override
    public void process(Integer e) {
        interestedClient.sendProgress(e);
    }
}
