package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import model.CalendarEvent;
import model.CalendarManager;
import model.ICalendarEvent;
import model.ICalendarManager;

/**
 * Consist of all the test cases for Calendar Manager.
 */
public class CalendarManagerTest {

  private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  @Test
  public void testAddEventNoConflict() throws Exception {
    // Instantiate CalendarManager with a default name and timezone.
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    // Create an event with no conflict.
    CalendarEvent event = new CalendarEvent("Meeting",
            LocalDateTime.parse("2024-05-01T10:00", dtf),
            LocalDateTime.parse("2024-05-01T11:00", dtf),
            false);

    // This should succeed without throwing an exception.
    manager.addEvent(event, true);
    assertEquals(1, manager.getAllEvents().size());
  }


  @Test
  public void testNoConflictAdd() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
            , 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, true);
    ICalendarEvent e2 = new CalendarEvent("Lunch", LocalDateTime.of(2025
            , 3, 1, 11, 30),
            LocalDateTime.of(2025, 3, 1, 12, 30)
            , false);
    manager.addEvent(e2, true);
    assertEquals(2, manager.getAllEvents().size());
  }

  @Test(expected = Exception.class)
  public void testConflictAutoDecline() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
            , 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1
                    , 11, 0), false);
    manager.addEvent(e1, true);

    ICalendarEvent e2 = new CalendarEvent("Meeting2", LocalDateTime.of(2025
            , 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1
                    , 11, 30), false);
    manager.addEvent(e2, true);
  }

  @Test(expected = Exception.class)
  public void testAddEventWithConflictAutoDecline() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    // Add a non-conflicting event
    CalendarEvent event1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    // Add a conflicting event. This should throw an Exception because conflicts are now always declined.
    CalendarEvent event2 = new CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, false);
  }

//  @Test(expected = Exception.class)
//  public void testGetEventsOn_BoundaryConditionsConflict() throws Exception {
//    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
//
//    // Add an all-day event covering March 5.
//    ICalendarEvent allDay = new CalendarEvent("Holiday",
//            LocalDate.of(2025, 3, 5).atStartOfDay(),
//            LocalDate.of(2025, 3, 6).atStartOfDay(), true);
//    manager.addEvent(allDay, true);
//
//    // Add a timed event that overlaps March 5.
//    ICalendarEvent timed = new CalendarEvent("LateMeeting",
//            LocalDateTime.of(2025, 3, 5, 23, 0),
//            LocalDateTime.of(2025, 3, 6, 1, 0), false);
//    // This should throw an exception because it conflicts with the all-day event.
//    manager.addEvent(timed, true);
//  }

