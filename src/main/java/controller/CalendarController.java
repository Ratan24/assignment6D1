package controller;

import model.MultiCalendarManager;
import view.OutputHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import model.CalendarEvent; // Added import for instantiation

/**
 * CalendarController implements ICalendarController. It uses a MultiCalendarManager to support
 * multiple calendars.
 */
public class CalendarController implements ICalendarController {

  private final MultiCalendarManager multiCal;

  /**
   * Constructs a CalendarController with the specified calendar manager.
   *
   * @param multiCal The MultiCalendarManager to be used
   */
  public CalendarController(MultiCalendarManager multiCal) {
    this.multiCal = multiCal;
  }

  /**
   * Interactive mode: continuously read user commands.
   */
  @Override
  public void runInteractiveMode() {
    Scanner scanner = new Scanner(System.in);
    OutputHandler.getInstance().println("Calendar App Interactive Mode. Type 'exit' to quit.");
    while (true) {
      OutputHandler.getInstance().println("> ");
      String userCommand = scanner.nextLine();
      if (userCommand.equalsIgnoreCase("exit")) {
        OutputHandler.getInstance().println("Exiting.");
        break;
      }
      try {
        CommandParser.processCommand(userCommand, multiCal);
      } catch (Exception e) {
        OutputHandler.getInstance().println("Error: " + e.getMessage());
      }
    }
    scanner.close();
  }

  /**
   * Headless mode: read commands from a file.
   *
   * @param fileName The name of the file containing commands
   */
  @Override
  public void runHeadlessMode(String fileName) {
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String command;
      while ((command = br.readLine()) != null) {
        OutputHandler.getInstance().println("> " + command);
        if (command.equalsIgnoreCase("exit")) {
          OutputHandler.getInstance().println("Exiting.");
          break;
        }
        CommandParser.processCommand(command, multiCal);
      }
    } catch (IOException e) {
      OutputHandler.getInstance().println("Error reading file: " + e.getMessage());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Command error: " + e.getMessage());
    }
  }

  // --- Implementations for GUI Interaction ---

  @Override
  public List<String> getAvailableCalendarNames() {
    java.util.List<String> names = new java.util.ArrayList<>();
    try {
      for (model.CalendarManager cal : multiCal.getAllCalendars()) {
        names.add(cal.getCalendarName());
      }
    } catch (Exception e) {
      // Log or handle error appropriately, maybe return empty list
      System.err.println("Error getting calendar names: " + e.getMessage());
    }
    return names;
  }

  @Override
  public String getCurrentCalendarName() {
    try {
      return multiCal.getCurrentCalendar().getCalendarName();
    } catch (IllegalStateException e) {
      return null; // No current calendar selected
    } catch (Exception e) {
      System.err.println("Error getting current calendar name: " + e.getMessage());
      return null;
    }
  }

  @Override
  public String getCurrentCalendarTimezone() {
     try {
      return multiCal.getCurrentCalendar().getTimeZone().getId();
    } catch (IllegalStateException e) {
      return null; // No current calendar selected
    } catch (Exception e) {
      System.err.println("Error getting current calendar timezone: " + e.getMessage());
      return null;
    }
  }

  @Override
  public void createCalendar(String name, String timezone) throws Exception {
    multiCal.createCalendar(name, timezone);
  }

  @Override
  public void editCalendar(String oldName, String property, String newValue) throws Exception {
    multiCal.editCalendar(oldName, property, newValue);
  }

  @Override
  public void switchCalendar(String name) throws Exception {
    multiCal.useCalendar(name);
  }

  @Override
  public void createDefaultCalendarIfNeeded() throws Exception {
    // This logic might be better placed in the GUI startup,
    // but implementing here as per interface requirement.
    if (multiCal.getAllCalendars().isEmpty()) {
        String defaultName = "My Calendar";
        String defaultTimezone = java.time.ZoneId.systemDefault().getId();
        multiCal.createCalendar(defaultName, defaultTimezone);
        multiCal.useCalendar(defaultName); // Also make it the current one
    }
  }

  @Override
  public String[] getAvailableTimezones() {
      java.util.List<String> availableZones = new java.util.ArrayList<>(java.time.ZoneId.getAvailableZoneIds());
      java.util.Collections.sort(availableZones);
      return availableZones.toArray(new String[0]);
  }

  @Override
  public List<model.ICalendarEvent> getEventsInRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
    try {
      return multiCal.getCurrentCalendar().getEventsInRange(start, end);
    } catch (IllegalStateException e) {
      // No current calendar
      return new java.util.ArrayList<>();
    } catch (Exception e) {
      System.err.println("Error getting events in range: " + e.getMessage());
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public List<model.ICalendarEvent> getEventsOn(java.time.LocalDate date) {
    try {
      return multiCal.getCurrentCalendar().getEventsOn(date);
    } catch (IllegalStateException e) {
      // No current calendar
      return new java.util.ArrayList<>();
    } catch (Exception e) {
      System.err.println("Error getting events on date: " + e.getMessage());
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public void addEvent(model.ICalendarEvent event, boolean autoDecline) throws Exception {
    multiCal.getCurrentCalendar().addEvent(event, autoDecline);
  }

  @Override
  public void createNewSingleEvent(String name, java.time.LocalDateTime start, java.time.LocalDateTime end, boolean isAllDay, String description, String location, boolean isPublic) throws Exception {
      // Create the event object here
      CalendarEvent newEvent = new CalendarEvent(name, start, end, isAllDay);
      newEvent.setDescription(description);
      newEvent.setLocation(location);
      newEvent.setPublic(isPublic);
      // Add it using the existing addEvent logic (which talks to the model)
      this.addEvent(newEvent, true); // Assuming autoDecline true as default
  }

  @Override
  public void addRecurringEvent(String name, java.time.LocalDateTime start, java.time.LocalDateTime end, String recurrenceRule, boolean isAllDay, String description, String location, boolean isPublic) throws Exception {
      // Generate occurrences using the model's generator
      List<model.CalendarEvent> occurrences = model.RecurringEventGenerator.generateRecurringEvents(
          name, start, end, recurrenceRule, isAllDay
      );
      // Add each occurrence via the current calendar manager
      for (model.CalendarEvent occurrence : occurrences) {
          occurrence.setDescription(description);
          occurrence.setLocation(location);
          occurrence.setPublic(isPublic);
          // Assuming autoDecline is true for simplicity here, might need adjustment
          multiCal.getCurrentCalendar().addEvent(occurrence, true);
      }
  }

  @Override
  public boolean editSingleEvent(String property, String originalName, java.time.LocalDateTime originalStart, java.time.LocalDateTime originalEnd, String newValue) throws Exception {
    return multiCal.getCurrentCalendar().editSingleEvent(property, originalName, originalStart, originalEnd, newValue);
  }

  @Override
  public boolean deleteEvent(String eventName, java.time.LocalDateTime start, java.time.LocalDateTime end) throws Exception {
    return multiCal.getCurrentCalendar().deleteEvent(eventName, start, end);
  }

  @Override
  public int importFromGoogleCSV(String filePath) throws Exception {
    return multiCal.getCurrentCalendar().importFromGoogleCSV(filePath);
  }

  @Override
  public void exportToGoogleCSV(String filePath) throws Exception {
    multiCal.getCurrentCalendar().exportToGoogleCSV(filePath);
  }
}
