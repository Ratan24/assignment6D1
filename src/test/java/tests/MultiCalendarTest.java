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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import model.CalendarEvent;
import model.CalendarManager;
import model.ICalendarEvent;
import model.MultiCalendarManager;
import controller.CommandParser;

/**
 * Tests the MultiCalendarManager functionality including calendar management operations,
 * cross-calendar event copying, and delegation of event operations to the currently active
 * calendar. Also tests error handling for operations when no calendar exists or when target
 * resources cannot be found.
 */
public class MultiCalendarTest {

  private MultiCalendarManager multiCal;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private PrintStream originalOut;

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
  public void testEditCalendar_TimezoneChange_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "edit calendar --name Work --property timezone America/Los_Angeles";
    CommandParser.processCommand(command, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Calendar timezone updated to: America/Los_Angeles"));
  }

  @Test
  public void testEditCalendar_NameChange_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    outContent.reset();
    String command = "edit calendar --name Work --property name Office";
    CommandParser.processCommand(command, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Calendar name updated to: Office"));
    multiCal.useCalendar("Office");
    assertEquals("Office", multiCal.getCurrentCalendar().getCalendarName());
  }

  @Test(expected = Exception.class)
  public void testEditCalendar_InvalidProperty() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "edit calendar --name Work --property invalidProp someValue";
    CommandParser.processCommand(command, multiCal);
  }

  @Test(expected = Exception.class)
  public void testUseCalendar_NonExistent() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "use calendar --name Personal";
    CommandParser.processCommand(command, multiCal);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetCurrentCalendar_NoCalendarSelected() {
    MultiCalendarManager manager = new MultiCalendarManager();
    manager.getCurrentCalendar();
  }

  @Test
  public void testGetCurrentCalendar_CalendarSelected() throws Exception {
    MultiCalendarManager manager = new MultiCalendarManager();
    manager.createCalendar("TestCal", "UTC");
    CalendarManager current = manager.getCurrentCalendar();
    assertNotNull(current);
    assertEquals("TestCal", current.getCalendarName());
  }

  @Test(expected = Exception.class)
  public void testCopyEvent_TargetCalendarNotFound() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String createCommand = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(createCommand, multiCal.getCurrentCalendar());
    outContent.reset();
    String copyCommand = "copy event Meeting on 2025-03-27T09:00 --target NonExistent "
        + "to 2025-03-27T11:00";
    CommandParser.processCommand(copyCommand, multiCal);
  }

  @Test(expected = Exception.class)
  public void testCopyEvent_EventNotFound() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String copyCommand = "copy event Meeting on 2025-03-27T09:00 --target Personal "
        + "to 2025-03-27T11:00";
    CommandParser.processCommand(copyCommand, multiCal);
  }

  @Test(expected = Exception.class)
  public void testCopyEventsOn_NoEvents() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String command = "copy events on 2025-03-27 --target Personal to 2025-03-28";
    CommandParser.processCommand(command, multiCal);
  }

  @Test
  public void testCopyEventsBetween_NoEvents() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String command = "copy events between 2025-03-27 and 2025-03-28 to --target Personal "
        + "2025-03-29";
    CommandParser.processCommand(command, multiCal);
    String output = outContent.toString();
    assertTrue(output.contains("Copied 0 event(s)"));
  }

  @Test
  public void testCopyEventsBetween_MultipleDays() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");
    String event1 = "create event Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    String event2 = "create event Seminar from 2025-03-28T11:00 to 2025-03-28T12:00";
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
    LocalDate firstEventDate = events.get(0).getStart().toLocalDate();
    LocalDate secondEventDate = events.get(1).getStart().toLocalDate();
    assertEquals(LocalDate.parse("2025-03-29"), firstEventDate);
    assertEquals(LocalDate.parse("2025-03-30"), secondEventDate);
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

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_MissingToClause() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "create event Meeting from 2025-03-27T09:00";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
  }

  @Test(expected = Exception.class)
  public void testProcessEditCommand_MissingWithClause() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "edit event subject Meeting from 2025-03-27T09:00 to 2025-03-27T10:00";
    CommandParser.processCommand(command, multiCal.getCurrentCalendar());
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

  @Test
  public void testCopyEvent_CopiesAllProperties() throws Exception {
    MultiCalendarManager multiCal = new MultiCalendarManager();
    multiCal.createCalendar("SourceCal", "UTC");
    multiCal.useCalendar("SourceCal");
    CalendarEvent original = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 30, 10, 0),
        LocalDateTime.of(2025, 3, 30, 11, 0), false);
    original.setDescription("Team meeting");
    original.setLocation("Conference Room A");
    original.setPublic(false);
    multiCal.addEvent(original, false);

    multiCal.createCalendar("TargetCal", "UTC");

    LocalDateTime sourceStart = original.getStart();
    LocalDateTime targetStart = LocalDateTime.of(2025, 3, 31, 10, 0);
    multiCal.copyEvent("Meeting", sourceStart, "TargetCal", targetStart);

    multiCal.useCalendar("TargetCal");
    List<ICalendarEvent> events = multiCal.getAllEvents();
    assertFalse(events.isEmpty());
    ICalendarEvent copied = events.get(0);

    assertEquals(original.getEventName(), copied.getEventName());
    assertEquals(targetStart, copied.getStart());

    long originalDuration = java.time.Duration.between(original.getStart(), original.getEnd())
        .toMinutes();
    long copiedDuration = java.time.Duration.between(copied.getStart(), copied.getEnd())
        .toMinutes();
    assertEquals(originalDuration, copiedDuration);

    assertEquals(original.getDescription(), copied.getDescription());
    assertEquals(original.getLocation(), copied.getLocation());
    assertEquals(original.isPublic(), copied.isPublic());
  }

  @Test
  public void testGetEventsOn_NoCalendar() {
    List<ICalendarEvent> events = multiCal.getEventsOn(LocalDate.now());
    assertNotNull(events);
    assertTrue("Expected no events when no calendar exists.", events.isEmpty());
  }

  @Test
  public void testGetEventsInRange_NoCalendar() {
    List<ICalendarEvent> events = multiCal.getEventsInRange(LocalDateTime.now(),
        LocalDateTime.now().plusHours(1));
    assertNotNull(events);
    assertTrue("Expected no events in range when no calendar exists.", events.isEmpty());
  }

  @Test
  public void testExportToCSV_NoCalendar() {
    multiCal.exportToCSV("dummy.csv");
    String output = outContent.toString();
    assertTrue("Expected error message when exporting CSV with no calendar.",
        output.contains("Error exporting CSV:"));
  }

  @Test
  public void testExportToGoogleCSV_NoCalendar() {
    multiCal.exportToGoogleCSV("dummy.csv");
    String output = outContent.toString();
    assertTrue("Expected error message when exporting Google CSV with no calendar.",
        output.contains("Error exporting Google CSV:"));
  }

  @Test
  public void testIsBusyAt_NoCalendar() {
    boolean busy = multiCal.isBusyAt(LocalDateTime.now());
    assertFalse("Expected isBusyAt to return false with no calendar.", busy);
  }

  @Test
  public void testEditSingleEvent_NoEvent() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    boolean result = multiCal.editSingleEvent("subject", "NonExistent",
        LocalDateTime.now(), LocalDateTime.now().plusHours(1), "NewValue");
    assertFalse("Expected editSingleEvent to return false for non-existent event.", result);
  }

  @Test
  public void testEditEventsByStart_NoEvent() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    int count = multiCal.editEventsByStart("subject", "NonExistent", LocalDateTime.now(),
        "NewValue");
    assertEquals("Expected 0 events updated by start.", 0, count);
  }

  @Test
  public void testEditEventsByName_NoEvent() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    int count = multiCal.editEventsByName("subject", "NonExistent", "NewValue");
    assertEquals("Expected 0 events updated by name.", 0, count);
  }

  @Test
  public void testEditSingleEvent_Valid() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    CalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0),
        LocalDateTime.of(2025, 3, 27, 10, 0), false);
    multiCal.addEvent(event, false);
    boolean result = multiCal.editSingleEvent("subject", "Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0),
        LocalDateTime.of(2025, 3, 27, 10, 0), "UpdatedMeeting");
    assertTrue("Expected editSingleEvent to succeed.", result);
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals("UpdatedMeeting", events.get(0).getEventName());
  }

  @Test
  public void testEditEventsByStart_NoMatchingEvent() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    int count = multiCal.editEventsByStart("subject", "Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0), "UpdatedMeeting");
    assertEquals(0, count);
  }

  @Test
  public void testEditEventsByName_NoMatchingEvent() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    int count = multiCal.editEventsByName("subject", "Meeting", "UpdatedMeeting");
    assertEquals(0, count);
  }

  @Test
  public void testCopyEventsOn_CopiesDescriptionLocationPublic() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");

    CalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0),
        LocalDateTime.of(2025, 3, 27, 10, 0), false);
    event.setDescription("Test Description");
    event.setLocation("Test Location");
    event.setPublic(false);
    multiCal.getCurrentCalendar().addEvent(event, false);

    String command = "copy events on 2025-03-27 --target Personal to 2025-03-28";
    CommandParser.processCommand(command, multiCal);

    multiCal.useCalendar("Personal");
    List<ICalendarEvent> events = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals("Expected one event copied", 1, events.size());
    ICalendarEvent copiedEvent = events.get(0);

    assertEquals("Test Description", copiedEvent.getDescription());
    assertEquals("Test Location", copiedEvent.getLocation());
    assertFalse("Expected the event to be private", copiedEvent.isPublic());
  }

  @Test
  public void testCopyEventsBetween_CopiesDescriptionLocationPublic() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    multiCal.createCalendar("Personal", "America/Los_Angeles");

    CalendarEvent event = new CalendarEvent("Conference",
        LocalDateTime.of(2025, 3, 27, 14, 0),
        LocalDateTime.of(2025, 3, 27, 16, 0), false);
    event.setDescription("Important conference");
    event.setLocation("Conference Hall A");
    event.setPublic(false);
    multiCal.getCurrentCalendar().addEvent(event, false);

    String command = "copy events between 2025-03-27 and 2025-03-27 to --target Personal "
        + "2025-03-29";
    CommandParser.processCommand(command, multiCal);

    multiCal.useCalendar("Personal");
    List<ICalendarEvent> copiedEvents = multiCal.getCurrentCalendar().getAllEvents();
    assertEquals("Expected one event copied", 1, copiedEvents.size());

    ICalendarEvent copiedEvent = copiedEvents.get(0);
    assertEquals("Important conference", copiedEvent.getDescription());
    assertEquals("Conference Hall A", copiedEvent.getLocation());
    assertFalse("Expected the event to be private", copiedEvent.isPublic());
  }

  @Test
  public void testEditCalendar_TimezoneUpdate() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    String command = "edit calendar --name Work --property timezone America/Los_Angeles";
    CommandParser.processCommand(command, multiCal);
    ZoneId tz = multiCal.getCurrentCalendar().getTimeZone();
    assertEquals("Timezone should be updated to America/Los_Angeles",
        ZoneId.of("America/Los_Angeles"), tz);
  }

  @Test
  public void testGetEventsOn_WithEvents() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    CalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0),
        LocalDateTime.of(2025, 3, 27, 10, 0),
        false);
    multiCal.getCurrentCalendar().addEvent(event, false);
    List<ICalendarEvent> events = multiCal.getEventsOn(LocalDate.of(2025, 3, 27));
    assertFalse("Expected non-empty list of events", events.isEmpty());
    assertEquals("Meeting", events.get(0).getEventName());
  }

  @Test
  public void testGetEventsInRange_WithEvents() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    LocalDateTime start = LocalDateTime.of(2025, 3, 27, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 27, 10, 0);
    CalendarEvent event = new CalendarEvent("Meeting", start, end, false);
    multiCal.getCurrentCalendar().addEvent(event, false);
    List<ICalendarEvent> events = multiCal.getEventsInRange(start.minusMinutes(1),
        end.plusMinutes(1));
    assertFalse("Expected non-empty event range", events.isEmpty());
    assertEquals("Meeting", events.get(0).getEventName());
  }

  @Test
  public void testExportToCSV() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    CalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0),
        LocalDateTime.of(2025, 3, 27, 10, 0),
        false);
    multiCal.getCurrentCalendar().addEvent(event, false);
    String fileName = "testExport.csv";
    outContent.reset();
    multiCal.exportToCSV(fileName);
    String output = outContent.toString();
    assertTrue("Expected success message for CSV export",
        output.contains("Exported to CSV:"));
    File file = new File(fileName);
    assertTrue("Exported CSV file should exist", file.exists());
    file.delete();
  }

  @Test
  public void testExportToGoogleCSV() throws Exception {
    multiCal.createCalendar("Work", "America/New_York");
    CalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 27, 9, 0),
        LocalDateTime.of(2025, 3, 27, 10, 0),
        false);
    multiCal.getCurrentCalendar().addEvent(event, false);
    String fileName = "testGoogle.csv";
    outContent.reset();
    multiCal.exportToGoogleCSV(fileName);
    String output = outContent.toString();
    assertTrue("Expected success message for Google CSV export",
        output.contains("Exported to Google CSV:"));
    File file = new File(fileName);
    assertTrue("Exported Google CSV file should exist", file.exists());
    file.delete();
  }
}