//  @Test(expected = Exception.class)
//  public void testGetEventsOn_BoundaryConflict() throws Exception {
//    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
//
//    // Create an all-day event that spans March 5.
//    ICalendarEvent e1 = new CalendarEvent("Holiday",
//            LocalDateTime.of(2025, 3, 5, 0, 0),
//            LocalDateTime.of(2025, 3, 6, 0, 0), true);
//    manager.addEvent(e1, true);
//
//    // Create a timed event on the same day (conflicting).
//    ICalendarEvent e2 = new CalendarEvent("LateMeeting",
//            LocalDateTime.of(2025, 3, 5, 23, 0),
//            LocalDateTime.of(2025, 3, 6, 1, 0), false);
//    // Should throw exception.
//    manager.addEvent(e2, true);
//  }

  @Test
  public void testGetEventsInRange() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
            , 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    ICalendarEvent e2 = new CalendarEvent("Lunch", LocalDateTime.of(2025
            , 3, 1, 12, 0),
            LocalDateTime.of(2025, 3, 1, 13, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    List<ICalendarEvent> range = manager.getEventsInRange(LocalDateTime.of(2025
                    , 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 12, 30));
    assertEquals(2, range.size());
  }

  @Test
  public void testIsBusyAt() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
            , 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1
                    , 11, 0), false);
    manager.addEvent(e1, false);
    assertTrue(manager.isBusyAt(LocalDateTime.of(2025, 3, 1
            , 10, 30)));
    assertFalse(manager.isBusyAt(LocalDateTime.of(2025, 3
            , 1, 11, 30)));
  }

  @Test
  public void testEditSingleEvent_Success() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
            , 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    boolean updated = manager.editSingleEvent("description", "Meeting",
            LocalDateTime.of(2025, 3, 1
                    , 10, 0), LocalDateTime.of(2025, 3
                    , 1, 11, 0), "UpdatedDesc");
    assertTrue(updated);
    assertEquals("UpdatedDesc", e1.getDescription());
  }

  @Test
  public void testEditSingleEvent_Failure() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    boolean updated = manager.editSingleEvent("description"
            , "NonExistent", LocalDateTime.now(),
            LocalDateTime.now().plusHours(1), "Test");
    assertFalse(updated);
  }

  @Test
  public void testEditEventsByStart() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Seminar"
            , LocalDateTime.of(2025, 3, 3, 9, 0),
            LocalDateTime.of(2025, 3, 3
                    , 10, 30), false);
    ICalendarEvent e2 = new CalendarEvent("Seminar"
            , LocalDateTime.of(2025, 3, 4, 9, 0),
            LocalDateTime.of(2025, 3
                    , 4, 10, 30), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByStart("description", "Seminar",
            LocalDateTime.of(2025, 3, 4
                    , 0, 0), "Updated");
    assertEquals(1, count);
    assertEquals("Updated", e2.getDescription());
    assertEquals("", e1.getDescription());
  }

  @Test
  public void testEditEventsByName() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Holiday"
            , LocalDateTime.of(2025, 3, 5, 0, 0),
            LocalDateTime.of(2025, 3
                    , 6, 0, 0), true);
    ICalendarEvent e2 = new CalendarEvent("Holiday"
            , LocalDateTime.of(2025, 3, 6, 0, 0),
            LocalDateTime.of(2025, 3
                    , 7, 0, 0), true);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByName("location"
            , "Holiday", "Beach");
    assertEquals(2, count);
    assertEquals("Beach", e1.getLocation());
    assertEquals("Beach", e2.getLocation());
  }

  @Test
  public void testExportToCSV() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting"
            , LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3
                    , 1, 11, 0), false);
    manager.addEvent(e1, false);
    String fileName = "test_export.csv";
    manager.exportToCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    assertTrue(file.length() > 0);
    file.delete();
  }

  @Test
  public void testExportToGoogleCSV() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
            , 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    String fileName = "test_google.csv";
    manager.exportToGoogleCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    assertTrue(file.length() > 0);
    file.delete();
  }

  @Test
  public void testSortingAfterAdd() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Event1",
            LocalDateTime.of(2025, 3, 1, 12, 0),
            LocalDateTime.of(2025, 3, 1, 13, 0), false);
    ICalendarEvent e2 = new CalendarEvent("Event2",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    ICalendarEvent e3 = new CalendarEvent("Event3",
            LocalDateTime.of(2025, 3, 1, 14, 0),
            LocalDateTime.of(2025, 3, 1, 15, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    manager.addEvent(e3, false);
    List<ICalendarEvent> events = manager.getAllEvents();

    assertEquals("Event2", events.get(0).getEventName());
    assertEquals("Event1", events.get(1).getEventName());
    assertEquals("Event3", events.get(2).getEventName());
  }

  @Test
  public void testGetEventsOn_MultiDayBoundary() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Overnight",
            LocalDateTime.of(2025, 3, 1, 23, 0),
            LocalDateTime.of(2025, 3, 2, 1, 0), false);
    manager.addEvent(e1, false);
    List<ICalendarEvent> eventsDay1 = manager.getEventsOn(LocalDate.of(2025, 3
            , 1));
    List<ICalendarEvent> eventsDay2 = manager.getEventsOn(LocalDate.of(2025, 3
            , 2));
    assertTrue("Event should be found on start day", eventsDay1.contains(e1));
    assertTrue("Event should be found on end day", eventsDay2.contains(e1));
  }

  @Test
  public void testGetEventsInRange_Boundaries() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);

    List<ICalendarEvent> range = manager.getEventsInRange(
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0));

    assertTrue("Event should be in range", range.contains(e1));
  }

  @Test
  public void testIsBusyAt_Boundary() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    CalendarEvent e1 = new CalendarEvent("BusyTest",
            LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 10, 0), false);
    manager.addEvent(e1, false);

    assertTrue(manager.isBusyAt(LocalDateTime.of(2025, 3, 1
            , 9, 0)));

    assertFalse(manager.isBusyAt(LocalDateTime.of(2025, 3
            , 1, 10, 0)));
  }

  @Test
  public void testUpdateProperty_ValidAndInvalid() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    CalendarEvent e1 = new CalendarEvent("TestEvent",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);

    try {
      manager.addEvent(e1, false);
    } catch (Exception ex) {
      fail("Unexpected exception while adding event: " + ex.getMessage());
    }


    boolean updated = manager.editSingleEvent("description", "TestEvent"
            , e1.getStart(), e1.getEnd(), "NewDesc");
    assertTrue(updated);
    assertEquals("NewDesc", e1.getDescription());


    boolean result = manager.editSingleEvent("unknown", "TestEvent"
            , e1.getStart(), e1.getEnd(), "X");
    assertFalse(result);
  }

  @Test
  public void testEditEventsByStart_Multiple() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("GroupEdit",
            LocalDateTime.of(2025, 3, 1, 8, 0),
            LocalDateTime.of(2025, 3, 1, 9, 0), false);
    ICalendarEvent e2 = new CalendarEvent("GroupEdit",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByStart("description", "GroupEdit",
            LocalDateTime.of(2025, 3, 1, 9, 0)
            , "Updated");
    assertEquals(1, count);
    assertEquals("", e1.getDescription());
    assertEquals("Updated", e2.getDescription());
  }

  @Test
  public void testEditEventsByName_Multiple() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("SameName",
            LocalDateTime.of(2025, 3, 1, 8, 0),
            LocalDateTime.of(2025, 3, 1, 9, 0), false);
    ICalendarEvent e2 = new CalendarEvent("SameName",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByName("location", "SameName"
            , "Office");
    assertEquals(2, count);
    assertEquals("Office", e1.getLocation());
    assertEquals("Office", e2.getLocation());
  }

  @Test
  public void testGetAllEvents_ReturnsCopy() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("CopyTest",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
    manager.addEvent(e1, false);
    List<ICalendarEvent> copy = manager.getAllEvents();
    copy.clear();

    assertEquals(1, manager.getAllEvents().size());
  }


  @Test
  public void testExportToCSV_Content() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("CSVTest",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    e1.setDescription("Desc");
    e1.setLocation("Loc");
    e1.setPublic(false);
    manager.addEvent(e1, false);
    String fileName = "temp_export.csv";
    manager.exportToCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    String content = new String(Files.readAllBytes(file.toPath()));
    assertTrue(content.contains("EventName,Start,End,AllDay,Description,Location,Public"));
    assertTrue(content.contains("CSVTest"));
    file.delete();
  }

  @Test
  public void testExportToGoogleCSV_Content() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent e1 = new CalendarEvent("GoogleTest",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    e1.setDescription("GDesc");
    e1.setLocation("GLoc");
    e1.setPublic(true);
    manager.addEvent(e1, false);
    String fileName = "temp_export_google.csv";
    manager.exportToGoogleCSV(fileName);
    File file = new File(fileName);
    assertTrue(file.exists());
    String content = new String(Files.readAllBytes(file.toPath()));
    assertTrue(content.contains("Subject,Start Date,Start Time,End Date" +
            ",End Time,All Day Event,Description,Location,Private"));
    assertTrue(content.contains("GoogleTest"));
    file.delete();
  }

  @Test
  public void testExportToCSV_Error() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");


    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    manager.exportToCSV("/invalid_path/test_export.csv");

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue("Should print error message", output.contains("Error exporting CSV:"));
  }

  @Test
  public void testEditEventName() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");


    ICalendarEvent event = new CalendarEvent("OriginalName",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    try {
      manager.addEvent(event, false);
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getMessage());
    }
    boolean updated = manager.editSingleEvent("name", "OriginalName",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0)
            , "NewName");
    assertTrue("Event should be updated", updated);
    assertEquals("NewName", manager.getAllEvents().get(0).getEventName());
  }

  @Test
  public void testIsBusyAt_BoundaryCheck() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("TestEvent",
            LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 10, 0), false);
    try {
      manager.addEvent(event, false);
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getMessage());
    }
    assertTrue(manager.isBusyAt(LocalDateTime.of(2025, 3, 1
            , 9, 0)));

    assertFalse(manager.isBusyAt(LocalDateTime.of(2025, 3, 1
            , 10, 0)));
  }

  @Test(expected = Exception.class)
  public void testAddEventWithConflictRejection() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
    CalendarEvent event1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    // Attempt to add a conflicting event. This should throw an exception.
    CalendarEvent event2 = new CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, true);
  }

