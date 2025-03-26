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
    // This should trigger the defensive check.
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
    // Create a MultiCalendarManager instance.
    MultiCalendarManager multiCal = new MultiCalendarManager();

    // Create two calendars.
    multiCal.createCalendar("SourceCalendar", "America/New_York");
    multiCal.createCalendar("OfficeCalendar", "America/New_York");

    // Set the current calendar to the source calendar.
    multiCal.useCalendar("SourceCalendar");
    ICalendarManager source = multiCal.getCurrentCalendar();

    // Add three events on different dates in the source calendar.
    // Event 1: May 1, 2024, 09:00 to 10:00, "Event1"
    ICalendarEvent event1 = new CalendarEvent("Event1",
            LocalDateTime.of(2024, 5, 1, 9, 0),
            LocalDateTime.of(2024, 5, 1, 10, 0), false);
    source.addEvent(event1, true);

    // Event 2: May 3, 2024, 09:00 to 10:00, "Event2"
    ICalendarEvent event2 = new CalendarEvent("Event2",
            LocalDateTime.of(2024, 5, 3, 9, 0),
            LocalDateTime.of(2024, 5, 3, 10, 0), false);
    source.addEvent(event2, true);

    // Event 3: May 5, 2024, 09:00 to 10:00, "Event3"
    ICalendarEvent event3 = new CalendarEvent("Event3",
            LocalDateTime.of(2024, 5, 5, 9, 0),
            LocalDateTime.of(2024, 5, 5, 10, 0), false);
    source.addEvent(event3, true);

    // Copy events from SourceCalendar (between May 1 and May 5)
    // to OfficeCalendar starting on May 6, 2024.
    multiCal.copyEventsBetween(
            LocalDate.of(2024, 5, 1),
            LocalDate.of(2024, 5, 5),
            "OfficeCalendar",
            LocalDate.of(2024, 5, 6)
    );

    // Retrieve OfficeCalendar (assuming getCalendar is public for testing).
    ICalendarManager officeCal = multiCal.getCurrentCalendar();
    List<ICalendarEvent> copiedEvents = officeCal.getAllEvents();

    // Expect three events to have been copied.
    assertEquals("There should be 3 copied events", 3, copiedEvents.size());

    // Verify the new start times:
    // For event1 (source date May 1), daysDiff = 0, new start should be May 6, 2024, 09:00.
    // For event2 (source date May 3), daysDiff = 2, new start should be May 8, 2024, 09:00.
    // For event3 (source date May 5), daysDiff = 4, new start should be May 10, 2024, 09:00.
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
