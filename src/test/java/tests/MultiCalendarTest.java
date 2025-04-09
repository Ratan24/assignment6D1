package tests;

// Specific JUnit imports
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.After; // Added missing import
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File; // Keep if file operations remain in tests
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter; // Added for copy tests
import java.util.List;

import controller.CommandParser;
// import controller.CalendarController; // No longer needed
// import controller.ICalendarController; // No longer needed
import model.CalendarEvent;
import model.CalendarManager;
import model.ICalendarEvent;
import model.MultiCalendarManager;
import model.EventNotFoundException; // Import specific exceptions
import model.CalendarConflictException;
import model.InvalidDataException; // Import exception

/**
 * Tests the MultiCalendarManager functionality including calendar management operations,
 * cross-calendar event copying, and delegation of event operations to the currently active
 * calendar. Also tests error handling for operations when no calendar exists or when target
 * resources cannot be found. Export tests removed as functionality moved to Controller/Util.
 */
public class MultiCalendarTest {

    private MultiCalendarManager model; // Use model directly for these tests
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"); // Added formatter

    @Before
    public void setUp() throws Exception { // Allow exceptions from createCalendar
        originalOut = System.out;
        System.setOut(new PrintStream(outContent)); // Capture output for verification
        model = new MultiCalendarManager();
    }

    @After
    public void tearDown() {
        System.setOut(originalOut); // Restore standard output
    }

//    @Test
//    public void testCreateAndUseCalendar() throws Exception {
//        CommandParser.processCommand("create calendar --name Personal --timezone America/Chicago", model); // Pass model
//        assertTrue(outContent.toString().contains("Calendar created: Personal"));
//        outContent.reset();
//
//        CommandParser.processCommand("create calendar --name Work --timezone America/New_York", model); // Pass model
//        assertTrue(outContent.toString().contains("Calendar created: Work"));
//        outContent.reset();
//
//        // Let's explicitly switch
//        CommandParser.processCommand("use calendar --name Personal", model); // Pass model
//        assertTrue(outContent.toString().contains("Using calendar: Personal"));
//        assertEquals("Personal", model.getCurrentCalendar().getCalendarName());
//        outContent.reset();
//
//        CommandParser.processCommand("use calendar --name Work", model); // Pass model
//        assertTrue(outContent.toString().contains("Using calendar: Work"));
//        assertEquals("Work", model.getCurrentCalendar().getCalendarName());
//    }
//
//    @Test
//    public void testEditCalendarName() throws Exception {
//        CommandParser.processCommand("create calendar --name Temp --timezone UTC", model); // Pass model
//        CommandParser.processCommand("use calendar --name Temp", model); // Pass model
//        outContent.reset();
//
//        CommandParser.processCommand("edit calendar --name Temp --property name Permanent", model); // Pass model
//        assertTrue(outContent.toString().contains("Calendar 'Temp' renamed to: Permanent"));
//        assertEquals("Permanent", model.getCurrentCalendar().getCalendarName()); // Active calendar name should update
//
//        // Verify the old name is gone
//        try {
//            model.useCalendar("Temp"); // Use model directly
//            fail("Should have failed to switch to old name 'Temp'");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("not found"));
//        }
//    }
//
//    @Test
//    public void testEditCalendarTimezone() throws Exception {
//        CommandParser.processCommand("create calendar --name ZoneTest --timezone UTC", model); // Pass model
//        CommandParser.processCommand("use calendar --name ZoneTest", model); // Pass model
//        outContent.reset();
//
//        CommandParser.processCommand("edit calendar --name ZoneTest --property timezone Asia/Tokyo", model); // Pass model
//        assertTrue(outContent.toString().contains("Active calendar timezone updated to: Asia/Tokyo"));
//        assertEquals("Asia/Tokyo", model.getCurrentCalendar().getTimeZone().getId());
//    }

