package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import model.CalendarEvent;
import model.ICalendarEvent;
import model.ICalendarManager;
import model.IRecurringEventGenerator;
import model.MultiCalendarManager;
import model.RecurringEventGenerator;

/**
 * Tests for the RecurringEventGenerator class which verifies functionality for generating
 * recurring calendar events based on different patterns and rule types. Tests include day
 * recognition, event generation with specific repetition counts, event generation until
 * specific dates, and cross-calendar event copying capabilities.
 */
public class RecurringEventGeneratorTest {

  @Test
  public void testIsRecurringDay_True() {
    String weekdays = "MWF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.MONDAY, weekdays));
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.WEDNESDAY, weekdays));
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.FRIDAY, weekdays));
  }

  @Test
  public void testIsRecurringDay_False() {
    String weekdays = "MWF";
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.TUESDAY, weekdays));
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.THURSDAY, weekdays));
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SATURDAY, weekdays));
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SUNDAY, weekdays));
  }

  @Test
  public void testGenerateRecurringEvents_ForBranch() throws Exception {
    LocalDateTime start = LocalDate.of(2025, 3, 3).atStartOfDay(); // Monday
    LocalDateTime end = start.plusHours(1); // 1-hour event
    String repeatPart = "MWF for 3 times";
    List<CalendarEvent> occurrences = RecurringEventGenerator
        .generateRecurringEvents("TestEvent", start, end, repeatPart, false);

    assertEquals(3, occurrences.size());
    for (CalendarEvent ev : occurrences) {
      DayOfWeek dow = ev.getStart().getDayOfWeek();
      assertTrue(dow == DayOfWeek.MONDAY || dow == DayOfWeek.WEDNESDAY ||
          dow == DayOfWeek.FRIDAY);
    }
  }

  @Test
  public void testGenerateRecurringEvents_UntilBranch() throws Exception {
    LocalDateTime start = LocalDate.of(2025, 3, 3).atStartOfDay();
    LocalDateTime end = start.plusHours(1);

    String repeatPart = "MWF until 2025-03-11T00:00";
    List<CalendarEvent> occurrences = RecurringEventGenerator
        .generateRecurringEvents("TestEvent", start, end, repeatPart, false);
    assertEquals(4, occurrences.size());

    assertEquals(DayOfWeek.MONDAY, occurrences.get(0).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.WEDNESDAY, occurrences.get(1).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.FRIDAY, occurrences.get(2).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.MONDAY, occurrences.get(3).getStart().getDayOfWeek());
  }

  @Test(expected = Exception.class)
  public void testGenerateRecurringEvents_InvalidFormat() throws Exception {
    LocalDateTime start = LocalDateTime.of(2025, 3, 3, 9, 0);
    LocalDateTime end = start.plusHours(1);
    String repeatPart = "XYZ invalid format";
    RecurringEventGenerator.generateRecurringEvents("TestEvent"
        , start, end, repeatPart, false);
  }

  @Test
  public void testIsRecurringDay_Monday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.MONDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Tuesday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.TUESDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Wednesday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.WEDNESDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Thursday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.THURSDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Friday() {
    String allowed = "MTWRF";
    assertTrue(RecurringEventGenerator.isRecurringDay(DayOfWeek.FRIDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Saturday() {
    String allowed = "MTWRF";
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SATURDAY, allowed));
  }

  @Test
  public void testIsRecurringDay_Sunday() {
    String allowed = "MTWRF";
    assertFalse(RecurringEventGenerator.isRecurringDay(DayOfWeek.SUNDAY, allowed));
  }

  @Test
  public void testDayToCharForAllDays() {
    assertEquals('M', RecurringEventGenerator.dayToChar(DayOfWeek.MONDAY));
    assertEquals('T', RecurringEventGenerator.dayToChar(DayOfWeek.TUESDAY));
    assertEquals('W', RecurringEventGenerator.dayToChar(DayOfWeek.WEDNESDAY));
    assertEquals('R', RecurringEventGenerator.dayToChar(DayOfWeek.THURSDAY));
    assertEquals('F', RecurringEventGenerator.dayToChar(DayOfWeek.FRIDAY));
    assertEquals('S', RecurringEventGenerator.dayToChar(DayOfWeek.SATURDAY));
    assertEquals('U', RecurringEventGenerator.dayToChar(DayOfWeek.SUNDAY));
  }

  @Test
  public void testGenerateRecurringEvents_InvalidRepeatPart() {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 10, 0);
    try {
      RecurringEventGenerator.generateRecurringEvents("TestEvent", start, end,
          " ", false);
      fail("Expected exception for invalid recurring event format.");
    } catch (Exception e) {
      assertEquals("Invalid recurring event format.", e.getMessage());
    }
  }

  @Test(expected = Exception.class)
  public void testGenerateRecurringEvents_EmptyRepeatPart() throws Exception {
    LocalDateTime start = LocalDateTime.of(2025, 3, 1, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 3, 1, 10, 0);
    RecurringEventGenerator.generateRecurringEvents("TestEvent", start,
        end, "   ", false);
  }

  @Test
  public void testGenerateRecurringEventsInterface_ForBranch() throws Exception {
    IRecurringEventGenerator generator = new RecurringEventGenerator();

    LocalDateTime start = LocalDate.of(2025, 3, 3).atStartOfDay();
    LocalDateTime end   = start.plusHours(1);
    String repeatPart   = "MWF for 3 times";

    List<ICalendarEvent> occurrences =
        generator.generateRecurringEventsInterface("TestEvent"
            , start, end, repeatPart, false);

    assertEquals(3, occurrences.size());
  }

  @Test
  public void testCopyEventsBetween() throws Exception {
    MultiCalendarManager multiCal = new MultiCalendarManager();

    multiCal.createCalendar("SourceCalendar", "America/New_York");
    multiCal.createCalendar("OfficeCalendar", "America/New_York");

    multiCal.useCalendar("SourceCalendar");
    ICalendarManager source = multiCal.getCurrentCalendar();

    ICalendarEvent event1 = new CalendarEvent("Event1",
        LocalDateTime.of(2024, 5, 1, 9, 0),
        LocalDateTime.of(2024, 5, 1, 10, 0), false);
    source.addEvent(event1, true);

    ICalendarEvent event2 = new CalendarEvent("Event2",
        LocalDateTime.of(2024, 5, 3, 9, 0),
        LocalDateTime.of(2024, 5, 3, 10, 0), false);
    source.addEvent(event2, true);

    ICalendarEvent event3 = new CalendarEvent("Event3",
        LocalDateTime.of(2024, 5, 5, 9, 0),
        LocalDateTime.of(2024, 5, 5, 10, 0), false);
    source.addEvent(event3, true);

    multiCal.copyEventsBetween(
        LocalDate.of(2024, 5, 1),
        LocalDate.of(2024, 5, 5),
        "OfficeCalendar",
        LocalDate.of(2024, 5, 6)
    );

    ICalendarManager officeCal = multiCal.getCurrentCalendar();
    List<ICalendarEvent> copiedEvents = officeCal.getAllEvents();

    assertEquals("There should be 3 copied events", 3, copiedEvents.size());

    ICalendarEvent copied1 = copiedEvents.stream()
        .filter(e -> e.getEventName().equals("Event1"))
        .findFirst().orElse(null);
    ICalendarEvent copied2 = copiedEvents.stream()
        .filter(e -> e.getEventName().equals("Event2"))
        .findFirst().orElse(null);
    ICalendarEvent copied3 = copiedEvents.stream()
        .filter(e -> e.getEventName().equals("Event3"))
        .findFirst().orElse(null);

    assertNotNull("Copied Event1 should not be null", copied1);
    assertNotNull("Copied Event2 should not be null", copied2);
    assertNotNull("Copied Event3 should not be null", copied3);

    assertEquals(LocalDateTime.of(2024, 5, 6, 9, 0), copied1.getStart());
    assertEquals(LocalDateTime.of(2024, 5, 8, 9, 0), copied2.getStart());
    assertEquals(LocalDateTime.of(2024, 5, 10, 9, 0), copied3.getStart());
  }
}