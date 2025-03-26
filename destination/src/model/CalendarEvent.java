package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A concrete implementation of ICalendarEvent, representing a single event
 * that can be either timed or all-day, plus optional description/location/public flags.
 */
public class CalendarEvent implements ICalendarEvent {

  // Internally renaming fields for clarity
  private String internalName;         // originally "eventName"
  private LocalDateTime internalStart; // originally "start"
  private LocalDateTime internalEnd;   // originally "end"
  private boolean allDayFlag;          // originally "isAllDay"
  private String details;              // originally "description"
  private String place;                // originally "location"
  private boolean publiclyVisible;     // originally "isPublic"

  /**
   * Constructs a CalendarEvent with the given parameters.
   *
   * @param eventName   the string name of the event
   * @param start       the start date/time
   * @param end         the end date/time
   * @param isAllDay    true if it's an all-day event
   */
  public CalendarEvent(String eventName, LocalDateTime start,
                       LocalDateTime end, boolean isAllDay) {
    this.internalName    = eventName;
    this.internalStart   = start;
    this.internalEnd     = end;
    this.allDayFlag      = isAllDay;
    this.details         = "";
    this.place           = "";
    this.publiclyVisible = true;
  }

  /**
   * Checks if this event conflicts with another event (via the ICalendarEvent interface).
   * We say there's a conflict if this event's [start, end) overlaps the other's time range.
   */
  @Override
  public boolean conflictsWith(ICalendarEvent other) {
    // Compare times via the interface getters on 'other'
    return this.internalStart.isBefore(other.getEnd())
            && this.internalEnd.isAfter(other.getStart());
  }

  /**
   * Produces a string describing this event, including:
   * - Name & time range (or all-day)
   * - Description
   * - Location
   * - Privacy status
   */
  @Override
  public String toString() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    String eventDetails;

    if (allDayFlag) {
      eventDetails = String.format("%s (All Day on %s)", internalName, internalStart.toLocalDate());
    } else {
      eventDetails = String.format(
              "%s from %s to %s",
              internalName,
              internalStart.format(dtf),
              internalEnd.format(dtf)
      );
    }

    String descDetails = (!details.isEmpty()) ? ", Description: " + details : "";
    String locDetails  = (!place.isEmpty())   ? ", Location: "   + place   : "";
    String privacy     = publiclyVisible ? "Public" : "Private";

    return eventDetails + descDetails + locDetails + ", " + privacy;
  }

  // ------------------------
  // ICalendarEvent methods:
  // ------------------------

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
