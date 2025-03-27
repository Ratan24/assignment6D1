package model;

import java.time.LocalDateTime;

/**
 * Interface representing a calendar event with basic event properties and conflict detection.
 */
public interface ICalendarEvent {

  /**
   * Gets the name of this event.
   *
   * @return The event name
   */
  String getEventName();

  /**
   * Sets the name of this event.
   *
   * @param eventName The new event name
   */
  void setEventName(String eventName);

  /**
   * Gets the start date and time of this event.
   *
   * @return The start date and time
   */
  LocalDateTime getStart();

  /**
   * Sets the start date and time of this event.
   *
   * @param start The new start date and time
   */
  void setStart(LocalDateTime start);

  /**
   * Gets the end date and time of this event.
   *
   * @return The end date and time
   */
  LocalDateTime getEnd();

  /**
   * Sets the end date and time of this event.
   *
   * @param end The new end date and time
   */
  void setEnd(LocalDateTime end);

  /**
   * Checks if this is an all-day event.
   *
   * @return true if this is an all-day event, false otherwise
   */
  boolean isAllDay();

  /**
   * Sets whether this is an all-day event.
   *
   * @param isAllDay true to make this an all-day event, false otherwise
   */
  void setAllDay(boolean isAllDay);

  /**
   * Gets the description of this event.
   *
   * @return The event description
   */
  String getDescription();

  /**
   * Sets the description of this event.
   *
   * @param description The new event description
   */
  void setDescription(String description);

  /**
   * Gets the location of this event.
   *
   * @return The event location
   */
  String getLocation();

  /**
   * Sets the location of this event.
   *
   * @param location The new event location
   */
  void setLocation(String location);

  /**
   * Checks if this event is publicly visible.
   *
   * @return true if this event is public, false if private
   */
  boolean isPublic();

  /**
   * Sets whether this event is publicly visible.
   *
   * @param isPublic true to make this event public, false to make it private
   */
  void setPublic(boolean isPublic);

  /**
   * Determines if this event conflicts with another event.
   *
   * @param other The other event to check for conflicts
   * @return true if there is a conflict, false otherwise
   */
  boolean conflictsWith(ICalendarEvent other);

}