    @Test
    public void testCopyEvent() throws Exception {
        CommandParser.processCommand("create calendar --name Source --timezone UTC", model); // Pass model
        CommandParser.processCommand("create calendar --name Target --timezone UTC", model); // Pass model
        CommandParser.processCommand("use calendar --name Source", model); // Pass model
        CommandParser.processCommand("create event CopyMe from 2025-04-01T10:00 to 2025-04-01T11:00", model); // Pass model
        outContent.reset();

        // Use the correct format expected by the parser
        String copyCmd = String.format("copy event CopyMe on %s --target Target to %s",
                                       "2025-04-01T10:00", // Source identifier time
                                       "2025-04-02T14:00"); // Target start time
        CommandParser.processCommand(copyCmd, model); // Pass model

        // Verify event exists in Target calendar
        model.useCalendar("Target");
        List<ICalendarEvent> targetEvents = model.getCurrentCalendar().getEventsOn(LocalDate.parse("2025-04-02")); // Use getCurrentCalendar()
        assertEquals(1, targetEvents.size());
        assertEquals("CopyMe", targetEvents.get(0).getEventName());
        assertEquals(LocalDateTime.parse("2025-04-02T14:00"), targetEvents.get(0).getStart());
    }

    @Test
    public void testCopyEventsOn() throws Exception {
        CommandParser.processCommand("create calendar --name Source --timezone UTC", model); // Pass model
        CommandParser.processCommand("create calendar --name Target --timezone UTC", model); // Pass model
        CommandParser.processCommand("use calendar --name Source", model); // Pass model
        CommandParser.processCommand("create event Event1 from 2025-04-03T09:00 to 2025-04-03T10:00", model); // Pass model
        CommandParser.processCommand("create event Event2 from 2025-04-03T11:00 to 2025-04-03T12:00", model); // Pass model
        outContent.reset();

        String copyCmd = "copy events on 2025-04-03 --target Target to 2025-04-04";
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("Target");
        List<ICalendarEvent> targetEvents = model.getCurrentCalendar().getEventsOn(LocalDate.parse("2025-04-04")); // Use getCurrentCalendar()
        assertEquals(2, targetEvents.size());
        // Check times (should match original times but on the new date)
        assertTrue(targetEvents.stream().anyMatch(e -> e.getStart().equals(LocalDateTime.parse("2025-04-04T09:00"))));
        assertTrue(targetEvents.stream().anyMatch(e -> e.getStart().equals(LocalDateTime.parse("2025-04-04T11:00"))));
    }

    @Test
    public void testCopyEventsBetween() throws Exception {
        CommandParser.processCommand("create calendar --name Source --timezone UTC", model); // Pass model
        CommandParser.processCommand("create calendar --name Target --timezone UTC", model); // Pass model
        CommandParser.processCommand("use calendar --name Source", model); // Pass model
        CommandParser.processCommand("create event Day1Event from 2025-04-05T09:00 to 2025-04-05T10:00", model); // Pass model
        CommandParser.processCommand("create event Day2Event from 2025-04-06T11:00 to 2025-04-06T12:00", model); // Pass model
        CommandParser.processCommand("create event Day3Event from 2025-04-07T13:00 to 2025-04-07T14:00", model); // Pass model (Outside range)
        outContent.reset();

        String copyCmd = "copy events between 2025-04-05 and 2025-04-06 to --target Target 2025-04-10";
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("Target");
        List<ICalendarEvent> targetEvents = model.getCurrentCalendar().getEventsInRange( // Use getCurrentCalendar()
            LocalDate.parse("2025-04-10").atStartOfDay(),
            LocalDate.parse("2025-04-12").atStartOfDay() // Check range including target start + offset
        );
        assertEquals(2, targetEvents.size());
        // Check dates and times
        assertTrue(targetEvents.stream().anyMatch(e -> e.getEventName().equals("Day1Event") && e.getStart().equals(LocalDateTime.parse("2025-04-10T09:00"))));
        assertTrue(targetEvents.stream().anyMatch(e -> e.getEventName().equals("Day2Event") && e.getStart().equals(LocalDateTime.parse("2025-04-11T11:00"))));
    }

    // Add tests for error conditions in multi-calendar commands if not covered elsewhere
    @Test(expected = Exception.class)
    public void testCopyEvent_SourceEventNotFound() throws Exception {
        CommandParser.processCommand("create calendar --name Source --timezone UTC", model); // Pass model
        CommandParser.processCommand("create calendar --name Target --timezone UTC", model); // Pass model
        CommandParser.processCommand("use calendar --name Source", model); // Pass model
        String copyCmd = "copy event NonExistent on 2025-04-01T10:00 --target Target to 2025-04-02T14:00";
        CommandParser.processCommand(copyCmd, model); // Pass model
    }

