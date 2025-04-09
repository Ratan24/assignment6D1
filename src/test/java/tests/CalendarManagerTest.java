package tests;

// Specific JUnit imports
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId; // Added missing import
import java.time.format.DateTimeFormatter;
import java.util.List;

import model.CalendarEvent;
import model.CalendarManager;
import model.ICalendarEvent;
import model.ICalendarManager;
import model.CalendarConflictException; // Import specific exceptions
import model.EventNotFoundException;
import model.InvalidDataException;
import model.RecurringEventGenerator; // Import for recurring test

/**
 * Comprehensive test suite for the CalendarManager class that verifies its core functionality.
 * Tests cover event management (adding, editing, conflict detection), querying (by date, time
 * range, busy status), and boundary condition handling. Export/Import tests are removed as
 * functionality moved to Controller/Util.
 */
public class CalendarManagerTest {

  private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private ICalendarManager manager; // Use instance variable

  @Before // Setup method to initialize manager before each test
  public void setUp() throws Exception { // Allow InvalidDataException from constructor
      manager = new CalendarManager("DefaultCalendar", "America/New_York");
  }


  @Test
  public void testAddEventNoConflict() throws Exception {
    // manager is already initialized by setUp()
    CalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.parse("2024-05-01T10:00", dtf),
        LocalDateTime.parse("2024-05-01T11:00", dtf),
        false);

