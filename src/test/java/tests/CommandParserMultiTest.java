package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.util.List;

import controller.CommandParser;
import model.ICalendarEvent;
import model.MultiCalendarManager;

/**
 * Integration tests for CommandParser with MultiCalendarManager. Tests various command types
 * including calendar management, event management, copying functionality, and export operations to
 * verify that commands are properly parsed and executed.
 */
public class CommandParserMultiTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private PrintStream originalOut;
  private MultiCalendarManager multiCal;

  @Before
  public void setUp() throws Exception {
    originalOut = System.out;
    System.setOut(new PrintStream(outContent));
    multiCal = new MultiCalendarManager();
  }

  @After
  public void tearDown() throws Exception {
    System.setOut(originalOut);
    outContent.reset();
  }

  @Test
  public void testProcessCreateCalendar_Valid() throws Exception {
    String command = "create calendar --name Work --timezone America/New_York";
    CommandParser.processCommand(command, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Calendar created: Work (America/New_York)"));
    assertNotNull(multiCal.getCurrentCalendar());
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
  }

  @Test
  public void testProcessEditCalendar_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "edit calendar --name Work --property timezone America/Los_Angeles";
    CommandParser.processCommand(command, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Calendar timezone updated to: America/Los_Angeles"));
  }

  @Test
  public void testProcessUseCalendar_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    outContent.reset();
    String command = "use calendar --name Personal";
    CommandParser.processCommand(command, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Using calendar: Personal"));
    assertEquals("Personal", multiCal.getCurrentCalendar().getCalendarName());
  }

  @Test
  public void testProcessCopyEvent_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String eventCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(eventCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String copyCommand = "copy event Meeting on 2025-03-27T09:00 --target Personal "
        + "to 2025-03-27T11:00";
    CommandParser.processCommand(copyCommand, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Event copied to calendar Personal:"));
    multiCal.useCalendar("Personal");
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertFalse(events.isEmpty());
    assertEquals("Meeting", events.get(0).getEventName());
  }

  @Test
  public void testProcessCopyEventsOn_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String eventCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(eventCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String copyCommand = "copy events on 2025-03-27 --target Personal to 2025-03-28";
    CommandParser.processCommand(copyCommand, multiCal);
    String output = outContent.toString();
    assertTrue(
        output.contains("Copied 1 event(s) from 2025-03-27 to Personal starting on 2025-03-28"));
    multiCal.useCalendar("Personal");
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertFalse(events.isEmpty());
    assertEquals("Meeting", events.get(0).getEventName());
  }

  @Test
  public void testProcessCopyEventsBetween_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String event1 = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    String event2 = "create event Meeting from 2025-03-28T09:00 to 2025-03-28T10:00";
    CommandParser.processCommand(event1, multiCal.getCurrentCalendar());
    CommandParser.processCommand(event2, multiCal.getCurrentCalendar());
    outContent.reset();
    String copyCommand = "copy events between 2025-03-27 and 2025-03-28 to --target Personal "
        + "2025-03-29";
    CommandParser.processCommand(copyCommand, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Copied 2 event(s)"));
    multiCal.useCalendar("Personal");
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals(2, events.size());
  }

  @Test
  public void testProcessCreateEvent_Timed_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Event created:"));
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getEventName());
  }

  @Test
  public void testProcessCreateEvent_AllDay_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "create event Holiday on 2025-12-25";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("All-day event created:"));
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Holiday", events.get(0).getEventName());
    assertTrue(events.get(0).isAllDay());
  }

  @Test
  public void testProcessCreateEvent_RepeatingTimed_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "create event Workshop from 2025-03-27T09:00 to 2025-03-27T10:00 "
        + "repeats M for 3 times";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Recurring event created with 3 occurrences."));
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals(3, events.size());
  }

  @Test
  public void testProcessCreateEvent_RepeatingAllDay_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "create event Conference on 2025-03-27 repeats M for 2 times";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Recurring all-day event created with 2 occurrences."));
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals(2, events.size());
  }

  @Test
  public void testProcessEditCommand_Singular_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String createCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(createCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String editCommand = "edit event subject Meeting from 2025-03-27T09:00 to 2025-03-27T10:00 "
        + "with UpdatedMeeting";
    CommandParser.processCommand(editCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Event updated successfully."));
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals("UpdatedMeeting", events.get(0).getEventName());
  }

  @Test
  public void testProcessEditCommand_Plural_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String event1 = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    String event2 = "create event Meeting from 2025-03-27T10:00 to 2025-03-27T11:00";
    CommandParser.processCommand(event1, multiCal.getCurrentCalendar());
    CommandParser.processCommand(event2, multiCal.getCurrentCalendar());
    outContent.reset();
    String editCommand = "edit events subject Meeting from 2025-03-27T09:00 with PluralUpdate";
    CommandParser.processCommand(editCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("event(s) updated starting from 2025-03-27T09:00"));
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    for (ICalendarEvent event : events) {
      assertEquals("PluralUpdate", event.getEventName());
    }
  }

  @Test
  public void testProcessPrintEventsOn_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String createCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(createCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String printCommand = "print events on 2025-03-27";
    CommandParser.processCommand(printCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Events on 2025-03-27"));
    assertTrue(output.contains("Meeting"));
  }

  @Test
  public void testProcessPrintEventsRange_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String eventCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(eventCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String rangeCommand = "print events from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(rangeCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Events between"));
    assertTrue(output.contains("Meeting"));
  }

  @Test
  public void testProcessExportCal_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String eventCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(eventCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String exportCommand = "export cal testExport.csv";
    CommandParser.processCommand(exportCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Exported to CSV:"));
    File f = new File("testExport.csv");
    if (f.exists()) {
      f.delete();
    }
  }

  @Test
  public void testProcessExportGoogleCSV_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String eventCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(eventCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String exportCommand = "export googlecsv testGoogle.csv";
    CommandParser.processCommand(exportCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Exported to Google CSV:"));
    File f = new File("testGoogle.csv");
    if (f.exists()) {
      f.delete();
    }
  }

  @Test
  public void testProcessShowStatus_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String eventCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(eventCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String statusCommand = "show status on 2025-03-27T09:30";
    CommandParser.processCommand(statusCommand, multiCal.getCurrentCalendar());
    String output = outContent.toString();
    assertTrue(output.contains("Status at 2025-03-27T09:30: Busy"));
  }

  @Test(expected = Exception.class)
  public void testProcessCommand_InvalidManager() throws Exception {
    CommandParser.processCommand("create event Test from 2025-03-27T09:00 to 2025-03-27T10:00",
        new Object());
  }

  @Test(expected = Exception.class)
  public void testCreateCalendar_MissingTimezone() throws Exception {
    String command = "create calendar --name Work";
    CommandParser.processCommand(command, multiCal);
  }

  @Test(expected = Exception.class)
  public void testCreateEvent_MissingToClause() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "create event Meeting from 2025-03-27T09:00";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
  }

  @Test(expected = Exception.class)
  public void testEditCommand_MissingWithClause() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "edit event subject Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
  }

  @Test(expected = Exception.class)
  public void testCopyEventsOn_MissingTargetToken() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "copy events on 2025-03-27 to 2025-03-28";
    CommandParser.processCommand(command, multiCal);
  }

  @Test(expected = Exception.class)
  public void testCopyEventsBetween_MissingAndToken() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "copy events between 2025-03-27 2025-03-29 to --target Personal 2025-03-28";
    CommandParser.processCommand(command, multiCal);
  }
}