     @Test(expected = Exception.class)
    public void testCopyEvent_TargetCalNotFound() throws Exception {
        CommandParser.processCommand("create calendar --name Source --timezone UTC", model); // Pass model
        CommandParser.processCommand("use calendar --name Source", model); // Pass model
        CommandParser.processCommand("create event CopyMe from 2025-04-01T10:00 to 2025-04-01T11:00", model); // Pass model
        String copyCmd = "copy event CopyMe on 2025-04-01T10:00 --target NoTarget to 2025-04-02T14:00";
        CommandParser.processCommand(copyCmd, model); // Pass model
    }

    // --- Tests Added Based on Assignment 5 Feedback ---

    @Test
    public void testCopyEvent_SameTimezone() throws Exception {
        CommandParser.processCommand("create calendar --name CalA --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("create calendar --name CalB --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("use calendar --name CalA", model); // Pass model
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 1, 11, 0);
        CommandParser.processCommand("create event EventSameTZ from " + start.format(dateTimeFormatter) + " to " + end.format(dateTimeFormatter), model); // Pass model

        LocalDateTime targetStart = LocalDateTime.of(2025, 5, 2, 10, 0);
        String copyCmd = String.format("copy event EventSameTZ on %s --target CalB to %s",
                                       start.format(dateTimeFormatter),
                                       targetStart.format(dateTimeFormatter));
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("CalB");
        List<ICalendarEvent> eventsB = model.getCurrentCalendar().getEventsOn(targetStart.toLocalDate()); // Use getCurrentCalendar()
        assertEquals(1, eventsB.size());
        assertEquals("EventSameTZ", eventsB.get(0).getEventName());
        assertEquals(targetStart, eventsB.get(0).getStart());
        assertEquals(targetStart.plusHours(1), eventsB.get(0).getEnd());
    }

