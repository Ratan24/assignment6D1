package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interface for managing multiple calendars, including calendar creation, selection, and
 * cross-calendar event operations.
 */
public interface IMultiCalendar {

  /**
   * Creates a new calendar with the specified name and timezone.
   *
   * @param name        The name of the new calendar
   * @param timezoneStr The timezone string (e.g., "America/New_York")
   * @throws Exception If the calendar already exists or timezone format is invalid
   */
  void createCalendar(String name, String timezoneStr) throws Exception;

  /**
   * Edits a property of an existing calendar.
   *
   * @param name     The name of the calendar to edit
   * @param property The property to edit (e.g., "name", "timezone")
   * @param newValue The new value for the property
   * @throws Exception If the calendar doesn't exist or the property is invalid
   */
  void editCalendar(String name, String property, String newValue) throws Exception;

  /**
   * Sets the specified calendar as the currently active calendar.
   *
   * @param name The name of the calendar to use
   * @throws Exception If the calendar doesn't exist
   */
  void useCalendar(String name) throws Exception;

  /**
   * Copies an event from the current calendar to another calendar, potentially with a different
   * start time.
   *
   * @param eventName          The name of the event to copy
   * @param sourceStart        The start time of the event in the source calendar
   * @param targetCalendarName The name of the target calendar
   * @param targetStart        The new start time for the event in the target calendar
   * @throws Exception If the event doesn't exist or the target calendar doesn't exist
   */
  void copyEvent(String eventName, LocalDateTime sourceStart, String targetCalendarName,
      LocalDateTime targetStart) throws Exception;

  /**
   * Copies all events on a specific date from the current calendar to another calendar.
   *
   * @param sourceDate         The date from which to copy events
   * @param targetCalendarName The name of the target calendar
   * @param targetDate         The target date where events should be copied to
   * @throws Exception If the target calendar doesn't exist
   */
  void copyEventsOn(LocalDate sourceDate, String targetCalendarName, LocalDate targetDate)
      throws Exception;

  /**
   * Copies all events between two dates from the current calendar to another calendar.
   *
   * @param sourceStart        The start date of the range from which to copy events
   * @param sourceEnd          The end date of the range from which to copy events
   * @param targetCalendarName The name of the target calendar
   * @param targetStart        The start date in the target calendar where events should begin
   * @throws Exception If the target calendar doesn't exist
   */
  void copyEventsBetween(LocalDate sourceStart, LocalDate sourceEnd,
      String targetCalendarName, LocalDate targetStart) throws Exception;
}