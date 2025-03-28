package model;

import view.OutputHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


/**
 * MultiCalendarManager implements both ICalendarManager and IMultiCalendar interfaces, providing
 * support for managing multiple calendars.
 */
public class MultiCalendarManager implements ICalendarManager, IMultiCalendar {

  private final Map<String, CalendarManager> allCalendars;
  private CalendarManager activeCalendar;

  /**
   * Constructs a new MultiCalendarManager with no calendars.
   */
  public MultiCalendarManager() {
    allCalendars = new HashMap<>();
  }

  /**
   * Creates a new calendar with the given name and timezone.
   *
   * @param calName The name of the new calendar
   * @param tzStr   The timezone string (e.g., "America/New_York")
   * @throws Exception If a calendar with that name already exists or timezone is invalid
   */
  @Override
  public void createCalendar(String calName, String tzStr) throws Exception {
    if (allCalendars.containsKey(calName)) {
      throw new Exception("Calendar with name " + calName + " already exists.");
    }
    CalendarManager freshCalendar = new CalendarManager(calName, tzStr);
    allCalendars.put(calName, freshCalendar);
    if (activeCalendar == null) {
      activeCalendar = freshCalendar;
    }
    OutputHandler.getInstance().println("Calendar created: " + calName + " (" + tzStr + ")");
  }

  /**
   * Edits a calendar's property (name or timezone).
   *
   * @param calName The name of the calendar to edit
   * @param calProp The property to edit ("name" or "timezone")
   * @param calVal  The new value for the property
   * @throws Exception If the calendar doesn't exist or the property is invalid
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
   *
   * @param calName The name of the calendar to use
   * @throws Exception If the calendar doesn't exist
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
   *
   * @return A collection of all calendar managers
   */
  public Collection<CalendarManager> getAllCalendars() {
    return allCalendars.values();
  }

  /**
   * Copies a single event from the current calendar to the target calendar. The event is identified
   * by name and source start time.
   *
   * @param label    The name of the event to copy
   * @param fromWhen The start time of the event in the source calendar
   * @param toCal    The name of the target calendar
   * @param toWhen   The new start time for the event in the target calendar
   * @throws Exception If the event or target calendar doesn't exist
   */
  @Override
  public void copyEvent(String label, LocalDateTime fromWhen, String toCal, LocalDateTime toWhen)
      throws Exception {
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
    long eventDurationMins = java.time.Duration.between(refEvent.getStart(), refEvent.getEnd())
        .toMinutes();
    LocalDateTime updatedEnd = toWhen.plusMinutes(eventDurationMins);
    CalendarEvent clonedEvent = new CalendarEvent(refEvent.getEventName(), toWhen, updatedEnd,
        refEvent.isAllDay());
    clonedEvent.setDescription(refEvent.getDescription());
    clonedEvent.setLocation(refEvent.getLocation());
    clonedEvent.setPublic(refEvent.isPublic());
    targetCal.addEvent(clonedEvent, true);
    OutputHandler.getInstance().println("Event copied to calendar " + toCal + ": " + clonedEvent);
  }

