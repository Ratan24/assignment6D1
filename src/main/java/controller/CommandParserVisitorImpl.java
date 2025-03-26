//package controller;
//
//import model.CalendarManager;
//import model.ICalendarManager;
//import model.MultiCalendarManager;
//
//public class CommandParserVisitorImpl implements CommandParserVisitor {
//
//
//  @Override
//  public void process(CalendarManager manager, String command) throws Exception {
//    String lower = command.toLowerCase();
//    if (lower.startsWith("create event")) {
//      CommandParser.processCreateEvent(command, manager);
//    } else if (lower.startsWith("edit events")) {
//      CommandParser.processEditCommand(command, manager, true);
//    } else if (lower.startsWith("edit event")) {
//      CommandParser.processEditCommand(command, manager, false);
//    } else if (lower.startsWith("print events on")) {
//      CommandParser.processPrintEventsOn(command, manager);
//    } else if (lower.startsWith("print events from")) {
//      CommandParser.processPrintEventsRange(command, manager);
//    } else if (lower.startsWith("export cal")) {
//      CommandParser.processExportCal(command, manager);
//    } else if (lower.startsWith("export googlecsv")) {
//      CommandParser.processExportGoogleCSV(command, manager);
//    } else if (lower.startsWith("show status on")) {
//      CommandParser.processShowStatus(command, manager);
//    } else {
//      throw new Exception("Invalid command: " + command);
//    }
//  }
//
//  @Override
//  public void process(MultiCalendarManager manager, String command) throws Exception {
//    String lower = command.toLowerCase();
//    if (lower.startsWith("create calendar")) {
//      CommandParser.processCreateCalendar(command, manager);
//    } else if (lower.startsWith("edit calendar")) {
//      CommandParser.processEditCalendar(command, manager);
//    } else if (lower.startsWith("use calendar")) {
//      CommandParser.processUseCalendar(command, manager);
//    } else if (lower.startsWith("copy event")) {
//      CommandParser.processCopyEvent(command, manager);
//    } else if (lower.startsWith("copy events on")) {
//      CommandParser.processCopyEventsOn(command, manager);
//    } else if (lower.startsWith("copy events between")) {
//      CommandParser.processCopyEventsBetween(command, manager);
//    } else {
//      // If not a calendar-level command, delegate to the current calendar.
//      if (manager.getCurrentCalendar() == null) {
//        throw new Exception("No calendar is currently in use.");
//      }
//      CommandParser.processCommand(command, manager.getCurrentCalendar());
//    }
//  }
//}
