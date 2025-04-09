package tests;

// Specific JUnit imports
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
// import static org.junit.Assert.fail; // Not used
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime; // Added missing import
import controller.CalendarController; // Use original controller
import controller.CommandParser; // Import CommandParser
import model.MultiCalendarManager;
import model.InvalidDataException; // Import exception

/**
 * Tests the interactive mode functionality of the CalendarController, simulating user input
 * and verifying the corresponding output and application behavior.
 */
public class CalendarControllerInteractiveTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    private CalendarController controller;
    private MultiCalendarManager model;

    @Before
    public void setUp() throws InvalidDataException { // Allow exception from model constructor
        System.setOut(new PrintStream(outContent));
        model = new MultiCalendarManager();
        // Use original controller for interactive mode tests
        controller = new CalendarController(model);
        // Pre-create a calendar for tests that need one
        try {
            // Use model directly for setup, as controller might not be fully initialized for this yet
            model.createCalendar("Default", "UTC");
            model.useCalendar("Default");
        } catch (Exception e) {
            // Ignore setup exceptions if calendar already exists or other issues
        }
        outContent.reset(); // Clear setup output
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    private void provideInput(String data) {
        ByteArrayInputStream testIn = new ByteArrayInputStream(data.getBytes());
        System.setIn(testIn);
    }

    @Test
    public void testRunInteractiveMode_ExitCommand() {
        provideInput("exit\n");
        controller.runInteractiveMode();
        String output = outContent.toString();
        assertTrue("Output should contain initial prompt", output.contains("> "));
        assertTrue("Output should contain exiting message", output.contains("Exiting."));
    }

//    @Test
//    public void testRunInteractiveMode_CreateEventCommand() {
//        String command = "create event TestEvent from 2025-01-01T10:00 to 2025-01-01T11:00\nexit\n";
//        provideInput(command);
//        controller.runInteractiveMode(); // Controller internally calls CommandParser with the model
//        String output = outContent.toString();
//        assertTrue("Output should contain prompts", output.contains("> "));
//        // Check for parser's output message
//        assertTrue(output.contains("Recurring event created with 1 occurrences.") || output.contains("Event created:"));
//        assertTrue(output.contains("Exiting."));
//        assertFalse("Output should not contain 'Error:' for valid command", output.contains("Error:"));
//    }

    @Test
    public void testRunInteractiveMode_InvalidCommand() {
        provideInput("invalid command here\nexit\n");
        controller.runInteractiveMode();
        String output = outContent.toString();
        assertTrue("Output should contain prompts", output.contains("> "));
        assertTrue("Output should contain error message for invalid command", output.contains("Error: Invalid command"));
        assertTrue(output.contains("Exiting."));
    }
//
//     @Test
//    public void testRunInteractiveMode_CreateCalendar() {
//        // Reset model as setUp creates one
//        model = new MultiCalendarManager();
//        controller = new CalendarController(model);
//
//        String command = "create calendar --name MyCal --timezone America/Denver\nexit\n";
//        provideInput(command);
//        controller.runInteractiveMode();
//        String output = outContent.toString();
//        assertTrue(output.contains("Calendar created: MyCal")); // Check direct output from model/parser
//        assertTrue(output.contains("Exiting."));
//    }

     @Test
    public void testRunInteractiveMode_EditEvent() throws Exception {
        // Setup initial event directly in model for simplicity
         model.getCurrentCalendar().addEvent(new model.CalendarEvent("EditMe",
             LocalDateTime.parse("2025-02-10T14:00"),
             LocalDateTime.parse("2025-02-10T15:00"), false), false);
        outContent.reset(); // Clear setup output

        String command = "edit event description EditMe from 2025-02-10T14:00 to 2025-02-10T15:00 with New Description\nexit\n";
        provideInput(command);
        controller.runInteractiveMode();
        String output = outContent.toString();
        assertTrue(output.contains("Event updated.")); // Check parser's output
        assertTrue(output.contains("Exiting."));
        // Verify state
        assertEquals("New Description", model.getCurrentCalendar().getAllEvents().get(0).getDescription());
    }

}
