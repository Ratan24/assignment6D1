package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class that handles generation of recurring events
 * and also provides an instance-based method to create ICalendarEvent lists.
 */
public class RecurringEventGenerator implements IRecurringEventGenerator {

  // Formatters for date parsing.
  private static final DateTimeFormatter dateFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter dateTimeFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  /**
   * Converts a DayOfWeek enum to its corresponding single-character code.
   * For example, MONDAY -> 'M', TUESDAY -> 'T', etc.
   */
  public static char dayToChar(DayOfWeek day) {
    switch (day) {
      case MONDAY:    return 'M';
      case TUESDAY:   return 'T';
      case WEDNESDAY: return 'W';
      case THURSDAY:  return 'R'; // 'R' is often used for Thursday
      case FRIDAY:    return 'F';
      case SATURDAY:  return 'S';
      case SUNDAY:    return 'U';
      default:
        throw new IllegalArgumentException("Unknown day: " + day);
    }
  }

  /**
   * Checks whether the given day is included in the weekday pattern string.
   * For example, "MWF" means Monday, Wednesday, Friday are allowed.
   */
  public static boolean isRecurringDay(DayOfWeek day, String weekdaysPattern) {
    char dayCode = dayToChar(day);
    // Check if the uppercase pattern contains that single-character code.
    return weekdaysPattern.toUpperCase().indexOf(dayCode) >= 0;
  }

  /**
   * Original static method (kept for backward compatibility)
   * that generates a list of CalendarEvent objects based on repeatPart instructions.
   */
  public static List<CalendarEvent> generateRecurringEvents(
          String eventName,
          LocalDateTime startDateTime,
          LocalDateTime endDateTime,
          String repeatPart,
          boolean isAllDay
  ) throws Exception {

    String trimmedRepeatPart = repeatPart.trim();
    if (trimmedRepeatPart.isEmpty()) {
      throw new Exception("Invalid recurring event format.");
    }

    List<CalendarEvent> generatedEvents = new ArrayList<>();
    String[] repeatTokens = repeatPart.split(" ");
    String weekdayString = repeatTokens[0].trim().toUpperCase();

    // Check if the repeatPart includes " for N times"
    if (repeatPart.toLowerCase().contains(" for ")) {
      // Example: "MWF for 3 times"
      if (repeatTokens.length < 4
              || !repeatTokens[1].equalsIgnoreCase("for")
              || !repeatTokens[3].equalsIgnoreCase("times")) {
        throw new Exception("Invalid recurring event format (for N times).");
      }
      int occurrencesCount = Integer.parseInt(repeatTokens[2]);
      LocalDateTime currentDay = startDateTime;

      // Keep adding events until we reach the requested count
      while (generatedEvents.size() < occurrencesCount) {
        if (isRecurringDay(currentDay.getDayOfWeek(), weekdayString)) {
          addOccurrence(generatedEvents, eventName, currentDay,
                  startDateTime, endDateTime, isAllDay);
        }
        currentDay = currentDay.plusDays(1);
      }
    }
    // Otherwise, check if it includes " until <dateTime>"
    else if (repeatPart.toLowerCase().contains(" until ")) {
      // Example: "MWF until 2025-03-11T00:00"
      int untilIndex = repeatPart.toLowerCase().indexOf("until");
      String untilPart = repeatPart.substring(untilIndex + "until".length()).trim();

      LocalDateTime boundaryDateTime;
      if (isAllDay) {
        // If there's a 'T' in the date/time string, remove the time portion
        // so that we handle all-day events by date only.
        if (untilPart.contains("T")) {
          untilPart = untilPart.substring(0, untilPart.indexOf("T"));
        }
        LocalDate boundaryDate = LocalDate.parse(untilPart, dateFormatter);
        boundaryDateTime = boundaryDate.plusDays(1).atStartOfDay();
      } else {
        boundaryDateTime = LocalDateTime.parse(untilPart, dateTimeFormatter);
      }

      LocalDateTime currentDay = startDateTime;
      // Keep iterating day by day until we surpass boundaryDateTime
      while (!currentDay.isAfter(boundaryDateTime.minusSeconds(1))) {
        if (isRecurringDay(currentDay.getDayOfWeek(), weekdayString)) {
          addOccurrence(generatedEvents, eventName, currentDay,
                  startDateTime, endDateTime, isAllDay);
        }
        currentDay = currentDay.plusDays(1);
      }
    }
    else {
      // If neither "for" nor "until" is present, assume invalid repeat instructions
      throw new Exception("Invalid recurring event format.");
    }

    return generatedEvents;
  }

  /**
   * Helper method that constructs one occurrence of the event on the specified day.
   */
  private static void addOccurrence(
          List<CalendarEvent> eventList,
          String eventName,
          LocalDateTime current,
          LocalDateTime originalStart,
          LocalDateTime originalEnd,
          boolean allDay
  ) {
    LocalDate currentDate = current.toLocalDate();

    // The new occurrence will have the same start/end times, but the date is replaced by currentDate.
    LocalDateTime occStart = LocalDateTime.of(currentDate, originalStart.toLocalTime());
    LocalDateTime occEnd   = LocalDateTime.of(currentDate, originalEnd.toLocalTime());

    eventList.add(new CalendarEvent(eventName, occStart, occEnd, allDay));
  }

  /**
   * Instance-level method for the IRecurringEventGenerator interface.
   * Delegates to the static generateRecurringEvents, then upcasts results to ICalendarEvent.
   */
  @Override
  public List<ICalendarEvent> generateRecurringEventsInterface(
          String eventName,
          LocalDateTime startDateTime,
          LocalDateTime endDateTime,
          String repeatPart,
          boolean isAllDay
  ) throws Exception {

    // Call the original static method for logic
    List<CalendarEvent> resultFromStatic = RecurringEventGenerator.generateRecurringEvents(
            eventName, startDateTime, endDateTime, repeatPart, isAllDay
    );

    // Convert CalendarEvent -> ICalendarEvent
    List<ICalendarEvent> interfaceList = new ArrayList<>();
    for (CalendarEvent oneEvent : resultFromStatic) {
      interfaceList.add(oneEvent);
    }
    return interfaceList;
  }
}
