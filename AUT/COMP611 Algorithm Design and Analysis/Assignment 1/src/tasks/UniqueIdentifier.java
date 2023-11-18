package tasks;

// @author Jared Scholz

public final class UniqueIdentifier { // singleton design pattern

    private static UniqueIdentifier instance;
    private int currentID; // held by the instance

    private UniqueIdentifier() {
        currentID = 0;
    }

    public static UniqueIdentifier getInstance() {
        if (instance == null) {
            synchronized (UniqueIdentifier.class) {
                if (instance == null) {
                    instance = new UniqueIdentifier();
                }
            }
        }
        return instance;
    }

    public synchronized int getID() {
        return currentID++;
    }
}
