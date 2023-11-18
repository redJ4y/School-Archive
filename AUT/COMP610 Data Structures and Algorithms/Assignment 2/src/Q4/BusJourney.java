package Q4;

// @author Jared Scholz
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BusJourney {

    private final List<BusTrip> busList;

    public BusJourney() {
        busList = new ArrayList<>();
    }

    public BusJourney(List<BusTrip> list) {
        this();
        for (BusTrip current : list) {
            addBus(current);
        }
    }

    public boolean addBus(BusTrip bus) {
        if (!busList.isEmpty()) { // the first trip is always valid
            if (!getDestination().equals(bus.getDepartLocation())) {
                return false; // invalid departure location
            }
            if (getDestinationTime().compareTo(bus.getDepartTime()) > 0) {
                return false; // invalid departure time
            }
            if (containsLocation(bus.getArrivalLocation())) {
                return false; // already visited
            }
        }
        busList.add(bus);
        return true;
    }

    public boolean removeLastTrip() {
        if (busList.isEmpty()) {
            return false;
        }
        busList.remove(busList.size() - 1);
        return true;
    }

    public boolean containsLocation(String location) {
        if (busList.isEmpty()) {
            return false; // protect from null getDestination
        }
        for (BusTrip current : busList) {
            // check all departure locations
            if (current.getDepartLocation().equals(location)) {
                return true;
            }
        }
        // check the arrival location of only the last trip
        return getDestination().equals(location);
    }

    public String getOrigin() {
        if (busList.isEmpty()) {
            return null;
        }
        return busList.get(0).getDepartLocation();
    }

    public String getDestination() {
        if (busList.isEmpty()) {
            return null;
        }
        return busList.get(busList.size() - 1).getArrivalLocation();
    }

    public LocalTime getOriginTime() {
        if (busList.isEmpty()) {
            return null;
        }
        return busList.get(0).getDepartTime();
    }

    public LocalTime getDestinationTime() {
        if (busList.isEmpty()) {
            return null;
        }
        return busList.get(busList.size() - 1).getArrivalTime();
    }

    public BusJourney cloneJourney() {
        return new BusJourney(busList);
    }

    public int getNumberOfBusTrips() {
        return busList.size();
    }

    public double getTotalCost() {
        double totalCost = 0.0;
        for (BusTrip current : busList) {
            totalCost += current.getCost();
        }
        return totalCost;
    }

    @Override
    public String toString() {
        String result = "Total Cost: $" + getTotalCost() + "\n";
        for (BusTrip current : busList) {
            result += current.toString() + "\n";
        }
        return result;
    }
}
