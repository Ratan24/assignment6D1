package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates recurring calendar events based on specified patterns. Supports "for N times" and
 * "until date" recurrence patterns.
 */
public class RecurringEventGenerator implements IRecurringEventGenerator {
 
   private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
   // Explicitly set Locale just in case
   private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
       "yyyy-MM-dd'T'HH:mm", java.util.Locale.ENGLISH);
 
  /**
   * Converts a day of the week to a single character code.
   *
   * @param day The day of the week
   * @return A character code representing the day (M, T, W, R, F, S, U)
   * @throws IllegalArgumentException If the day is invalid
   */
  public static char dayToChar(DayOfWeek day) {
    switch (day) {
      case MONDAY:
        return 'M';
      case TUESDAY:
        return 'T';
      case WEDNESDAY:
        return 'W';
      case THURSDAY:
        return 'R';
      case FRIDAY:
        return 'F';
      case SATURDAY:
        return 'S';
      case SUNDAY:
        return 'U';
      default:
        throw new IllegalArgumentException("Unknown day: " + day);
    }
  }

  /**
   * Checks if a given day is included in a weekdays pattern string.
   *
   * @param day             The day to check
   * @param weekdaysPattern A string containing day codes (e.g., "MWF" for Monday, Wednesday,
   *                        Friday)
   * @return true if the day is included in the pattern, false otherwise
   */
  public static boolean isRecurringDay(DayOfWeek day, String weekdaysPattern) {
    char dayCode = dayToChar(day);
    return weekdaysPattern.toUpperCase().indexOf(dayCode) >= 0;
  }

  /**
   * Generates a list of recurring calendar events based on the specified pattern.
   *
   * @param eventName     The name of the events
   * @param startDateTime The start date and time of the first event
   * @param endDateTime   The end date and time of the first event
   * @param repeatPart    The recurrence pattern (e.g., "MWF for 10 times" or "TR until
   *                      2023-12-31")
   * @param isAllDay      Whether these are all-day events
   * @return A list of generated calendar events
   * @throws Exception If the recurrence pattern is invalid
   */
  public static List<CalendarEvent> generateRecurringEvents(
      String eventName,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime,
      String repeatPart,
      boolean isAllDay) throws Exception {

    String trimmedRepeatPart = repeatPart.trim();
    if (trimmedRepeatPart.isEmpty()) {
      throw new Exception("Invalid recurring event format.");
    }

    List<CalendarEvent> generatedEvents = new ArrayList<>();
    String[] repeatTokens = trimmedRepeatPart.split(" ");
     String weekdayString = repeatTokens[0].trim().toUpperCase();
 
     if (trimmedRepeatPart.toLowerCase().contains(" for ")) {
       // Check for "<Weekdays> for N times" format - requires exactly 4 parts
       if (repeatTokens.length != 4
           || !repeatTokens[1].equalsIgnoreCase("for")
           || !repeatTokens[3].equalsIgnoreCase("times")) {
        throw new Exception("Invalid recurring event format (for N times).");
      }
      int occurrencesCount = Integer.parseInt(repeatTokens[2]);
      LocalDateTime currentDay = startDateTime;
      while (generatedEvents.size() < occurrencesCount) {
        if (isRecurringDay(currentDay.getDayOfWeek(), weekdayString)) {
          addOccurrence(generatedEvents, eventName, currentDay, startDateTime, endDateTime,
              isAllDay);
        }
        currentDay = currentDay.plusDays(1);
      }
    } else if (trimmedRepeatPart.toLowerCase().contains(" until ")) {
      int untilIndex = trimmedRepeatPart.toLowerCase().indexOf("until");
      String untilPart = trimmedRepeatPart.substring(untilIndex + "until".length()).trim();
      LocalDateTime boundaryDateTime;
      if (isAllDay) {
        if (untilPart.contains("T")) {
          untilPart = untilPart.substring(0, untilPart.indexOf("T"));
        }
        LocalDate boundaryDate = LocalDate.parse(untilPart, dateFormatter);
        boundaryDateTime = boundaryDate.plusDays(1).atStartOfDay();
      } else {
        boundaryDateTime = LocalDateTime.parse(untilPart, dateTimeFormatter);
      }
      LocalDateTime currentDay = startDateTime;
      while (!currentDay.isAfter(boundaryDateTime.minusSeconds(1))) {
        if (isRecurringDay(currentDay.getDayOfWeek(), weekdayString)) {
          addOccurrence(generatedEvents, eventName, currentDay, startDateTime, endDateTime,
              isAllDay);
        }
        currentDay = currentDay.plusDays(1);
      }
    } else {
      throw new Exception("Invalid recurring event format.");
    }
    return generatedEvents;
  }

  /**
   * Adds an occurrence of an event to the event list.
   *
   * @param eventList     The list to add the event to
   * @param eventName     The name of the event
   * @param current       The current date/time being considered
   * @param originalStart The original start time
   * @param originalEnd   The original end time
   * @param allDay        Whether this is an all-day event
   */
  private static void addOccurrence(List<CalendarEvent> eventList, String eventName,
      LocalDateTime current, LocalDateTime originalStart,
      LocalDateTime originalEnd, boolean allDay) {
    LocalDate currentDate = current.toLocalDate();
    LocalDateTime occStart = LocalDateTime.of(currentDate, originalStart.toLocalTime());
    LocalDateTime occEnd = LocalDateTime.of(currentDate, originalEnd.toLocalTime());
    eventList.add(new CalendarEvent(eventName, occStart, occEnd, allDay));
  }

  /**
   * Interface-compatible version of generateRecurringEvents that returns ICalendarEvent objects.
   *
   * @param eventName     The name of the events
   * @param startDateTime The start date and time of the first event
   * @param endDateTime   The end date and time of the first event
   * @param repeatPart    The recurrence pattern (e.g., "MWF for 10 times" or "TR until
   *                      2023-12-31")
   * @param isAllDay      Whether these are all-day events
   * @return A list of generated calendar events as ICalendarEvent objects
   * @throws Exception If the recurrence pattern is invalid
   */
  @Override
  public List<ICalendarEvent> generateRecurringEventsInterface(
      String eventName,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime,
      String repeatPart,
      boolean isAllDay) throws Exception {

    List<CalendarEvent> resultFromStatic = generateRecurringEvents(eventName, startDateTime,
        endDateTime, repeatPart, isAllDay);
    List<ICalendarEvent> interfaceList = new ArrayList<>();
    for (CalendarEvent oneEvent : resultFromStatic) {
      interfaceList.add(oneEvent);
    }
    return interfaceList;
  }
}
