package tests;

// Specific JUnit imports
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import controller.CommandParser;
// import controller.CalendarController; // No longer needed
// import controller.ICalendarController; // No longer needed
import model.MultiCalendarManager;
import model.InvalidDataException; // Import exception
import java.time.ZoneId; // Import ZoneId

/**
 * Tests specifically focused on the 'edit calendar' command parsing and execution
 * via the CommandParser.
 */
public class CommandParserEditCalendarTest {

    private MultiCalendarManager model; // Use model directly for these tests

    @Before
    public void setUp() throws Exception { // Allow exceptions from createCalendar
        model = new MultiCalendarManager();
        // Pre-create a calendar for editing
        model.createCalendar("Work", "America/New_York");
        model.useCalendar("Work"); // Ensure it's active
    }

    @Test
    public void testProcessEditCalendar_ValidNameChange() throws Exception {
        String command = "edit calendar --name Work --property name Office";
        CommandParser.processCommand(command, model); // Pass model
        // Verify state through the model instance
        assertEquals("Office", model.getCurrentCalendar().getCalendarName());
        // Try switching to the new name using the model directly
        model.useCalendar("Office");
        assertEquals("Office", model.getCurrentCalendar().getCalendarName());
    }

     @Test
    public void testProcessEditCalendar_ValidTimezoneChange() throws Exception {
        String command = "edit calendar --name Work --property timezone America/Los_Angeles";
        CommandParser.processCommand(command, model); // Pass model
        assertEquals("America/Los_Angeles", model.getCurrentCalendar().getTimeZone().getId());
    }

    @Test(expected = Exception.class)
    public void testProcessEditCalendar_MissingName() throws Exception {
        String command = "edit calendar --property timezone America/Chicago";
        CommandParser.processCommand(command, model); // Pass model
    }

    @Test(expected = Exception.class)
    public void testProcessEditCalendar_MissingProperty() throws Exception {
        String command = "edit calendar --name Work America/Chicago";
        CommandParser.processCommand(command, model); // Pass model
    }

     @Test(expected = Exception.class)
    public void testProcessEditCalendar_MissingValue() throws Exception {
        String command = "edit calendar --name Work --property timezone";
        CommandParser.processCommand(command, model); // Pass model
    }

    @Test(expected = Exception.class)
    public void testProcessEditCalendar_InvalidProperty() throws Exception {
        String command = "edit calendar --name Work --property color blue";
        CommandParser.processCommand(command, model); // Pass model
    }

    @Test(expected = Exception.class)
    public void testProcessEditCalendar_InvalidTimezoneValue() throws Exception {
        String command = "edit calendar --name Work --property timezone Invalid/Zone";
        CommandParser.processCommand(command, model); // Pass model
    }

     @Test(expected = Exception.class)
    public void testProcessEditCalendar_TargetNameExists() throws Exception {
        model.createCalendar("Personal", "UTC"); // Create another calendar directly in model
        String command = "edit calendar --name Work --property name Personal"; // Try renaming Work to Personal
        CommandParser.processCommand(command, model); // Pass model
    }

     @Test(expected = Exception.class)
    public void testProcessEditCalendar_CalendarNotFound() throws Exception {
        String command = "edit calendar --name NonExistent --property name NewName";
        CommandParser.processCommand(command, model); // Pass model
    }

     @Test(expected = Exception.class)
    public void testProcessEditCalendar_InvalidFormat() throws Exception {
        String command = "edit calendar Work timezone UTC"; // Missing keywords
        CommandParser.processCommand(command, model); // Pass model
    }
}