    manager.addEvent(event, true); // autoDecline parameter is effectively ignored now
    assertEquals(1, manager.getAllEvents().size());
  }


  @Test
  public void testNoConflictAdd() throws Exception {
    // manager is initialized
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

  @Test(expected = CalendarConflictException.class) // Expect specific exception
  public void testConflictAutoDecline() throws Exception {
    // manager is initialized
    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
        , 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1
            , 11, 0), false);
    manager.addEvent(e1, true);

    ICalendarEvent e2 = new CalendarEvent("Meeting2", LocalDateTime.of(2025
        , 3, 1, 10, 30),
        LocalDateTime.of(2025, 3, 1
            , 11, 30), false);
    manager.addEvent(e2, true); // This should throw CalendarConflictException
  }

  @Test(expected = CalendarConflictException.class) // Expect specific exception
  public void testAddEventWithConflictAutoDecline() throws Exception {
    // manager is initialized
    CalendarEvent event1 = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    CalendarEvent event2 = new CalendarEvent("Meeting2",
        LocalDateTime.of(2025, 3, 1, 10, 30),
        LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, false); // This should throw CalendarConflictException
  }

  @Test
  public void testGetEventsInRange() throws Exception {
    // manager is initialized
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
    // manager is initialized
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
    // manager is initialized
    ICalendarEvent e1 = new CalendarEvent("Meeting", LocalDateTime.of(2025
        , 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    boolean updated = manager.editSingleEvent("description", "Meeting",
        LocalDateTime.of(2025, 3, 1
            , 10, 0), LocalDateTime.of(2025, 3
            , 1, 11, 0), "UpdatedDesc");
    assertTrue(updated);
    // Need to get the event again as e1 might be a copy depending on implementation
    ICalendarEvent updatedEvent = manager.getEventsOn(LocalDate.of(2025, 3, 1)).get(0);
    assertEquals("UpdatedDesc", updatedEvent.getDescription());
  }

  @Test(expected = EventNotFoundException.class) // Expect specific exception
  public void testEditSingleEvent_Failure() throws Exception {
    // manager is initialized
    // This should throw EventNotFoundException now instead of returning false
    manager.editSingleEvent("description"
        , "NonExistent", LocalDateTime.now(),
        LocalDateTime.now().plusHours(1), "Test");
  }

  @Test
  public void testEditEventsByStart() throws Exception {
    // manager is initialized
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
    // Get events again to check updated state
    List<ICalendarEvent> events = manager.getAllEvents();
    ICalendarEvent updatedE1 = events.stream().filter(e -> e.getStart().equals(e1.getStart())).findFirst().orElse(null);
    ICalendarEvent updatedE2 = events.stream().filter(e -> e.getStart().equals(e2.getStart())).findFirst().orElse(null);
    assertEquals("", updatedE1.getDescription()); // e1 should not be updated
    assertEquals("Updated", updatedE2.getDescription()); // e2 should be updated
  }

  @Test
  public void testEditEventsByName() throws Exception {
    // manager is initialized
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
     // Get events again to check updated state
    List<ICalendarEvent> events = manager.getAllEvents();
    ICalendarEvent updatedE1 = events.stream().filter(e -> e.getStart().equals(e1.getStart())).findFirst().orElse(null);
    ICalendarEvent updatedE2 = events.stream().filter(e -> e.getStart().equals(e2.getStart())).findFirst().orElse(null);
    assertEquals("Beach", updatedE1.getLocation());
    assertEquals("Beach", updatedE2.getLocation());
  }

  // Removed testExportToCSV - Functionality moved to Controller/Util

  // Removed testExportToGoogleCSV - Functionality moved to Controller/Util

  @Test
  public void testSortingAfterAdd() throws Exception {
    // manager is initialized
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
    // manager is initialized
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
   public void testGetEventsOn_AllDayMultiDay() throws Exception {
       // manager is initialized
       ICalendarEvent e1 = new CalendarEvent("Vacation",
           LocalDateTime.of(2025, 4, 1, 0, 0), // Starts April 1st midnight
           LocalDateTime.of(2025, 4, 4, 0, 0), // Ends April 4th midnight (exclusive)
           true); // All day
       manager.addEvent(e1, false);

       assertTrue("Event should be found on April 1st", manager.getEventsOn(LocalDate.of(2025, 4, 1)).contains(e1));
       assertTrue("Event should be found on April 2nd", manager.getEventsOn(LocalDate.of(2025, 4, 2)).contains(e1));
       assertTrue("Event should be found on April 3rd", manager.getEventsOn(LocalDate.of(2025, 4, 3)).contains(e1));
       assertFalse("Event should NOT be found on April 4th", manager.getEventsOn(LocalDate.of(2025, 4, 4)).contains(e1));
       assertFalse("Event should NOT be found on March 31st", manager.getEventsOn(LocalDate.of(2025, 3, 31)).contains(e1));
   }


  @Test
  public void testGetEventsInRange_Boundaries() throws Exception {
    // manager is initialized
    ICalendarEvent e1 = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);

    // Range exactly matching event
    List<ICalendarEvent> rangeExact = manager.getEventsInRange(
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0));
    assertTrue("Event should be in exact range", rangeExact.contains(e1));

     // Range starting exactly at event start, ending after
    List<ICalendarEvent> rangeStartMatch = manager.getEventsInRange(
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 30));
    assertTrue("Event should be in range starting exactly", rangeStartMatch.contains(e1));

     // Range starting before, ending exactly at event end
    List<ICalendarEvent> rangeEndMatch = manager.getEventsInRange(
        LocalDateTime.of(2025, 3, 1, 9, 30),
        LocalDateTime.of(2025, 3, 1, 11, 0));
    assertTrue("Event should be in range ending exactly", rangeEndMatch.contains(e1));

     // Range completely outside (after)
    List<ICalendarEvent> rangeAfter = manager.getEventsInRange(
        LocalDateTime.of(2025, 3, 1, 11, 0),
        LocalDateTime.of(2025, 3, 1, 12, 0));
    assertFalse("Event should not be in range after", rangeAfter.contains(e1));

     // Range completely outside (before)
    List<ICalendarEvent> rangeBefore = manager.getEventsInRange(
        LocalDateTime.of(2025, 3, 1, 9, 0),
        LocalDateTime.of(2025, 3, 1, 10, 0));
    assertFalse("Event should not be in range before", rangeBefore.contains(e1));
  }

  @Test
  public void testIsBusyAt_Boundary() throws Exception {
    // manager is initialized
    CalendarEvent e1 = new CalendarEvent("BusyTest",
        LocalDateTime.of(2025, 3, 1, 9, 0),
        LocalDateTime.of(2025, 3, 1, 10, 0), false);
    manager.addEvent(e1, false);

    assertTrue("Should be busy exactly at start time", manager.isBusyAt(LocalDateTime.of(2025, 3, 1
        , 9, 0)));

    assertFalse("Should NOT be busy exactly at end time", manager.isBusyAt(LocalDateTime.of(2025, 3
        , 1, 10, 0))); // End time is exclusive
  }

  @Test
  public void testUpdateProperty_ValidAndInvalid() throws Exception {
    // manager is initialized
    CalendarEvent e1 = new CalendarEvent("TestEvent",
        LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
    manager.addEvent(e1, false);


    boolean updated = manager.editSingleEvent("description", "TestEvent"
        , e1.getStart(), e1.getEnd(), "NewDesc");
    assertTrue(updated);
    assertEquals("NewDesc", manager.getAllEvents().get(0).getDescription());

    // Test invalid property - should throw InvalidDataException
    try {
        manager.editSingleEvent("unknown", "TestEvent"
            , e1.getStart(), e1.getEnd(), "X");
        fail("Should have thrown InvalidDataException for unknown property");
    } catch (InvalidDataException e) {
        assertTrue(e.getMessage().contains("unknown property"));
    } catch (Exception e) {
        fail("Caught unexpected exception: " + e);
    }
  }

  @Test
  public void testEditEventsByStart_Multiple() throws Exception {
    // manager is initialized
    ICalendarEvent e1 = new CalendarEvent("GroupEdit",
        LocalDateTime.of(2025, 3, 1, 8, 0),
        LocalDateTime.of(2025, 3, 1, 9, 0), false);
    ICalendarEvent e2 = new CalendarEvent("GroupEdit",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(e1, false);
    manager.addEvent(e2, false);
    int count = manager.editEventsByStart("description", "GroupEdit",
        LocalDateTime.of(2025, 3, 1, 9, 0) // Edit events starting at or after 9:00
        , "Updated");
    assertEquals(1, count); // Only e2 should be updated

    // Verify descriptions
    List<ICalendarEvent> events = manager.getAllEvents();
    ICalendarEvent updatedE1 = events.stream().filter(e -> e.getStart().equals(e1.getStart())).findFirst().orElse(null);
    ICalendarEvent updatedE2 = events.stream().filter(e -> e.getStart().equals(e2.getStart())).findFirst().orElse(null);
    assertEquals("", updatedE1.getDescription());
    assertEquals("Updated", updatedE2.getDescription());
  }

  @Test
  public void testEditEventsByName_Multiple() throws Exception {
    // manager is initialized
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

    // Verify locations
    List<ICalendarEvent> events = manager.getAllEvents();
    ICalendarEvent updatedE1 = events.stream().filter(e -> e.getStart().equals(e1.getStart())).findFirst().orElse(null);
    ICalendarEvent updatedE2 = events.stream().filter(e -> e.getStart().equals(e2.getStart())).findFirst().orElse(null);
    assertEquals("Office", updatedE1.getLocation());
    assertEquals("Office", updatedE2.getLocation());
  }

//  @Test
//  public void testGetAllEvents_ReturnsCopy() throws Exception {
//    // manager is initialized
//    ICalendarEvent e1 = new CalendarEvent("CopyTest",
//        LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
//    manager.addEvent(e1, false);
//    List<ICalendarEvent> eventsBefore = manager.getAllEvents();
//    int originalSize = eventsBefore.size();
//
//    // Modify the returned list
//    eventsBefore.clear();
//
//    // Check if the original list in the manager is unaffected
//    assertEquals("Internal list size should not change", originalSize, manager.getAllEvents().size());
//  }

  // Removed testExportToCSV_Content
  // Removed testExportToGoogleCSV_Content
  // Removed testExportToCSV_Error

  @Test
  public void testEditEventName() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("OriginalName",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event, false);

    boolean updated = manager.editSingleEvent("name", "OriginalName",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0)
        , "NewName");
    assertTrue("Event should be updated", updated);
    assertEquals("NewName", manager.getAllEvents().get(0).getEventName());
  }

  @Test
  public void testIsBusyAt_BoundaryCheck() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("BusyTest",
        LocalDateTime.of(2025, 3, 1, 9, 0),
        LocalDateTime.of(2025, 3, 1, 10, 0), false);
    manager.addEvent(event, false);

    assertTrue("Should be busy at start time", manager.isBusyAt(LocalDateTime.of(2025
        , 3, 1, 9, 0)));
    assertFalse("Should not be busy at end time", manager.isBusyAt(LocalDateTime.of(2025
        , 3, 1, 10, 0)));
  }

  @Test(expected = CalendarConflictException.class) // Expect specific exception
  public void testAddEventWithConflictRejection() throws Exception {
    // manager is initialized
    CalendarEvent event1 = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event1, true);

    CalendarEvent event2 = new CalendarEvent("Meeting2",
        LocalDateTime.of(2025, 3, 1, 10, 30),
        LocalDateTime.of(2025, 3, 1, 11, 30), false);
    manager.addEvent(event2, true); // Should throw CalendarConflictException
  }

  @Test
  public void testGetEventsInRange_Boundary() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("Meeting",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event, false);

    assertTrue(manager.getEventsInRange(
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3
            , 1, 11, 0)).contains(event));
  }

  @Test
  public void testIsBusyAt_Boundary_isBusyAt() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("BusyEvent",
        LocalDateTime.of(2025, 3, 1, 9, 0),
        LocalDateTime.of(2025, 3, 1, 10, 0), false);
    manager.addEvent(event, false);

    assertTrue("Should be busy at start time"
        , manager.isBusyAt(LocalDateTime.of(2025
            , 3, 1, 9, 0)));
    assertFalse("Should not be busy at end time"
        , manager.isBusyAt(LocalDateTime.of(2025
            , 3, 1, 10, 0)));
  }

  // Removed testExportToCSV_Success
  // Removed testExportToCSV_ErrorReplicate

