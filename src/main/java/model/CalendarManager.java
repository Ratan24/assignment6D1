package model;

import view.OutputHandler;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a single calendar with a unique name, associated timezone,
 * and a list of events.
 */
public class CalendarManager implements ICalendarManager {
  private final List<ICalendarEvent> events;
  private String calendarName;
  private ZoneId timeZone;

  /**
   * Constructs a new calendar manager with the specified name and timezone.
   *
   * @param calendarName The name of the calendar
   * @param timezoneStr The timezone string (e.g., "America/New_York")
   * @throws Exception If the timezone format is invalid
   */
  public CalendarManager(String calendarName, String timezoneStr) throws Exception {
    this.events = new ArrayList<>();
    this.calendarName = calendarName;
    try {
      this.timeZone = ZoneId.of(timezoneStr);
    } catch (Exception e) {
      throw new Exception("Invalid timezone format: " + timezoneStr);
    }
  }

  /**
   * Sets the timezone for this calendar.
   *
   * @param timezoneStr The timezone string to set
   * @throws Exception If the timezone format is invalid
   */
  public void setTimeZone(String timezoneStr) throws Exception {
    try {
      this.timeZone = ZoneId.of(timezoneStr);
    } catch (Exception e) {
      throw new Exception("Invalid timezone format: " + timezoneStr);
    }
  }

  /**
   * Adds a new event to the calendar.
   *
   * @param newEvent The event to add
   * @param autoDecline Whether to automatically decline conflicting events
   * @throws Exception If there is a conflict with an existing event
   */
  @Override
  public void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws Exception {
    checkAndHandleConflict(newEvent);
    events.add(newEvent);
    events.sort(Comparator.comparing(ICalendarEvent::getStart));
  }

  private void checkAndHandleConflict(ICalendarEvent newEvent) throws Exception {
    for (ICalendarEvent existingEvent : events) {
      if (newEvent.conflictsWith(existingEvent)) {
        throw new Exception("Conflict detected with event: " + existingEvent.getEventName());
      }
    }
  }

  /**
   * Gets all events scheduled on the specified date.
   *
   * @param date The date to check
   * @return A list of events on the specified date
   */
  @Override
  public List<ICalendarEvent> getEventsOn(LocalDate date) {
    List<ICalendarEvent> matchingEvents = new ArrayList<>();
    for (ICalendarEvent event : events) {
      if (event.isAllDay()) {
        if (event.getStart().toLocalDate().equals(date)) {
          matchingEvents.add(event);
        }
      } else {
        LocalDate eventStartDate = event.getStart().toLocalDate();
        LocalDate eventEndDate = event.getEnd().toLocalDate();
        if (!eventStartDate.isAfter(date) && !eventEndDate.isBefore(date)) {
          matchingEvents.add(event);
        }
      }
    }
    return matchingEvents;
  }

