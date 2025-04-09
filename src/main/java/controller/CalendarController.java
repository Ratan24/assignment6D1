package controller;

import model.*;
import util.*; // Import utilities
import view.IOutputHandler; // Import OutputHandler interface
import view.OutputHandler; // Import OutputHandler implementation
import java.io.*; // Import IO classes
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // Added missing import
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Original controller implementation, primarily for text-based interaction.
 * It directly interacts with the model (MultiCalendarManager).
 * Note: This controller does NOT use the event listener system effectively.
 */
public class CalendarController implements ICalendarController {

    private final MultiCalendarManager model; // Use the multi-calendar model
    private final IOutputHandler outputHandler; // Use the output handler

    // Importer/Exporter instances
    private final ICalendarImporter googleImporter = new GoogleCsvImporter();
    private final ICalendarExporter googleExporter = new GoogleCsvExporter();

    public CalendarController(MultiCalendarManager model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null.");
        }
        this.model = model;
        this.outputHandler = OutputHandler.getInstance(); // Get singleton instance
        // Does NOT register as a listener to the model
    }

    @Override
    public void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        outputHandler.println("Calendar App Interactive Mode (Original Controller). Type 'exit' to quit.");
        // Ensure default calendar exists if needed
        try {
            createDefaultCalendarIfNeeded();
        } catch (Exception e) {
             outputHandler.println("Warning: Could not ensure default calendar: " + e.getMessage());
        }

        while (true) {
            outputHandler.println("> "); // Changed from print to println
            String userCommand = scanner.nextLine();
            if (userCommand.equalsIgnoreCase("exit")) {
                outputHandler.println("Exiting.");
                break;
             }
             try {
                 // Pass the model instance to CommandParser
                 CommandParser.processCommand(userCommand, this.model);
             } catch (Exception e) {
                 outputHandler.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    @Override
    public void runHeadlessMode(String fileName) {
         try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String command;
            // Ensure default calendar exists if needed
            try {
                createDefaultCalendarIfNeeded();
            } catch (Exception e) {
                 outputHandler.println("Warning: Could not ensure default calendar: " + e.getMessage());
            }

            while ((command = br.readLine()) != null) {
                outputHandler.println("> " + command);
                if (command.equalsIgnoreCase("exit")) {
                    outputHandler.println("Exiting.");
                    break;
                 }
                 try {
                      // Pass the model instance to CommandParser
                     CommandParser.processCommand(command, this.model);
                 } catch (Exception e) {
                     outputHandler.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            outputHandler.println("Error reading file: " + e.getMessage());
        }
    }

    // --- Delegate methods to MultiCalendarManager or current CalendarManager ---

    @Override
    public List<String> getAvailableCalendarNames() {
       try {
            List<String> names = new ArrayList<>();
            for (CalendarManager cal : model.getAllCalendars()) {
                names.add(cal.getCalendarName());
            }
            return names;
        } catch (Exception e) {
            outputHandler.println("Error getting calendar names: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getCurrentCalendarName() {
       try {
            return model.getCurrentCalendar().getCalendarName();
        } catch (IllegalStateException e) {
            return null; // No calendar active
        } catch (Exception e) {
             outputHandler.println("Error getting current calendar name: " + e.getMessage());
             return null;
        }
    }

     @Override
    public String getCurrentCalendarTimezone() {
         try {
            return model.getCurrentCalendar().getTimeZone().getId();
        } catch (IllegalStateException e) {
            return null; // No calendar active
        } catch (Exception e) {
             outputHandler.println("Error getting current calendar timezone: " + e.getMessage());
             return null;
        }
    }

    @Override
    public void createCalendar(String name, String timezone) throws Exception {
        // Directly call model method
        model.createCalendar(name, timezone);
        // Output handled by model's fireModelEvent -> OutputHandler
    }

    @Override
    public void editCalendar(String oldName, String property, String newValue) throws Exception {
        // Directly call model method
        model.editCalendar(oldName, property, newValue);
         // Output handled by model's fireModelEvent -> OutputHandler
    }

    @Override
    public void switchCalendar(String name) throws Exception {
         // Directly call model method
        model.useCalendar(name);
         // Output handled by model's fireModelEvent -> OutputHandler
    }

     @Override
    public void createDefaultCalendarIfNeeded() throws Exception {
        if (model.getAllCalendars().isEmpty()) {
            String defaultName = "My Calendar";
            String defaultTimezone = java.time.ZoneId.systemDefault().getId();
            model.createCalendar(defaultName, defaultTimezone);
            model.useCalendar(defaultName); // Ensure it's active
        }
    }

    @Override
    public String[] getAvailableTimezones() {
        try {
            List<String> availableZones = new ArrayList<>(java.time.ZoneId.getAvailableZoneIds());
            Collections.sort(availableZones);
            return availableZones.toArray(new String[0]);
        } catch (Exception e) {
            outputHandler.println("Error getting available timezones: " + e.getMessage());
            return new String[0];
        }
    }

    @Override
    public List<ICalendarEvent> getEventsInRange(LocalDateTime start, LocalDateTime end) {
        try {
            return model.getCurrentCalendar().getEventsInRange(start, end);
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             return Collections.emptyList();
        } catch (Exception e) {
             outputHandler.println("Error getting events in range: " + e.getMessage());
             return Collections.emptyList();
        }
    }

    @Override
    public List<ICalendarEvent> getEventsOn(LocalDate date) {
         try {
            return model.getCurrentCalendar().getEventsOn(date);
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             return Collections.emptyList();
        } catch (Exception e) {
             outputHandler.println("Error getting events on date: " + e.getMessage());
             return Collections.emptyList();
        }
    }

    // @Override // Removed - Not in ICalendarController
    public boolean isBusyAt(LocalDateTime dateTime) {
         try {
            return model.getCurrentCalendar().isBusyAt(dateTime);
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             return false; // Assume not busy if no calendar? Or rethrow?
        } catch (Exception e) {
             outputHandler.println("Error checking busy status: " + e.getMessage());
             return false;
        }
    }

    @Override
    public void addEvent(ICalendarEvent event, boolean autoDecline) throws Exception {
        try {
            model.getCurrentCalendar().addEvent(event, autoDecline);
             // Output handled by model's fireModelEvent -> OutputHandler
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             throw e;
        } catch (Exception e) {
             outputHandler.println("Error adding event: " + e.getMessage());
             throw e;
        }
    }

    @Override
    public void createNewSingleEvent(String name, LocalDateTime start, LocalDateTime end, boolean isAllDay, String description, String location, boolean isPublic) throws Exception {
         try {
            CalendarEvent newEvent = new CalendarEvent(name, start, end, isAllDay);
            newEvent.setDescription(description);
            newEvent.setLocation(location);
            newEvent.setPublic(isPublic);
            model.getCurrentCalendar().addEvent(newEvent, true); // Assuming autoDecline true
             // Output handled by model's fireModelEvent -> OutputHandler
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             throw e;
        } catch (Exception e) {
             outputHandler.println("Error creating event: " + e.getMessage());
             throw e;
        }
    }

    @Override
    public void addRecurringEvent(String name, LocalDateTime start, LocalDateTime end, String recurrenceRule, boolean isAllDay, String description, String location, boolean isPublic) throws Exception {
        // This controller might just delegate to the model's generator and add loop,
        // similar to Enhanced controller, but without sophisticated event handling.
         try {
            List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                name, start, end, recurrenceRule, isAllDay
            );
            CalendarManager currentCal = model.getCurrentCalendar();
            int addedCount = 0;
            int conflictCount = 0;
            for (CalendarEvent occurrence : occurrences) {
                occurrence.setDescription(description);
                occurrence.setLocation(location);
                occurrence.setPublic(isPublic);
                try {
                    currentCal.addEvent(occurrence, true);
                    addedCount++;
                } catch (CalendarConflictException e) {
                    conflictCount++;
                    outputHandler.println("Skipping recurring instance due to conflict: " + occurrence.getEventName() + " at " + occurrence.getStart() + " - " + e.getMessage());
                }
            }
             outputHandler.println("Added " + addedCount + " of " + occurrences.size() + " recurring event instances. Conflicts: " + conflictCount);

        } catch (IllegalStateException e) {
            outputHandler.println("Error: No calendar selected.");
            throw e;
        } catch (Exception e) {
            outputHandler.println("Error adding recurring event: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean editSingleEvent(String property, String originalName, LocalDateTime originalStart, LocalDateTime originalEnd, String newValue) throws Exception {
        try {
            boolean success = model.getCurrentCalendar().editSingleEvent(property, originalName, originalStart, originalEnd, newValue);
             // Output handled by model's fireModelEvent -> OutputHandler
            return success;
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             throw e;
        } catch (Exception e) {
             outputHandler.println("Error editing event: " + e.getMessage());
             throw e;
        }
    }

    @Override
    public boolean deleteEvent(String eventName, LocalDateTime start, LocalDateTime end) throws Exception {
         try {
            boolean success = model.getCurrentCalendar().deleteEvent(eventName, start, end);
             // Output handled by model's fireModelEvent -> OutputHandler
            return success;
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected.");
             throw e;
        } catch (Exception e) {
             outputHandler.println("Error deleting event: " + e.getMessage());
             throw e;
        }
    }

    // --- Implement Import/Export ---

    @Override
    public int importFromGoogleCSV(String filePath) throws IOException, ImportException, CalendarConflictException {
        int importedCount = 0;
        List<ICalendarEvent> importedEvents;

        try (Reader reader = new FileReader(filePath)) {
            importedEvents = googleImporter.importEvents(reader);
        } catch (FileNotFoundException e) {
            outputHandler.println("Error: Import file not found: " + filePath);
            throw new IOException("Import file not found: " + filePath, e);
        } catch (IOException | ImportException e) {
            outputHandler.println("Error reading or parsing import file: " + e.getMessage());
            throw e;
        } catch (Exception e) {
             outputHandler.println("Unexpected error during import parsing: " + e.getMessage());
            throw new ImportException("Unexpected error during import parsing: " + e.getMessage(), e);
        }

        CalendarManager currentCal;
        try {
             currentCal = model.getCurrentCalendar();
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected to import into.");
             throw e;
        }

        int conflictCount = 0;
        for (ICalendarEvent event : importedEvents) {
            try {
                currentCal.addEvent(event, true); // Model handles conflict check
                importedCount++;
            } catch (CalendarConflictException e) {
                conflictCount++;
                outputHandler.println("Skipping imported event due to conflict: " + event.getEventName() + " - " + e.getMessage());
            } catch (Exception e) {
                 outputHandler.println("Error adding imported event '" + event.getEventName() + "': " + e.getMessage());
            }
        }
        outputHandler.println("Import finished. Added: " + importedCount + ", Conflicts/Skipped: " + conflictCount);
        return importedCount;
    }

    @Override
    public void exportToGoogleCSV(String filePath) throws IOException, ExportException {
        List<ICalendarEvent> eventsToExport;
        try {
            eventsToExport = model.getCurrentCalendar().getAllEvents();
        } catch (IllegalStateException e) {
             outputHandler.println("Error: No calendar selected to export from.");
            throw e;
        }

        try (Writer writer = new FileWriter(filePath)) {
            googleExporter.export(eventsToExport, writer);
            outputHandler.println("Exported to Google CSV: " + filePath);
        } catch (IOException | ExportException e) {
             outputHandler.println("Error during export: " + e.getMessage());
            throw e;
        } catch (Exception e) {
             outputHandler.println("Unexpected error during export: " + e.getMessage());
            throw new ExportException("Unexpected error during export: " + e.getMessage(), e);
        }
    }
}
