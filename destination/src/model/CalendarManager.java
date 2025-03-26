package model;

import view.OutputHandler;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Concrete implementation of ICalendarManager that manages a list of calendar events.
 * It allows event creation, editing, exporting, and conflict handling.
 */
public class CalendarManager implements ICalendarManager {

  // List of events (ICalendarEvent)
  private final List<ICalendarEvent> events;

  public CalendarManager() {
    this.events = new ArrayList<>();
  }

  /**
   * Adds an event to the calendar. If autoDecline is true and a conflict is found,
   * we throw an Exception rather than adding the event. Otherwise, we print a warning
   * about the conflict but still add the event.
   */
  @Override
  public void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws Exception {
    checkAndHandleConflict(newEvent, autoDecline);
    events.add(newEvent);
    // Sort all events by start date/time
    events.sort(Comparator.comparing(ICalendarEvent::getStart));
  }

  /**
   * Checks each existing event for conflicts with newEvent.
   * If autoDecline == true and a conflict is found, throws an exception.
   * Otherwise, prints a warning but still adds it.
   *
   * @return true if a conflict was found, otherwise false
   */
  private boolean checkAndHandleConflict(ICalendarEvent newEvent, boolean autoDecline) throws Exception {
    boolean conflictFound = false;
    for (ICalendarEvent existingEvent : events) {
      if (newEvent.conflictsWith(existingEvent)) {
        conflictFound = true;
        if (autoDecline) {
          throw new Exception("Conflict detected with event: " + existingEvent.getEventName());
        } else {
          OutputHandler.getInstance().println("Warning: Event conflicts with " + existingEvent.getEventName());
        }
      }
    }
    return conflictFound;
  }

  /**
   * Returns a list of events that occur on the given date (including all-day events).
   */
  @Override
  public List<ICalendarEvent> getEventsOn(LocalDate date) {
    List<ICalendarEvent> matchingEvents = new ArrayList<>();
    for (ICalendarEvent event : events) {
      if (event.isAllDay()) {
        // All-day events match if their start date == the requested date
        if (event.getStart().toLocalDate().equals(date)) {
          matchingEvents.add(event);
        }
      } else {
        // Timed events match if they overlap that calendar day
        LocalDate eventStartDate = event.getStart().toLocalDate();
        LocalDate eventEndDate   = event.getEnd().toLocalDate();
        if (!eventStartDate.isAfter(date) && !eventEndDate.isBefore(date)) {
          matchingEvents.add(event);
        }
      }
    }
    return matchingEvents;
  }

  /**
   * Returns a list of events that intersect the given time range [startRange, endRange).
   */
  @Override
  public List<ICalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange) {
    List<ICalendarEvent> matchingEvents = new ArrayList<>();
    for (ICalendarEvent event : events) {
      // We say it intersects if event.start < endRange and event.end > startRange
      if (event.getStart().isBefore(endRange) && event.getEnd().isAfter(startRange)) {
        matchingEvents.add(event);
      }
    }
    return matchingEvents;
  }

  /**
   * Exports all events to a CSV file. Each row includes name, start, end, all-day flag,
   * description, location, and a public (true/false) field.
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
   * Exports all events to a Google-compatible CSV file. Different columns, with
   * "Private" being 'True' if the event is private (i.e. isPublic == false).
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
          // For all-day events, we put the start date in Start Date, no time, and the same date in End Date.
          sb.append(event.getStart().format(dateFormatter)).append(",,");
          sb.append(event.getStart().format(dateFormatter)).append(",,");
          sb.append("True,");
        } else {
          // For timed events, fill out date/time pairs
          sb.append(event.getStart().format(dateFormatter)).append(",");
          sb.append(event.getStart().format(timeFormatter)).append(",");
          sb.append(event.getEnd().format(dateFormatter)).append(",");
          sb.append(event.getEnd().format(timeFormatter)).append(",");
          sb.append("False,");
        }

        // Append description, location, and "Private" flag
        sb.append("\"").append(event.getDescription()).append("\",");
        sb.append("\"").append(event.getLocation()).append("\",");

        // If event is public, "Private" = False; if event is private, "Private" = True
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
   * Determines if the calendar is busy at a particular moment:
   * i.e., if there's any event whose start is <= that time and end is > that time.
   */
  @Override
  public boolean isBusyAt(LocalDateTime dateTime) {
    for (ICalendarEvent event : events) {
      // If event.start <= dateTime and event.end > dateTime, we call it busy
      if (!event.getStart().isAfter(dateTime) && event.getEnd().isAfter(dateTime)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Edits a single event matching (eventName, start, end) for a property
   * (like "description" or "location"), giving it a new value.
   */
  @Override
  public boolean editSingleEvent(String property, String eventName,
                                 LocalDateTime start, LocalDateTime end, String newValue) {
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
   * Edits any event with the given (eventName) whose start time is
   * at or after 'start'.
   */
  @Override
  public int editEventsByStart(String property, String eventName,
                               LocalDateTime start, String newValue) {
    int numberUpdated = 0;

    for (ICalendarEvent event : events) {
      boolean nameMatches = event.getEventName().equals(eventName);
      boolean afterStart  = event.getStart().equals(start) || event.getStart().isAfter(start);

      if (nameMatches && afterStart) {
        if (updateProperty(event, property, newValue)) {
          numberUpdated++;
        }
      }
    }
    return numberUpdated;
  }

  /**
   * Edits all events matching eventName for the given property,
   * setting them to newValue.
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

  /**
   * Helper method that updates one property of the event to newValue.
   */
  private boolean updateProperty(ICalendarEvent event, String property, String newValue) {
    switch (property.toLowerCase()) {
      case "name":
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
        return false; // property not recognized
    }
    return true;
  }

  /**
   * Returns a defensive copy of the entire event list.
   */
  @Override
  public List<ICalendarEvent> getAllEvents() {
    return new ArrayList<>(events);
  }
}
