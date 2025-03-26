package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import model.CalendarEvent;
import model.ICalendarEvent;
import model.ICalendarManager;
import view.OutputHandler;
import model.RecurringEventGenerator;

/**
 * CommandParser is a utility class that interprets user commands
 * (strings like "create event..." or "edit event...") and delegates the work
 * to an ICalendarManager or the recurring event generator, etc.
 */
public class CommandParser {

  // Formats for parsing date/time from command strings
  private static final DateTimeFormatter dateTimeFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter dateFormatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * The main entry point: reads the first keyword (like "create event")
   * and calls the appropriate helper method (processCreateEvent, etc.).
   */
  public static void processCommand(String userCommand, ICalendarManager calendar) throws Exception {
    String lowerCaseCommand = userCommand.toLowerCase();

    if (lowerCaseCommand.startsWith("create event")) {
      processCreateEvent(userCommand, calendar);
    } else if (lowerCaseCommand.startsWith("edit events")) {
      processEditCommand(userCommand, calendar, true);
    } else if (lowerCaseCommand.startsWith("edit event")) {
      processEditCommand(userCommand, calendar, false);
    } else if (lowerCaseCommand.startsWith("print events on")) {
      processPrintEventsOn(userCommand, calendar);
    } else if (lowerCaseCommand.startsWith("print events from")) {
      processPrintEventsRange(userCommand, calendar);
    } else if (lowerCaseCommand.startsWith("export cal")) {
      processExportCal(userCommand, calendar);
    } else if (lowerCaseCommand.startsWith("show status on")) {
      processShowStatus(userCommand, calendar);
    } else if (lowerCaseCommand.startsWith("export googlecsv")) {
      processExportGoogleCSV(userCommand, calendar);
    } else {
      throw new Exception("Invalid command: " + userCommand);
    }
  }

  /**
   * Detects "--autodecline" in a command string, returning true if present.
   */
  public static boolean hasAutoDecline(String userCommand) {
    return userCommand.toLowerCase().contains("--autodecline");
  }

