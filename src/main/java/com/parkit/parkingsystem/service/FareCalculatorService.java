package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount) {
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();

        double duration = (double) (outTime - inTime) / (1000 * 60 * 60);

        if (duration <= 0.5) {
            ticket.setPrice(0);
            return;
        }

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                double rate = Fare.CAR_RATE_PER_HOUR;
                ticket.setPrice(discount ? duration * rate * 0.95 : duration * rate);
                break;
            }

            case BIKE: {
                double rate = Fare.BIKE_RATE_PER_HOUR;
                ticket.setPrice(discount ? duration * rate * 0.95 : duration * rate);
                break;
            }

            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }

}