  /**
   * Copies all events on a given day from the current calendar to the target calendar. The dates
   * are shifted to the target date.
   *
   * @param fromDay The date from which to copy events
   * @param toCal   The name of the target calendar
   * @param toDay   The target date where events should be copied to
   * @throws Exception If the target calendar doesn't exist or no events exist on source date
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
      CalendarEvent clonedEvent = new CalendarEvent(e.getEventName(), mirroredStart, mirroredEnd,
          e.isAllDay());
      clonedEvent.setDescription(e.getDescription());
      clonedEvent.setLocation(e.getLocation());
      clonedEvent.setPublic(e.isPublic());
      destCal.addEvent(clonedEvent, true);
    }
    OutputHandler.getInstance().println(
        "Copied " + dayEvents.size() + " event(s) from " + fromDay + " to " + toCal
            + " starting on " + toDay);
  }

  /**
   * Copies all events between two dates (inclusive) from the current calendar to the target
   * calendar. The first target date corresponds to the start of the source interval.
   *
   * @param sourceStart The start date of the range from which to copy events
   * @param sourceEnd   The end date of the range from which to copy events
   * @param toCal       The name of the target calendar
   * @param targetStart The start date in the target calendar where events should begin
   * @throws Exception If the target calendar doesn't exist
   */
  @Override
  public void copyEventsBetween(LocalDate sourceStart, LocalDate sourceEnd, String toCal,
      LocalDate targetStart) throws Exception {
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
        CalendarEvent replicate = new CalendarEvent(ev.getEventName(), clonedStart, clonedEnd,
            ev.isAllDay());
        replicate.setDescription(ev.getDescription());
        replicate.setLocation(ev.getLocation());
        replicate.setPublic(ev.isPublic());
        destCal.addEvent(replicate, true);
        totalCopied++;
      }
      iterDay = iterDay.plusDays(1);
    }
    OutputHandler.getInstance().println(
        "Copied " + totalCopied + " event(s) from between " + sourceStart + " and " + sourceEnd
            + " to "
            + toCal + " starting on " + targetStart
    );
  }

  /**
   * Adds a new event to the current calendar.
   *
   * @param newEntry      The event to add
   * @param shouldDecline Whether to automatically decline conflicting events
   * @throws Exception If there is a conflict with an existing event
   */
  @Override
  public void addEvent(ICalendarEvent newEntry, boolean shouldDecline) throws Exception {
    getCurrentCalendar().addEvent(newEntry, shouldDecline);
  }

  /**
   * Gets all events scheduled on the specified date in the current calendar.
   *
   * @param exactDate The date to check
   * @return A list of events on the specified date
   */
  @Override
  public List<ICalendarEvent> getEventsOn(LocalDate exactDate) {
    try {
      return getCurrentCalendar().getEventsOn(exactDate);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  /**
   * Gets all events that occur within the specified time range in the current calendar.
   *
   * @param lowBound  The start of the time range
   * @param highBound The end of the time range
   * @return A list of events within the specified range
   */
  @Override
  public List<ICalendarEvent> getEventsInRange(LocalDateTime lowBound, LocalDateTime highBound) {
    try {
      return getCurrentCalendar().getEventsInRange(lowBound, highBound);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  /**
   * Exports all events from the current calendar to a CSV file.
   *
   * @param filename The name of the file to export to
   */
  @Override
  public void exportToCSV(String filename) {
    try {
      getCurrentCalendar().exportToCSV(filename);
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting CSV: " + e.getMessage());
    }
  }

  /**
   * Exports all events from the current calendar to a Google Calendar compatible CSV file.
   *
   * @param filename The name of the file to export to
   */
  @Override
  public void exportToGoogleCSV(String filename) {
    try {
      getCurrentCalendar().exportToGoogleCSV(filename);
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error exporting Google CSV: " + e.getMessage());
    }
  }

  /**
   * Checks if there is any event scheduled at the specified time in the current calendar.
   *
   * @param checkDateTime The date and time to check
   * @return true if there is an event at the specified time, false otherwise
   */
  @Override
  public boolean isBusyAt(LocalDateTime checkDateTime) {
    try {
      return getCurrentCalendar().isBusyAt(checkDateTime);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Edits a single event in the current calendar that matches the specified criteria.
   *
   * @param property   The property to edit
   * @param label      The name of the event
   * @param startTs    The start time of the event
   * @param endTs      The end time of the event
   * @param updatedVal The new value for the property
   * @return true if the event was found and updated, false otherwise
   */
  @Override
  public boolean editSingleEvent(String property, String label, LocalDateTime startTs,
      LocalDateTime endTs, String updatedVal) {
    try {
      return getCurrentCalendar().editSingleEvent(property, label, startTs, endTs, updatedVal);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Edits all events in the current calendar with the specified name that start at or after the
   * specified time.
   *
   * @param property   The property to edit
   * @param label      The name of the events to edit
   * @param startTs    The start time to filter events
   * @param updatedVal The new value for the property
   * @return The number of events that were updated
   */
  @Override
  public int editEventsByStart(String property, String label, LocalDateTime startTs,
      String updatedVal) {
    try {
      return getCurrentCalendar().editEventsByStart(property, label, startTs, updatedVal);
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Edits all events in the current calendar with the specified name.
   *
   * @param property   The property to edit
   * @param label      The name of the events to edit
   * @param updatedVal The new value for the property
   * @return The number of events that were updated
   */
  @Override
  public int editEventsByName(String property, String label, String updatedVal) {
    try {
      return getCurrentCalendar().editEventsByName(property, label, updatedVal);
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Gets all events in the current calendar.
   *
   * @return A list of all events in the current calendar
   */
  @Override
  public List<ICalendarEvent> getAllEvents() {
    try {
      return getCurrentCalendar().getAllEvents();
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  /**
   * Gets the currently active calendar.
   *
   * @return The current calendar manager
   * @throws IllegalStateException If no calendar is currently in use
   */
  public CalendarManager getCurrentCalendar() {
    if (activeCalendar == null) {
      throw new IllegalStateException("No calendar is currently in use.");
    }
    return activeCalendar;
  }
}