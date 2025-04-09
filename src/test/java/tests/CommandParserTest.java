package tests;

// Specific JUnit imports
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull; // Added missing import
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
// import org.junit.Before; // Not used
import org.junit.Test;
// import org.junit.After; // Not used
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import controller.CommandParser;
// import controller.CalendarController; // No longer needed here
// import controller.ICalendarController; // No longer needed here
import model.CalendarEvent;
import model.CalendarManager; // Still needed to check underlying state sometimes
import model.ICalendarEvent;
import model.ICalendarManager; // Keep for type casting if needed, though less direct access now
import model.MultiCalendarManager; // Need to instantiate for controller
import model.InvalidDataException; // Import exception

/**
 * Tests the CommandParser functionality with various command types, formats, and edge cases to
 * verify correct parsing and delegation to the controller.
 */
public class CommandParserTest {

    // Helper to create a manager instance for tests
    private ICalendarManager createTestManager() throws InvalidDataException {
        // Use CalendarManager directly for these parser tests, as they target single calendar commands
        return new CalendarManager("DefaultCalendar", "America/New_York");
    }

    @Test
    public void testProcessCommand_InvalidCommand() throws Exception {
        ICalendarManager manager = createTestManager();
        try {
            CommandParser.processCommand("nonexistent command", manager); // Pass manager
            fail("Expected Exception for invalid command");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid command"));
        }
    }

