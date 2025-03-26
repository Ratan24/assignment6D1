package model;

import view.OutputHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

// MultiCalendarManager now implements ICalendarManager
public class MultiCalendarManager implements ICalendarManager {

  // Map from calendar name to a CalendarManager instance.
  private final Map<String, CalendarManager> calendars;
  // The currently selected calendar.
  private CalendarManager currentCalendar;

  public MultiCalendarManager() {
    calendars = new HashMap<>();
  }

  // ===== Multi-Calendar Specific Methods =====

  /**
   * Creates a new calendar with the given name and timezone.
   * Throws an Exception if a calendar with that name already exists.
   */
  public void createCalendar(String name, String timezoneStr) throws Exception {
    if (calendars.containsKey(name)) {
      throw new Exception("Calendar with name " + name + " already exists.");
    }
    CalendarManager newCal = new CalendarManager(name, timezoneStr);
    calendars.put(name, newCal);
    // Set as current calendar if none is in use.
    if (currentCalendar == null) {
      currentCalendar = newCal;
    }
    OutputHandler.getInstance().println("Calendar created: " + name + " (" + timezoneStr + ")");
  }

  /**
   * Edits a calendar's property (name or timezone).
   */
  public void editCalendar(String name, String property, String newValue) throws Exception {
    CalendarManager cal = calendars.get(name);
    if (cal == null) {
      throw new Exception("Calendar not found: " + name);
    }
    switch (property.toLowerCase()) {
      case "name":
        if (calendars.containsKey(newValue)) {
          throw new Exception("Another calendar with that name already exists.");
        }
        calendars.remove(name);
        cal.setCalendarName(newValue);
        calendars.put(newValue, cal);
        OutputHandler.getInstance().println("Calendar name updated to: " + newValue);
        break;
      case "timezone":
        cal.setTimeZone(newValue);
        OutputHandler.getInstance().println("Calendar timezone updated to: " + newValue);
        break;
      default:
        throw new Exception("Invalid calendar property: " + property);
    }
  }

  /**
   * Sets the current calendar context.
   */
  public void useCalendar(String name) throws Exception {
    CalendarManager cal = calendars.get(name);
    if (cal == null) {
      throw new Exception("Calendar not found: " + name);
    }
    currentCalendar = cal;
    OutputHandler.getInstance().println("Using calendar: " + name);
  }

  /**
   * Returns all calendars.
   */
  public Collection<CalendarManager> getAllCalendars() {
    return calendars.values();
  }

  // ===== Copy Functions =====

  /**
   * Copies a single event from the current calendar to the target calendar.
   * The event is identified by name and source start time.
   */
  public void copyEvent(String eventName, LocalDateTime sourceStart, String targetCalendarName, LocalDateTime targetStart) throws Exception {
    CalendarManager sourceCal = getCurrentCalendar();
    CalendarManager targetCal = calendars.get(targetCalendarName);
    if (targetCal == null) {
      throw new Exception("Target calendar not found: " + targetCalendarName);
    }
    ICalendarEvent eventToCopy = null;
    for (ICalendarEvent ev : sourceCal.getAllEvents()) {
      if (ev.getEventName().equals(eventName) && ev.getStart().equals(sourceStart)) {
        eventToCopy = ev;
        break;
      }
    }
    if (eventToCopy == null) {
      throw new Exception("Event not found in source calendar.");
    }
    long durationMinutes = java.time.Duration.between(eventToCopy.getStart(), eventToCopy.getEnd()).toMinutes();
    LocalDateTime newEnd = targetStart.plusMinutes(durationMinutes);
    CalendarEvent newEvent = new CalendarEvent(eventToCopy.getEventName(), targetStart, newEnd, eventToCopy.isAllDay());
    newEvent.setDescription(eventToCopy.getDescription());
    newEvent.setLocation(eventToCopy.getLocation());
    newEvent.setPublic(eventToCopy.isPublic());
    targetCal.addEvent(newEvent, true);
    OutputHandler.getInstance().println("Event copied to calendar " + targetCalendarName + ": " + newEvent);
  }

