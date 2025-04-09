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
 * Tests specifically focused on the 'create calendar' command parsing and execution
 * via the CommandParser.
 */
public class CommandParserCreateCalendarTest {

    private MultiCalendarManager model; // Use model directly for these tests

    @Before
    public void setUp() throws InvalidDataException {
        model = new MultiCalendarManager();
    }

    @Test
    public void testProcessCreateCalendar_Valid() throws Exception {
        String command = "create calendar --name Work --timezone America/New_York";
        CommandParser.processCommand(command, model); // Pass model
        // Verify state through the model instance
        assertNotNull(model.getCurrentCalendar()); // Check if a calendar became active
        assertEquals("Work", model.getCurrentCalendar().getCalendarName());
        assertEquals("America/New_York", model.getCurrentCalendar().getTimeZone().getId());
    }

    @Test(expected = Exception.class)
    public void testProcessCreateCalendar_MissingName() throws Exception {
        String command = "create calendar --timezone America/New_York";
        CommandParser.processCommand(command, model); // Pass model
    }

    @Test(expected = Exception.class)
    public void testProcessCreateCalendar_MissingTimezone() throws Exception {
        String command = "create calendar --name Work";
        CommandParser.processCommand(command, model); // Pass model
    }

    @Test(expected = Exception.class)
    public void testProcessCreateCalendar_InvalidTimezone() throws Exception {
        String command = "create calendar --name Work --timezone Invalid/Zone";
        // The exception might come from the model constructor now
        CommandParser.processCommand(command, model); // Pass model
    }

    @Test(expected = Exception.class)
    public void testProcessCreateCalendar_DuplicateName() throws Exception {
        String command1 = "create calendar --name Work --timezone America/New_York";
        CommandParser.processCommand(command1, model); // Pass model
        String command2 = "create calendar --name Work --timezone America/Los_Angeles";
        CommandParser.processCommand(command2, model); // Pass model (Should fail in model)
    }

    @Test(expected = Exception.class)
    public void testProcessCreateCalendar_InvalidFormat() throws Exception {
        String command = "create calendar Work America/New_York"; // Missing keywords
        CommandParser.processCommand(command, model); // Pass model
    }
}
