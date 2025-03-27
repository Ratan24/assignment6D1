package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import calendar.CalendarApp;
import controller.CalendarController;
import controller.CommandParser;
import model.CalendarEvent;
import model.CalendarManager;
import model.ICalendarEvent;
import model.ICalendarManager;
import model.MultiCalendarManager;

/**
 * Comprehensive test suite for the Calendar application that verifies the functionality
 * of individual components and their integration. Tests cover event creation, editing,
 * recurring events, conflict detection, command parsing, and controller operations in
 * both interactive and headless modes.
 */
public class CalendarAppTest {

  @Test
  public void testCreateTimedEvent() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    String command = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(command, manager);
    assertEquals(1, manager.getAllEvents().size());
  }

  @Test
  public void testCreateAllDayEvent() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String command = "create event Holiday on 2025-03-05";
    CommandParser.processCommand(command, manager);
    assertEquals(1, manager.getAllEvents().size());
    assertTrue(manager.getAllEvents().get(0).isAllDay());
  }

  @Test
  public void testRecurringEventFixedTimes() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String command = "create event Workshop on 2025-03-02 repeats MTWRF for 3 times";
    CommandParser.processCommand(command, manager);
    assertEquals(3, manager.getAllEvents().size());
  }

  @Test
  public void testRecurringEventUntil() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String command = "create event Seminar from 2025-03-03T09:00 to 2025-03-03T10:30 repeats WF " +
        "until 2025-03-10T00:00";
    CommandParser.processCommand(command, manager);
    assertTrue(manager.getAllEvents().size() > 0);
  }

  @Test
  public void testEditSingleEvent() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 to 2025-03-01T11:00 " +
        "with Quarterly results";
    CommandParser.processCommand(editCmd, manager);
    assertEquals("Quarterly results", manager.getAllEvents().get(0).getDescription());
  }

  @Test
  public void testEditEventsByStart() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Seminar from 2025-03-03T09:00 to " +
        "2025-03-03T10:30", manager);
    CommandParser.processCommand("create event Seminar from 2025-03-04T09:00 to " +
        "2025-03-04T10:30", manager);
    String editCmd = "edit events description Seminar from 2025-03-04T00:00 with UpdatedSeminar";
    CommandParser.processCommand(editCmd, manager);
    for (ICalendarEvent event : manager.getAllEvents()) {
      if (event.getStart().equals(LocalDateTime.parse("2025-03-04T09:00"))) {
        assertEquals("UpdatedSeminar", event.getDescription());
      } else {
        assertEquals("", event.getDescription());
      }
    }
  }

  @Test
  public void testEditEventsByName() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Holiday on 2025-03-05", manager);
    CommandParser.processCommand("create event Holiday on 2025-03-06", manager);
    String editCmd = "edit events location Holiday with Beach";
    CommandParser.processCommand(editCmd, manager);
    for (ICalendarEvent event : manager.getAllEvents()) {
      if (event.getEventName().equals("Holiday")) {
        assertEquals("Beach", event.getLocation());
      }
    }
  }

  @Test(expected = Exception.class)
  public void testMissingFromKeyword() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String command = "create event Meeting 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(command, manager);
  }

  @Test(expected = Exception.class)
  public void testMissingToKeywordInEdit() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 with NoToClause";
    CommandParser.processCommand(editCmd, manager);
  }

  @Test
  public void testPrintEventsOn() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String printCmd = "print events on 2025-03-01";
    CommandParser.processCommand(printCmd, manager);
    List<ICalendarEvent> events = manager.getEventsOn(LocalDate.parse("2025-03-01"));
    assertFalse("There should be events on 2025-03-01", events.isEmpty());
  }

  @Test
  public void testShowStatus() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(outContent));

    String statusCmd = "show status on 2025-03-01T10:30";
    CommandParser.processCommand(statusCmd, manager);

    System.setOut(originalOut);

    String statusOutput = outContent.toString().trim();

    String expectedOutput = "Status at 2025-03-01T10:30: Busy";

    assertEquals(expectedOutput, statusOutput);
  }

  @Test
  public void testExportGoogleCSV() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String exportCmd = "export googlecsv test_google.csv";
    CommandParser.processCommand(exportCmd, manager);
    File file = new File("test_google.csv");
    assertTrue("The exported Google CSV file should exist", file.exists());
    assertTrue("The exported file should not be empty", file.length() > 0);
    file.delete();
  }

  @Test
  public void testInvalidCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    try {
      CommandParser.processCommand("invalid command", manager);
      fail("Expected exception for invalid command");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test
  public void testExportCalCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String exportCmd = "export cal test_export.csv";
    CommandParser.processCommand(exportCmd, manager);
    File f = new File("test_export.csv");
    assertTrue("Exported CSV file should exist", f.exists());
    assertTrue("Exported file should not be empty", f.length() > 0);
    f.delete();
  }

  @Test
  public void testShowStatusOutput() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Expected status output to contain 'Busy'", output.contains("Busy"));
  }

  @Test
  public void testEditEventsWithoutFromClause() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Holiday on 2025-03-05", manager);
    CommandParser.processCommand("create event Holiday on 2025-03-06", manager);
    CommandParser.processCommand("edit events location Holiday with Beach", manager);
    for (ICalendarEvent event : manager.getAllEvents()) {
      if (event.getEventName().equals("Holiday")) {
        assertEquals("Beach", event.getLocation());
      }
    }
  }

  @Test
  public void testPrintEventsRange() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Meeting from 2025-03-01T10:00 to " +
        "2025-03-01T11:00", manager);
    CommandParser.processCommand("create event Workshop on 2025-03-02", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("print events from 2025-03-01T00:00 to " +
        "2025-03-03T00:00", manager);
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("Workshop"));
  }

  @Test
  public void testMainInsufficientArgs() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarApp.main(new String[]{});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Should print usage instructions",
        output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  @Test
  public void testMainInvalidMode() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(baos));
    CalendarApp.main(new String[]{"--mode", "foobar"});
    System.setOut(oldOut);
    String output = baos.toString();
    assertTrue("Should indicate invalid mode",
        output.contains("Invalid mode. Use interactive or headless."));
  }

  @Test
  public void testConflictsWithOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 30);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 30);

    CalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    CalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);

    assertTrue("Events that overlap should conflict", event1.conflictsWith(event2));
    assertTrue("Events that overlap should conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWithNonOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 12, 0);

    CalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    CalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);

    assertFalse("Events that do not overlap should not conflict"
        , event1.conflictsWith(event2));
    assertFalse("Events that do not overlap should not conflict"
        , event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWithBoundaryTouching() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 0);

    CalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    CalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);

    assertFalse("Events that touch boundaries should not conflict", event1.conflictsWith(event2));
    assertFalse("Events that touch boundaries should not conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testToString_AllDayWithDescriptionAndLocation() {
    LocalDateTime start = LocalDate.of(2025, 3, 1).atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    CalendarEvent event = new CalendarEvent("Holiday", start, end, true);
    event.setDescription("Vacation");
    event.setLocation("Beach");
    event.setPublic(false);
    String expected = "Holiday (All Day on 2025-03-01)" +
        ", Description: Vacation, Location: Beach, Private";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_TimedEventWithoutExtras() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 11, 0);
    CalendarEvent event = new CalendarEvent("Meeting", start, end, false);
    String expected = "Meeting from 2025-03-01 10:00 to 2025-03-01 11:00, Public";
    assertEquals(expected, event.toString());
  }

  @Test(expected = Exception.class)
  public void testAddEventWithConflictAutoDecline() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CalendarEvent event1 = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);
    CalendarEvent event2 = new CalendarEvent("Meeting2",
        LocalDateTime.of(2025, 3, 1, 10, 30),
        LocalDateTime.of(2025, 3, 1, 11, 30)
        , false);
    manager.addEvent(event2, true);
  }

  @Test(expected = Exception.class)
  public void testAddEventWithConflictAutoDecline1() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CalendarEvent event1 = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    CalendarEvent event2 = new CalendarEvent("Meeting2",
        LocalDateTime.of(2025, 3, 1, 10, 30),
        LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, true);
  }

  @Test
  public void testEventsAreSortedAfterAdd() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    CalendarEvent event1 = new CalendarEvent(
        "Event1",
        LocalDateTime.of(2025, 3, 1, 12, 0),
        LocalDateTime.of(2025, 3, 1, 13, 0),
        false);
    CalendarEvent event2 = new CalendarEvent(
        "Event2",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0),
        false);
    CalendarEvent event3 = new CalendarEvent(
        "Event3",
        LocalDateTime.of(2025, 3, 1, 14, 0),
        LocalDateTime.of(2025, 3, 1, 15, 0),
        false);

    manager.addEvent(event1, false);
    manager.addEvent(event2, false);
    manager.addEvent(event3, false);

    List<ICalendarEvent> sorted = manager.getAllEvents();
    assertEquals("First event should be Event2", "Event2"
        , sorted.get(0).getEventName());
    assertEquals("Second event should be Event1", "Event1"
        , sorted.get(1).getEventName());
    assertEquals("Third event should be Event3", "Event3"
        , sorted.get(2).getEventName());
  }

  @Test(expected = Exception.class)
  public void testConflictAutoDecline() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CalendarEvent e1 = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, true);
    CalendarEvent e2 = new CalendarEvent("Meeting2",
        LocalDateTime.of(2025, 3, 1, 10, 30),
        LocalDateTime.of(2025, 3, 1, 11, 30)
        , false);
    manager.addEvent(e2, true);
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
  public void testRunInteractiveMode_exitImmediately() {
    String simulatedInput = "exit\n";
    InputStream originalIn = System.in;
    System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

    MultiCalendarManager multiCal = new MultiCalendarManager();
    CalendarController controller = new CalendarController(multiCal);

    String output = captureOutput(() -> controller.runInteractiveMode());

    System.setIn(originalIn);

    assertTrue(output.contains("Calendar App Interactive Mode. Type 'exit' to quit."));
    assertTrue(output.contains("> "));
    assertTrue(output.contains("Exiting."));
  }

  @Test
  public void testRunHeadlessMode_validFile() throws Exception {
    File tempFile = File.createTempFile("commands", ".txt");
    try (PrintWriter writer = new PrintWriter(tempFile)) {
      writer.println("create calendar --name TestCal --timezone America/New_York");
      writer.println("exit");
    }

    MultiCalendarManager multiCal = new MultiCalendarManager();
    CalendarController controller = new CalendarController(multiCal);

    String output = captureOutput(() -> controller.runHeadlessMode(tempFile.getAbsolutePath()));
    tempFile.delete();

    assertTrue(output.contains("> create calendar --name TestCal --timezone America/New_York"));
    assertTrue(output.contains("Calendar created: TestCal (America/New_York)"));
    assertTrue(output.contains("Exiting."));
  }

  @Test
  public void testRunHeadlessMode_invalidFile() {
    MultiCalendarManager multiCal = new MultiCalendarManager();
    CalendarController controller = new CalendarController(multiCal);
    String output = captureOutput(() -> controller.runHeadlessMode("nonexistent_file.txt"));
    assertTrue(output.contains("Error reading file:"));
  }
}