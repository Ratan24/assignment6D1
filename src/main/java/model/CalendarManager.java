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
  private ZoneId timeZone; // Using IANA ZoneId

  public CalendarManager(String calendarName, String timezoneStr) throws Exception {
    this.events = new ArrayList<>();
    this.calendarName = calendarName;
    try {
      this.timeZone = ZoneId.of(timezoneStr);
    } catch (Exception e) {
      throw new Exception("Invalid timezone format: " + timezoneStr);
    }
  }

  public String getCalendarName() {
    return calendarName;
  }

  public void setCalendarName(String newName) {
    this.calendarName = newName;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timezoneStr) throws Exception {
    try {
      this.timeZone = ZoneId.of(timezoneStr);
    } catch (Exception e) {
      throw new Exception("Invalid timezone format: " + timezoneStr);
    }
  }

  @Override
  public void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws Exception {
    // In this version, conflicts are always declined
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

  @Override
  public boolean isBusyAt(LocalDateTime dateTime) {
    for (ICalendarEvent event : events) {
      if (!event.getStart().isAfter(dateTime) && event.getEnd().isAfter(dateTime)) {
        return true;
      }
    }
    return false;
  }

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
      case "subject": // added alias: subject means event name
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

  @Override
  public List<ICalendarEvent> getAllEvents() {
    return new ArrayList<>(events);
  }

//  @Override
//  public void accept(CommandParserVisitor visitor, String command) throws Exception {
//    visitor.process(command, this);
//  }
}
