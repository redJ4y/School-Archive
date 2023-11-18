package Q4;
        
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Objects;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Seth
 */
public class BusTrip {
    private final LocalTime departTime;
    private final LocalTime arrivalTime;
    private final String busNumber;
    private final float cost;
    private final String arrivalLocation;
    private final String departLocation;
    public final static float ONE_STAGE = 3.5f;
    public final static float TWO_STAGE = 5.5f;
    public final static float THREE_STAGE = 7.5f;
    public final static float FOUR_STAGE = 9.0f;
    public final static float FIVE_STAGE = 10.5f;
    public final static float SIX_STAGE = 12.0f;
    

    public BusTrip(String busNumber, String departLocation, LocalTime departTime, String arrivalLocation, LocalTime arrivalTime, float cost) {
        this.departLocation = departLocation;
        this.arrivalLocation = arrivalLocation;
        this.busNumber = busNumber;
        this.cost = cost;
        this.arrivalTime = arrivalTime;
        this.departTime = departTime;
        if(arrivalLocation.equalsIgnoreCase(departLocation))
            throw new IllegalArgumentException("BusTrip constructor: ARRIVAL PORT "+ arrivalLocation+" IS EQUAL TO DEPART PORT "+departLocation);
        if(departTime.compareTo(arrivalTime)>=0)
            throw new IllegalArgumentException("BusTrip constructor: ARRIVAL TIME "+arrivalTime+" LATER THAN DEPART TIME "+departTime);
    }

    public LocalTime getDepartTime() {
        return departTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public float getCost() {
        return cost;
    }

    public String getArrivalLocation() {
        return arrivalLocation;
    }

    public String getDepartLocation() {
        return departLocation;
    }
    @Override
    public String toString()
    {   String s = "> BUS: "+busNumber+" LEAVING "+departLocation+" ("+departTime+") and ARRIVING "+arrivalLocation+" ("+arrivalTime+") $"+cost;
        return s;
    }
    @Override
    public boolean equals(Object o)
    {   if(o instanceof BusTrip)
        {   BusTrip s = (BusTrip)o;
        
            return busNumber.equals(s.busNumber) && departLocation.equals(s.departLocation) 
                    && departTime.equals(s.departTime);
        }
        else return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.departTime);
        hash = 17 * hash + Objects.hashCode(this.busNumber);
        hash = 17 * hash + Objects.hashCode(this.departLocation);
        return hash;
    }
}