  /**
   * Copies all events on a given day from the current calendar to the target calendar.
   * The dates are shifted to the target date.
   */
  public void copyEventsOn(LocalDate sourceDate, String targetCalendarName, LocalDate targetDate) throws Exception {
    CalendarManager sourceCal = getCurrentCalendar();
    CalendarManager targetCal = calendars.get(targetCalendarName);
    if (targetCal == null) {
      throw new Exception("Target calendar not found: " + targetCalendarName);
    }
    List<ICalendarEvent> eventsToCopy = sourceCal.getEventsOn(sourceDate);
    if (eventsToCopy.isEmpty()) {
      throw new Exception("No events to copy on " + sourceDate);
    }
    for (ICalendarEvent ev : eventsToCopy) {
      LocalDateTime newStart = targetDate.atTime(ev.getStart().toLocalTime());
      LocalDateTime newEnd = targetDate.atTime(ev.getEnd().toLocalTime());
      CalendarEvent newEvent = new CalendarEvent(ev.getEventName(), newStart, newEnd, ev.isAllDay());
      newEvent.setDescription(ev.getDescription());
      newEvent.setLocation(ev.getLocation());
      newEvent.setPublic(ev.isPublic());
      targetCal.addEvent(newEvent, true);
    }
    OutputHandler.getInstance().println("Copied " + eventsToCopy.size() + " event(s) from " + sourceDate + " to " + targetCalendarName + " starting on " + targetDate);
  }

  /**
   * Copies all events between two dates (inclusive) from the current calendar to the target calendar.
   * The first target date corresponds to the start of the source interval.
   */
  public void copyEventsBetween(LocalDate sourceStart, LocalDate sourceEnd, String targetCalendarName, LocalDate targetStart) throws Exception {
    CalendarManager sourceCal = getCurrentCalendar();
    CalendarManager targetCal = calendars.get(targetCalendarName);
    if (targetCal == null) {
      throw new Exception("Target calendar not found: " + targetCalendarName);
    }
    LocalDate current = sourceStart;
    int totalCopied = 0;
    while (!current.isAfter(sourceEnd)) {
      List<ICalendarEvent> eventsToCopy = sourceCal.getEventsOn(current);
      for (ICalendarEvent ev : eventsToCopy) {
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(sourceStart, current);
        LocalDate newDate = targetStart.plusDays(daysDiff);
        LocalDateTime newStart = newDate.atTime(ev.getStart().toLocalTime());
        LocalDateTime newEnd = newDate.atTime(ev.getEnd().toLocalTime());
        CalendarEvent newEvent = new CalendarEvent(ev.getEventName(), newStart, newEnd, ev.isAllDay());
        newEvent.setDescription(ev.getDescription());
        newEvent.setLocation(ev.getLocation());
        newEvent.setPublic(ev.isPublic());
        targetCal.addEvent(newEvent, true);
        totalCopied++;
      }
      current = current.plusDays(1);
    }
    OutputHandler.getInstance().println("Copied " + totalCopied + " event(s) from between " + sourceStart + " and " + sourceEnd + " to " + targetCalendarName + " starting on " + targetStart);
  }

  // ===== Implementation of ICalendarManager methods =====
  // Here, for methods that deal with events, we delegate to the currently active calendar.

  @Override
  public void addEvent(ICalendarEvent newEvent, boolean autoDecline) throws Exception {
    getCurrentCalendar().addEvent(newEvent, autoDecline);
  }

  @Override
  public List<ICalendarEvent> getEventsOn(LocalDate date) {
    try {
      return getCurrentCalendar().getEventsOn(date);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  @Override
  public List<ICalendarEvent> getEventsInRange(LocalDateTime startRange, LocalDateTime endRange) {
    try {
      return getCurrentCalendar().getEventsInRange(startRange, endRange);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  @Override
  public void exportToCSV(String fileName) {
    try {
      getCurrentCalendar().exportToCSV(fileName);
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting CSV: " + e.getMessage());
    }
  }

  @Override
  public void exportToGoogleCSV(String fileName) {
    try {
      getCurrentCalendar().exportToGoogleCSV(fileName);
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting Google CSV: " + e.getMessage());
    }
  }

  @Override
  public boolean isBusyAt(LocalDateTime dateTime) {
    try {
      return getCurrentCalendar().isBusyAt(dateTime);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean editSingleEvent(String property, String eventName, LocalDateTime start, LocalDateTime end, String newValue) {
    try {
      return getCurrentCalendar().editSingleEvent(property, eventName, start, end, newValue);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public int editEventsByStart(String property, String eventName, LocalDateTime start, String newValue) {
    try {
      return getCurrentCalendar().editEventsByStart(property, eventName, start, newValue);
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public int editEventsByName(String property, String eventName, String newValue) {
    try {
      return getCurrentCalendar().editEventsByName(property, eventName, newValue);
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public List<ICalendarEvent> getAllEvents() {
    try {
      return getCurrentCalendar().getAllEvents();
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

//  @Override
//  public void accept(CommandParserVisitor visitor, String command) throws Exception {
//    visitor.process(command, this);
//  }

  public CalendarManager getCurrentCalendar() {
    if (currentCalendar == null) {
      throw new IllegalStateException("No calendar is currently in use.");
    }
    return currentCalendar;
  }
}
