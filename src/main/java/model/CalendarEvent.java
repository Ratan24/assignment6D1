package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A concrete implementation of ICalendarEvent, representing a single event.
 */
public class CalendarEvent implements ICalendarEvent {

  private String internalName;
  private LocalDateTime internalStart;
  private LocalDateTime internalEnd;
  private boolean allDayFlag;
  private String details;
  private String place;
  private boolean publiclyVisible;

  /**
   * Constructs a new calendar event with the specified parameters.
   *
   * @param eventName The name of the event
   * @param start     The start date and time
   * @param end       The end date and time
   * @param isAllDay  Whether this is an all-day event
   */
  public CalendarEvent(String eventName, LocalDateTime start, LocalDateTime end, boolean isAllDay) {
    this.internalName = eventName;
    this.internalStart = start;
    this.internalEnd = end;
    this.allDayFlag = isAllDay;
    this.details = "";
    this.place = "";
    this.publiclyVisible = true;
  }

  /**
   * Determines if this event conflicts with another event.
   *
   * @param other The other event to check for conflicts
   * @return true if there is a conflict, false otherwise
   */
  @Override
  public boolean conflictsWith(ICalendarEvent other) {
    if (this.isAllDay() ^ other.isAllDay()) {
      return false;
    }

    if (this.isAllDay() && other.isAllDay()) {
      return this.getStart().toLocalDate().equals(other.getStart().toLocalDate());
    }

    return this.getStart().isBefore(other.getEnd()) && this.getEnd().isAfter(other.getStart());
  }

  /**
   * Returns a string representation of this event.
   *
   * @return A formatted string with the event details
   */
  @Override
  public String toString() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    String eventDetails;
    if (allDayFlag) {
      eventDetails = String.format("%s (All Day on %s)", internalName, internalStart.toLocalDate());
    } else {
      eventDetails = String.format("%s from %s to %s", internalName,
          internalStart.format(dtf), internalEnd.format(dtf));
    }
    String descDetails = (!details.isEmpty()) ? ", Description: " + details : "";
    String locDetails = (!place.isEmpty()) ? ", Location: " + place : "";
    String privacy = publiclyVisible ? "Public" : "Private";
    return eventDetails + descDetails + locDetails + ", " + privacy;
  }

  /**
   * Gets the name of this event.
   *
   * @return The event name
   */
  @Override
  public String getEventName() {
    return internalName;
  }

  /**
   * Sets the name of this event.
   *
   * @param eventName The new event name
   */
  @Override
  public void setEventName(String eventName) {
    this.internalName = eventName;
  }

  /**
   * Gets the start date and time of this event.
   *
   * @return The start date and time
   */
  @Override
  public LocalDateTime getStart() {
    return internalStart;
  }

  /**
   * Sets the start date and time of this event.
   *
   * @param start The new start date and time
   */
  @Override
  public void setStart(LocalDateTime start) {
    this.internalStart = start;
  }

  /**
   * Gets the end date and time of this event.
   *
   * @return The end date and time
   */
  @Override
  public LocalDateTime getEnd() {
    return internalEnd;
  }

  /**
   * Sets the end date and time of this event.
   *
   * @param end The new end date and time
   */
  @Override
  public void setEnd(LocalDateTime end) {
    this.internalEnd = end;
  }

  /**
   * Checks if this is an all-day event.
   *
   * @return true if this is an all-day event, false otherwise
   */
  @Override
  public boolean isAllDay() {
    return allDayFlag;
  }

  /**
   * Sets whether this is an all-day event.
   *
   * @param isAllDay true to make this an all-day event, false otherwise
   */
  @Override
  public void setAllDay(boolean isAllDay) {
    this.allDayFlag = isAllDay;
  }

  /**
   * Gets the description of this event.
   *
   * @return The event description
   */
  @Override
  public String getDescription() {
    return details;
  }

  /**
   * Sets the description of this event.
   *
   * @param description The new event description
   */
  @Override
  public void setDescription(String description) {
    this.details = description;
  }

  /**
   * Gets the location of this event.
   *
   * @return The event location
   */
  @Override
  public String getLocation() {
    return place;
  }

  /**
   * Sets the location of this event.
   *
   * @param location The new event location
   */
  @Override
  public void setLocation(String location) {
    this.place = location;
  }

  /**
   * Checks if this event is publicly visible.
   *
   * @return true if this event is public, false if private
   */
  @Override
  public boolean isPublic() {
    return publiclyVisible;
  }

  /**
   * Sets whether this event is publicly visible.
   *
   * @param isPublic true to make this event public, false to make it private
   */
  @Override
  public void setPublic(boolean isPublic) {
    this.publiclyVisible = isPublic;
  }
}