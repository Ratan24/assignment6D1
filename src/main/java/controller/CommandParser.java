package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import model.CalendarEvent;
import model.ICalendarEvent;
import model.ICalendarManager;
import model.MultiCalendarManager;
import model.RecurringEventGenerator;
import view.OutputHandler;

/**
 * CommandParser interprets user commands and delegates work to the appropriate methods.
 * It now supports additional commands for multiple calendars and event copying.
 */
public class CommandParser {
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Processes a user command. Accepts commands that target either events or calendars.
   */
  public static void processCommand(String userCommand, Object manager) throws Exception {
    // We support two types: MultiCalendarManager and ICalendarManager.
    if (manager instanceof MultiCalendarManager) {
      processMultiCalendarCommand(userCommand, (MultiCalendarManager) manager);
    } else if (manager instanceof ICalendarManager) {
      processEventCommand(userCommand, (ICalendarManager) manager);
    } else {
      throw new Exception("Unsupported manager type.");
    }
  }

  // If the manager is a MultiCalendarManager, handle calendar-level commands:
  private static void processMultiCalendarCommand(String userCommand, MultiCalendarManager multiCal) throws Exception {
    String lower = userCommand.toLowerCase();
    System.out.println("usharma lower: " + lower);
    if (lower.startsWith("create calendar")) {
      System.out.println(" create calendar  I have entered");
      processCreateCalendar(userCommand, multiCal);
    } else if (lower.startsWith("edit calendar")) {
      System.out.println(" edit calendar  I have entered");
      processEditCalendar(userCommand, multiCal);
    } else if (lower.startsWith("use calendar")) {
      System.out.println(" use calendar  I have entered");
      processUseCalendar(userCommand, multiCal);
    } else if (lower.startsWith("copy events on")) {
      System.out.println("Copy event on I have entered");
      processCopyEventsOn(userCommand, multiCal);
    } else if (lower.startsWith("copy events between")) {
      System.out.println("Copy event between I have entered");
      processCopyEventsBetween(userCommand, multiCal);
    } else if (lower.startsWith("copy event")) {
      System.out.println("Copy event I have entered");
      processCopyEvent(userCommand, multiCal);
    } else {
      // Otherwise, assume it's an event command in the current calendar.
      System.out.println(" userCommand  I have entered");
      processEventCommand(userCommand, multiCal.getCurrentCalendar());
    }
  }

  // Process commands meant for a single calendar
  private static void processEventCommand(String userCommand, ICalendarManager calendar) throws Exception {
    String lower = userCommand.toLowerCase();
    if (lower.startsWith("create event")) {
      processCreateEvent(userCommand, calendar);
    } else if (lower.startsWith("edit events")) {
      processEditCommand(userCommand, calendar, true);
    } else if (lower.startsWith("edit event")) {
      processEditCommand(userCommand, calendar, false);
    } else if (lower.startsWith("print events on")) {
      processPrintEventsOn(userCommand, calendar);
    } else if (lower.startsWith("print events from")) {
      processPrintEventsRange(userCommand, calendar);
    } else if (lower.startsWith("export cal")) {
      processExportCal(userCommand, calendar);
    } else if (lower.startsWith("export googlecsv")) {
      processExportGoogleCSV(userCommand, calendar);
    } else if (lower.startsWith("show status on")) {
      processShowStatus(userCommand, calendar);
    } else {
      throw new Exception("Invalid command: " + userCommand);
    }
  }

  // New calendar commands:
  static void processCreateCalendar(String command, MultiCalendarManager multiCal) throws Exception {
    // Expected format: create calendar --name <calName> --timezone <tz>
    String[] tokens = command.split(" ");
    if (tokens.length < 5) {
      throw new Exception("Invalid create calendar command format.");
    }
    String calName = null, tz = null;
    for (int i = 2; i < tokens.length; i++) {
      if (tokens[i].equalsIgnoreCase("--name") && i + 1 < tokens.length) {
        calName = tokens[++i];
      } else if (tokens[i].equalsIgnoreCase("--timezone") && i + 1 < tokens.length) {
        tz = tokens[++i];
      }
    }
    if (calName == null || tz == null) {
      throw new Exception("Calendar name and timezone must be provided.");
    }
    multiCal.createCalendar(calName, tz);
  }

