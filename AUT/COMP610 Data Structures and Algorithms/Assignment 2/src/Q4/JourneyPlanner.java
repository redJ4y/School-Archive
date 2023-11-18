package Q4;

// @author Jared Scholz
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JourneyPlanner {

    private final Map<String, Set<BusTrip>> locationMap;

    public JourneyPlanner() {
        locationMap = new HashMap<>();
    }

    public boolean add(BusTrip bus) {
        if (bus == null) {
            return false;
        }
        Set<BusTrip> departures = locationMap.get(bus.getDepartLocation());
        if (departures == null) {
            departures = new LinkedHashSet<>();
            departures.add(bus);
            locationMap.put(bus.getDepartLocation(), departures);
        } else {
            departures.add(bus);
        }
        return true;
    }

    public List<BusJourney> getPossibleJourneys(String startLocation, LocalTime startTime, String endLocation, LocalTime endTime) {
        List<BusJourney> journeyList = new LinkedList<>();
        if (!startLocation.equals(endLocation)) { // protect against adding an empty BusJourney
            findPaths(startLocation, startTime, endLocation, endTime, new BusJourney(), journeyList);
        }
        return journeyList;
    }

    private void findPaths(String currentLocation, LocalTime currentTime, String endLocation, LocalTime endTime, BusJourney currentJourney, List<BusJourney> journeyList) {
        if (currentLocation.equals(endLocation)) {
            // we have discovered a possible route
            journeyList.add(currentJourney.cloneJourney());
        } else {
            for (BusTrip bus : locationMap.get(currentLocation)) {
                // ensure valid arrival and departure times, addBus validates the rest
                if (bus.getArrivalTime().compareTo(endTime) <= 0 && bus.getDepartTime().compareTo(currentTime) >= 0 && currentJourney.addBus(bus)) {
                    // stack recursive calls for every valid departing bus...
                    // these calls will stack all the way out to either a dead end or the target location, building up the current journey
                    findPaths(bus.getArrivalLocation(), bus.getArrivalTime(), endLocation, endTime, currentJourney, journeyList);
                }
            }
        }
        // once a dead end or the target location is found, return to traverse other routes...
        // update the current journey to reflect this backtrack
        currentJourney.removeLastTrip();
    }
}
