package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import controller.CommandParser;
import model.CalendarEvent;
import model.CalendarManager;
import model.ICalendarEvent;
import model.ICalendarManager;

/**
 * Tests the CommandParser functionality with various command types, formats, and edge cases to
 * verify correct parsing and execution of calendar management commands.
 */
public class CommandParserTest {

  @Test
  public void testProcessCommand_InvalidCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    try {
      CommandParser.processCommand("nonexistent command", manager);
      fail("Expected Exception for invalid command");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Invalid command"));
    }
  }

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_MissingTo() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Test from 2025-03-01T10:00", manager);
  }

  @Test(expected = Exception.class)
  public void testProcessEditCommand_MissingWith() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("edit event description Test from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
  }

  @Test
  public void testProcessShowStatus() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Test from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue("Should indicate Busy", output.contains("Busy"));
  }

  @Test
  public void testProcessPrintEventsOn() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Test from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    CommandParser.processCommand("print events on 2025-03-01", manager);
    System.setOut(originalOut);
    String output = baos.toString();
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
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    ICalendarEvent ev = events.get(0);
    assertFalse(ev.isAllDay());
    assertEquals("Meeting", ev.getEventName());
  }

  @Test
  public void testProcessCreateEvent_AllDay() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Holiday on 2025-03-05";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    ICalendarEvent ev = events.get(0);
    assertTrue(ev.isAllDay());
    assertEquals("Holiday", ev.getEventName());
  }

  @Test
  public void testProcessCreateEvent_RepeatingTimed() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Workshop from 2025-03-02T09:00 to "
        + "2025-03-02T10:00 repeats MWF for 3 times";
    CommandParser.processCommand(cmd, manager);
    assertEquals(3, manager.getAllEvents().size());
  }

  @Test
  public void testProcessCreateEvent_RepeatingAllDay() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Seminar on 2025-03-03 repeats MWF until 2025-03-10";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertTrue("Should create occurrences", events.size() > 0);
    for (ICalendarEvent ev : events) {
      assertTrue(ev.isAllDay());
    }
  }

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_MissingToKeyword() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Faulty from 2025-03-01T10:00 2025-03-01T11:00";
    CommandParser.processCommand(cmd, manager);
  }

  @Test
  public void testProcessEditCommand_SingularSuccess() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 to "
        + "2025-03-01T11:00 with UpdatedDesc";
    CommandParser.processCommand(editCmd, manager);
    ICalendarEvent ev = manager.getAllEvents().get(0);
    assertEquals("UpdatedDesc", ev.getDescription());
  }

  @Test(expected = Exception.class)
  public void testProcessEditCommand_MissingToForSingular() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 with Updated";
    CommandParser.processCommand(editCmd, manager);
  }

  @Test
  public void testProcessEditCommand_Plural() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Conference from "
        + "2025-03-01T09:00 to 2025-03-01T10:00", manager);
    CommandParser.processCommand("create event Conference from "
        + "2025-03-02T09:00 to 2025-03-02T10:00", manager);
    String editCmd = "edit events location Conference with NewLocation";
    CommandParser.processCommand(editCmd, manager);
    for (ICalendarEvent ev : manager.getAllEvents()) {
      assertEquals("NewLocation", ev.getLocation());
    }
  }

  @Test
  public void testProcessPrintEventsOn_Valid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Meeting from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String output = captureOutput(() -> {
      try {
        CommandParser.processCommand("print events on 2025-03-01", manager);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(output.contains("Meeting"));
  }

  @Test(expected = Exception.class)
  public void testProcessPrintEventsOn_InvalidFormat() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("print events 2025-03-01", manager);
  }

  @Test
  public void testProcessPrintEventsRange_Valid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Meeting from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    CommandParser.processCommand("create event Workshop from "
        + "2025-03-01T12:00 to 2025-03-01T13:00", manager);
    String output = captureOutput(() -> {
      try {
        CommandParser.processCommand("print events from "
            + "2025-03-01T09:00 to 2025-03-01T14:00", manager);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("Workshop"));
  }

  @Test(expected = Exception.class)
  public void testProcessPrintEventsRange_MissingTo() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("print events from "
        + "2025-03-01T09:00 2025-03-01T14:00", manager);
  }

  @Test
  public void testProcessExportCal_Valid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event ExportTest from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String fileName = "test_export_cal.csv";
    CommandParser.processCommand("export cal " + fileName, manager);
    File f = new File(fileName);
    assertTrue(f.exists());
    assertTrue(f.length() > 0);
    f.delete();
  }

  @Test(expected = Exception.class)
  public void testProcessExportCal_Invalid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("export cal", manager);
  }

  @Test
  public void testProcessExportGoogleCSV_Valid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event ExportGoogle from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String fileName = "test_export_google.csv";
    CommandParser.processCommand("export googlecsv " + fileName, manager);
    File f = new File(fileName);
    assertTrue(f.exists());
    assertTrue(f.length() > 0);
    f.delete();
  }

  @Test(expected = Exception.class)
  public void testProcessExportGoogleCSV_Invalid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("export googlecsv", manager);
  }

  @Test
  public void testProcessShowStatus_Valid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event StatusTest from "
        + "2025-03-01T10:00 to 2025-03-01T11:00", manager);
    String output = captureOutput(() -> {
      try {
        CommandParser.processCommand("show status on 2025-03-01T10:30", manager);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertTrue(output.contains("Busy"));
  }

  @Test(expected = Exception.class)
  public void testProcessCreateEvent_AutoDeclineConflict() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd1 = "create event ConflictTest from 2025-03-01T10:00 to "
        + "2025-03-01T11:00 --autodecline";
    CommandParser.processCommand(cmd1, manager);
    String cmd2 = "create event ConflictTest2 from 2025-03-01T10:30 to "
        + "2025-03-01T11:30 --autodecline";
    CommandParser.processCommand(cmd2, manager);
  }

  @Test
  public void testProcessCreateEvent_Time() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    ICalendarEvent ev = events.get(0);
    assertFalse(ev.isAllDay());
    assertEquals("Meeting", ev.getEventName());
  }

  @Test
  public void testProcessCreateEvent_AllDayCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Holiday on 2025-03-05";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    ICalendarEvent ev = events.get(0);
    assertTrue(ev.isAllDay());
  }

  @Test
  public void testProcessCreateEvent_RepeatingTimedCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Workshop from 2025-03-02T09:00 "
        + "to 2025-03-02T10:00 repeats MWF for 3 times";
    CommandParser.processCommand(cmd, manager);
    assertEquals(3, manager.getAllEvents().size());
  }

  @Test
  public void testProcessCreateEvent_RepeatingAllDayCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String cmd = "create event Seminar on 2025-03-03 repeats MWF until 2025-03-10T00:00";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertTrue("Should create multiple occurrences", events.size() > 0);
    for (ICalendarEvent ev : events) {
      assertTrue(ev.isAllDay());
    }
  }

  @Test
  public void testProcessEditCommand_SingularSuccessEditcommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from "
        + "2025-03-01T10:00 to 2025-03-01T11:00 with Quarterly results";
    CommandParser.processCommand(editCmd, manager);
    ICalendarEvent ev = manager.getAllEvents().get(0);
    assertEquals("Quarterly results", ev.getDescription());
  }

  @Test
  public void testProcessEditCommand_PluralCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event "
        + "Conference from 2025-03-01T09:00 to 2025-03-01T10:00", manager);
    CommandParser.processCommand("create event "
        + "Conference from 2025-03-02T09:00 to 2025-03-02T10:00", manager);
    String editCmd = "edit events location Conference from 2025-03-01T00:00 with NewLocation";
    CommandParser.processCommand(editCmd, manager);
    for (ICalendarEvent ev : manager.getAllEvents()) {
      if (ev.getEventName().equals("Conference")) {
        assertEquals("NewLocation", ev.getLocation());
      }
    }
  }

  @Test
  public void testProcessPrintEventsOnCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Meeting "
        + "from 2025-03-01T10:00 to 2025-03-01T11:00", manager);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    CommandParser.processCommand("print events on 2025-03-01", manager);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue(output.contains("Meeting from"));
  }

  @Test
  public void testProcessPrintEventsRange() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event Meeting "
        + "from 2025-03-01T10:00 to 2025-03-01T11:00", manager);
    CommandParser.processCommand("create event Workshop "
        + "from 2025-03-01T12:00 to 2025-03-01T13:00", manager);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    CommandParser.processCommand("print events "
        + "from 2025-03-01T09:00 to 2025-03-01T14:00", manager);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("Workshop"));
  }

  @Test
  public void testProcessExportCalAndGoogleCSV() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event ExportTest "
        + "from 2025-03-01T10:00 to 2025-03-01T11:00", manager);

    String calFile = "temp_export_cal.csv";
    String googleFile = "temp_export_google.csv";

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    CommandParser.processCommand("export cal " + calFile, manager);
    CommandParser.processCommand("export googlecsv " + googleFile, manager);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue(output.contains("Exported to CSV:"));
    assertTrue(output.contains("Exported to Google CSV:"));

    File f1 = new File(calFile);
    File f2 = new File(googleFile);
    assertTrue(f1.exists());
    assertTrue(f2.exists());
    f1.delete();
    f2.delete();
  }

  @Test
  public void testProcessShowStatusCommand() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CommandParser.processCommand("create event StatusTest "
        + "from 2025-03-01T10:00 to 2025-03-01T11:00", manager);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    CommandParser.processCommand("show status on 2025-03-01T10:30", manager);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue(output.contains("Busy"));
  }

  @Test
  public void testProcessCreateEvent_TimedReplicate() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    String cmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    ICalendarEvent ev = events.get(0);
    assertFalse(ev.isAllDay());
    assertEquals("Meeting", ev.getEventName());
  }

  @Test
  public void testProcessCreateEvent_AllDayReplicate() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    String cmd = "create event Holiday on 2025-03-05";
    CommandParser.processCommand(cmd, manager);
    List<ICalendarEvent> events = manager.getAllEvents();
    assertEquals(1, events.size());
    ICalendarEvent ev = events.get(0);
    assertTrue(ev.isAllDay());
  }

  @Test
  public void testProcessEditCommand_SingularSuccessEditcommandReplicate() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    String createCmd = "create event Meeting from 2025-03-01T10:00 to 2025-03-01T11:00";
    CommandParser.processCommand(createCmd, manager);
    String editCmd = "edit event description Meeting from 2025-03-01T10:00 to "
        + "2025-03-01T11:00 with Quarterly results";
    CommandParser.processCommand(editCmd, manager);
    ICalendarEvent ev = manager.getAllEvents().get(0);
    assertEquals("Quarterly results", ev.getDescription());
  }

  // @Test
  // public void testGetUpdateMessage() {
  //   assertEquals("Event updated successfully."
  //       , CommandParser.getUpdateMessage(true));
  //   assertEquals("Event not found or update failed."
  //       , CommandParser.getUpdateMessage(false));
  // }

  @Test
  public void testEditEventSubjectSingular() throws Exception {
    ICalendarManager manager = new CalendarManager("TestCal", "America/New_York");
    String createCmd = "create event office-hours-cs5010-2 from 2024-03-13T10:00 "
        + "to 2024-03-13T12:00";
    CommandParser.processCommand(createCmd, manager);

    String editCmd = "edit event subject office-hours-cs5010-2 from 2024-03-13T10:00 "
        + "to 2024-03-13T12:00 with office-hours-cs5010-second";
    CommandParser.processCommand(editCmd, manager);

    List<?> events = manager.getAllEvents();
    assertEquals(1, events.size());
    CalendarEvent ev = (CalendarEvent) events.get(0);
    assertEquals("office-hours-cs5010-second", ev.getEventName());
  }

  @Test
  public void testEditEventsSubjectPlural() throws Exception {
    ICalendarManager manager = new CalendarManager("TestCal", "America/New_York");
    String cmd1 = "create event doctor-appointment from 2024-03-14T07:00 to 2024-03-14T08:00";
    String cmd2 = "create event doctor-appointment from 2024-03-14T09:00 to 2024-03-14T10:00";
    CommandParser.processCommand(cmd1, manager);
    CommandParser.processCommand(cmd2, manager);

    String editCmd = "edit events subject doctor-appointment from 2024-03-14T07:00 "
        + "with annual-physical";
    CommandParser.processCommand(editCmd, manager);

    List<?> events = manager.getAllEvents();
    assertEquals(2, events.size());
    for (Object o : events) {
      CalendarEvent ev = (CalendarEvent) o;
      assertEquals("annual-physical", ev.getEventName());
    }
  }
}