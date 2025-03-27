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
 * It supports additional commands for multiple calendars and event copying.
 */
public class CommandParser {
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Processes a user command. Accepts commands that target either events or calendars.
   *
   * @param cmdInput The command string to process
   * @param mgrObj The manager object (MultiCalendarManager or ICalendarManager)
   * @throws Exception If the command is invalid or execution fails
   */
  public static void processCommand(String cmdInput, Object mgrObj) throws Exception {
    if (mgrObj instanceof MultiCalendarManager) {
      processMultiCalendarCommand(cmdInput, (MultiCalendarManager) mgrObj);
    } else if (mgrObj instanceof ICalendarManager) {
      processEventCommand(cmdInput, (ICalendarManager) mgrObj);
    } else {
      throw new Exception("Unsupported manager type.");
    }
  }

  private static void processMultiCalendarCommand(String multiCmd, MultiCalendarManager multiMgr) throws Exception {
    String lowerCmd = multiCmd.toLowerCase();
    if (lowerCmd.startsWith("create calendar")) {
      processCreateCalendar(multiCmd, multiMgr);
    } else if (lowerCmd.startsWith("edit calendar")) {
      processEditCalendar(multiCmd, multiMgr);
    } else if (lowerCmd.startsWith("use calendar")) {
      processUseCalendar(multiCmd, multiMgr);
    } else if (lowerCmd.startsWith("copy events on")) {
      processCopyEventsOn(multiCmd, multiMgr);
    } else if (lowerCmd.startsWith("copy events between")) {
      processCopyEventsBetween(multiCmd, multiMgr);
    } else if (lowerCmd.startsWith("copy event")) {
      processCopyEvent(multiCmd, multiMgr);
    } else {
      processEventCommand(multiCmd, multiMgr.getCurrentCalendar());
    }
  }

  private static void processEventCommand(String eventCmd, ICalendarManager singleCal) throws Exception {
    String lowerCmd = eventCmd.toLowerCase();
    if (lowerCmd.startsWith("create event")) {
      processCreateEvent(eventCmd, singleCal);
    } else if (lowerCmd.startsWith("edit events")) {
      processEditCommand(eventCmd, singleCal, true);
    } else if (lowerCmd.startsWith("edit event")) {
      processEditCommand(eventCmd, singleCal, false);
    } else if (lowerCmd.startsWith("print events on")) {
      processPrintEventsOn(eventCmd, singleCal);
    } else if (lowerCmd.startsWith("print events from")) {
      processPrintEventsRange(eventCmd, singleCal);
    } else if (lowerCmd.startsWith("export cal")) {
      processExportCal(eventCmd, singleCal);
    } else if (lowerCmd.startsWith("export googlecsv")) {
      processExportGoogleCSV(eventCmd, singleCal);
    } else if (lowerCmd.startsWith("show status on")) {
      processShowStatus(eventCmd, singleCal);
    } else {
      throw new Exception("Invalid command: " + eventCmd);
    }
  }

  /**
   * Processes a command to create a new calendar.
   *
   * @param cmdLine The command string to process
   * @param multiMgr The MultiCalendarManager instance
   * @throws Exception If the command format is invalid
   */
  public static void processCreateCalendar(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
    String[] segments = cmdLine.split(" ");
    if (segments.length < 5) {
      throw new Exception("Invalid create calendar command format.");
    }
    String calendarName = null, timeZone = null;
    for (int i = 2; i < segments.length; i++) {
      if (segments[i].equalsIgnoreCase("--name") && i + 1 < segments.length) {
        calendarName = segments[++i];
      } else if (segments[i].equalsIgnoreCase("--timezone") && i + 1 < segments.length) {
        timeZone = segments[++i];
      }
    }
    if (calendarName == null || timeZone == null) {
      throw new Exception("Calendar name and timezone must be provided.");
    }
    multiMgr.createCalendar(calendarName, timeZone);
  }

  /**
   * Processes a command to edit a calendar's properties.
   *
   * @param cmdLine The command string to process
   * @param multiMgr The MultiCalendarManager instance
   * @throws Exception If the command format is invalid
   */
  public static void processEditCalendar(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
    String[] segments = cmdLine.split(" ");
    if (segments.length < 6) {
      throw new Exception("Invalid edit calendar command format.");
    }
    String calendarName = null, fieldName = null, newValue = null;
    for (int i = 2; i < segments.length; i++) {
      if (segments[i].equalsIgnoreCase("--name") && i + 1 < segments.length) {
        calendarName = segments[++i];
      } else if (segments[i].equalsIgnoreCase("--property") && i + 1 < segments.length) {
        fieldName = segments[++i];
        if (i + 1 < segments.length) {
          newValue = segments[++i];
        }
      }
    }
    if (calendarName == null || fieldName == null || newValue == null) {
      throw new Exception("Invalid edit calendar command parameters.");
    }
    multiMgr.editCalendar(calendarName, fieldName, newValue);
  }

