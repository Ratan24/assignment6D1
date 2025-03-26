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

  public CalendarEvent(String eventName, LocalDateTime start, LocalDateTime end, boolean isAllDay) {
    this.internalName = eventName;
    this.internalStart = start;
    this.internalEnd = end;
    this.allDayFlag = isAllDay;
    this.details = "";
    this.place = "";
    this.publiclyVisible = true;
  }

  @Override
  public boolean conflictsWith(ICalendarEvent other) {
    // If exactly one event is all-day, do not consider them conflicting.
    if (this.isAllDay() ^ other.isAllDay()) {
      return false;
    }
    // If both are all-day, consider them conflicting if they occur on the same day.
    if (this.isAllDay() && other.isAllDay()) {
      return this.getStart().toLocalDate().equals(other.getStart().toLocalDate());
    }
    // For timed events, check if the intervals overlap.
    return this.getStart().isBefore(other.getEnd()) && this.getEnd().isAfter(other.getStart());
  }

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

  // Getters and setters
  @Override
  public String getEventName() {
    return internalName;
  }

  @Override
  public void setEventName(String eventName) {
    this.internalName = eventName;
  }

  @Override
  public LocalDateTime getStart() {
    return internalStart;
  }

  @Override
  public void setStart(LocalDateTime start) {
    this.internalStart = start;
  }

  @Override
  public LocalDateTime getEnd() {
    return internalEnd;
  }

  @Override
  public void setEnd(LocalDateTime end) {
    this.internalEnd = end;
  }

  @Override
  public boolean isAllDay() {
    return allDayFlag;
  }

  @Override
  public void setAllDay(boolean isAllDay) {
    this.allDayFlag = isAllDay;
  }

  @Override
  public String getDescription() {
    return details;
  }

  @Override
  public void setDescription(String description) {
    this.details = description;
  }

  @Override
  public String getLocation() {
    return place;
  }

  @Override
  public void setLocation(String location) {
    this.place = location;
  }

  @Override
  public boolean isPublic() {
    return publiclyVisible;
  }

  @Override
  public void setPublic(boolean isPublic) {
    this.publiclyVisible = isPublic;
  }
}
