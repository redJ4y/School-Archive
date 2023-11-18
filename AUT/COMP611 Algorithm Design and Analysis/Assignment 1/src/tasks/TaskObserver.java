package tasks;

// @author Jared Scholz

public interface TaskObserver<F> {

    public void process(F e);
}