    @Test(expected = Exception.class)
    public void testProcessCreateEvent_MissingTo() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event Test from 2025-03-01T10:00", manager); // Pass manager
    }

    @Test(expected = Exception.class)
    public void testProcessEditCommand_MissingWith() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("edit event description Test from "
            + "2025-03-01T10:00 to 2025-03-01T11:00", manager); // Pass manager
    }

    @Test
    public void testProcessShowStatus() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event Test from "
            + "2025-03-01T10:00 to 2025-03-01T11:00", manager); // Pass manager
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        CommandParser.processCommand("show status on 2025-03-01T10:30", manager); // Pass manager
        System.setOut(originalOut);
        String output = baos.toString();
        assertTrue("Should indicate Busy", output.contains("Busy"));
    }

    @Test
    public void testProcessPrintEventsOn() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event Test from "
            + "2025-03-01T10:00 to 2025-03-01T11:00", manager); // Pass manager
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        CommandParser.processCommand("print events on 2025-03-01", manager); // Pass manager
        System.setOut(originalOut);
        String output = baos.toString();
        // Check output printed by CommandParser using OutputHandler
        assertTrue("Should list event Test", output.contains("Test from"));
    }

    private String captureOutput(Runnable runnable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return baos.toString();
    }

    @Test
    public void testProcessCreateEvent_Timed() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
        CommandParser.processCommand(cmd, manager); // Pass manager
        List<ICalendarEvent> events = manager.getAllEvents();
        assertEquals(1, events.size());
        ICalendarEvent ev = events.get(0);
        assertFalse(ev.isAllDay());
        assertEquals("Meeting", ev.getEventName());
    }

    @Test
    public void testProcessCreateEvent_AllDay() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Holiday on 2025-03-05";
        CommandParser.processCommand(cmd, manager); // Pass manager
        List<ICalendarEvent> events = manager.getAllEvents();
        assertEquals(1, events.size());
        ICalendarEvent ev = events.get(0);
        assertTrue(ev.isAllDay());
        assertEquals("Holiday", ev.getEventName());
    }

    @Test
    public void testProcessCreateEvent_RepeatingTimed() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Workshop from 2025-03-02T09:00 to "
            + "2025-03-02T10:00 repeats MWF for 3 times";
        CommandParser.processCommand(cmd, manager); // Pass manager
        assertEquals(3, manager.getAllEvents().size());
    }

    @Test
    public void testProcessCreateEvent_RepeatingAllDay() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Seminar on 2025-03-03 repeats MWF until 2025-03-10";
        CommandParser.processCommand(cmd, manager); // Pass manager
        List<ICalendarEvent> events = manager.getAllEvents();
        assertTrue("Should create occurrences", events.size() > 0);
        for (ICalendarEvent ev : events) {
            assertTrue(ev.isAllDay());
        }
    }

    @Test(expected = Exception.class)
    public void testProcessCreateEvent_MissingToKeyword() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Faulty from 2025-03-01T10:00 2025-03-01T11:00";
        CommandParser.processCommand(cmd, manager); // Pass manager
    }

    @Test
    public void testProcessEditCommand_SingularSuccess() throws Exception {
        ICalendarManager manager = createTestManager();
        String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
        CommandParser.processCommand(createCmd, manager); // Pass manager
        String editCmd = "edit event description Meeting from 2025-03-01T10:00 to "
            + "2025-03-01T11:00 with UpdatedDesc";
        CommandParser.processCommand(editCmd, manager); // Pass manager
        ICalendarEvent ev = manager.getAllEvents().get(0);
        assertEquals("UpdatedDesc", ev.getDescription());
    }

    @Test(expected = Exception.class)
    public void testProcessEditCommand_MissingToForSingular() throws Exception {
        ICalendarManager manager = createTestManager();
        String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
        CommandParser.processCommand(createCmd, manager); // Pass manager
        String editCmd = "edit event description Meeting from 2025-03-01T10:00 with Updated";
        CommandParser.processCommand(editCmd, manager); // Pass manager
    }

    @Test
    public void testProcessEditCommand_Plural() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event Conference from "
            + "2025-03-01T09:00 to 2025-03-01T10:00", manager); // Pass manager
        CommandParser.processCommand("create event Conference from "
            + "2025-03-02T09:00 to 2025-03-02T10:00", manager); // Pass manager
        String editCmd = "edit events location Conference with NewLocation"; // Edit by name
        CommandParser.processCommand(editCmd, manager); // Pass manager
        for (ICalendarEvent ev : manager.getAllEvents()) {
            assertEquals("NewLocation", ev.getLocation());
        }
    }

    @Test
    public void testProcessPrintEventsOn_Valid() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event Meeting from "
            + "2025-03-01T10:00 to 2025-03-01T11:00", manager); // Pass manager
        String output = captureOutput(() -> {
            try {
                CommandParser.processCommand("print events on 2025-03-01", manager); // Pass manager
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(output.contains("Meeting"));
    }

    @Test(expected = Exception.class)
    public void testProcessPrintEventsOn_InvalidFormat() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("print events 2025-03-01", manager); // Pass manager
    }

    @Test
    public void testProcessPrintEventsRange_Valid() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event Meeting from "
            + "2025-03-01T10:00 to 2025-03-01T11:00", manager); // Pass manager
        CommandParser.processCommand("create event Workshop from "
            + "2025-03-01T12:00 to 2025-03-01T13:00", manager); // Pass manager
        String output = captureOutput(() -> {
            try {
                CommandParser.processCommand("print events from "
                    + "2025-03-01T09:00 to 2025-03-01T14:00", manager); // Pass manager
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(output.contains("Meeting"));
        assertTrue(output.contains("Workshop"));
    }

    @Test(expected = Exception.class)
    public void testProcessPrintEventsRange_MissingTo() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("print events from "
            + "2025-03-01T09:00 2025-03-01T14:00", manager); // Pass manager
    }

    // @Test // Commented out - export logic moved to controller
    // public void testProcessExportCal_Valid() throws Exception { ... }

    @Test(expected = Exception.class)
    public void testProcessExportCal_Invalid() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("export cal", manager); // Pass manager
    }

    // @Test // Commented out - export logic moved to controller
    // public void testProcessExportGoogleCSV_Valid() throws Exception { ... }

    @Test(expected = Exception.class)
    public void testProcessExportGoogleCSV_Invalid() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("export googlecsv", manager); // Pass manager
    }

    @Test
    public void testProcessShowStatus_Valid() throws Exception {
        ICalendarManager manager = createTestManager();
        CommandParser.processCommand("create event StatusTest from "
            + "2025-03-01T10:00 to 2025-03-01T11:00", manager); // Pass manager
        String output = captureOutput(() -> {
            try {
                CommandParser.processCommand("show status on 2025-03-01T10:30", manager); // Pass manager
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(output.contains("Busy"));
    }

    @Test(expected = Exception.class) // Expect CalendarConflictException now wrapped by parser
    public void testProcessCreateEvent_AutoDeclineConflict() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd1 = "create event ConflictTest from 2025-03-01T10:00 to "
            + "2025-03-01T11:00 --autodecline"; // autodecline is ignored
        CommandParser.processCommand(cmd1, manager); // Pass manager
        String cmd2 = "create event ConflictTest2 from 2025-03-01T10:30 to "
            + "2025-03-01T11:30 --autodecline";
        CommandParser.processCommand(cmd2, manager); // Pass manager
    }

    @Test
    public void testProcessCreateEvent_Time() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
        CommandParser.processCommand(cmd, manager); // Pass manager
        List<ICalendarEvent> events = manager.getAllEvents();
        assertEquals(1, events.size());
        ICalendarEvent ev = events.get(0);
        assertFalse(ev.isAllDay());
        assertEquals("Meeting", ev.getEventName());
    }

    @Test
    public void testProcessCreateEvent_AllDayCommand() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Holiday on 2025-03-05";
        CommandParser.processCommand(cmd, manager); // Pass manager
        List<ICalendarEvent> events = manager.getAllEvents();
        assertEquals(1, events.size());
        ICalendarEvent ev = events.get(0);
        assertTrue(ev.isAllDay());
    }

    @Test
    public void testProcessCreateEvent_RepeatingTimedCommand() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd = "create event Workshop from 2025-03-02T09:00 "
            + "to 2025-03-02T10:00 repeats MWF for 3 times";
        CommandParser.processCommand(cmd, manager); // Pass manager
        assertEquals(3, manager.getAllEvents().size());
    }

    @Test
    public void testProcessCreateEvent_RepeatingAllDayCommand() throws Exception {
        ICalendarManager manager = createTestManager();
        // Assuming parser handles date-only until
        String cmd = "create event Seminar on 2025-03-03 repeats MWF until 2025-03-10";
        CommandParser.processCommand(cmd, manager); // Pass manager
        List<ICalendarEvent> events = manager.getAllEvents();
        assertTrue("Should create multiple occurrences", events.size() > 0);
        for (ICalendarEvent ev : events) {
            assertTrue(ev.isAllDay());
        }
    }

    @Test
    public void testProcessEditCommand_SingularSuccessEditcommand() throws Exception {
        ICalendarManager manager = createTestManager();
        String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
        CommandParser.processCommand(createCmd, manager); // Pass manager
        String editCmd = "edit event description Meeting from "
            + "2025-03-01T10:00 to 2025-03-01T11:00 with Quarterly results";
        CommandParser.processCommand(editCmd, manager); // Pass manager
        ICalendarEvent ev = manager.getAllEvents().get(0);
        assertEquals("Quarterly results", ev.getDescription());
    }

    // @Test // Commented out - helper method is private
    // public void testGetUpdateMessage() { ... }

    @Test
    public void testEditEventSubjectSingular() throws Exception {
        ICalendarManager manager = createTestManager();
        String createCmd = "create event office-hours-cs5010-2 from 2024-03-13T10:00 "
            + "to 2024-03-13T12:00";
        CommandParser.processCommand(createCmd, manager); // Pass manager

        String editCmd = "edit event subject office-hours-cs5010-2 from 2024-03-13T10:00 "
            + "to 2024-03-13T12:00 with office-hours-cs5010-second";
        CommandParser.processCommand(editCmd, manager); // Pass manager

        List<ICalendarEvent> events = manager.getAllEvents();
        assertEquals(1, events.size());
        assertEquals("office-hours-cs5010-second", events.get(0).getEventName());
    }

    @Test
    public void testEditEventsSubjectPlural() throws Exception {
        ICalendarManager manager = createTestManager();
        String cmd1 = "create event doctor-appointment from 2024-03-14T07:00 to 2024-03-14T08:00";
        String cmd2 = "create event doctor-appointment from 2024-03-14T09:00 to 2024-03-14T10:00";
        CommandParser.processCommand(cmd1, manager); // Pass manager
        CommandParser.processCommand(cmd2, manager); // Pass manager

        String editCmd = "edit events subject doctor-appointment from 2024-03-14T09:00 " // Edit by start time >= 09:00
            + "with annual-physical";
        CommandParser.processCommand(editCmd, manager); // Pass manager

        List<ICalendarEvent> events = manager.getAllEvents();
        assertEquals(2, events.size());
        // Only the second event should be updated
        assertEquals("doctor-appointment", events.get(0).getEventName());
        assertEquals("annual-physical", events.get(1).getEventName());
    }
}