  private static void processUseCalendar(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
    String[] parts = cmdLine.split(" ");
    if (parts.length < 3) {
      throw new Exception("Invalid use calendar command format.");
    }
    String calendarName = null;
    for (int i = 0; i < parts.length; i++) {
      if (parts[i].equalsIgnoreCase("--name") && i + 1 < parts.length) {
        calendarName = parts[++i];
      }
    }
    if (calendarName == null) {
      throw new Exception("Calendar name must be provided.");
    }
    multiMgr.useCalendar(calendarName);
  }

  private static void processCopyEvent(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
    String[] segments = cmdLine.split(" ");
    if (segments.length < 8) {
      throw new Exception("Invalid copy event command format.");
    }

    String eventLabel = segments[2];
    LocalDateTime sourceDateTime = LocalDateTime.parse(segments[4], dateTimeFormatter);
    String targetCal = segments[6];
    LocalDateTime targetDateTime = LocalDateTime.parse(segments[8], dateTimeFormatter);
    multiMgr.copyEvent(eventLabel, sourceDateTime, targetCal, targetDateTime);
  }

  private static void processCopyEventsOn(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
    Scanner scanner = new Scanner(cmdLine);

    scanner.next();
    scanner.next();
    scanner.next();

    String srcDateStr = scanner.next();

    String token = scanner.next();
    if (!token.equalsIgnoreCase("--target")) {
      throw new Exception("Missing '--target' token in copy events on command.");
    }

    String targetCalName = scanner.next();

    token = scanner.next();
    if (!token.equalsIgnoreCase("to")) {
      throw new Exception("Missing 'to' token in copy events on command.");
    }

    String destDateStr = scanner.next();

    LocalDate srcDate = LocalDate.parse(srcDateStr, dateFormatter);
    LocalDate dstDate = LocalDate.parse(destDateStr, dateFormatter);

    multiMgr.copyEventsOn(srcDate, targetCalName, dstDate);
  }

  private static void processCopyEventsBetween(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
    Scanner scanner = new Scanner(cmdLine);

    scanner.next();
    scanner.next();
    scanner.next();

    String startDateStr = scanner.next();

    String nextTok = scanner.next();
    if (!nextTok.equalsIgnoreCase("and")) {
      throw new Exception("Missing 'and' token in copy events between command.");
    }

    String endDateStr = scanner.next();

    nextTok = scanner.next();
    if (!nextTok.equalsIgnoreCase("to")) {
      throw new Exception("Missing 'to' token in copy events between command.");
    }

    nextTok = scanner.next();
    if (!nextTok.equalsIgnoreCase("--target")) {
      throw new Exception("Missing '--target' token in copy events between command.");
    }

    String targetCal = scanner.next();
    String targetDateStr = scanner.next();

    LocalDate sourceStart = LocalDate.parse(startDateStr, dateFormatter);
    LocalDate sourceEnd = LocalDate.parse(endDateStr, dateFormatter);
    LocalDate targetStart = LocalDate.parse(targetDateStr, dateFormatter);

    multiMgr.copyEventsBetween(sourceStart, sourceEnd, targetCal, targetStart);
  }

