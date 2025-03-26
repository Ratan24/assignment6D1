package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RecurringEventGenerator implements IRecurringEventGenerator {
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  public static char dayToChar(DayOfWeek day) {
    switch (day) {
      case MONDAY:    return 'M';
      case TUESDAY:   return 'T';
      case WEDNESDAY: return 'W';
      case THURSDAY:  return 'R';
      case FRIDAY:    return 'F';
      case SATURDAY:  return 'S';
      case SUNDAY:    return 'U';
      default:
        throw new IllegalArgumentException("Unknown day: " + day);
    }
  }

  public static boolean isRecurringDay(DayOfWeek day, String weekdaysPattern) {
    char dayCode = dayToChar(day);
    return weekdaysPattern.toUpperCase().indexOf(dayCode) >= 0;
  }

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
      if (repeatTokens.length < 4
              || !repeatTokens[1].equalsIgnoreCase("for")
              || !repeatTokens[3].equalsIgnoreCase("times")) {
        throw new Exception("Invalid recurring event format (for N times).");
      }
      int occurrencesCount = Integer.parseInt(repeatTokens[2]);
      LocalDateTime currentDay = startDateTime;
      while (generatedEvents.size() < occurrencesCount) {
        if (isRecurringDay(currentDay.getDayOfWeek(), weekdayString)) {
          addOccurrence(generatedEvents, eventName, currentDay, startDateTime, endDateTime, isAllDay);
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
          addOccurrence(generatedEvents, eventName, currentDay, startDateTime, endDateTime, isAllDay);
        }
        currentDay = currentDay.plusDays(1);
      }
    } else {
      throw new Exception("Invalid recurring event format.");
    }
    return generatedEvents;
  }

  private static void addOccurrence(List<CalendarEvent> eventList, String eventName,
                                    LocalDateTime current, LocalDateTime originalStart,
                                    LocalDateTime originalEnd, boolean allDay) {
    LocalDate currentDate = current.toLocalDate();
    LocalDateTime occStart = LocalDateTime.of(currentDate, originalStart.toLocalTime());
    LocalDateTime occEnd = LocalDateTime.of(currentDate, originalEnd.toLocalTime());
    eventList.add(new CalendarEvent(eventName, occStart, occEnd, allDay));
  }

  @Override
  public List<ICalendarEvent> generateRecurringEventsInterface(
          String eventName,
          LocalDateTime startDateTime,
          LocalDateTime endDateTime,
          String repeatPart,
          boolean isAllDay) throws Exception {

    List<CalendarEvent> resultFromStatic = generateRecurringEvents(eventName, startDateTime, endDateTime, repeatPart, isAllDay);
    List<ICalendarEvent> interfaceList = new ArrayList<>();
    for (CalendarEvent oneEvent : resultFromStatic) {
      interfaceList.add(oneEvent);
    }
    return interfaceList;
  }
}
