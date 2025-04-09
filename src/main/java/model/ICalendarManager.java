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
   * @param autoDecline Whether to automatically decline conflicting events (currently ignored, throws exception on conflict)
   * @throws CalendarConflictException If there is a conflict with an existing event
   * @throws NullPointerException If newEvent is null
   */
  void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws CalendarConflictException;

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

  // Removed exportToCSV(String fileName);
  // Removed exportToGoogleCSV(String fileName);

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
   * @return true if the event was found and updated
   * @throws EventNotFoundException if the specified event doesn't exist.
   * @throws InvalidDataException if the property name or new value is invalid.
   * @throws CalendarConflictException if updating start/end time causes a conflict.
   */
  boolean editSingleEvent(String property, String eventName,
      LocalDateTime start, LocalDateTime end, String newValue)
      throws EventNotFoundException, InvalidDataException, CalendarConflictException;

  /**
   * Edits all events with the specified name that start at or after the specified time.
   *
   * @param property  The property to edit
   * @param eventName The name of the events to edit
   * @param start     The start time to filter events
   * @param newValue  The new value for the property
   * @return The number of events that were updated
   * @throws InvalidDataException if the property name or new value is invalid.
   * @throws CalendarConflictException if updating start/end time causes a conflict.
   */
  int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue)
      throws InvalidDataException, CalendarConflictException;

  /**
   * Edits all events with the specified name.
   *
   * @param property  The property to edit
   * @param eventName The name of the events to edit
   * @param newValue  The new value for the property
   * @return The number of events that were updated
   * @throws InvalidDataException if the property name or new value is invalid.
   * @throws CalendarConflictException if updating start/end time causes a conflict.
   */
  int editEventsByName(String property, String label, String updatedVal)
      throws InvalidDataException, CalendarConflictException;

  /**
   * Deletes a single event identified by its properties.
   *
   * @param eventName The name/subject of the event to delete.
   * @param start     The exact start time of the event to delete.
   * @param end       The exact end time of the event to delete.
   * @return true if the event was found and deleted.
   * @throws EventNotFoundException if the specified event doesn't exist.
   */
  boolean deleteEvent(String eventName, LocalDateTime start, LocalDateTime end) throws EventNotFoundException;

  /**
   * Imports events from a Google Calendar compatible CSV file into this calendar.
   * Existing events are preserved. Conflicts are handled according to the calendar's rules (likely rejected).
   *
   * @param filePath The absolute path to the CSV file.
   * @return The number of events successfully imported.
   * @throws Exception If there's an error reading the file or parsing its content.
    */
   // Removed importFromGoogleCSV(String filePath);

   /**
    * Gets all events in this calendar.
   *
   * @return A copy of the list of all events for safety
   */
  List<ICalendarEvent> getAllEvents();

  /**
   * Adds a listener that will be notified of events occurring in this calendar manager.
   *
   * @param listener The listener to add.
   */
  void addModelEventListener(IModelEventListener listener);

  /**
   * Removes a previously added listener.
   *
   * @param listener The listener to remove.
   */
  void removeModelEventListener(IModelEventListener listener);
}
