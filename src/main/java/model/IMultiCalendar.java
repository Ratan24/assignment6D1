package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IMultiCalendar {

  void createCalendar(String name, String timezoneStr) throws Exception;

  void editCalendar(String name, String property, String newValue) throws Exception;

  void useCalendar(String name) throws Exception;

  void copyEvent(String eventName, LocalDateTime sourceStart, String targetCalendarName,
                 LocalDateTime targetStart) throws Exception;

  void copyEventsOn(LocalDate sourceDate, String targetCalendarName, LocalDate targetDate)
          throws Exception;

  void copyEventsBetween(LocalDate sourceStart, LocalDate sourceEnd, String targetCalendarName
          , LocalDate targetStart) throws Exception;
}