//  @Test(expected = Exception.class)
//  public void testGetEventsOn_BoundaryConditions() throws Exception {
//    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
//
//    ICalendarEvent allDay = new CalendarEvent("Holiday",
//            LocalDate.of(2025, 3, 5).atStartOfDay(),
//            LocalDate.of(2025, 3, 6).atStartOfDay(), true);
//    manager.addEvent(allDay, true);
//
//    ICalendarEvent timed = new CalendarEvent("LateMeeting",
//            LocalDateTime.of(2025, 3, 5, 23, 0),
//            LocalDateTime.of(2025, 3, 6, 1, 0), false);
//    // This should throw an exception because the timed event conflicts with the all-day event.
//    manager.addEvent(timed, true);
//  }

  @Test
  public void testGetEventsInRange_Boundary() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    try {
      manager.addEvent(event, false);
    } catch(Exception e) { fail(e.getMessage()); }

    assertTrue(manager.getEventsInRange(
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3
                    , 1, 11, 0)).contains(event));
  }

  @Test
  public void testIsBusyAt_Boundary_isBusyAt() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("BusyEvent",
            LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 10, 0), false);
    try {
      manager.addEvent(event, false);
    } catch(Exception e) { fail(e.getMessage()); }
    assertTrue(manager.isBusyAt(LocalDateTime.of(2025
            , 3, 1, 9, 0)));
    assertFalse(manager.isBusyAt(LocalDateTime.of(2025, 3
            , 1, 10, 0)));
  }

  @Test
  public void testExportToCSV_Output() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("CSVEvent",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    event.setDescription("TestDesc");
    event.setLocation("TestLoc");
    event.setPublic(false);
    manager.addEvent(event, false);

    String fileName = "temp_export.csv";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));

    manager.exportToCSV(fileName);

    System.setOut(originalOut);
    String output = baos.toString();
    assertTrue(output.contains("Exported to CSV:"));

    File f = new File(fileName);
    assertTrue("CSV file should exist", f.exists());
    String content = new String(Files.readAllBytes(f.toPath()));
    assertTrue(content.contains("CSVEvent"));
    f.delete();
  }

  @Test
  public void testUpdateProperty_AllProperties() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("TestEvent",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    try {
      manager.addEvent(event, false);
    } catch(Exception e) { fail(e.getMessage()); }

    boolean updated = manager.editSingleEvent("name", "TestEvent",
            event.getStart(), event.getEnd(), "NewName");
    assertTrue(updated);
    assertEquals("NewName", manager.getAllEvents().get(0).getEventName());

    updated = manager.editSingleEvent("description", "NewName",
            event.getStart(), event.getEnd(), "NewDesc");
    assertTrue(updated);
    assertEquals("NewDesc", manager.getAllEvents().get(0).getDescription());

    updated = manager.editSingleEvent("location", "NewName",
            event.getStart(), event.getEnd(), "NewLoc");
    assertTrue(updated);
    assertEquals("NewLoc", manager.getAllEvents().get(0).getLocation());

    updated = manager.editSingleEvent("public", "NewName",
            event.getStart(), event.getEnd(), "true");
    assertTrue(updated);
    assertTrue(manager.getAllEvents().get(0).isPublic());
  }

  @Test
  public void testGetAllEventsReturnsCopy() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("CopyTest",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
    manager.addEvent(event, false);
    int originalSize = manager.getAllEvents().size();

    manager.getAllEvents().clear();

    assertEquals(originalSize, manager.getAllEvents().size());
  }


  @Test
  public void testUpdateAllProperties() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("TestEvent",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    try {
      manager.addEvent(event, false);
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getMessage());
    }

    boolean updated = manager.editSingleEvent("name", "TestEvent"
            , event.getStart(), event.getEnd(), "NewName");
    assertTrue("Event name should be updated", updated);
    assertEquals("NewName", manager.getAllEvents().get(0).getEventName());


    updated = manager.editSingleEvent("description", "NewName"
            , event.getStart(), event.getEnd(), "NewDescription");
    assertTrue("Event description should be updated", updated);
    assertEquals("NewDescription", manager.getAllEvents().get(0).getDescription());


    updated = manager.editSingleEvent("location", "NewName"
            , event.getStart(), event.getEnd(), "NewLocation");
    assertTrue("Event location should be updated", updated);
    assertEquals("NewLocation", manager.getAllEvents().get(0).getLocation());


    updated = manager.editSingleEvent("public", "NewName"
            , event.getStart(), event.getEnd(), "false");
    assertTrue("Event public flag should be updated", updated);
    assertFalse(manager.getAllEvents().get(0).isPublic());

    updated = manager.editSingleEvent("public", "NewName"
            , event.getStart(), event.getEnd(), "true");
    assertTrue("Event public flag should be updated", updated);
    assertTrue(manager.getAllEvents().get(0).isPublic());
  }

  @Test
  public void testUpdateInvalidProperty() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("TestEvent",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    try {
      manager.addEvent(event, false);
    } catch(Exception e){
      fail("Unexpected exception: " + e.getMessage());
    }

    boolean updated = manager.editSingleEvent("invalid"
            , "TestEvent", event.getStart(), event.getEnd(), "value");
    assertFalse("Editing an invalid property should return false", updated);
  }