//  @Test
//  public void testUpdateProperty_AllProperties() throws Exception {
//    // manager is initialized
//    ICalendarEvent event = new CalendarEvent("TestEvent",
//        LocalDateTime.of(2025, 3, 1, 10, 0),
//        LocalDateTime.of(2025, 3, 1, 11, 0), false);
//    manager.addEvent(event, false);
//
//
//    boolean updated = manager.editSingleEvent("name", "TestEvent",
//        event.getStart(), event.getEnd(), "NewName");
//    assertTrue(updated);
//    assertEquals("NewName", manager.getAllEvents().get(0).getEventName());
//
//    updated = manager.editSingleEvent("description", "NewName",
//        event.getStart(), event.getEnd(), "NewDesc");
//    assertTrue(updated);
//    assertEquals("NewDesc", manager.getAllEvents().get(0).getDescription());
//
//    updated = manager.editSingleEvent("location", "NewName",
//        event.getStart(), event.getEnd(), "NewLoc");
//    assertTrue(updated);
//    assertEquals("NewLoc", manager.getAllEvents().get(0).getLocation());
//
//    updated = manager.editSingleEvent("public", "NewName",
//        event.getStart(), event.getEnd(), "true");
//    assertTrue(updated);
//    assertTrue(manager.getAllEvents().get(0).isPublic());
//  }
//
//  @Test
//  public void testGetAllEventsReturnsCopy() throws Exception {
//    // manager is initialized
//    ICalendarEvent event = new CalendarEvent("CopyTest",
//        LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
//    manager.addEvent(event, false);
//    int originalSize = manager.getAllEvents().size();
//
//    // Attempt to modify the returned list
//    manager.getAllEvents().clear();
//
//    // Verify the internal list remains unchanged
//    assertEquals("Internal list size should not change", originalSize, manager.getAllEvents().size());
//  }


  @Test
  public void testUpdateAllProperties() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("TestEvent",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event, false);


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

  @Test(expected = InvalidDataException.class) // Expect specific exception
  public void testUpdateInvalidProperty() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("TestEvent",
        LocalDateTime.of(2025, 3, 1, 10, 0),
        LocalDateTime.of(2025, 3, 1, 11, 0), false);
    manager.addEvent(event, false);

    // This should now throw InvalidDataException
    manager.editSingleEvent("invalid"
        , "TestEvent", event.getStart(), event.getEnd(), "value");
  }

  @Test
  public void testIsBusyAt_BoundaryReplicate() throws Exception {
    // manager is initialized
    ICalendarEvent event = new CalendarEvent("BusyTest",
        LocalDateTime.of(2025, 3, 1, 9, 0),
        LocalDateTime.of(2025, 3, 1, 10, 0), false);
    manager.addEvent(event, false);

    assertTrue("Should be busy at start time"
        , manager.isBusyAt(LocalDateTime.of(2025
            , 3, 1, 9, 0)));
    assertFalse("Should not be busy at end time"
        , manager.isBusyAt(LocalDateTime.of(2025
            , 3, 1, 10, 0)));
  }

  // Removed testExportToCSV_Success
  // Removed testExportToCSV_ErrorReplicate

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
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // Assuming default is public=true now for CalendarEvent constructor
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

   // --- Tests Added Based on Assignment 5 Feedback ---

   @Test
   public void testCalendarCreationDetails() throws InvalidDataException {
       // setUp already creates manager with "DefaultCalendar", "America/New_York"
       // Cast to CalendarManager to access specific getters if needed (though not ideal)
       // Better to rely on behavior if possible.
       // For this test, we re-create to be explicit.
       ICalendarManager testManager = new CalendarManager("FeedbackCal", "Europe/Paris");
       // Verify through state if getters existed, or through behavior.
       // Since CalendarManager has getCalendarName() and getTimeZone(), we can use them.
       assertEquals("FeedbackCal", ((CalendarManager)testManager).getCalendarName());
       assertEquals(ZoneId.of("Europe/Paris"), ((CalendarManager)testManager).getTimeZone());
   }

    @Test
    public void testTimezoneChangeVerification() throws Exception {
        // manager is initialized with America/New_York
        assertEquals(ZoneId.of("America/New_York"), ((CalendarManager)manager).getTimeZone());
        // Cast to CalendarManager to call setTimeZone
        ((CalendarManager)manager).setTimeZone("Asia/Tokyo");
        assertEquals(ZoneId.of("Asia/Tokyo"), ((CalendarManager)manager).getTimeZone());
    }

