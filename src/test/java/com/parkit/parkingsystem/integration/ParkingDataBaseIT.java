package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final Logger logger = LogManager.getLogger("ParkingDataBaseIT");

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        // Préparation de la BDD

        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;

        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;

        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){
        // À la fin des tests on remet la BDD à son état initial
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        // TEST 1

        // Création d'un parking
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // On simule l'entrée du véhicule
        parkingService.processIncomingVehicle();

        // Récupération du ticket associé à notre véhicule
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        /*
            Vérification (dans l'ordre de haut en bas) :

            - de l'existence du ticket
            - de l'existence de l'heure d'arrivée
            - du prix du ticket à zéro euro
            - de la plaque du véhicule
            - de l'état de la place (occupée)
         */

        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        assertEquals(0.0, ticket.getPrice());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertFalse(ticket.getParkingSpot().isAvailable());
    }

    @Test
    public void testParkingLotExit() {
        // TEST 2

        // On rappelle le test précédent pour avoir une voiture dans la BDD
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Manipulation manuelle de la base de données pour que la voiture soit garée depuis 1h
        Connection con = null;

        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE ticket SET IN_TIME = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE VEHICLE_REG_NUMBER = 'ABCDEF';"
            );
            ps.executeUpdate();
        }

        catch (Exception e) {
            logger.error("Impossible de modifier l'heure d'entrée en base de données", e);
        }

        finally {
            dataBaseTestConfig.closeConnection(con);
        }

        // Simulation de la sortie de la voiture
        parkingService.processExitingVehicle();

        // Récupération du ticket associé à notre véhicule
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");

        /*
            Vérification (dans l'ordre de haut en bas) :

            - de l'existence de l'heure de sortie
            - que le prix du ticket est supérieur à zéro
            - que la place est à nouveau libre
         */

        assertNotNull(updatedTicket.getOutTime());
        assertTrue(updatedTicket.getPrice() > 0);
        assertTrue(updatedTicket.getParkingSpot().isAvailable());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        // TEST 3

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // On simule un premier passage d'un véhicule dans le parking
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        // On simule un second passage
        parkingService.processIncomingVehicle();

        // Idem que plus haut, on édite l'heure d'arrivée manuellement
        Connection con = null;

        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE ticket SET IN_TIME = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE VEHICLE_REG_NUMBER = 'ABCDEF';"
            );
            ps.executeUpdate();
        }

        catch (Exception e) {
            logger.error("Impossible de modifier l'heure d'entrée en base de données", e);
        }

        finally {
            dataBaseTestConfig.closeConnection(con);
        }

        // On simule la sortie du véhicule du parking
        parkingService.processExitingVehicle();

        // Récupération du ticket associé à notre véhicule
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");

        // Récupération des horaires d'arrivée et de départ du véhicule
        long inTime = updatedTicket.getInTime().getTime();
        long outTime = updatedTicket.getOutTime().getTime();

        // Calcul du prix selon le temps passé dans le parking, on prend en compte la réduction de 5%
        double duration = (double) (outTime - inTime) / (1000 * 60 * 60);
        double expectedPriceRaw = duration * 1.5 * 0.95;

        // Récupération du prix renvoyé par le système
        double actualPrice = updatedTicket.getPrice();

        // On arrondit à 2 après la virgule
        double expectedPriceTruncated = Math.floor(expectedPriceRaw * 100.0) / 100.0;
        double actualPriceTruncated = Math.floor(actualPrice * 100.0) / 100.0;

        // Enfin on vérifie que les deux prix sont égaux
        assertEquals(expectedPriceTruncated, actualPriceTruncated);
    }
}