  private static void processCreateEvent(String eventCmd, ICalendarManager singleCal) throws Exception {
    boolean declineFlag = false;
    if (eventCmd.toLowerCase().contains("--autodecline")) {
      declineFlag = true;
      eventCmd = eventCmd.replace("--autodecline", "").trim();
    }
    if (!eventCmd.contains(" from ") && !eventCmd.contains(" on ")) {
      throw new Exception("Invalid create event command format.");
    }

    if (eventCmd.contains(" from ")) {
      String[] partedFrom = eventCmd.split(" from ", 2);
      String rawLabel = partedFrom[0].replace("create event", "").trim();
      String remainStr = partedFrom[1];
      if (!remainStr.contains(" to ")) {
        throw new Exception("Invalid format: missing 'to' keyword.");
      }
      String[] partedTo = remainStr.split(" to ", 2);
      String rawBegin = partedTo[0].trim();
      String postTo = partedTo[1].trim();
      if (postTo.toLowerCase().contains(" repeats ")) {
        String[] partedRepeat = postTo.split(" repeats ", 2);
        String rawFinish = partedRepeat[0].trim();
        String recRule = partedRepeat[1].trim();
        LocalDateTime beginDateTime = LocalDateTime.parse(rawBegin, dateTimeFormatter);
        LocalDateTime finishDateTime = LocalDateTime.parse(rawFinish, dateTimeFormatter);
        List<CalendarEvent> repeatedEvents = RecurringEventGenerator.generateRecurringEvents(
            rawLabel, beginDateTime, finishDateTime, recRule, false);
        for (CalendarEvent occurrence : repeatedEvents) {
          singleCal.addEvent(occurrence, declineFlag);
        }
        OutputHandler.getInstance().println("Recurring event created with " + repeatedEvents.size() + " occurrences.");
      } else {
        LocalDateTime beginDateTime = LocalDateTime.parse(rawBegin, dateTimeFormatter);
        LocalDateTime finishDateTime = LocalDateTime.parse(postTo, dateTimeFormatter);
        CalendarEvent createdEvent = new CalendarEvent(rawLabel, beginDateTime, finishDateTime, false);
        singleCal.addEvent(createdEvent, declineFlag);
        OutputHandler.getInstance().println("Event created: " + createdEvent);
      }
    } else if (eventCmd.contains(" on ")) {
      String[] partedOn = eventCmd.split(" on ", 2);
      String rawLabel = partedOn[0].replace("create event", "").trim();
      String leftover = partedOn[1].trim();

      String dateString = leftover;
      if (dateString.contains("T")) {
        dateString = dateString.substring(0, dateString.indexOf("T")).trim();
      }

      if (leftover.toLowerCase().contains(" repeats ")) {
        String[] partedRepeat = leftover.split(" repeats ", 2);
        dateString = partedRepeat[0].trim();
        if (dateString.contains("T")) {
          dateString = dateString.substring(0, dateString.indexOf("T")).trim();
        }
        String recRule = partedRepeat[1].trim();
        LocalDate dateVal = LocalDate.parse(dateString, dateFormatter);
        LocalDateTime beginDateTime = dateVal.atStartOfDay();
        LocalDateTime finishDateTime = dateVal.plusDays(1).atStartOfDay();
        List<CalendarEvent> repeatedEvents = RecurringEventGenerator.generateRecurringEvents(
            rawLabel, beginDateTime, finishDateTime, recRule, true);
        for (CalendarEvent occurrence : repeatedEvents) {
          singleCal.addEvent(occurrence, declineFlag);
        }
        OutputHandler.getInstance().println("Recurring all-day event created with " + repeatedEvents.size() + " occurrences.");
      } else {
        LocalDate dateVal = LocalDate.parse(dateString, dateFormatter);
        LocalDateTime beginDateTime = dateVal.atStartOfDay();
        LocalDateTime finishDateTime = dateVal.plusDays(1).atStartOfDay();
        CalendarEvent createdEvent = new CalendarEvent(rawLabel, beginDateTime, finishDateTime, true);
        singleCal.addEvent(createdEvent, declineFlag);
        OutputHandler.getInstance().println("All-day event created: " + createdEvent);
      }
    } else {
      throw new Exception("Invalid create event command format.");
    }
  }

  private static String getUpdateMessage(boolean updatedFlag) {
    return updatedFlag ? "Event updated successfully." : "Event not found or update failed.";
  }

  private static void processEditCommand(String cmdLine, ICalendarManager singleCal, boolean isMulti) throws Exception {
    String prefix = isMulti ? "edit events" : "edit event";
    String remainingStr = cmdLine.substring(prefix.length()).trim();
    if (remainingStr.contains(" with ")) {
      String[] partedWith = remainingStr.split(" with ", 2);
      String beforeWith = partedWith[0].trim();
      String updatedVal = partedWith[1].trim();
      if (beforeWith.contains(" from ")) {
        String[] partedFrom = beforeWith.split(" from ", 2);
        String firstFragment = partedFrom[0].trim();
        String postFrom = partedFrom[1].trim();
        if (!isMulti && !postFrom.contains(" to ")) {
          throw new Exception("Missing 'to' clause for singular edit command.");
        }
        String[] tokens = firstFragment.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String attribute = tokens[0].trim();
        String labelEvent = tokens[1].trim();
        if (!isMulti) {
          String[] partedTo = postFrom.split(" to ", 2);
          if (partedTo.length < 2) {
            throw new Exception("Missing 'to' clause for singular edit command.");
          }
          String rawBegin = partedTo[0].trim();
          String rawFinish = partedTo[1].trim();
          LocalDateTime beginDateTime = LocalDateTime.parse(rawBegin, dateTimeFormatter);
          LocalDateTime finishDateTime = LocalDateTime.parse(rawFinish, dateTimeFormatter);
          boolean updated = singleCal.editSingleEvent(attribute, labelEvent, beginDateTime, finishDateTime, updatedVal);
          OutputHandler.getInstance().println(getUpdateMessage(updated));
        } else {
          LocalDateTime beginDateTime = LocalDateTime.parse(postFrom, dateTimeFormatter);
          int count = singleCal.editEventsByStart(attribute, labelEvent, beginDateTime, updatedVal);
          OutputHandler.getInstance().println(count + " event(s) updated starting from " + beginDateTime);
        }
      } else {
        String[] tokens = beforeWith.split(" ", 2);
        if (tokens.length < 2) {
          throw new Exception("Invalid edit command format.");
        }
        String attribute = tokens[0].trim();
        String labelEvent = tokens[1].trim();
        int count = singleCal.editEventsByName(attribute, labelEvent, updatedVal);
        OutputHandler.getInstance().println(count + " event(s) updated with new " + attribute);
      }
    } else {
      throw new Exception("Edit command must contain 'with' clause.");
    }
  }