    @Test
    public void testCopyEvent_DifferentTimezone() throws Exception {
        CommandParser.processCommand("create calendar --name CalNY --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("create calendar --name CalLA --timezone America/Los_Angeles", model); // Pass model
        CommandParser.processCommand("use calendar --name CalNY", model); // Pass model
        LocalDateTime startNY = LocalDateTime.of(2025, 5, 1, 10, 0); // 10 AM NY
        LocalDateTime endNY = LocalDateTime.of(2025, 5, 1, 11, 0);
        CommandParser.processCommand("create event EventDiffTZ from " + startNY.format(dateTimeFormatter) + " to " + endNY.format(dateTimeFormatter), model); // Pass model

        LocalDateTime targetStartLA = LocalDateTime.of(2025, 5, 1, 10, 0); // 10 AM LA
        String copyCmd = String.format("copy event EventDiffTZ on %s --target CalLA to %s",
                                       startNY.format(dateTimeFormatter),
                                       targetStartLA.format(dateTimeFormatter));
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("CalLA");
        List<ICalendarEvent> eventsLA = model.getCurrentCalendar().getEventsOn(targetStartLA.toLocalDate()); // Use getCurrentCalendar()
        assertEquals(1, eventsLA.size());
        assertEquals("EventDiffTZ", eventsLA.get(0).getEventName());
        assertEquals(targetStartLA, eventsLA.get(0).getStart());
        assertEquals(targetStartLA.plusHours(1), eventsLA.get(0).getEnd());
    }

     @Test
    public void testCopyEventsOn_SameTimezone() throws Exception {
        CommandParser.processCommand("create calendar --name CalA --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("create calendar --name CalB --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("use calendar --name CalA", model); // Pass model
        CommandParser.processCommand("create event EventA1 from 2025-05-03T09:00 to 2025-05-03T10:00", model); // Pass model
        CommandParser.processCommand("create event EventA2 from 2025-05-03T14:00 to 2025-05-03T15:00", model); // Pass model

        String copyCmd = "copy events on 2025-05-03 --target CalB to 2025-05-04";
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("CalB");
        List<ICalendarEvent> eventsB = model.getCurrentCalendar().getEventsOn(LocalDate.parse("2025-05-04")); // Use getCurrentCalendar()
        assertEquals(2, eventsB.size());
        assertTrue(eventsB.stream().anyMatch(e -> e.getStart().equals(LocalDateTime.parse("2025-05-04T09:00"))));
        assertTrue(eventsB.stream().anyMatch(e -> e.getStart().equals(LocalDateTime.parse("2025-05-04T14:00"))));
    }

     @Test
    public void testCopyEventsOn_DifferentTimezone() throws Exception {
        CommandParser.processCommand("create calendar --name CalNY --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("create calendar --name CalLA --timezone America/Los_Angeles", model); // Pass model
        CommandParser.processCommand("use calendar --name CalNY", model); // Pass model
        CommandParser.processCommand("create event EventNY1 from 2025-05-05T09:00 to 2025-05-05T10:00", model); // Pass model
        CommandParser.processCommand("create event EventNY2 from 2025-05-05T14:00 to 2025-05-05T15:00", model); // Pass model

        String copyCmd = "copy events on 2025-05-05 --target CalLA to 2025-05-06";
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("CalLA");
        List<ICalendarEvent> eventsLA = model.getCurrentCalendar().getEventsOn(LocalDate.parse("2025-05-06")); // Use getCurrentCalendar()
        assertEquals(2, eventsLA.size());
        // Times should be preserved locally on the new date
        assertTrue(eventsLA.stream().anyMatch(e -> e.getStart().equals(LocalDateTime.parse("2025-05-06T09:00"))));
        assertTrue(eventsLA.stream().anyMatch(e -> e.getStart().equals(LocalDateTime.parse("2025-05-06T14:00"))));
    }

     @Test
    public void testCopyEventsBetween_SameTimezone() throws Exception {
        CommandParser.processCommand("create calendar --name CalA --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("create calendar --name CalB --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("use calendar --name CalA", model); // Pass model
        CommandParser.processCommand("create event Day1 from 2025-05-10T09:00 to 2025-05-10T10:00", model); // Pass model
        CommandParser.processCommand("create event Day2 from 2025-05-11T11:00 to 2025-05-11T12:00", model); // Pass model

        String copyCmd = "copy events between 2025-05-10 and 2025-05-11 to --target CalB 2025-05-15";
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("CalB");
        List<ICalendarEvent> eventsB = model.getCurrentCalendar().getEventsInRange(LocalDate.parse("2025-05-15").atStartOfDay(), LocalDate.parse("2025-05-17").atStartOfDay()); // Use getCurrentCalendar()
        assertEquals(2, eventsB.size());
        assertTrue(eventsB.stream().anyMatch(e -> e.getEventName().equals("Day1") && e.getStart().equals(LocalDateTime.parse("2025-05-15T09:00"))));
        assertTrue(eventsB.stream().anyMatch(e -> e.getEventName().equals("Day2") && e.getStart().equals(LocalDateTime.parse("2025-05-16T11:00"))));
    }

     @Test
    public void testCopyEventsBetween_DifferentTimezone() throws Exception {
        CommandParser.processCommand("create calendar --name CalNY --timezone America/New_York", model); // Pass model
        CommandParser.processCommand("create calendar --name CalLA --timezone America/Los_Angeles", model); // Pass model
        CommandParser.processCommand("use calendar --name CalNY", model); // Pass model
        CommandParser.processCommand("create event DayNY1 from 2025-05-10T09:00 to 2025-05-10T10:00", model); // Pass model
        CommandParser.processCommand("create event DayNY2 from 2025-05-11T11:00 to 2025-05-11T12:00", model); // Pass model

        String copyCmd = "copy events between 2025-05-10 and 2025-05-11 to --target CalLA 2025-05-15";
        CommandParser.processCommand(copyCmd, model); // Pass model

        model.useCalendar("CalLA");
        List<ICalendarEvent> eventsLA = model.getCurrentCalendar().getEventsInRange(LocalDate.parse("2025-05-15").atStartOfDay(), LocalDate.parse("2025-05-17").atStartOfDay()); // Use getCurrentCalendar()
        assertEquals(2, eventsLA.size());
        // Times should be preserved locally relative to the target start date
        assertTrue(eventsLA.stream().anyMatch(e -> e.getEventName().equals("DayNY1") && e.getStart().equals(LocalDateTime.parse("2025-05-15T09:00"))));
        assertTrue(eventsLA.stream().anyMatch(e -> e.getEventName().equals("DayNY2") && e.getStart().equals(LocalDateTime.parse("2025-05-16T11:00"))));
    }

}