  static void processEditCalendar(String command, MultiCalendarManager multiCal) throws Exception {
    // Expected format: edit calendar --name <calName> --property <prop> <newValue>
    String[] tokens = command.split(" ");
    if (tokens.length < 6) {
      throw new Exception("Invalid edit calendar command format.");
    }
    String calName = null, property = null, newValue = null;
    for (int i = 2; i < tokens.length; i++) {
      if (tokens[i].equalsIgnoreCase("--name") && i + 1 < tokens.length) {
        calName = tokens[++i];
      } else if (tokens[i].equalsIgnoreCase("--property") && i + 1 < tokens.length) {
        property = tokens[++i];
        if (i + 1 < tokens.length) {
          newValue = tokens[++i];
        }
      }
    }
    if (calName == null || property == null || newValue == null) {
      throw new Exception("Invalid edit calendar command parameters.");
    }
    multiCal.editCalendar(calName, property, newValue);
  }

  static void processUseCalendar(String command, MultiCalendarManager multiCal) throws Exception {
    // Expected format: use calendar --name <calName>
    String[] tokens = command.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid use calendar command format.");
    }
    String calName = null;
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].equalsIgnoreCase("--name") && i + 1 < tokens.length) {
        calName = tokens[++i];
      }
    }
    if (calName == null) {
      throw new Exception("Calendar name must be provided.");
    }
    multiCal.useCalendar(calName);
  }

    // Copy commands:
    static void processCopyEvent(String command, MultiCalendarManager multiCal) throws Exception {
      // Expected format:
      // copy event <eventName> on <dateTime> --target <calendarName> to <dateTime>
      String[] tokens = command.split(" ");
      if (tokens.length < 8) {
        throw new Exception("Invalid copy event command format.");
      }

      // We'll assume eventName is a single token for simplicity.
      String eventName = tokens[2];
      // "on" token at index 3; dateTime at index 4.
      LocalDateTime sourceStart = LocalDateTime.parse(tokens[4], dateTimeFormatter);
      // "--target" at index 5; target calendar name at index 6.
      String targetCalendarName = tokens[6];
      // "to" at index 7; target date/time at index 8.
      LocalDateTime targetStart = LocalDateTime.parse(tokens[8], dateTimeFormatter);
      multiCal.copyEvent(eventName, sourceStart, targetCalendarName, targetStart);
    }

//    private static void processCopyEventsOn(String command, MultiCalendarManager multiCal) throws Exception {
//      // Expected format:
//      // copy events on <yyyy-MM-dd> --target <calendarName> to <yyyy-MM-dd>
//      Scanner scanner = new Scanner(command);
//
//      // Consume the tokens "copy", "events", "on"
//      if (!scanner.hasNext("copy")) {
//        throw new Exception("Command must start with 'copy'");
//      }
//      scanner.next(); // copy
//      if (!scanner.hasNext("events")) {
//        throw new Exception("Expected 'events' after 'copy'");
//      }
//      scanner.next(); // events
//      if (!scanner.hasNext("on")) {
//        throw new Exception("Expected 'on' after 'copy events'");
//      }
//      scanner.next(); // on
//
//      // Next token is the source date
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected source date.");
//      }
//      String sourceDateStr = scanner.next();
//
//      // Next, expect the --target token.
//      if (!scanner.hasNext("--target")) {
//        throw new Exception("Missing '--target' token in copy events on command.");
//      }
//      scanner.next(); // --target
//
//      // Next token is the target calendar name.
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected target calendar name.");
//      }
//      String targetCalendarName = scanner.next();
//
//      // Next, expect the token "to"
//      if (!scanner.hasNext("to")) {
//        throw new Exception("Missing 'to' token in copy events on command.");
//      }
//      scanner.next(); // to
//
//      // Next token is the target date.
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected target date.");
//      }
//      String targetDateStr = scanner.next();
//
//      // Parse the dates using your dateFormatter (yyyy-MM-dd)
//      LocalDate sourceDate = LocalDate.parse(sourceDateStr, dateFormatter);
//      LocalDate targetDate = LocalDate.parse(targetDateStr, dateFormatter);
//
//      multiCal.copyEventsOn(sourceDate, targetCalendarName, targetDate);
//    }

  static void processCopyEventsOn(String command, MultiCalendarManager multiCal) throws Exception {
    // Expected format:
    // copy events on <yyyy-MM-dd> --target <calendarName> to <yyyy-MM-dd>
    Scanner scanner = new Scanner(command);

    // Consume the tokens "copy", "events", "on"
    String token = scanner.next(); // "copy"
    token = scanner.next();        // "events"
    token = scanner.next();        // "on"

    // Next token: source date
    String sourceDateStr = scanner.next();

    // Next token should be "--target"
    token = scanner.next();
    if (!token.equalsIgnoreCase("--target")) {
      throw new Exception("Missing '--target' token in copy events on command.");
    }

    // Next token: target calendar name
    String targetCalendarName = scanner.next();

    // Next token should be "to"
    token = scanner.next();
    if (!token.equalsIgnoreCase("to")) {
      throw new Exception("Missing 'to' token in copy events on command.");
    }

    // Next token: target date
    String targetDateStr = scanner.next();

    // Parse dates (expected format yyyy-MM-dd)
    LocalDate sourceDate = LocalDate.parse(sourceDateStr, dateFormatter);
    LocalDate targetDate = LocalDate.parse(targetDateStr, dateFormatter);

    multiCal.copyEventsOn(sourceDate, targetCalendarName, targetDate);
  }