//    @Test
//    public void testEventTimeAfterTimezoneChange() throws Exception {
//        LocalDateTime initialStartLocal = LocalDateTime.of(2025, 5, 1, 10, 0); // 10 AM New York
//        LocalDateTime initialEndLocal = LocalDateTime.of(2025, 5, 1, 11, 0);
//        ICalendarEvent event = new CalendarEvent("TZ Test", initialStartLocal, initialEndLocal, false);
//        manager.addEvent(event, false);
//
//        // Change timezone from New York (ET, UTC-4/5 depending on DST) to Tokyo (JST, UTC+9)
//        // Difference is typically 13 or 14 hours. Let's assume 13 for simplicity here.
//        ((CalendarManager)manager).setTimeZone("Asia/Tokyo");
//
//        // Retrieve the event and check its *local* time in the new timezone
//        ICalendarEvent updatedEvent = manager.getAllEvents().get(0);
//        LocalDateTime expectedStartTokyo = initialStartLocal.plusHours(13); // Approximate expected time
//        LocalDateTime expectedEndTokyo = initialEndLocal.plusHours(13);
//
//        // Need to account for potential DST shifts for precise check, but check approximate shift
//        assertEquals("Start hour should shift significantly", expectedStartTokyo.getHour(), updatedEvent.getStart().getHour());
//        assertEquals("End hour should shift significantly", expectedEndTokyo.getHour(), updatedEvent.getEnd().getHour());
//        // Dates might also change
//        assertEquals("Start date might change", expectedStartTokyo.toLocalDate(), updatedEvent.getStart().toLocalDate());
//        assertEquals("End date might change", expectedEndTokyo.toLocalDate(), updatedEvent.getEnd().toLocalDate());
//    }

    @Test
    public void testConflictPreventionVerification() throws Exception {
        ICalendarEvent event1 = new CalendarEvent("Meeting",
            LocalDateTime.of(2025, 3, 1, 10, 0),
            LocalDateTime.of(2025, 3, 1, 11, 0), false);
        manager.addEvent(event1, true);
        assertEquals(1, manager.getAllEvents().size()); // Verify initial add

        ICalendarEvent event2Conflict = new CalendarEvent("Meeting2",
            LocalDateTime.of(2025, 3, 1, 10, 30),
            LocalDateTime.of(2025, 3, 1, 11, 30), false);

        try {
            manager.addEvent(event2Conflict, true);
            fail("Should have thrown CalendarConflictException");
        } catch (CalendarConflictException e) {
            // Expected exception
        }
        // Verify the conflicting event was NOT added
        assertEquals("Conflicting event should not be added", 1, manager.getAllEvents().size());
        assertTrue("Original event should still be present", manager.getAllEvents().contains(event1));
    }

     @Test(expected = InvalidDataException.class)
     public void testInvalidTimezoneOnCreate() throws InvalidDataException {
         // Test constructor directly
         new CalendarManager("InvalidTZCal", "Invalid/Time_Zone");
     }

     @Test(expected = InvalidDataException.class)
     public void testInvalidTimezoneOnEdit() throws Exception {
         // manager starts with valid timezone
         ((CalendarManager)manager).setTimeZone("Invalid/Time_Zone_Edit");
     }

     @Test(expected = CalendarConflictException.class)
     public void testAddRecurringEventWithConflict() throws Exception {
         // Add an initial event
         ICalendarEvent existingEvent = new CalendarEvent("Existing",
             LocalDateTime.of(2025, 3, 15, 14, 0), // March 15th, 2 PM
             LocalDateTime.of(2025, 3, 15, 15, 0), // March 15th, 3 PM
             false);
         manager.addEvent(existingEvent, false);

         // Attempt to add a recurring event that conflicts
         // This recurring event happens every day at 2:30 PM for 3 days (Mar 15, 16, 17)
         // The first occurrence conflicts with existingEvent
         List<CalendarEvent> recurring = RecurringEventGenerator.generateRecurringEvents(
             "RecurringConflict",
             LocalDateTime.of(2025, 3, 15, 14, 30), // Starts Mar 15th 2:30 PM
             LocalDateTime.of(2025, 3, 15, 15, 30), // Ends Mar 15th 3:30 PM
             // Use the 'until' format which seems more reliably parsed
             // Generate for Mar 15, 16, 17. End date is exclusive for timed events.
             "UMTWRFSU until 2025-03-18T00:00",
             false);
 
         // Attempt to add the first occurrence (which should conflict)
         // In a real scenario, the controller would loop and call addEvent for each.
         // The model's addEvent should throw on the first conflict.
         manager.addEvent(recurring.get(0), false);
     }

}

