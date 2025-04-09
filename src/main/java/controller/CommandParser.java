package controller;

import java.io.FileReader; // Added for import
import java.io.FileWriter; // Added for export
import java.io.IOException;
import java.io.Reader; // Added for import
import java.io.Writer; // Added for export
import java.io.FileNotFoundException; // Added missing import
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import model.CalendarEvent;
import model.ICalendarEvent;
import model.ICalendarManager; // Use model interface
import model.MultiCalendarManager; // Use model class
import model.RecurringEventGenerator;
import view.OutputHandler;
import util.GoogleCsvImporter; // Use utilities directly
import util.GoogleCsvExporter; // Use utilities directly
import util.ICalendarImporter; // Added missing import
import util.ICalendarExporter; // Added missing import
import util.ImportException;
import util.ExportException;
import model.CalendarConflictException;
import model.EventNotFoundException;
import model.InvalidDataException;


/**
 * CommandParser interprets user commands and delegates work to the appropriate model methods.
 * It supports additional commands for multiple calendars and event copying.
 * Note: This interacts directly with the model for text/headless modes.
 */
public class CommandParser {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Importer/Exporter instances for direct use in parser methods
    private static final ICalendarImporter googleImporter = new GoogleCsvImporter();
    private static final ICalendarExporter googleExporter = new GoogleCsvExporter();

    /**
     * Processes a user command. Accepts commands that target either events or calendars.
     *
     * @param cmdInput The command string to process
     * @param mgrObj   The manager object (MultiCalendarManager or ICalendarManager)
     * @throws Exception If the command is invalid or execution fails
     */
    public static void processCommand(String cmdInput, Object mgrObj) throws Exception {
        // Check the type of the manager object passed
        if (mgrObj instanceof MultiCalendarManager) {
            processMultiCalendarCommand(cmdInput, (MultiCalendarManager) mgrObj);
        } else if (mgrObj instanceof ICalendarManager) {
            // This case might occur if a single CalendarManager is passed directly,
            // though typically MultiCalendarManager is used.
            processEventCommand(cmdInput, (ICalendarManager) mgrObj);
        }
        // Removed check for ICalendarController - Parser now only accepts model managers
         else {
            throw new Exception("Unsupported manager type passed to CommandParser: " + mgrObj.getClass().getName());
        }
    }

