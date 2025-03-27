package model;

import view.OutputHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * MultiCalendarManager now implements ICalendarManager.
 */
public class MultiCalendarManager implements ICalendarManager, IMultiCalendar {

  // Map from calendar name to a CalendarManager instance.
  private final Map<String, CalendarManager> allCalendars;
  // The currently selected calendar.
  private CalendarManager activeCalendar;

  public MultiCalendarManager() {
    allCalendars = new HashMap<>();
  }

  // ===== Multi-Calendar Specific Methods =====

  /**
   * Creates a new calendar with the given name and timezone.
   * Throws an Exception if a calendar with that name already exists.
   */
  @Override
  public void createCalendar(String calName, String tzStr) throws Exception {
    if (allCalendars.containsKey(calName)) {
      throw new Exception("Calendar with name " + calName + " already exists.");
    }
    CalendarManager freshCalendar = new CalendarManager(calName, tzStr);
    allCalendars.put(calName, freshCalendar);
    // Set as current calendar if none is in use.
    if (activeCalendar == null) {
      activeCalendar = freshCalendar;
    }
    OutputHandler.getInstance().println("Calendar created: " + calName + " (" + tzStr + ")");
  }

  /**
   * Edits a calendar's property (name or timezone).
   */
  @Override
  public void editCalendar(String calName, String calProp, String calVal) throws Exception {
    CalendarManager chosenCalendar = allCalendars.get(calName);
    if (chosenCalendar == null) {
      throw new Exception("Calendar not found: " + calName);
    }
    switch (calProp.toLowerCase()) {
      case "name":
        if (allCalendars.containsKey(calVal)) {
          throw new Exception("Another calendar with that name already exists.");
        }
        allCalendars.remove(calName);
        chosenCalendar.setCalendarName(calVal);
        allCalendars.put(calVal, chosenCalendar);
        OutputHandler.getInstance().println("Calendar name updated to: " + calVal);
        break;
      case "timezone":
        chosenCalendar.setTimeZone(calVal);
        OutputHandler.getInstance().println("Calendar timezone updated to: " + calVal);
        break;
      default:
        throw new Exception("Invalid calendar property: " + calProp);
    }
  }

  /**
   * Sets the current calendar context.
   */
  @Override
  public void useCalendar(String calName) throws Exception {
    CalendarManager foundCalendar = allCalendars.get(calName);
    if (foundCalendar == null) {
      throw new Exception("Calendar not found: " + calName);
    }
    activeCalendar = foundCalendar;
    OutputHandler.getInstance().println("Using calendar: " + calName);
  }

  /**
   * Returns all calendars.
   */
  public Collection<CalendarManager> getAllCalendars() {
    return allCalendars.values();
  }

  // ===== Copy Functions =====

  /**
   * Copies a single event from the current calendar to the target calendar.
   * The event is identified by name and source start time.
   */
  @Override
  public void copyEvent(String label, LocalDateTime fromWhen, String toCal, LocalDateTime toWhen) throws Exception {
    CalendarManager fromCal = getCurrentCalendar();
    CalendarManager targetCal = allCalendars.get(toCal);
    if (targetCal == null) {
      throw new Exception("Target calendar not found: " + toCal);
    }
    ICalendarEvent refEvent = null;
    for (ICalendarEvent evItem : fromCal.getAllEvents()) {
      if (evItem.getEventName().equals(label) && evItem.getStart().equals(fromWhen)) {
        refEvent = evItem;
        break;
      }
    }
    if (refEvent == null) {
      throw new Exception("Event not found in source calendar.");
    }
    long eventDurationMins = java.time.Duration.between(refEvent.getStart(), refEvent.getEnd()).toMinutes();
    LocalDateTime updatedEnd = toWhen.plusMinutes(eventDurationMins);
    CalendarEvent clonedEvent = new CalendarEvent(refEvent.getEventName(), toWhen, updatedEnd, refEvent.isAllDay());
    clonedEvent.setDescription(refEvent.getDescription());
    clonedEvent.setLocation(refEvent.getLocation());
    clonedEvent.setPublic(refEvent.isPublic());
    targetCal.addEvent(clonedEvent, true);
    OutputHandler.getInstance().println("Event copied to calendar " + toCal + ": " + clonedEvent);
  }

