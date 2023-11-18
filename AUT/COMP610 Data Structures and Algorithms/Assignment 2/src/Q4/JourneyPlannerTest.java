package Q4;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;

import java.util.List;

/**
 *
 * @author Seth
 */

public class JourneyPlannerTest {
    
    public static void main(String[] args)
    {   System.out.println("------ JOURNEY PLANNER -----");
        JourneyPlanner planner = new JourneyPlanner();
        buildGraph(planner);
        
        List<BusJourney> options = planner.getPossibleJourneys("Auckland CBD", LocalTime.of(13, 0), "Manukau", LocalTime.of(15, 0));

        /* Addition by Jared Scholz */
        Collections.sort(options, new Comparator<BusJourney>() { // custom comparator:
            @Override
            public int compare(BusJourney j1, BusJourney j2) {
                int result = j1.getDestinationTime().compareTo(j2.getDestinationTime());
                // OPTIONAL backup comparisons:
                if (result == 0) {
                    result = (int) ((j1.getTotalCost() * 100) - (j2.getTotalCost() * 100));
                    if (result == 0) {
                        result = j1.getNumberOfBusTrips() - j2.getNumberOfBusTrips();
                    }
                }
                return result;
            }
        });
        /* End addition */

        System.out.println(">>>  There are "+options.size()+" possible journeys for your search\n");
        for(int i=0;i<options.size();i++)
        {   System.out.println("Journey Option "+(i+1)+": "+options.get(i));
        }
    }
    //Builds a graph of Bustrips - do not modify.. Feel free to create another buildgraph method for simplistic testing but do not touch this one
    public static void buildGraph(JourneyPlanner planner)
    {   
        // AUCKLAND CBD to MT ALBERT
        LocalTime departTime = LocalTime.of(6, 0);
        LocalTime arriveTime = LocalTime.of(6,30);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("22N", "Auckland CBD", departTime, "Mt Albert", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            t = new BusTrip("22N", "Mt Albert", departTime, "Auckland CBD", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("23N", "Auckland CBD", departTime, "Mt Albert", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            t = new BusTrip("23N", "Mt Albert", departTime, "Auckland CBD", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
        }
        //MT Albert to New Lynn
        departTime = LocalTime.of(7, 0);
        arriveTime = LocalTime.of(7,25);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("23M", "Mt Albert", departTime, "New Lynn", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
            t = new BusTrip("23M", "Mt Albert", departTime, "New Lynn", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
        }
        //Kingsland to CBD
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(6,20);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("21K", "Kingsland", departTime, "Auckland CBD", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            t = new BusTrip("21K", "Auckland CBD", departTime, "Kingsland", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("22K", "Kingsland", departTime, "Auckland CBD", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            t = new BusTrip("22K", "Auckland CBD", departTime, "Kingsland", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
        }
        //New Lynn to Henderson
        departTime = LocalTime.of(7, 0);
        arriveTime = LocalTime.of(7,30);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("12H", "New Lynn", departTime, "Henderson", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
            t = new BusTrip("12H", "Henderson", departTime, "New Lynn", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
        }
        //Henderson to Waitakere
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(6,40);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("34W", "Waitakere", departTime, "Henderson", arriveTime, BusTrip.FOUR_STAGE);
            planner.add(t);
            t = new BusTrip("34W", "Henderson", departTime, "Waitakere", arriveTime, BusTrip.FOUR_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(60);
            arriveTime = arriveTime.plusMinutes(60);
        }
        //New Lyn to Waitakere
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(7,0);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("76W", "Waitakere", departTime, "New Lynn", arriveTime, BusTrip.FIVE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(60);
            arriveTime = arriveTime.plusMinutes(60);
            t = new BusTrip("76W", "Waitakere", departTime, "New Lynn", arriveTime, BusTrip.FIVE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(60);
            arriveTime = arriveTime.plusMinutes(60);
        }
        //New Lyn to CBD
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(6,30);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("10N", "New Lynn", departTime, "Auckland CBD", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("11N", "New Lynn", departTime, "Auckland CBD", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("10N", "Auckland CBD", departTime, "New Lynn", arriveTime, BusTrip.FOUR_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("11N", "Auckland CBD", departTime, "New Lynn", arriveTime, BusTrip.FOUR_STAGE);
            planner.add(t);
        }
        //New Lynn to Manukau
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(6,50);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("82M", "New Lynn", departTime, "Manukau", arriveTime, BusTrip.SIX_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(60);
            arriveTime = arriveTime.plusMinutes(60);
            t = new BusTrip("82M", "Manukau", departTime, "New Lynn", arriveTime, BusTrip.SIX_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(60);
            arriveTime = arriveTime.plusMinutes(60);
        }
        //CBD to Manukau
        departTime = LocalTime.of(6,10);
        arriveTime = LocalTime.of(6,55);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("92M", "Auckland CBD", departTime, "Manukau", arriveTime, BusTrip.FIVE_STAGE);
            planner.add(t);
            t = new BusTrip("92M", "Manukau", departTime, "Auckland CBD", arriveTime, BusTrip.FIVE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(60);
            arriveTime = arriveTime.plusMinutes(60);
        }
        //PAPAKURA to Manukau
        departTime = LocalTime.of(6,15);
        arriveTime = LocalTime.of(6,45);
        while(departTime.compareTo(LocalTime.of(21,0)) <= 0)
        {   BusTrip t = new BusTrip("88P", "Papakura", departTime, "Manukau", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            t = new BusTrip("88P", "Manukau", departTime, "Papakura", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
        }
        //Kingsland to Ponsonby
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(6,15);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("10P", "Kingsland", departTime, "Ponsonby", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("10P", "Ponsonby", departTime, "Kingsland", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(45);
            arriveTime = arriveTime.plusMinutes(45);
        }
        //Ponsonby to CBD
        departTime = LocalTime.of(6, 0);
        arriveTime = LocalTime.of(6,15);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("00P", "Ponsonby", departTime, "Auckland CBD", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            t = new BusTrip("00P", "Auckland CBD", departTime, "Ponsonby", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            t = new BusTrip("02P", "Ponsonby", departTime, "Auckland CBD", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            t = new BusTrip("02P", "Auckland CBD", departTime, "Ponsonby", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
        }
        //New Market to CBD
        departTime = LocalTime.of(6,10);
        arriveTime = LocalTime.of(6,25);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("53N", "New Market", departTime, "Auckland CBD", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            t = new BusTrip("53N", "Auckland CBD", departTime, "New Market", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);
            
            t = new BusTrip("54N", "New Market", departTime, "Auckland CBD", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            t = new BusTrip("54N", "Auckland CBD", departTime, "New Market", arriveTime, BusTrip.ONE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(15);
            arriveTime = arriveTime.plusMinutes(15);

        }
        //Mission Bay to CBD
        departTime = LocalTime.of(6,15);
        arriveTime = LocalTime.of(6,35);
        while(departTime.compareTo(LocalTime.of(22,0)) <= 0)
        {   BusTrip t = new BusTrip("44B", "Auckland CBD", departTime, "Mission Bay", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(20);
            arriveTime = arriveTime.plusMinutes(20);
            t = new BusTrip("44B", "Mission Bay", departTime, "Auckland CBD", arriveTime, BusTrip.TWO_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(20);
            arriveTime = arriveTime.plusMinutes(20);
        }
        //New Market to Misssion Bay
        departTime = LocalTime.of(8,00);
        arriveTime = LocalTime.of(8,30);
        while(departTime.compareTo(LocalTime.of(20,0)) <= 0)
        {   BusTrip t = new BusTrip("41B", "New Market", departTime, "Mission Bay", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
            t = new BusTrip("41B", "Mission Bay", departTime, "New Market", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
        }
        //Takapuna  to CBD
        departTime = LocalTime.of(7,00);
        arriveTime = LocalTime.of(7,30);
        while(departTime.compareTo(LocalTime.of(20,0)) <= 0)
        {   BusTrip t = new BusTrip("44B", "Auckland CBD", departTime, "Takapuna", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
            t = new BusTrip("44B", "Takapuna", departTime, "Auckland CBD", arriveTime, BusTrip.THREE_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(30);
            arriveTime = arriveTime.plusMinutes(30);
        }
        //Takapuna  to Gulf Harbour
        departTime = LocalTime.of(7,00);
        arriveTime = LocalTime.of(8,30);
        while(departTime.compareTo(LocalTime.of(20,0)) <= 0)
        {   BusTrip t = new BusTrip("99G", "Gulf Harbour", departTime, "Takapuna", arriveTime, BusTrip.SIX_STAGE);
            planner.add(t);

            t = new BusTrip("99G", "Takapuna", departTime, "Gulf Harbour", arriveTime, BusTrip.SIX_STAGE);
            planner.add(t);
            departTime = departTime.plusMinutes(90);
            arriveTime = arriveTime.plusMinutes(90);
        } 
    }
}