    /**
     * Processes commands specific to MultiCalendarManager or delegates event commands.
     */
    private static void processMultiCalendarCommand(String multiCmd, MultiCalendarManager multiMgr)
            throws Exception {
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
            // Delegate event-specific commands to the currently active calendar manager
            try {
                 processEventCommand(multiCmd, multiMgr.getCurrentCalendar());
            } catch (IllegalStateException e) {
                throw new Exception("No calendar is currently selected. Use 'use calendar --name <name>' first.", e);
            }
        }
    }

    /**
     * Processes commands specific to a single calendar (ICalendarManager).
     */
    private static void processEventCommand(String eventCmd, ICalendarManager singleCal)
            throws Exception {
        String lowerCmd = eventCmd.toLowerCase();
        if (lowerCmd.startsWith("create event")) {
            processCreateEvent(eventCmd, singleCal);
        } else if (lowerCmd.startsWith("edit events")) { // Plural
            processEditCommand(eventCmd, singleCal, true);
        } else if (lowerCmd.startsWith("edit event")) { // Singular
            processEditCommand(eventCmd, singleCal, false);
        } else if (lowerCmd.startsWith("delete event")) { // Added delete
             processDeleteCommand(eventCmd, singleCal);
        } else if (lowerCmd.startsWith("print events on")) {
            processPrintEventsOn(eventCmd, singleCal);
        } else if (lowerCmd.startsWith("print events from")) {
            processPrintEventsRange(eventCmd, singleCal);
        } else if (lowerCmd.startsWith("export googlecsv")) {
            processExportGoogleCSV(eventCmd, singleCal); // Pass ICalendarManager
        } else if (lowerCmd.startsWith("import googlecsv")) {
             processImportGoogleCSV(eventCmd, singleCal); // Pass ICalendarManager
        } else if (lowerCmd.startsWith("show status on")) {
            processShowStatus(eventCmd, singleCal); // Pass ICalendarManager
        }
         else {
            throw new Exception("Invalid command for the current calendar: " + eventCmd);
        }
    }

    /**
     * Processes a command to create a new calendar. (Uses MultiCalendarManager)
     */
    private static void processCreateCalendar(String cmdLine, MultiCalendarManager multiMgr)
            throws Exception {
        // ... (Implementation unchanged) ...
        String[] segments = cmdLine.split(" ");
        if (segments.length < 5) {
            throw new Exception("Invalid create calendar command format. Use: create calendar --name <name> --timezone <zone>");
        }
        String calendarName = null;
        String timeZone = null;
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
        // Output handled by model listener -> OutputHandler
    }

    /**
     * Processes a command to edit a calendar's properties. (Uses MultiCalendarManager)
     */
    private static void processEditCalendar(String cmdLine, MultiCalendarManager multiMgr)
            throws Exception {
        // ... (Implementation unchanged) ...
         String[] segments = cmdLine.split(" ");
        if (segments.length < 6) {
            throw new Exception("Invalid edit calendar command format. Use: edit calendar --name <name> --property <prop> <value>");
        }
        String calendarName = null;
        String fieldName = null;
        String newValue = null;

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
        // Output handled by model listener -> OutputHandler
    }

    /**
     * Processes a command to switch the active calendar. (Uses MultiCalendarManager)
     */
    private static void processUseCalendar(String cmdLine, MultiCalendarManager multiMgr)
            throws Exception {
        // ... (Implementation unchanged) ...
        String[] parts = cmdLine.split(" ");
        if (parts.length < 3) {
            throw new Exception("Invalid use calendar command format. Use: use calendar --name <name>");
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
        // Output handled by model listener -> OutputHandler
    }

     // --- Event Copying (Uses MultiCalendarManager) ---
     private static void processCopyEvent(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
         // ... (Implementation unchanged) ...
         String[] segments = cmdLine.split(" ");
         if (segments.length < 9) {
             throw new Exception("Invalid copy event command format.");
         }
         String eventLabel = null;
         LocalDateTime sourceDateTime = null;
         String targetCal = null;
         LocalDateTime targetDateTime = null;

         try {
             eventLabel = segments[2];
             if (!segments[3].equalsIgnoreCase("on")) throw new Exception("Missing 'on'");
             sourceDateTime = LocalDateTime.parse(segments[4], dateTimeFormatter);
             if (!segments[5].equalsIgnoreCase("--target")) throw new Exception("Missing --target");
             targetCal = segments[6];
             if (!segments[7].equalsIgnoreCase("to")) throw new Exception("Missing to");
             targetDateTime = LocalDateTime.parse(segments[8], dateTimeFormatter);
         } catch (Exception e) {
              throw new Exception("Invalid copy event command format or date/time.", e);
         }
         multiMgr.copyEvent(eventLabel, sourceDateTime, targetCal, targetDateTime);
         // Output handled by model listener -> OutputHandler
     }

     private static void processCopyEventsOn(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
         // ... (Implementation unchanged) ...
         Scanner scanner = new Scanner(cmdLine);
         try {
             scanner.next(); // copy
             scanner.next(); // events
             scanner.next(); // on
             String srcDateStr = scanner.next();
             if (!scanner.next().equalsIgnoreCase("--target")) throw new Exception("Missing '--target'");
             String targetCalName = scanner.next();
             if (!scanner.next().equalsIgnoreCase("to")) throw new Exception("Missing 'to'");
             String destDateStr = scanner.next();

             LocalDate srcDate = LocalDate.parse(srcDateStr, dateFormatter);
             LocalDate dstDate = LocalDate.parse(destDateStr, dateFormatter);
             multiMgr.copyEventsOn(srcDate, targetCalName, dstDate);
             // Output handled by model listener -> OutputHandler
         } catch (Exception e) {
             throw new Exception("Invalid copy events on command format or date.", e);
         } finally {
             scanner.close();
         }
     }

     private static void processCopyEventsBetween(String cmdLine, MultiCalendarManager multiMgr) throws Exception {
         // ... (Implementation unchanged) ...
         Scanner scanner = new Scanner(cmdLine);
         try {
             scanner.next(); // copy
             scanner.next(); // events
             scanner.next(); // between
             String startDateStr = scanner.next();
             if (!scanner.next().equalsIgnoreCase("and")) throw new Exception("Missing 'and'");
             String endDateStr = scanner.next();
             if (!scanner.next().equalsIgnoreCase("to")) throw new Exception("Missing 'to'");
             if (!scanner.next().equalsIgnoreCase("--target")) throw new Exception("Missing '--target'");
             String targetCal = scanner.next();
             String targetDateStr = scanner.next();

             LocalDate sourceStart = LocalDate.parse(startDateStr, dateFormatter);
             LocalDate sourceEnd = LocalDate.parse(endDateStr, dateFormatter);
             LocalDate targetStart = LocalDate.parse(targetDateStr, dateFormatter);
             multiMgr.copyEventsBetween(sourceStart, sourceEnd, targetCal, targetStart);
             // Output handled by model listener -> OutputHandler
         } catch (Exception e) {
              throw new Exception("Invalid copy events between command format or date.", e);
         } finally {
             scanner.close();
         }
     }

    /**
     * Processes commands to create events. (Uses ICalendarManager)
     */
    private static void processCreateEvent(String eventCmd, ICalendarManager singleCal)
            throws Exception {
        // ... (Parsing logic unchanged) ...
         boolean declineFlag = false; // autoDecline is effectively ignored by model now
         if (eventCmd.toLowerCase().contains("--autodecline")) {
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
                 int addedCount = 0;
                 for (CalendarEvent occurrence : repeatedEvents) {
                     try {
                         singleCal.addEvent(occurrence, declineFlag); // Call model directly
                         addedCount++;
                     } catch (model.CalendarConflictException e) {
                         OutputHandler.getInstance().println("Skipping recurring instance due to conflict: " + e.getMessage());
                     }
                 }
                 OutputHandler.getInstance()
                     .println("Recurring event created with " + addedCount + " occurrences.");
             } else {
                 LocalDateTime beginDateTime = LocalDateTime.parse(rawBegin, dateTimeFormatter);
                 LocalDateTime finishDateTime = LocalDateTime.parse(postTo, dateTimeFormatter);
                 CalendarEvent createdEvent = new CalendarEvent(rawLabel, beginDateTime, finishDateTime, false);
                 singleCal.addEvent(createdEvent, declineFlag); // Call model directly
                 // Output handled by model listener -> OutputHandler
             }
         } else if (eventCmd.contains(" on ")) {
             String[] partedOn = eventCmd.split(" on ", 2);
             String rawLabel = partedOn[0].replace("create event", "").trim();
             String leftover = partedOn[1].trim();

             String dateString = leftover;
             boolean repeats = leftover.toLowerCase().contains(" repeats ");
             String recRule = null;

             if (repeats) {
                 String[] partedRepeat = leftover.split(" repeats ", 2);
                 dateString = partedRepeat[0].trim();
                 recRule = partedRepeat[1].trim();
             }
             if (dateString.contains("T")) {
                  throw new Exception("Invalid format for all-day event: Date should not contain time (use YYYY-MM-DD).");
             }

             LocalDate dateVal = LocalDate.parse(dateString, dateFormatter);
             LocalDateTime beginDateTime = dateVal.atStartOfDay();
             LocalDateTime finishDateTime = dateVal.plusDays(1).atStartOfDay(); // Exclusive end

             if (recRule != null) {
                 List<CalendarEvent> repeatedEvents = RecurringEventGenerator.generateRecurringEvents(
                     rawLabel, beginDateTime, finishDateTime, recRule, true);
                 int addedCount = 0;
                 for (CalendarEvent occurrence : repeatedEvents) {
                      try {
                         singleCal.addEvent(occurrence, declineFlag); // Call model directly
                         addedCount++;
                     } catch (model.CalendarConflictException e) {
                         OutputHandler.getInstance().println("Skipping recurring instance due to conflict: " + e.getMessage());
                     }
                 }
                 OutputHandler.getInstance().println(
                     "Recurring all-day event created with " + addedCount + " occurrences.");
             } else {
                 CalendarEvent createdEvent = new CalendarEvent(rawLabel, beginDateTime, finishDateTime, true);
                 singleCal.addEvent(createdEvent, declineFlag); // Call model directly
                 // Output handled by model listener -> OutputHandler
             }
         } else {
             throw new Exception("Invalid create event command format.");
         }
    }

    /**
     * Processes commands to edit events. (Uses ICalendarManager)
     */
    private static void processEditCommand(String cmdLine, ICalendarManager singleCal, boolean isMulti) throws Exception {
        // ... (Implementation unchanged, calls singleCal.editSingleEvent/editEventsByStart/editEventsByName) ...
         String prefix = isMulti ? "edit events" : "edit event";
         String remainingStr = cmdLine.substring(prefix.length()).trim();
         if (!remainingStr.contains(" with ")) {
             throw new Exception("Edit command must contain 'with' clause.");
         }
         String[] partedWith = remainingStr.split(" with ", 2);
         String identificationPart = partedWith[0].trim();
         String newValue = partedWith[1].trim();

         String[] idTokens = identificationPart.split(" ", 2);
         if (idTokens.length < 2) {
             throw new Exception("Invalid edit command format. Specify property and event name.");
         }
         String property = idTokens[0].trim();
         String nameAndMaybeTimes = idTokens[1].trim();

         boolean updated = false;
         int count = 0;

         if (nameAndMaybeTimes.contains(" from ")) {
             String[] nameFromParts = nameAndMaybeTimes.split(" from ", 2);
             String eventName = nameFromParts[0].trim();
             String timePart = nameFromParts[1].trim();

             if (!isMulti) { // Singular edit requires "to"
                 if (!timePart.contains(" to ")) {
                     throw new Exception("Missing 'to' clause for singular edit command.");
                 }
                 String[] times = timePart.split(" to ", 2);
                 LocalDateTime start = LocalDateTime.parse(times[0].trim(), dateTimeFormatter);
                 LocalDateTime end = LocalDateTime.parse(times[1].trim(), dateTimeFormatter);
                 updated = singleCal.editSingleEvent(property, eventName, start, end, newValue); // Call model
                 OutputHandler.getInstance().println(updated ? "Event updated." : "Event not found or update failed.");
             } else { // Plural edit by start time
                 LocalDateTime start = LocalDateTime.parse(timePart, dateTimeFormatter);
                 count = singleCal.editEventsByStart(property, eventName, start, newValue); // Call model
                 OutputHandler.getInstance().println(count + " event(s) updated starting from " + start);
             }
         } else { // Edit by name (must be plural or singular without time)
             String eventName = nameAndMaybeTimes;
             if (!isMulti) {
                  throw new Exception("Singular edit command requires 'from ... to ...' to identify the event instance.");
             }
             count = singleCal.editEventsByName(property, eventName, newValue); // Call model
             OutputHandler.getInstance().println(count + " event(s) named '" + eventName + "' updated.");
         }
         // Output handled by model listener -> OutputHandler
    }

     /**
      * Processes commands to delete events. (Uses ICalendarManager)
      */
     private static void processDeleteCommand(String cmdLine, ICalendarManager singleCal) throws Exception {
          String prefix = "delete event";
          String remaining = cmdLine.substring(prefix.length()).trim();

          if (!remaining.contains(" from ") || !remaining.contains(" to ")) {
              throw new Exception("Invalid delete command format. Use: delete event <name> from <start> to <end>");
          }

          String[] fromParts = remaining.split(" from ", 2);
          String eventName = fromParts[0].trim();
          String timePart = fromParts[1].trim();

          String[] toParts = timePart.split(" to ", 2);
          if (toParts.length < 2) {
              throw new Exception("Invalid delete command format: missing 'to'.");
          }
          LocalDateTime start = LocalDateTime.parse(toParts[0].trim(), dateTimeFormatter);
          LocalDateTime end = LocalDateTime.parse(toParts[1].trim(), dateTimeFormatter);

          boolean deleted = singleCal.deleteEvent(eventName, start, end); // Call model
          // Output handled by model listener -> OutputHandler
          // OutputHandler.getInstance().println(deleted ? "Event deleted." : "Event not found.");
     }


    /**
     * Processes commands to print events. (Uses ICalendarManager)
     */
    private static void processPrintEventsOn(String cmdLine, ICalendarManager singleCal)
            throws Exception {
         String[] partedOn = cmdLine.split(" on ", 2);
         if (partedOn.length < 2) {
             throw new Exception("Invalid command format for printing events. Use: print events on YYYY-MM-DD");
         }
         String dateStr = partedOn[1].trim();
         LocalDate dateVal = LocalDate.parse(dateStr, dateFormatter);
         List<ICalendarEvent> dayEvents = singleCal.getEventsOn(dateVal); // Call model
         if (dayEvents.isEmpty()) {
             OutputHandler.getInstance().println("No events found on " + dateVal);
         } else {
             OutputHandler.getInstance().println("Events on " + dateVal + ":");
             for (ICalendarEvent eventIt : dayEvents) {
                 OutputHandler.getInstance().println(" - " + eventIt);
             }
         }
    }

    /**
     * Processes commands to print events in a range. (Uses ICalendarManager)
     */
    private static void processPrintEventsRange(String cmdLine, ICalendarManager singleCal)
            throws Exception {
         String[] partedFrom = cmdLine.split(" from ", 2);
         if (partedFrom.length < 2) {
             throw new Exception("Invalid command format for printing events in range. Use: print events from <start> to <end>");
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
         List<ICalendarEvent> rangeEvents = singleCal.getEventsInRange(beginDateTime, finishDateTime); // Call model
         if (rangeEvents.isEmpty()) {
             OutputHandler.getInstance()
                     .println("No events found between " + beginDateTime + " and " + finishDateTime);
         } else {
             OutputHandler.getInstance()
                     .println("Events between " + beginDateTime + " and " + finishDateTime + ":");
             for (ICalendarEvent eventIt : rangeEvents) {
                 OutputHandler.getInstance().println(" - " + eventIt);
             }
         }
    }

    /**
     * Processes command to export to Google CSV. (Uses ICalendarManager and utility)
     */
    private static void processExportGoogleCSV(String cmdLine, ICalendarManager singleCal)
            throws Exception {
        String[] segments = cmdLine.split(" ");
        if (segments.length < 3) {
            throw new Exception("Invalid export googlecsv command format. Use: export googlecsv <filename>");
        }
        String fileName = segments[2].trim();
        try (Writer writer = new FileWriter(fileName)) {
            List<ICalendarEvent> events = singleCal.getAllEvents(); // Get events from model
            googleExporter.export(events, writer); // Use utility
            OutputHandler.getInstance().println("Exported to Google CSV: " + fileName);
        } catch (IOException | ExportException e) {
             throw new Exception("Error during Google CSV export: " + e.getMessage(), e);
        } catch (Exception e) { // Catch other unexpected errors
             throw new Exception("Unexpected error during export: " + e.getMessage(), e);
        }
    }

     /**
     * Processes command to import from Google CSV. (Uses ICalendarManager and utility)
     */
     private static void processImportGoogleCSV(String cmdLine, ICalendarManager singleCal)
             throws Exception {
         String[] segments = cmdLine.split(" ");
         if (segments.length < 3) {
             throw new Exception("Invalid import googlecsv command format. Use: import googlecsv <filename>");
         }
         String fileName = segments[2].trim();
         int importedCount = 0;
         int conflictCount = 0;
         try (Reader reader = new FileReader(fileName)) {
             List<ICalendarEvent> importedEvents = googleImporter.importEvents(reader); // Use utility
             for (ICalendarEvent event : importedEvents) {
                 try {
                     singleCal.addEvent(event, true); // Add to model
                     importedCount++;
                 } catch (CalendarConflictException e) {
                     conflictCount++;
                     OutputHandler.getInstance().println("Skipping imported event due to conflict: " + event.getEventName() + " - " + e.getMessage());
                 }
             }
             OutputHandler.getInstance().println("Import finished. Added: " + importedCount + ", Conflicts/Skipped: " + conflictCount);
         } catch (FileNotFoundException e) {
             throw new Exception("Error: Import file not found: " + fileName, e);
         } catch (IOException | ImportException e) {
             throw new Exception("Error reading or parsing import file: " + e.getMessage(), e);
         } catch (Exception e) { // Catch other unexpected errors
             throw new Exception("Unexpected error during import: " + e.getMessage(), e);
         }
     }


    /**
     * Processes command to show busy status. (Uses ICalendarManager)
     */
    private static void processShowStatus(String cmdLine, ICalendarManager singleCal)
            throws Exception {
         String[] partedOn = cmdLine.split(" on ", 2);
         if (partedOn.length < 2) {
             throw new Exception("Invalid command format for show status. Use: show status on YYYY-MM-DDTHH:mm");
         }
         String rawTimestamp = partedOn[1].trim();
         LocalDateTime dateTimeCheck = LocalDateTime.parse(rawTimestamp, dateTimeFormatter);
         boolean busyFlag = singleCal.isBusyAt(dateTimeCheck); // Call ICalendarManager method
         OutputHandler.getInstance()
                 .println("Status at " + dateTimeCheck + ": " + (busyFlag ? "Busy" : "Available"));
    }
}
//
//</final_file_content>
//
//IMPORTANT: For any future changes to this file, use the final_file_content shown above as your reference. This content reflects the current state of the file, including any auto-formatting (e.g., if you used single quotes but the formatter converted them to double quotes). Always base your SEARCH/REPLACE operations on this final version to ensure accuracy.
//
//
//
//New problems detected after saving the file:
//src/main/java/controller/CommandParser.java
//- [Java Error] Line 64: The method getModel() is undefined for the type CalendarController<environment_details>
//# VSCode Visible Files
//src/main/java/controller/CommandParser.java
//
//# VSCode Open Tabs
//src/main/res/USEME.md
//src/main/java/view/ColorManager.java
//src/main/java/view/MonthViewPanel.java
//src/main/java/view/FileOperationDialog.java
//src/main/java/view/ICalendarGUI.java
//src/main/java/view/CreateCalendarDialog.java
//src/main/java/model/ModelEvent.java
//src/main/java/model/IModelEventListener.java
//src/main/java/controller/IEnhancedCalendarController.java
//src/main/java/calendar/CalendarApp.java
//src/main/java/model/CalendarConflictException.java
//src/main/java/model/EventNotFoundException.java
//src/main/java/model/InvalidDataException.java
//src/main/java/util/ICalendarExporter.java
//src/main/java/util/ExportException.java
//src/main/java/util/ICalendarImporter.java
//src/main/java/util/ImportException.java
//src/main/java/util/GoogleCsvExporter.java
//src/main/java/model/ICalendarManager.java
//src/main/java/controller/EnhancedCalendarController.java
//src/main/java/view/EventDialog.java
//src/main/java/view/DayEventsDialog.java
//pom.xml
//src/test/java/tests/CommandParserTest.java
//src/test/java/tests/CommandParserCreateCalendarTest.java
//src/test/java/tests/CommandParserEditCalendarTest.java
//src/test/java/tests/CalendarControllerInteractiveTest.java
//src/main/java/util/GoogleCsvImporter.java
//src/test/java/tests/CalendarManagerTest.java
//src/main/java/model/RecurringEventGenerator.java
//src/main/java/view/CalendarGUI.java
//src/main/java/model/CalendarManager.java
//src/main/java/model/MultiCalendarManager.java
//src/test/java/tests/MultiCalendarTest.java
//src/test/java/tests/CommandParserMultiTest.java
//src/test/java/tests/CalendarAppTest.java
//src/test/java/tests/EnhancedCalendarControllerTest.java
//src/test/java/tests/CalendarAppMainTest.java
//src/main/java/controller/ICalendarController.java
//src/main/java/controller/CalendarController.java
//src/main/java/controller/CommandParser.java
//src/main/java/model/CalendarEvent.java
//
//# Current Time
//4/8/2025, 9:10:11 PM (America/New_York, UTC-4:00)
//
//# Current Mode
//ACT MODE
//</environment_details>
