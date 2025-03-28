package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;

import model.CalendarEvent;
import model.ICalendarEvent;

/**
 * This is a test file for Calendar Event class.
 */
public class CalendarEventTest {

  @Test
  public void testGettersAndSetters() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 11, 0);
    ICalendarEvent event = new CalendarEvent("Initial", start, end, false);

    assertEquals("Initial", event.getEventName());
    assertEquals(start, event.getStart());
    assertEquals(end, event.getEnd());
    assertFalse("Timed event should not be all-day", event.isAllDay());
    assertEquals("", event.getDescription());
    assertEquals("", event.getLocation());
    assertTrue("Default should be public", event.isPublic());

    event.setEventName("Updated");
    LocalDateTime newStart = start.plusHours(1);
    LocalDateTime newEnd = end.plusHours(1);
    event.setStart(newStart);
    event.setEnd(newEnd);
    event.setAllDay(true);
    event.setDescription("Test Description");
    event.setLocation("Test Location");
    event.setPublic(false);

    assertEquals("Updated", event.getEventName());
    assertEquals(newStart, event.getStart());
    assertEquals(newEnd, event.getEnd());
    assertTrue("Event should now be all-day", event.isAllDay());
    assertEquals("Test Description", event.getDescription());
    assertEquals("Test Location", event.getLocation());
    assertFalse("Event should now be private", event.isPublic());
  }

  @Test
  public void testConflictsWith_Overlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 10, 30);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 11, 30);
    ICalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    ICalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);
    assertTrue("Overlapping events should conflict", event1.conflictsWith(event2));
    assertTrue("Overlapping events should conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testConflictsWith_NonOverlapping() {
    LocalDateTime start1 = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 3, 1, 11, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 3, 1, 12, 0);
    ICalendarEvent event1 = new CalendarEvent("Event1", start1, end1, false);
    ICalendarEvent event2 = new CalendarEvent("Event2", start2, end2, false);
    assertFalse("Non-overlapping events should not conflict", event1.conflictsWith(event2));
    assertFalse("Non-overlapping events should not conflict", event2.conflictsWith(event1));
  }

  @Test
  public void testToString_TimedEventWithoutExtras() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 11, 0);
    ICalendarEvent event = new CalendarEvent("Meeting", start, end, false);
    String expected = "Meeting from 2025-03-01 10:00 to 2025-03-01 11:00, Public";
    assertEquals(expected, event.toString());
  }

  @Test
  public void testToString_AllDayWithDescriptionAndLocation() {
    LocalDateTime start = LocalDate.of(2025, 3, 1).atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    ICalendarEvent event = new CalendarEvent("Holiday", start, end, true);
    event.setDescription("Vacation");
    event.setLocation("Beach");
    event.setPublic(false);
    String expected = "Holiday (All Day on 2025-03-01)" +
        ", Description: Vacation, Location: Beach, Private";
    assertEquals(expected, event.toString());
  }
}