  /**
   * Gets all events that occur within the specified time range.
   *
   * @param startRange The start of the time range
   * @param endRange The end of the time range
   * @return A list of events within the specified range
   */
  @Override
  public List<ICalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange) {
    List<ICalendarEvent> matchingEvents = new ArrayList<>();
    for (ICalendarEvent event : events) {
      if (event.getStart().isBefore(endRange) && event.getEnd().isAfter(startRange)) {
        matchingEvents.add(event);
      }
    }
    return matchingEvents;
  }

  /**
   * Exports all events to a CSV file.
   *
   * @param fileName The name of the file to export to
   */
  @Override
  public void exportToCSV(String fileName) {
    try (PrintWriter writer = new PrintWriter(new File(fileName))) {
      StringBuilder sb = new StringBuilder();
      sb.append("EventName,Start,End,AllDay,Description,Location,Public\n");
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      for (ICalendarEvent event : events) {
        sb.append("\"").append(event.getEventName()).append("\",");
        sb.append(event.getStart().format(dtf)).append(",");
        sb.append(event.getEnd().format(dtf)).append(",");
        sb.append(event.isAllDay()).append(",");
        sb.append("\"").append(event.getDescription()).append("\",");
        sb.append("\"").append(event.getLocation()).append("\",");
        sb.append(event.isPublic()).append("\n");
      }
      writer.write(sb.toString());
      OutputHandler.getInstance().println("Exported to CSV: " + new File(fileName).getAbsolutePath());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting CSV: " + e.getMessage());
    }
  }

  /**
   * Exports all events to a Google Calendar compatible CSV file.
   *
   * @param fileName The name of the file to export to
   */
  @Override
  public void exportToGoogleCSV(String fileName) {
    try (PrintWriter writer = new PrintWriter(new File(fileName))) {
      StringBuilder sb = new StringBuilder();
      sb.append("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private\n");
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
      for (ICalendarEvent event : events) {
        sb.append("\"").append(event.getEventName()).append("\",");
        if (event.isAllDay()) {
          sb.append(event.getStart().format(dateFormatter)).append(",,");
          sb.append(event.getStart().format(dateFormatter)).append(",,");
          sb.append("True,");
        } else {
          sb.append(event.getStart().format(dateFormatter)).append(",");
          sb.append(event.getStart().format(timeFormatter)).append(",");
          sb.append(event.getEnd().format(dateFormatter)).append(",");
          sb.append(event.getEnd().format(timeFormatter)).append(",");
          sb.append("False,");
        }
        sb.append("\"").append(event.getDescription()).append("\",");
        sb.append("\"").append(event.getLocation()).append("\",");
        sb.append(event.isPublic() ? "False" : "True");
        sb.append("\n");
      }
      writer.write(sb.toString());
      OutputHandler.getInstance().println("Exported to Google CSV: " + new File(fileName).getAbsolutePath());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting Google CSV: " + e.getMessage());
    }
  }

  /**
   * Checks if there is any event scheduled at the specified time.
   *
   * @param dateTime The date and time to check
   * @return true if there is an event at the specified time, false otherwise
   */
  @Override
  public boolean isBusyAt(LocalDateTime dateTime) {
    for (ICalendarEvent event : events) {
      if (!event.getStart().isAfter(dateTime) && event.getEnd().isAfter(dateTime)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Edits a single event that matches the specified criteria.
   *
   * @param property The property to edit
   * @param eventName The name of the event
   * @param start The start time of the event
   * @param end The end time of the event
   * @param newValue The new value for the property
   * @return true if the event was found and updated, false otherwise
   */
  @Override
  public boolean editSingleEvent(String property, String eventName, LocalDateTime start,
      LocalDateTime end, String newValue) {
    for (ICalendarEvent event : events) {
      if (event.getEventName().equals(eventName)
          && event.getStart().equals(start)
          && event.getEnd().equals(end)) {
        if (updateProperty(event, property, newValue)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Edits all events with the specified name that start at or after the specified time.
   *
   * @param property The property to edit
   * @param eventName The name of the events to edit
   * @param start The start time to filter events
   * @param newValue The new value for the property
   * @return The number of events that were updated
   */
  @Override
  public int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue) {
    int numberUpdated = 0;
    for (ICalendarEvent event : events) {
      if (event.getEventName().equals(eventName)
          && (event.getStart().equals(start) || event.getStart().isAfter(start))) {
        if (updateProperty(event, property, newValue)) {
          numberUpdated++;
        }
      }
    }
    return numberUpdated;
  }

  /**
   * Edits all events with the specified name.
   *
   * @param property The property to edit
   * @param eventName The name of the events to edit
   * @param newValue The new value for the property
   * @return The number of events that were updated
   */
  @Override
  public int editEventsByName(String property, String eventName, String newValue) {
    int numberUpdated = 0;
    for (ICalendarEvent event : events) {
      if (event.getEventName().equals(eventName)) {
        if (updateProperty(event, property, newValue)) {
          numberUpdated++;
        }
      }
    }
    return numberUpdated;
  }

  private boolean updateProperty(ICalendarEvent event, String property, String newValue) {
    switch (property.toLowerCase()) {
      case "name":
      case "subject":
        event.setEventName(newValue);
        break;
      case "description":
        event.setDescription(newValue);
        break;
      case "location":
        event.setLocation(newValue);
        break;
      case "public":
        event.setPublic(Boolean.parseBoolean(newValue));
        break;
      default:
        return false;
    }
    return true;
  }

  /**
   * Gets all events in this calendar.
   *
   * @return A list of all events
   */
  @Override
  public List<ICalendarEvent> getAllEvents() {
    return new ArrayList<>(events);
  }

  /**
   * Gets the name of this calendar.
   *
   * @return The calendar name
   */
  public String getCalendarName() {
    return calendarName;
  }

  /**
   * Sets the name of this calendar.
   *
   * @param newName The new calendar name
   */
  public void setCalendarName(String newName) {
    this.calendarName = newName;
  }

  /**
   * Gets the timezone of this calendar.
   *
   * @return The calendar timezone
   */
  public ZoneId getTimeZone() {
    return timeZone;
  }
}