package Q1;

// @author Jared Scholz
import java.util.ArrayList;
import java.util.List;

public class Element implements Runnable {

    public static double HEAT_CONSTANT; // changed to static to update all elements easily
    // HEAT_CONSTANT does not need to be declared volatile because slightly out-of-date cached values are acceptable
    private List<Element> neighbors;
    private double currentTemp;
    private boolean stopRequested;

    public Element(double startTemp) {
        neighbors = new ArrayList<>(4);
        currentTemp = startTemp;
    }

    public synchronized double getTemperature() {
        return currentTemp;
    }

    public synchronized void applyTempToElement(double appliedTemp) {
        currentTemp += (appliedTemp - currentTemp) * HEAT_CONSTANT;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void requestStop() {
        stopRequested = true;
    }

    @Override
    public void run() {
        stopRequested = false;
        while (!stopRequested) {
            applyTempToElement(getNeighborAverage());
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    /* New utility method for checking neighbors */
    private double getNeighborAverage() {
        double sum = 0.0;
        for (Element current : neighbors) {
            sum += current.getTemperature();
        }
        return sum / neighbors.size();
    }

    public void addNeighbor(Element element) {
        neighbors.add(element);
    }

    /* MAIN FOR TESTNG */
    public static void main(String[] args) {
        Element.HEAT_CONSTANT = 0.1;
        Element elem1 = new Element(100);
        Element elem2 = new Element(0);
        elem1.addNeighbor(elem2);
        elem2.addNeighbor(elem1);
        elem1.start();
        elem2.start();

        for (int ms10 = 0; ms10 < 100; ms10++) { // 10 ms * 100 = 1 second
            double temp1 = elem1.getTemperature();
            double temp2 = elem2.getTemperature();
            System.out.println("elem1: " + temp1);
            System.out.println("elem2: " + temp2);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        elem1.requestStop();
        elem2.requestStop();
    }
}
