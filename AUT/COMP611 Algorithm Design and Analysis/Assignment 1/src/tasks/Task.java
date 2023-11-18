package tasks;


import java.util.ArrayList;
import java.util.List;

// @author Jared Scholz
public abstract class Task<E, F> implements Runnable { // uses the template pattern

    private final List<TaskObserver<F>> listeners;
    protected final int uniqueID;
    protected E param;

    public Task(E param) {
        listeners = new ArrayList<>();
        uniqueID = UniqueIdentifier.getInstance().getID();
        this.param = param;
    }

    public int getId() { // hook method
        return uniqueID;
    }

    @Override
    public abstract void run(); // abstract method

    public final void addListener(TaskObserver<F> o) { // concrete method
        synchronized (listeners) {
            listeners.add(o);
        }
    }

    public final void removeListener(TaskObserver<F> o) { // concrete method
        synchronized (listeners) {
            listeners.remove(o);
        }
    }

    protected final void notifyAll(F progress) { // concrete method
        synchronized (listeners) {
            for (TaskObserver<F> listener : listeners) {
                listener.process(progress);
            }
        }
    }
}