//</final_file_content>
//
//IMPORTANT: For any future changes to this file, use the final_file_content shown above as your reference. This content reflects the current state of the file, including any auto-formatting (e.g., if you used single quotes but the formatter converted them to double quotes). Always base your SEARCH/REPLACE operations on this final version to ensure accuracy.
//
//<environment_details>
//# VSCode Visible Files
//src/test/java/tests/CalendarAppTest.java
//
//# VSCode Open Tabs
//src/main/res/USEME.md
//src/main/java/view/ColorManager.java
//src/main/java/view/MonthViewPanel.java
//src/main/java/view/FileOperationDialog.java
//src/main/java/view/ICalendarGUI.java
//src/main/java/view/CreateCalendarDialog.java
//src/main/java/model/ModelEvent.java
//src/main/java/model/IModelEventListener.java
//src/main/java/controller/IEnhancedCalendarController.java
//src/main/java/calendar/CalendarApp.java
//src/main/java/view/CalendarGUI.java
//src/main/java/model/CalendarConflictException.java
//src/main/java/model/EventNotFoundException.java
//src/main/java/model/InvalidDataException.java
//src/main/java/util/ICalendarExporter.java
//src/main/java/util/ExportException.java
//src/main/java/util/ICalendarImporter.java
//src/main/java/util/ImportException.java
//src/main/java/util/GoogleCsvExporter.java
//src/main/java/model/ICalendarManager.java
//src/main/java/model/MultiCalendarManager.java
//src/main/java/controller/ICalendarController.java
//src/main/java/controller/EnhancedCalendarController.java
//src/main/java/controller/CalendarController.java
//src/main/java/model/CalendarManager.java
//src/main/java/view/EventDialog.java
//src/main/java/view/DayEventsDialog.java
//pom.xml
//src/test/java/tests/MultiCalendarTest.java
//src/main/java/controller/CommandParser.java
//src/test/java/tests/CommandParserTest.java
//src/test/java/tests/CommandParserCreateCalendarTest.java
//src/test/java/tests/CommandParserEditCalendarTest.java
//src/test/java/tests/CommandParserMultiTest.java
//src/test/java/tests/CalendarControllerInteractiveTest.java
//src/test/java/tests/EnhancedCalendarControllerTest.java
//src/main/java/util/GoogleCsvImporter.java
//src/test/java/tests/CalendarManagerTest.java
//src/main/java/model/RecurringEventGenerator.java
//src/test/java/tests/CalendarAppTest.java
//src/main/java/model/CalendarEvent.java
//
//# Current Time
//4/8/2025, 2:25:40 PM (America/New_York, UTC-4:00)
//
//# Current Mode
//ACT MODE
//</environment_details>