//  private static void processCopyEventsBetween(String command, MultiCalendarManager multiCal) throws Exception {
//      // Expected format:
//      // copy events between <yyyy-MM-dd> and <yyyy-MM-dd> to --target <calendarName> <yyyy-MM-dd>
//      Scanner scanner = new Scanner(command);
//
//      // Consume the tokens "copy", "events", "between"
//      if (!scanner.hasNext("copy")) {
//        throw new Exception("Command must start with 'copy'");
//      }
//      scanner.next(); // copy
//      if (!scanner.hasNext("events")) {
//        throw new Exception("Expected 'events' after 'copy'");
//      }
//      scanner.next(); // events
//      if (!scanner.hasNext("between")) {
//        throw new Exception("Expected 'between' after 'copy events'");
//      }
//      scanner.next(); // between
//
//      // Next token is the source start date.
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected source start date.");
//      }
//      String sourceStartStr = scanner.next();
//
//      // Next token should be "and"
//      if (!scanner.hasNext("and")) {
//        throw new Exception("Missing 'and' token in copy events between command.");
//      }
//      scanner.next(); // and
//
//      // Next token is the source end date.
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected source end date.");
//      }
//      String sourceEndStr = scanner.next();
//
//      // Next, expect the token "to"
//      if (!scanner.hasNext("to")) {
//        throw new Exception("Missing 'to' token in copy events between command.");
//      }
//      scanner.next(); // to
//
//      // Next, expect the "--target" token
//      if (!scanner.hasNext("--target")) {
//        throw new Exception("Missing '--target' token in copy events between command.");
//      }
//      scanner.next(); // --target
//
//      // Next token is the target calendar name.
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected target calendar name.");
//      }
//      String targetCalendarName = scanner.next();
//
//      // Next token is the target start date.
//      if (!scanner.hasNext()) {
//        throw new Exception("Expected target start date.");
//      }
//      String targetStartStr = scanner.next();
//
//      LocalDate sourceStart = LocalDate.parse(sourceStartStr, dateFormatter);
//      LocalDate sourceEnd = LocalDate.parse(sourceEndStr, dateFormatter);
//      LocalDate targetStart = LocalDate.parse(targetStartStr, dateFormatter);
//
//      multiCal.copyEventsBetween(sourceStart, sourceEnd, targetCalendarName, targetStart);
//    }

  static void processCopyEventsBetween(String command, MultiCalendarManager multiCal) throws Exception {
    // Expected format:
    // copy events between <yyyy-MM-dd> and <yyyy-MM-dd> to --target <calendarName> <yyyy-MM-dd>
    Scanner scanner = new Scanner(command);

    // Consume tokens: "copy", "events", "between"
    String token = scanner.next(); // "copy"
    token = scanner.next();        // "events"
    token = scanner.next();        // "between"

    // Next token: source start date
    String sourceStartStr = scanner.next();

    // Next token must be "and"
    token = scanner.next();
    if (!token.equalsIgnoreCase("and")) {
      throw new Exception("Missing 'and' token in copy events between command.");
    }

    // Next token: source end date
    String sourceEndStr = scanner.next();

    // Next token must be "to"
    token = scanner.next();

    System.out.println("token:"+token);

    if (!token.equalsIgnoreCase("to")) {
      throw new Exception("Missing 'to' token in copy events between command.");
    }

    // Next token must be "--target"
    token = scanner.next();

    System.out.println("token for target:"+token);

    if (!token.equalsIgnoreCase("--target")) {
      throw new Exception("Missing '--target' token in copy events between command.");
    }

    // Next token: target calendar name
    String targetCalendarName = scanner.next();

    System.out.println("targetCalendarName:"+targetCalendarName);

    // Next token: target start date
    String targetStartStr = scanner.next();

    LocalDate sourceStart = LocalDate.parse(sourceStartStr, dateFormatter);
    LocalDate sourceEnd = LocalDate.parse(sourceEndStr, dateFormatter);
    LocalDate targetStart = LocalDate.parse(targetStartStr, dateFormatter);

    multiCal.copyEventsBetween(sourceStart, sourceEnd, targetCalendarName, targetStart);
  }







  // --- The original event commands follow below ---
  static void processCreateEvent(String userCommand, ICalendarManager calendar) throws Exception {
    boolean autoDeclineFlag = false;
    if (userCommand.toLowerCase().contains("--autodecline")) {
      autoDeclineFlag = true;
      userCommand = userCommand.replace("--autodecline", "").trim();
    }
    // Ensure the command contains either " from " or " on "
    if (!userCommand.contains(" from ") && !userCommand.contains(" on ")) {
      throw new Exception("Invalid create event command format.");
    }

    if (userCommand.contains(" from ")) {
      // Timed event branch
      String[] splittedFrom = userCommand.split(" from ", 2);
      String rawName = splittedFrom[0].replace("create event", "").trim();
      String leftover = splittedFrom[1];
      if (!leftover.contains(" to ")) {
        throw new Exception("Invalid format: missing 'to' keyword.");
      }
      String[] splittedTo = leftover.split(" to ", 2);
      String rawStart = splittedTo[0].trim();
      String afterTo = splittedTo[1].trim();
      if (afterTo.toLowerCase().contains(" repeats ")) {
        String[] splittedRepeat = afterTo.split(" repeats ", 2);
        String rawEnd = splittedRepeat[0].trim();
        String repeatPart = splittedRepeat[1].trim();
        LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
        LocalDateTime endDateTime = LocalDateTime.parse(rawEnd, dateTimeFormatter);
        List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                rawName, startDateTime, endDateTime, repeatPart, false);
        for (CalendarEvent singleOccurrence : occurrences) {
          calendar.addEvent(singleOccurrence, autoDeclineFlag);
        }
        OutputHandler.getInstance().println("Recurring event created with " + occurrences.size() + " occurrences.");
      } else {
        String rawEnd = afterTo.trim();
        LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
        LocalDateTime endDateTime = LocalDateTime.parse(rawEnd, dateTimeFormatter);
        CalendarEvent eventObj = new CalendarEvent(rawName, startDateTime, endDateTime, false);
        calendar.addEvent(eventObj, autoDeclineFlag);
        OutputHandler.getInstance().println("Event created: " + eventObj);
      }
    } else if (userCommand.contains(" on ")) {
      // All-day event branch
      String[] splittedOn = userCommand.split(" on ", 2);
      String rawName = splittedOn[0].replace("create event", "").trim();
      String leftover = splittedOn[1].trim();

      // For safety, if a time is appended (like "T25:00"), take only the date part.
      String dateStr = leftover;
      if (dateStr.contains("T")) {
        dateStr = dateStr.substring(0, dateStr.indexOf("T")).trim();
      }

      if (leftover.toLowerCase().contains(" repeats ")) {
        String[] splittedRepeat = leftover.split(" repeats ", 2);
        dateStr = splittedRepeat[0].trim();
        if (dateStr.contains("T")) {
          dateStr = dateStr.substring(0, dateStr.indexOf("T")).trim();
        }
        String repeatPart = splittedRepeat[1].trim();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                rawName, startDateTime, endDateTime, repeatPart, true);
        for (CalendarEvent singleOccurrence : occurrences) {
          calendar.addEvent(singleOccurrence, autoDeclineFlag);
        }
        OutputHandler.getInstance().println("Recurring all-day event created with " + occurrences.size() + " occurrences.");
      } else {
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        CalendarEvent eventObj = new CalendarEvent(rawName, startDateTime, endDateTime, true);
        calendar.addEvent(eventObj, autoDeclineFlag);
        OutputHandler.getInstance().println("All-day event created: " + eventObj);
      }
    } else {
      throw new Exception("Invalid create event command format.");
    }
  }


  public static String getUpdateMessage(boolean updated) {
    return updated ? "Event updated successfully." : "Event not found or update failed.";
  }

  static void processEditCommand(String userCommand, ICalendarManager calendar, boolean isPlural) throws Exception {
    String prefix = isPlural ? "edit events" : "edit event";
    String leftover = userCommand.substring(prefix.length()).trim();
    if (leftover.contains(" with ")) {
      String[] splittedWith = leftover.split(" with ", 2);
      String beforeWith = splittedWith[0].trim();
      String newValue = splittedWith[1].trim();
      if (beforeWith.contains(" from ")) {
        String[] splittedFrom = beforeWith.split(" from ", 2);
        String firstPart = splittedFrom[0].trim();
        String afterFrom = splittedFrom[1].trim();
        if (!isPlural && !afterFrom.contains(" to ")) {
          throw new Exception("Missing 'to' clause for singular edit command.");
        }
        String[] tokens = firstPart.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String property = tokens[0].trim();
        String eventName = tokens[1].trim();
        if (!isPlural) {
          String[] splittedTo = afterFrom.split(" to ", 2);
          if (splittedTo.length < 2) {
            throw new Exception("Missing 'to' clause for singular edit command.");
          }
          String rawStart = splittedTo[0].trim();
          String rawEnd = splittedTo[1].trim();
          LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
          LocalDateTime endDateTime = LocalDateTime.parse(rawEnd, dateTimeFormatter);
          boolean updated = calendar.editSingleEvent(property, eventName, startDateTime, endDateTime, newValue);
          OutputHandler.getInstance().println(getUpdateMessage(updated));
        } else {
          LocalDateTime startDateTime = LocalDateTime.parse(afterFrom, dateTimeFormatter);
          int count = calendar.editEventsByStart(property, eventName, startDateTime, newValue);
          OutputHandler.getInstance().println(count + " event(s) updated starting from " + startDateTime);
        }
      } else {
        String[] tokens = beforeWith.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String property = tokens[0].trim();
        String eventName = tokens[1].trim();
        int count = calendar.editEventsByName(property, eventName, newValue);
        OutputHandler.getInstance().println(count + " event(s) updated with new " + property);
      }
    } else {
      throw new Exception("Edit command must contain 'with' clause.");
    }
  }

  static void processPrintEventsOn(String userCommand, ICalendarManager calendar) throws Exception {
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

  static void processPrintEventsRange(String userCommand, ICalendarManager calendar) throws Exception {
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
    String rawEnd = splittedTo[1].trim();
    LocalDateTime startDateTime = LocalDateTime.parse(rawStart, dateTimeFormatter);
    LocalDateTime endDateTime = LocalDateTime.parse(rawEnd, dateTimeFormatter);
    List<ICalendarEvent> eventsInRange = calendar.getEventsInRange(startDateTime, endDateTime);
    if (eventsInRange.isEmpty()) {
      OutputHandler.getInstance().println("No events found between " + startDateTime + " and " + endDateTime);
    } else {
      OutputHandler.getInstance().println("Events between " + startDateTime + " and " + endDateTime + ":");
      for (ICalendarEvent singleEvent : eventsInRange) {
        OutputHandler.getInstance().println(" - " + singleEvent);
      }
    }
  }

  static void processExportCal(String userCommand, ICalendarManager calendar) throws Exception {
    String[] tokens = userCommand.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid export command format.");
    }
    String fileName = tokens[2].trim();
    calendar.exportToCSV(fileName);
  }

  static void processExportGoogleCSV(String userCommand, ICalendarManager calendar) throws Exception {
    String[] tokens = userCommand.split(" ");
    if (tokens.length < 3) {
      throw new Exception("Invalid export googlecsv command format.");
    }
    String fileName = tokens[2].trim();
    calendar.exportToGoogleCSV(fileName);
  }

  static void processShowStatus(String userCommand, ICalendarManager calendar) throws Exception {
    String[] splittedOn = userCommand.split(" on ", 2);
    if (splittedOn.length < 2) {
      throw new Exception("Invalid command format for show status.");
    }
    String rawDateTime = splittedOn[1].trim();
    LocalDateTime dateTime = LocalDateTime.parse(rawDateTime, dateTimeFormatter);
    boolean busyFlag = calendar.isBusyAt(dateTime);
    OutputHandler.getInstance().println("Status at " + dateTime + ": " + (busyFlag ? "Busy" : "Available"));
  }
}