  /**
   * Copies all events on a given day from the current calendar to the target calendar.
   * The dates are shifted to the target date.
   */
  @Override
  public void copyEventsOn(LocalDate fromDay, String toCal, LocalDate toDay) throws Exception {
    CalendarManager sourceCal = getCurrentCalendar();
    CalendarManager destCal = allCalendars.get(toCal);
    if (destCal == null) {
      throw new Exception("Target calendar not found: " + toCal);
    }
    List<ICalendarEvent> dayEvents = sourceCal.getEventsOn(fromDay);
    if (dayEvents.isEmpty()) {
      throw new Exception("No events to copy on " + fromDay);
    }
    for (ICalendarEvent e : dayEvents) {
      LocalDateTime mirroredStart = toDay.atTime(e.getStart().toLocalTime());
      LocalDateTime mirroredEnd = toDay.atTime(e.getEnd().toLocalTime());
      CalendarEvent clonedEvent = new CalendarEvent(e.getEventName(), mirroredStart, mirroredEnd, e.isAllDay());
      clonedEvent.setDescription(e.getDescription());
      clonedEvent.setLocation(e.getLocation());
      clonedEvent.setPublic(e.isPublic());
      destCal.addEvent(clonedEvent, true);
    }
    OutputHandler.getInstance().println(
            "Copied " + dayEvents.size() + " event(s) from " + fromDay + " to " + toCal + " starting on " + toDay);
  }

  /**
   * Copies all events between two dates (inclusive) from the current calendar to the target calendar.
   * The first target date corresponds to the start of the source interval.
   */
  @Override
  public void copyEventsBetween(LocalDate sourceStart, LocalDate sourceEnd, String toCal, LocalDate targetStart) throws Exception {
    CalendarManager baseCal = getCurrentCalendar();
    CalendarManager destCal = allCalendars.get(toCal);
    if (destCal == null) {
      throw new Exception("Target calendar not found: " + toCal);
    }
    LocalDate iterDay = sourceStart;
    int totalCopied = 0;
    while (!iterDay.isAfter(sourceEnd)) {
      List<ICalendarEvent> matchingEvents = baseCal.getEventsOn(iterDay);
      for (ICalendarEvent ev : matchingEvents) {
        long offsetDays = ChronoUnit.DAYS.between(sourceStart, iterDay);
        LocalDate shiftDate = targetStart.plusDays(offsetDays);
        LocalDateTime clonedStart = shiftDate.atTime(ev.getStart().toLocalTime());
        LocalDateTime clonedEnd = shiftDate.atTime(ev.getEnd().toLocalTime());
        CalendarEvent replicate = new CalendarEvent(ev.getEventName(), clonedStart, clonedEnd, ev.isAllDay());
        replicate.setDescription(ev.getDescription());
        replicate.setLocation(ev.getLocation());
        replicate.setPublic(ev.isPublic());
        destCal.addEvent(replicate, true);
        totalCopied++;
      }
      iterDay = iterDay.plusDays(1);
    }
    OutputHandler.getInstance().println(
            "Copied " + totalCopied + " event(s) from between " + sourceStart + " and " + sourceEnd + " to "
                    + toCal + " starting on " + targetStart
    );
  }

  // ===== Implementation of ICalendarManager methods =====
  // Here, for methods that deal with events, we delegate to the currently active calendar.

  @Override
  public void addEvent(ICalendarEvent newEntry, boolean shouldDecline) throws Exception {
    getCurrentCalendar().addEvent(newEntry, shouldDecline);
  }

  @Override
  public List<ICalendarEvent> getEventsOn(LocalDate exactDate) {
    try {
      return getCurrentCalendar().getEventsOn(exactDate);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  @Override
  public List<ICalendarEvent> getEventsInRange(LocalDateTime lowBound, LocalDateTime highBound) {
    try {
      return getCurrentCalendar().getEventsInRange(lowBound, highBound);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  @Override
  public void exportToCSV(String filename) {
    try {
      getCurrentCalendar().exportToCSV(filename);
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting CSV: " + e.getMessage());
    }
  }

  @Override
  public void exportToGoogleCSV(String filename) {
    try {
      getCurrentCalendar().exportToGoogleCSV(filename);
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting Google CSV: " + e.getMessage());
    }
  }

  @Override
  public boolean isBusyAt(LocalDateTime checkDateTime) {
    try {
      return getCurrentCalendar().isBusyAt(checkDateTime);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean editSingleEvent(String property, String label, LocalDateTime startTs, LocalDateTime endTs, String updatedVal) {
    try {
      return getCurrentCalendar().editSingleEvent(property, label, startTs, endTs, updatedVal);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public int editEventsByStart(String property, String label, LocalDateTime startTs, String updatedVal) {
    try {
      return getCurrentCalendar().editEventsByStart(property, label, startTs, updatedVal);
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public int editEventsByName(String property, String label, String updatedVal) {
    try {
      return getCurrentCalendar().editEventsByName(property, label, updatedVal);
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

  public CalendarManager getCurrentCalendar() {
    if (activeCalendar == null) {
      throw new IllegalStateException("No calendar is currently in use.");
    }
    return activeCalendar;
  }
}
