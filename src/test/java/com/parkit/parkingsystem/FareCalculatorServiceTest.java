package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @BeforeAll
    public static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    public void setUpPerTest() {
        ticket = new Ticket();
    }

    @Test
    public void calculateFareCar(){
        // TEST 1 - Test du calcul du prix du parking pour 1h (Voiture)

        // Création d'un temps factice d'une heure dans le parking
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        // On attribue les résultats au ticket
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // On exécute le calcul
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.CAR_RATE_PER_HOUR);
    }

    @Test
    public void calculateFareBike(){
        // TEST 2 - Test du calcul du prix du parking pour 1h (Moto)

        // Création d'un temps factice d'une heure dans le parking
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        // On attribue les résultats au ticket
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // On exécute le calcul
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.BIKE_RATE_PER_HOUR);
    }

    @Test
    public void calculateFareCarWithLessThan30minutesParkingTime() {
        // TEST 3 - Test du calcul du prix du parking pour moins de 30 min (Voiture)

        // Création d'un temps factice d'une demi-heure dans le parking
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 30 * 60 * 1000 ) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        // On attribue les résultats au ticket
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // On exécute le calcul
        fareCalculatorService.calculateFare(ticket);

        // Vérification que le prix est bien zéro
        assertEquals(0, ticket.getPrice());
    }

    @Test
    public void calculateFareBikeWithLessThan30minutesParkingTime() {
        // TEST 4 - Test du calcul du prix du parking pour moins de 30 min (Moto)

        // Création d'un temps factice d'une demi-heure dans le parking
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 30 * 60 * 1000 ) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        // On attribue les résultats au ticket
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // On exécute le calcul
        fareCalculatorService.calculateFare(ticket);

        // Vérification que le prix est bien zéro
        assertEquals(0, ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithDiscount() {
        // TEST 5 - Test du calcul du prix du parking pour 1 utilisateur récurrent (Voiture)

        // Création d'un temps factice d'une heure dans le parking
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000 ) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        // On attribue les résultats au ticket
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // On exécute le calcul
        fareCalculatorService.calculateFare(ticket, true);

        // Vérification que 5% sont bien déduits du prix
        assertEquals(1.425, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareBikeWithDiscount() {
        // TEST 6 - Test du calcul du prix du parking pour 1 utilisateur récurrent (Moto)

        // Création d'un temps factice d'une heure dans le parking
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000 ) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        // On attribue les résultats au ticket
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // On exécute le calcul
        fareCalculatorService.calculateFare(ticket, true);

        // Vérification que 5% sont bien déduits du prix
        assertEquals(0.95, ticket.getPrice(), 0.001);
    }
}