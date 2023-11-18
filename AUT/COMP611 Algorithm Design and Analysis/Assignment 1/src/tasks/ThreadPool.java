package tasks;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// @author Jared Scholz
public class ThreadPool {

    private int size;
    private volatile BlockingQueue<ReusableThread> threadPool;
    private final BlockingQueue<Runnable> waitingTasks;
    private boolean destroyed;
    /* Variables for shrinking (resize) below */
    private boolean shrinking; // not declared volatile for better average speed
    private volatile int decreaseBy;
    private final Object shrinkMonitor;

    public ThreadPool(int initialSize) {
        size = initialSize;
        threadPool = new ArrayBlockingQueue<>(size);
        waitingTasks = new LinkedBlockingQueue<>();
        destroyed = false;
        shrinking = false;
        decreaseBy = 0;
        shrinkMonitor = new Object();
        addThreads(size);
    }

    private void addThreads(int amount) {
        for (int i = 0; i < amount; i++) {
            ReusableThread newThread = new ReusableThread();
            newThread.start(); // threads add themselves to the pool
        }
    }

    public int getSize() {
        return size;
    }

    public int getAvailable() {
        return threadPool.size();
    }

    public synchronized void resize(int newSize) { // returns immediately
        if (destroyed) {
            throw new IllegalStateException("ThreadPool is destroyed");
        }
        if (newSize < 1) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        if (shrinking) {
            throw new IllegalStateException("ThreadPool is currently shrinking");
        }
        if (newSize > size) {
            BlockingQueue<ReusableThread> oldPool = threadPool;
            threadPool = new ArrayBlockingQueue<>(newSize);
            addThreads(newSize - size);
            threadPool.addAll(oldPool); // also collects any threads added to oldPool during transition
        } else if (newSize < size) {
            synchronized (shrinkMonitor) {
                decreaseBy = size - newSize;
                shrinking = true;
            }
        }
        size = newSize;
    }

    public synchronized void destroyPool() { // returns immediately
        if (!destroyed) {
            // destroy the pool using one of its own threads:
            perform(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                    int targetSize = size - 1;
                    size = 0; // update size to reflect destruction
                    do {
                        // reactivate any lazy threads:
                        for (ReusableThread current : threadPool) {
                            synchronized (current) {
                                current.notify(); // prompt waiting threads to check waitingTasks again
                            }
                        }
                        // ensure all tasks are completed:
                        while (threadPool.size() < targetSize) { // (a thread will not re-enter the pool with tasks waiting)
                            // this approach, although inefficient now, means that threads do not need to notify externally
                            Thread.yield();
                        }
                    } while (!waitingTasks.isEmpty()); // destroy only with 100% confidence that all tasks are complete...
                    // stop all threads (including this one):
                    for (ReusableThread current : threadPool) {
                        current.requestStop();
                    }
                    ((ReusableThread) Thread.currentThread()).requestStop();
                }
            });
            destroyed = true; // block any further task additions
        }
    }

    public synchronized boolean perform(Runnable task) { // returns immediately
        if (destroyed) {
            throw new IllegalStateException("ThreadPool is destroyed");
        }
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        ReusableThread worker = threadPool.poll();
        if (worker == null) {
            waitingTasks.add(task); // queue for a future worker
            return false;
        } else {
            worker.runTask(task);
            return true;
        }
    }

    /* A ReusableThread is a Thread that can switch Runnables */
    private class ReusableThread extends Thread {

        private Runnable currentTask;
        private boolean stopRequested;

        public ReusableThread() {
            super();
            currentTask = null;
            stopRequested = false;
        }

        public synchronized void runTask(Runnable task) { // only to be used while in waiting state (in pool)
            currentTask = task;
            this.notify();
        }

        public synchronized void requestStop() { // only to be used while in waiting state (in pool)
            stopRequested = true;
            this.notify();
        }

        @Override
        public void run() {
            while (!stopRequested) { // a stop may be requested at any stage...
                // Stage 1 - try to immediately pick up a waiting task, or shrink if necessary:
                if (shrinking) { // may be cached for better average speed (not volatile)
                    synchronized (shrinkMonitor) {
                        if (decreaseBy > 0) {
                            stopRequested = true;
                            decreaseBy--;
                        } else { // shrinking is already complete...
                            shrinking = false;
                            currentTask = waitingTasks.poll(); // null if empty
                        }
                    }
                } else {
                    currentTask = waitingTasks.poll(); // null if empty
                }
                // Stage 2 - enter the pool if there is nothing to do immediately:
                if (currentTask == null && !stopRequested) {
                    synchronized (this) { // do not allow runTask/requestStop to occur before entering waiting state
                        threadPool.add(this);
                        try {
                            wait(); // wait to be reused
                        } catch (InterruptedException ex) {
                            // IGNORE
                        }
                    }
                }
                // Stage 3 - run the current task:
                if (!stopRequested && currentTask != null) {
                    currentTask.run();
                    setPriority(Thread.NORM_PRIORITY); // reset any changes made in task
                }
            }
        }
    }
}