  private static void processPrintEventsOn(String cmdLine, ICalendarManager singleCal) throws Exception {
    String[] partedOn = cmdLine.split(" on ", 2);
    if (partedOn.length < 2) {
      throw new Exception("Invalid command format for printing events.");
    }
    String dateStr = partedOn[1].trim();
    LocalDate dateVal = LocalDate.parse(dateStr, dateFormatter);
    List<ICalendarEvent> dayEvents = singleCal.getEventsOn(dateVal);
    if (dayEvents.isEmpty()) {
      OutputHandler.getInstance().println("No events found on " + dateVal);
    } else {
      OutputHandler.getInstance().println("Events on " + dateVal + ":");
      for (ICalendarEvent eventIt : dayEvents) {
        OutputHandler.getInstance().println(" - " + eventIt);
      }
    }
  }

  private static void processPrintEventsRange(String cmdLine, ICalendarManager singleCal) throws Exception {
    String[] partedFrom = cmdLine.split(" from ", 2);
    if (partedFrom.length < 2) {
      throw new Exception("Invalid command format for printing events in range.");
    }
    String remainStr = partedFrom[1].trim();
    if (!remainStr.contains(" to ")) {
      throw new Exception("Missing 'to' clause in range query.");
    }
    String[] partedTo = remainStr.split(" to ", 2);
    String rawBegin = partedTo[0].trim();
    String rawFinish = partedTo[1].trim();
    LocalDateTime beginDateTime = LocalDateTime.parse(rawBegin, dateTimeFormatter);
    LocalDateTime finishDateTime = LocalDateTime.parse(rawFinish, dateTimeFormatter);
    List<ICalendarEvent> rangeEvents = singleCal.getEventsInRange(beginDateTime, finishDateTime);
    if (rangeEvents.isEmpty()) {
      OutputHandler.getInstance().println("No events found between " + beginDateTime + " and " + finishDateTime);
    } else {
      OutputHandler.getInstance().println("Events between " + beginDateTime + " and " + finishDateTime + ":");
      for (ICalendarEvent eventIt : rangeEvents) {
        OutputHandler.getInstance().println(" - " + eventIt);
      }
    }
  }

  private static void processExportCal(String cmdLine, ICalendarManager singleCal) throws Exception {
    String[] segments = cmdLine.split(" ");
    if (segments.length < 3) {
      throw new Exception("Invalid export command format.");
    }
    String fileName = segments[2].trim();
    singleCal.exportToCSV(fileName);
  }

  private static void processExportGoogleCSV(String cmdLine, ICalendarManager singleCal) throws Exception {
    String[] segments = cmdLine.split(" ");
    if (segments.length < 3) {
      throw new Exception("Invalid export googlecsv command format.");
    }
    String fileName = segments[2].trim();
    singleCal.exportToGoogleCSV(fileName);
  }

  private static void processShowStatus(String cmdLine, ICalendarManager singleCal) throws Exception {
    String[] partedOn = cmdLine.split(" on ", 2);
    if (partedOn.length < 2) {
      throw new Exception("Invalid command format for show status.");
    }
    String rawTimestamp = partedOn[1].trim();
    LocalDateTime dateTimeCheck = LocalDateTime.parse(rawTimestamp, dateTimeFormatter);
    boolean busyFlag = singleCal.isBusyAt(dateTimeCheck);
    OutputHandler.getInstance().println("Status at " + dateTimeCheck + ": " + (busyFlag ? "Busy" : "Available"));
  }
}