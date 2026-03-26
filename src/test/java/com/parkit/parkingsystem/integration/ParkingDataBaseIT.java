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
    public static void setUp() throws Exception{
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
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        System.out.println("----------- INTEGRATION -----------");
        System.out.println("----------- TEST 1 -----------");

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        assertEquals(0.0, ticket.getPrice());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertFalse(ticket.getParkingSpot().isAvailable());
    }

    @Test
    public void testParkingLotExit() {
        System.out.println("----------- TEST 2 -----------");

        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

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

        parkingService.processExitingVehicle();

        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(updatedTicket.getOutTime());
        assertTrue(updatedTicket.getPrice() > 0);
        assertTrue(updatedTicket.getParkingSpot().isAvailable());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        parkingService.processIncomingVehicle();

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

        parkingService.processExitingVehicle();

        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
        long inTime = updatedTicket.getInTime().getTime();
        long outTime = updatedTicket.getOutTime().getTime();

        double duration = (double) (outTime - inTime) / (1000 * 60 * 60);
        double expectedPriceRaw = duration * 1.5 * 0.95;

        double actualPrice = updatedTicket.getPrice();

        double expectedPriceTruncated = Math.floor(expectedPriceRaw * 100.0) / 100.0;
        double actualPriceTruncated = Math.floor(actualPrice * 100.0) / 100.0;

        assertEquals(expectedPriceTruncated, actualPriceTruncated);
    }
}