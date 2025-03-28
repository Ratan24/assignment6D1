package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for managing a calendar, including adding, retrieving, and modifying events.
 */
public interface ICalendarManager {

  /**
   * Adds a new event to the calendar.
   *
   * @param newEvent    The event to add
   * @param autoDecline Whether to automatically decline conflicting events
   * @throws Exception If there is a conflict with an existing event
   */
  void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws Exception;

  /**
   * Gets all events scheduled on the specified date.
   *
   * @param date The date to check
   * @return A list of events on the specified date
   */
  List<ICalendarEvent> getEventsOn(LocalDate date);

  /**
   * Gets all events that occur within the specified time range.
   *
   * @param startRange The start of the time range
   * @param endRange   The end of the time range
   * @return A list of events within the specified range
   */
  List<ICalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange);

  /**
   * Exports all events to a CSV file.
   *
   * @param fileName The name of the file to export to
   */
  void exportToCSV(String fileName);

  /**
   * Exports all events to a Google Calendar compatible CSV file.
   *
   * @param fileName The name of the file to export to
   */
  void exportToGoogleCSV(String fileName);

  /**
   * Checks if there is any event scheduled at the specified time.
   *
   * @param dateTime The date and time to check
   * @return true if there is an event at the specified time, false otherwise
   */
  boolean isBusyAt(LocalDateTime dateTime);

  /**
   * Edits a single event that matches the specified criteria.
   *
   * @param property  The property to edit
   * @param eventName The name of the event
   * @param start     The start time of the event
   * @param end       The end time of the event
   * @param newValue  The new value for the property
   * @return true if the event was found and updated, false otherwise
   */
  boolean editSingleEvent(String property, String eventName,
      LocalDateTime start, LocalDateTime end, String newValue);

  /**
   * Edits all events with the specified name that start at or after the specified time.
   *
   * @param property  The property to edit
   * @param eventName The name of the events to edit
   * @param start     The start time to filter events
   * @param newValue  The new value for the property
   * @return The number of events that were updated
   */
  int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue);

  /**
   * Edits all events with the specified name.
   *
   * @param property  The property to edit
   * @param eventName The name of the events to edit
   * @param newValue  The new value for the property
   * @return The number of events that were updated
   */
  int editEventsByName(String property, String eventName, String newValue);

  /**
   * Gets all events in this calendar.
   *
   * @return A copy of the list of all events for safety
   */
  List<ICalendarEvent> getAllEvents();

}