//  @Test(expected = Exception.class)
//  public void testGetEventsOn_BoundaryReplicate() throws Exception {
//    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");
//
//    ICalendarEvent allDay = new CalendarEvent("Holiday",
//            LocalDate.of(2025, 3, 5).atStartOfDay(),
//            LocalDate.of(2025, 3, 6).atStartOfDay(), true);
//    manager.addEvent(allDay, true); // Using true to ensure conflicts are auto-declined
//
//    // Attempt to add a conflicting timed event.
//    ICalendarEvent timed = new CalendarEvent("LateMeeting",
//            LocalDateTime.of(2025, 3, 5, 23, 0),
//            LocalDateTime.of(2025, 3, 6, 1, 0), false);
//    // This should throw an exception due to conflict.
//    manager.addEvent(timed, true);
//  }

  @Test
  public void testIsBusyAt_BoundaryReplicate() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("BusyTest",
            LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 10, 0), false);
    try {
      manager.addEvent(event, false);
    } catch(Exception e){
      fail(e.getMessage());
    }
    assertTrue("Should be busy at start time"
            , manager.isBusyAt(LocalDateTime.of(2025
                    , 3, 1, 9, 0)));
    assertFalse("Should not be busy at end time"
            , manager.isBusyAt(LocalDateTime.of(2025
                    , 3, 1, 10, 0)));
  }

  @Test
  public void testExportToCSV_Success() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");

    ICalendarEvent event = new CalendarEvent("CSVTest",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
    event.setDescription("Desc");
    event.setLocation("Loc");
    event.setPublic(false);
    manager.addEvent(event, false);

    String fileName = "temp_export.csv";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream origOut = System.out;
    System.setOut(new PrintStream(baos));

    manager.exportToCSV(fileName);

    System.setOut(origOut);
    String output = baos.toString();
    assertTrue("Should indicate export success", output.contains("Exported to CSV:"));

    File file = new File(fileName);
    assertTrue("CSV file should exist", file.exists());
    String content = new String(Files.readAllBytes(file.toPath()));
    assertTrue("CSV content should contain event name", content.contains("CSVTest"));
    file.delete();
  }

  @Test
  public void testExportToCSV_ErrorReplicate() throws Exception {
    ICalendarManager manager = new CalendarManager("DefaultCalendar", "America/New_York");


    String invalidFileName = "/invalid_path/export.csv";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream origOut = System.out;
    System.setOut(new PrintStream(baos));

    manager.exportToCSV(invalidFileName);

    System.setOut(origOut);
    String output = baos.toString();
    assertTrue("Should indicate export error", output.contains("Error exporting CSV:"));
  }

  @Test
  public void testGettersAndSetters() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 30, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 30, 11, 0);
    CalendarEvent event = new CalendarEvent("Meeting", start, end, false);

    event.setDescription("Team meeting");
    event.setLocation("Conference Room A");
    event.setPublic(false);

    assertEquals("Meeting", event.getEventName());
    assertEquals(start, event.getStart());
    assertEquals(end, event.getEnd());
    assertEquals("Team meeting", event.getDescription());
    assertEquals("Conference Room A", event.getLocation());
    assertFalse(event.isPublic());
  }

  @Test
  public void testToString_TimedEvent() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 30, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 30, 11, 0);
    CalendarEvent event = new CalendarEvent("Meeting", start, end, false);
    // No description/location, public by default
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    String expected = "Meeting from " + start.format(dtf) + " to " + end.format(dtf) + ", Public";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_AllDayEvent() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 30, 0, 0);
    CalendarEvent event = new CalendarEvent("Holiday", start, start.plusDays(1), true);
    String expected = "Holiday (All Day on " + start.toLocalDate() + "), Public";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testConflictsWith_NonOverlapping() {
    CalendarEvent event1 = new CalendarEvent("Event1", LocalDateTime.of(2025, 3, 30, 9, 0),
            LocalDateTime.of(2025, 3, 30, 10, 0), false);
    CalendarEvent event2 = new CalendarEvent("Event2", LocalDateTime.of(2025, 3, 30, 10, 0),
            LocalDateTime.of(2025, 3, 30, 11, 0), false);
    assertFalse(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_Overlapping() {
    CalendarEvent event1 = new CalendarEvent("Event1", LocalDateTime.of(2025, 3, 30, 9, 0),
            LocalDateTime.of(2025, 3, 30, 11, 0), false);
    CalendarEvent event2 = new CalendarEvent("Event2", LocalDateTime.of(2025, 3, 30, 10, 0),
            LocalDateTime.of(2025, 3, 30, 12, 0), false);
    assertTrue(event1.conflictsWith(event2));
  }

  @Test
  public void testConflictsWith_AllDay() {
    CalendarEvent event1 = new CalendarEvent("Holiday", LocalDateTime.of(2025, 3, 30, 0, 0),
            LocalDateTime.of(2025, 3, 31, 0, 0), true);
    CalendarEvent event2 = new CalendarEvent("Another Holiday", LocalDateTime.of(2025, 3, 30, 0, 0),
            LocalDateTime.of(2025, 3, 31, 0, 0), true);
    assertTrue(event1.conflictsWith(event2));
  }

}