  /**
   * Responsible for parsing "create event" commands, which may be:
   * - Timed events: "from ... to ..." possibly with repeats
   * - All-day events: "on ..." possibly with repeats
   */
  private static void processCreateEvent(String userCommand, ICalendarManager calendar) throws Exception {
    boolean autoDeclineFlag = false;

    // Check if the command contains --autodecline, remove it from the string if so
    if (userCommand.toLowerCase().contains("--autodecline")) {
      autoDeclineFlag = true;
      userCommand = userCommand.replace("--autodecline", "").trim();
    }

    // See if this is a timed event (contains " from ... to ...")
    if (userCommand.contains(" from ")) {
      String[] splittedFrom = userCommand.split(" from ", 2);
      String rawName = splittedFrom[0].replace("create event", "").trim();
      String leftover = splittedFrom[1];

      // Must have a " to " for timed events
      if (!leftover.contains(" to ")) {
        throw new Exception("Invalid format: missing 'to' keyword.");
      }
      String[] splittedTo = leftover.split(" to ", 2);
      String rawStart = splittedTo[0].trim();
      String afterTo = splittedTo[1].trim();

      // Check if there's a "repeats" portion after " to "
      if (afterTo.toLowerCase().contains(" repeats ")) {
        String[] splittedRepeat = afterTo.split(" repeats ", 2);
        String rawEnd = splittedRepeat[0].trim();
        String repeatPart = splittedRepeat[1].trim();

        LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
        LocalDateTime endDateTime   = LocalDateTime.parse(rawEnd,   dateTimeFormatter);

        List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                rawName, startDateTime, endDateTime, repeatPart, false
        );

        for (CalendarEvent singleOccurrence : occurrences) {
          calendar.addEvent(singleOccurrence, autoDeclineFlag);
        }
        OutputHandler.getInstance().println(
                "Recurring event created with " + occurrences.size() + " occurrences."
        );
      } else {
        // It's just a single timed event
        String rawEnd = afterTo.trim();
        LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
        LocalDateTime endDateTime   = LocalDateTime.parse(rawEnd,   dateTimeFormatter);

        CalendarEvent eventObj = new CalendarEvent(rawName, startDateTime, endDateTime, false);
        calendar.addEvent(eventObj, autoDeclineFlag);
        OutputHandler.getInstance().println("Event created: " + eventObj);
      }
    }
    // Otherwise, check if it's an all-day event: " on ..."
    else if (userCommand.contains(" on ")) {
      String[] splittedOn = userCommand.split(" on ", 2);
      String rawName = splittedOn[0].replace("create event", "").trim();
      String leftover = splittedOn[1].trim();

      if (leftover.toLowerCase().contains(" repeats ")) {
        // e.g.: "create event Holiday on 2025-03-05 repeats MWF for 2 times"
        String[] splittedRepeat = leftover.split(" repeats ", 2);
        String dateStr = splittedRepeat[0].trim();
        String repeatPart = splittedRepeat[1].trim();

        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime   = date.plusDays(1).atStartOfDay();

        List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                rawName, startDateTime, endDateTime, repeatPart, true
        );

        for (CalendarEvent singleOccurrence : occurrences) {
          calendar.addEvent(singleOccurrence, autoDeclineFlag);
        }
        OutputHandler.getInstance().println(
                "Recurring all-day event created with " + occurrences.size() + " occurrences."
        );
      } else {
        // Single all-day event
        String dateStr = leftover.trim();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime   = date.plusDays(1).atStartOfDay();

        CalendarEvent eventObj = new CalendarEvent(rawName, startDateTime, endDateTime, true);
        calendar.addEvent(eventObj, autoDeclineFlag);
        OutputHandler.getInstance().println("All-day event created: " + eventObj);
      }
    } else {
      // If neither 'from' nor 'on' is present, it's invalid
      throw new Exception("Invalid create event command format.");
    }
  }

  /**
   * Utility method to format a success/failure message for edit commands.
   */
  public static String getUpdateMessage(boolean updated) {
    return updated
            ? "Event updated successfully."
            : "Event not found or update failed.";
  }

  /**
   * Handles "edit event" (singular) or "edit events" (plural) commands,
   * e.g. "edit event description Meeting from ... to ... with 'newValue'"
   */
  private static void processEditCommand(String userCommand, ICalendarManager calendar, boolean isPlural) throws Exception {
    // "edit events" or "edit event"
    String prefix = isPlural ? "edit events" : "edit event";
    String leftover = userCommand.substring(prefix.length()).trim();

    if (leftover.contains(" with ")) {
      String[] splittedWith = leftover.split(" with ", 2);
      String beforeWith = splittedWith[0].trim();
      String newValue   = splittedWith[1].trim();

      if (beforeWith.contains(" from ")) {
        // e.g. "description Meeting from 2025-03-01T10:00 to 2025-03-01T11:00"
        String[] splittedFrom = beforeWith.split(" from ", 2);
        String firstPart  = splittedFrom[0].trim();
        String afterFrom  = splittedFrom[1].trim();

        if (!isPlural && !afterFrom.contains(" to ")) {
          throw new Exception("Missing 'to' clause for singular edit command.");
        }

        String[] tokens = firstPart.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String property   = tokens[0].trim();
        String eventName  = tokens[1].trim();

        if (!isPlural) {
          // e.g. "edit event description Meeting from 2025-03-01T10:00 to 2025-03-01T11:00 with ..."
          String[] splittedTo = afterFrom.split(" to ", 2);
          if (splittedTo.length < 2) {
            throw new Exception("Missing 'to' clause for singular edit command.");
          }
          String rawStart = splittedTo[0].trim();
          String rawEnd   = splittedTo[1].trim();

          LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
          LocalDateTime endDateTime   = LocalDateTime.parse(rawEnd,   dateTimeFormatter);

          boolean updated = calendar.editSingleEvent(property, eventName, startDateTime, endDateTime, newValue);
          OutputHandler.getInstance().println(getUpdateMessage(updated));

        } else {
          // e.g. "edit events description Meeting from 2025-03-04T00:00 with ..."
          LocalDateTime startDateTime = LocalDateTime.parse(afterFrom, dateTimeFormatter);
          int count = calendar.editEventsByStart(property, eventName, startDateTime, newValue);
          OutputHandler.getInstance().println(count + " event(s) updated starting from " + startDateTime);
        }
      } else {
        // "description Meeting" => edit events by name only
        String[] tokens = beforeWith.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String property  = tokens[0].trim();
        String eventName = tokens[1].trim();

        int count = calendar.editEventsByName(property, eventName, newValue);
        OutputHandler.getInstance().println(count + " event(s) updated with new " + property);
      }
    } else {
      throw new Exception("Edit command must contain 'with' clause.");
    }
  }

  /**
   * "print events on 2025-03-01" => get events for that date and display them
   */
  private static void processPrintEventsOn(String userCommand, ICalendarManager calendar) throws Exception {
    String[] splittedOn = userCommand.split(" on ", 2);
    if (splittedOn.length < 2) {
      throw new Exception("Invalid command format for printing events.");
    }
    String dateStr = splittedOn[1].trim();
    LocalDate date = LocalDate.parse(dateStr, dateFormatter);

    List<ICalendarEvent> eventsOnDate = calendar.getEventsOn(date);
    if (eventsOnDate.isEmpty()) {
      OutputHandler.getInstance().println("No events found on " + date);
    } else {
      OutputHandler.getInstance().println("Events on " + date + ":");
      for (ICalendarEvent singleEvent : eventsOnDate) {
        OutputHandler.getInstance().println(" - " + singleEvent);
      }
    }
  }

  /**
   * "print events from 2025-03-01T09:00 to 2025-03-01T14:00" => get events in that range
   */
  private static void processPrintEventsRange(String userCommand, ICalendarManager calendar) throws Exception {
    String[] splittedFrom = userCommand.split(" from ", 2);
    if (splittedFrom.length < 2) {
      throw new Exception("Invalid command format for printing events in range.");
    }
    String leftover = splittedFrom[1].trim();

    if (!leftover.contains(" to ")) {
      throw new Exception("Missing 'to' clause in range query.");
    }
    String[] splittedTo = leftover.split(" to ", 2);
    String rawStart = splittedTo[0].trim();
    String rawEnd   = splittedTo[1].trim();

    LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
    LocalDateTime endDateTime   = LocalDateTime.parse(rawEnd,   dateTimeFormatter);

    List<ICalendarEvent> eventsInRange = calendar.getEventsInRange(startDateTime, endDateTime);
    if (eventsInRange.isEmpty()) {
      OutputHandler.getInstance().println(
              "No events found between " + startDateTime + " and " + endDateTime
      );
    } else {
      OutputHandler.getInstance().println(
              "Events between " + startDateTime + " and " + endDateTime + ":"
      );
      for (ICalendarEvent singleEvent : eventsInRange) {
        OutputHandler.getInstance().println(" - " + singleEvent);
      }
    }
  }

  /**
   * "export cal <filename>" => calls calendar.exportToCSV(...)
   */
  private static void processExportCal(String userCommand, ICalendarManager calendar) throws Exception {
    String[] tokens = userCommand.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid export command format.");
    }
    String fileName = tokens[2].trim();
    calendar.exportToCSV(fileName);
  }

  /**
   * "export googlecsv <filename>" => calls calendar.exportToGoogleCSV(...)
   */
  private static void processExportGoogleCSV(String userCommand, ICalendarManager calendar) throws Exception {
    String[] tokens = userCommand.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid export googlecsv command format.");
    }
    String fileName = tokens[2].trim();
    calendar.exportToGoogleCSV(fileName);
  }

  /**
   * "show status on 2025-03-01T10:30" => prints Busy or Available
   */
  private static void processShowStatus(String userCommand, ICalendarManager calendar) throws Exception {
    String[] splittedOn = userCommand.split(" on ", 2);
    if (splittedOn.length < 2) {
      throw new Exception("Invalid command format for show status.");
    }
    String rawDateTime = splittedOn[1].trim();
    LocalDateTime dateTime = LocalDateTime.parse(rawDateTime, dateTimeFormatter);

    boolean busyFlag = calendar.isBusyAt(dateTime);
    OutputHandler.getInstance().println(
            "Status at " + dateTime + ": " + (busyFlag ? "Busy" : "Available")
    );
  }
}
