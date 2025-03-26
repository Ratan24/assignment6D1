package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public interface ICalendarManager {

  void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws Exception;

  List<ICalendarEvent> getEventsOn(LocalDate date);

  List<ICalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange);

  void exportToCSV(String fileName);

  void exportToGoogleCSV(String fileName);

  boolean isBusyAt(LocalDateTime dateTime);

  boolean editSingleEvent(String property, String eventName,
                          LocalDateTime start, LocalDateTime end, String newValue);

  int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue);

  int editEventsByName(String property, String eventName, String newValue);

  List<ICalendarEvent> getAllEvents();  // Return a copy of the list for safety


//  void accept(CommandParserVisitor visitor, String command) throws Exception;
}
