package controller;

import model.*;
import view.*;
import util.*; // Import utility package for Importer/Exporter/Exceptions
import java.io.*; // Need more IO classes (Writer, Reader, FileWriter, FileReader)
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Enhanced implementation of the calendar controller that properly
 * implements the MVC pattern by handling model events and mediating
 * between model and view. It uses an event-based system for communication
 * from the model, ensuring the model remains independent of the view.
 */
public class EnhancedCalendarController implements IEnhancedCalendarController {

    private final MultiCalendarManager calendarManager; // The model instance
    private final IOutputHandler outputHandler; // Handles output (console, status bar, etc.)
    private ICalendarGUI gui; // Reference to the GUI view, if applicable

    // Importer/Exporter instances (could be injected or created here)
    private final ICalendarImporter googleImporter = new GoogleCsvImporter();
    private final ICalendarExporter googleExporter = new GoogleCsvExporter();

    /**
     * Constructs a new enhanced controller.
     *
     * @param calendarManager The model (MultiCalendarManager) to control.
     */
    public EnhancedCalendarController(MultiCalendarManager calendarManager) {
        if (calendarManager == null) {
            throw new IllegalArgumentException("CalendarManager cannot be null");
        }
        this.calendarManager = calendarManager;
        // Use the singleton OutputHandler for now, could be made injectable later
        this.outputHandler = OutputHandler.getInstance();
    }

    /**
     * Initializes the controller by registering itself as a listener to the model.
     * This should be called after the controller is constructed.
     */
    @Override
    public void initialize() {
        // Register this controller to listen for events from the model
        this.calendarManager.addModelEventListener(this);
        // Ensure a default calendar exists if needed (now handled via event/controller)
        try {
            createDefaultCalendarIfNeeded();
        } catch (Exception e) {
            // Notify about the error during initialization
            notifyError("Error creating default calendar: " + e.getMessage());
        }
    }

    /**
     * Sets the GUI view associated with this controller.
     * This allows the controller to trigger updates on the GUI.
     *
     * @param gui The ICalendarGUI instance.
     */
    public void setGUI(ICalendarGUI gui) {
        this.gui = gui;
    }

    /**
     * Handles events received from the model. This is the core of the
     * Observer pattern implementation for Model -> Controller communication.
     *
     * @param event The event object containing details about the model change.
     */
    @Override
    public void onModelEvent(ModelEvent event) {
        // Log the message from the event using the output handler
        if (event.getMessage() != null && !event.getMessage().isEmpty()) {
            outputHandler.println(event.getMessage());
        }

        // Based on the event type, potentially update the GUI
        switch (event.getType()) {
            case CALENDAR_CREATED:
            case CALENDAR_EDITED:
            case CALENDAR_SWITCHED:
            case EVENT_ADDED:
            case EVENT_EDITED:
            case EVENT_DELETED:
            case IMPORT_COMPLETED: // Refresh view after these significant changes
                if (gui != null) {
                    // Use SwingUtilities.invokeLater if GUI updates are not thread-safe
                    javax.swing.SwingUtilities.invokeLater(gui::updateView);
                }
                break;

            case EXPORT_COMPLETED:
                // Maybe show a confirmation dialog or status update
                // outputHandler.println is already called above
                 if (gui != null) {
                    // Example: Show simple status message
                    final String statusMessage = event.getMessage() != null ? event.getMessage() : "Export completed.";
                    // Assuming GUI has a setStatus method or similar
                    // javax.swing.SwingUtilities.invokeLater(() -> gui.setStatus(statusMessage));
                 }
                break;

            case ERROR:
                // Error message already printed by outputHandler above
                if (gui != null) {
                    // Use SwingUtilities.invokeLater if GUI updates are not thread-safe
                    final String errorMessage = event.getMessage() != null ? event.getMessage() : "An unknown error occurred.";
                    javax.swing.SwingUtilities.invokeLater(() -> gui.showError(errorMessage));
                }
                break;
        }
    }

    /**
     * Helper method to notify listeners (like the GUI) about an error.
     *
     * @param message The error message.
     */
    private void notifyError(String message) {
        // We directly call onModelEvent here as if the model sent the error
        // In a more complex system, might have a separate error reporting mechanism
        onModelEvent(new ModelEvent(ModelEvent.EventType.ERROR, message, null));
    }

    // --- Implementation of IOutputHandler Getter ---

    @Override
    public IOutputHandler getOutputHandler() {
        return this.outputHandler;
    }

    // --- Implementation of ICalendarController methods ---
    // These methods delegate calls to the model and handle exceptions,
    // notifying via events instead of direct output.

    @Override
    public void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        outputHandler.println("Calendar App Interactive Mode (Enhanced Controller). Type 'exit' to quit.");
        while (true) {
            outputHandler.println("> "); // Use outputHandler
            String userCommand = scanner.nextLine();
            if (userCommand.equalsIgnoreCase("exit")) {
                outputHandler.println("Exiting."); // Use outputHandler
                break;
            }
            try {
                // CommandParser needs to be adapted or replaced if it uses OutputHandler directly
                // or if it calls removed model methods (like import/export)
                // TODO: Refactor CommandParser to use Controller methods instead of Model directly
                CommandParser.processCommand(userCommand, calendarManager);
            } catch (Exception e) {
                // Notify error through the event system
                notifyError("Command error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    @Override
    public void runHeadlessMode(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String command;
            while ((command = br.readLine()) != null) {
                outputHandler.println("> " + command); // Use outputHandler
                if (command.equalsIgnoreCase("exit")) {
                    outputHandler.println("Exiting."); // Use outputHandler
                    break;
                }
                // Assume CommandParser works or will be refactored
                // TODO: Refactor CommandParser
                CommandParser.processCommand(command, calendarManager);
            }
        } catch (IOException e) {
            notifyError("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            notifyError("Command error: " + e.getMessage());
        }
    }

    @Override
    public List<String> getAvailableCalendarNames() {
        try {
            List<String> names = new ArrayList<>();
            // Access model data directly
            for (CalendarManager cal : calendarManager.getAllCalendars()) {
                names.add(cal.getCalendarName());
            }
            return names;
        } catch (Exception e) {
            notifyError("Error getting calendar names: " + e.getMessage());
            return Collections.emptyList(); // Return empty list on error
        }
    }

    @Override
    public String getCurrentCalendarName() {
        try {
            return calendarManager.getCurrentCalendar().getCalendarName();
        } catch (IllegalStateException e) {
            return null; // No current calendar selected
        } catch (Exception e) {
            notifyError("Error getting current calendar name: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getCurrentCalendarTimezone() {
        try {
            return calendarManager.getCurrentCalendar().getTimeZone().getId();
        } catch (IllegalStateException e) {
            return null; // No current calendar selected
        } catch (Exception e) {
            notifyError("Error getting current calendar timezone: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void createCalendar(String name, String timezone) throws Exception {
        try {
            calendarManager.createCalendar(name, timezone);
            // Success notification is handled by the model via onModelEvent
        } catch (Exception e) {
            notifyError("Error creating calendar: " + e.getMessage());
            throw e; // Re-throw for GUI dialogs that might need it
        }
    }

    @Override
    public void editCalendar(String oldName, String property, String newValue) throws Exception {
        try {
            calendarManager.editCalendar(oldName, property, newValue);
            // Success notification is handled by the model via onModelEvent
        } catch (Exception e) {
            notifyError("Error editing calendar: " + e.getMessage());
            throw e; // Re-throw
        }
    }

    @Override
    public void switchCalendar(String name) throws Exception {
        try {
            calendarManager.useCalendar(name);
            // Success notification is handled by the model via onModelEvent
        } catch (Exception e) {
            notifyError("Error switching calendar: " + e.getMessage());
            throw e; // Re-throw
        }
    }

    @Override
    public void createDefaultCalendarIfNeeded() throws Exception {
        // This logic is now slightly different. The controller initiates the check.
        // The actual creation and notification happen in the model if needed.
        if (calendarManager.getAllCalendars().isEmpty()) {
            try {
                String defaultName = "My Calendar";
                String defaultTimezone = java.time.ZoneId.systemDefault().getId();
                // Call the model's create method
                calendarManager.createCalendar(defaultName, defaultTimezone);
                // The model will notify CALENDAR_CREATED via event
                // Switch to it
                calendarManager.useCalendar(defaultName);
                // The model will notify CALENDAR_SWITCHED via event
            } catch (Exception e) {
                // Let the initialization caller handle this exception
                throw new Exception("Failed to create or switch to default calendar: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String[] getAvailableTimezones() {
        try {
            List<String> availableZones = new ArrayList<>(java.time.ZoneId.getAvailableZoneIds());
            Collections.sort(availableZones);
            return availableZones.toArray(new String[0]);
        } catch (Exception e) {
            notifyError("Error getting available timezones: " + e.getMessage());
            return new String[0]; // Return empty array on error
        }
    }

    @Override
    public List<ICalendarEvent> getEventsInRange(LocalDateTime start, LocalDateTime end) {
        try {
            // Delegate directly to the current calendar within the model
            return calendarManager.getCurrentCalendar().getEventsInRange(start, end);
        } catch (IllegalStateException e) {
            // No current calendar - this is not necessarily an error to notify about
            // unless the GUI expects one to always be selected.
            // outputHandler.println("No calendar selected to get events from.");
            return Collections.emptyList();
        } catch (Exception e) {
            notifyError("Error getting events in range: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ICalendarEvent> getEventsOn(LocalDate date) {
        try {
            // Delegate directly to the current calendar within the model
            return calendarManager.getCurrentCalendar().getEventsOn(date);
        } catch (IllegalStateException e) {
            // outputHandler.println("No calendar selected to get events from.");
            return Collections.emptyList();
        } catch (Exception e) {
            notifyError("Error getting events on date: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addEvent(ICalendarEvent event, boolean autoDecline) throws Exception {
        try {
            // Delegate to model, which throws specific exceptions now
            calendarManager.getCurrentCalendar().addEvent(event, autoDecline);
            // Success notification handled by model event
        } catch (IllegalStateException e) {
            notifyError("No calendar selected to add event to.");
            throw e; // Re-throw
        } catch (CalendarConflictException | NullPointerException e) {
            // Catch specific exceptions from addEvent
            notifyError("Error adding event: " + e.getMessage());
            throw e; // Re-throw
        } catch (Exception e) { // Catch unexpected errors
             notifyError("Unexpected error adding event: " + e.getMessage());
             throw e;
        }
    }

    @Override
    public void createNewSingleEvent(String name, LocalDateTime start, LocalDateTime end, boolean isAllDay, String description, String location, boolean isPublic) throws Exception {
        try {
            // Create the event object
            CalendarEvent newEvent = new CalendarEvent(name, start, end, isAllDay);
            newEvent.setDescription(description);
            newEvent.setLocation(location);
            newEvent.setPublic(isPublic);
            // Add it using the model's logic (which will notify and throw specific exceptions)
            calendarManager.getCurrentCalendar().addEvent(newEvent, true); // Assuming autoDecline true
        } catch (IllegalStateException e) {
            notifyError("No calendar selected to create event in.");
            throw e; // Re-throw
        } catch (CalendarConflictException | NullPointerException e) {
             notifyError("Error creating new single event: " + e.getMessage());
             throw e; // Re-throw
        } catch (Exception e) { // Catch unexpected errors
             notifyError("Unexpected error creating new single event: " + e.getMessage());
             throw e;
        }
    }

    @Override
    public void addRecurringEvent(String name, LocalDateTime start, LocalDateTime end, String recurrenceRule, boolean isAllDay, String description, String location, boolean isPublic) throws Exception {
        try {
            // Generate occurrences using the model's generator
            // Note: RecurringEventGenerator might need refactoring if it uses OutputHandler
            // TODO: Refactor RecurringEventGenerator if needed
            List<CalendarEvent> occurrences = RecurringEventGenerator.generateRecurringEvents(
                name, start, end, recurrenceRule, isAllDay
            );
            // Add each occurrence via the current calendar manager (which will notify)
            CalendarManager currentCal = calendarManager.getCurrentCalendar();
            int addedCount = 0;
            for (CalendarEvent occurrence : occurrences) {
                occurrence.setDescription(description);
                occurrence.setLocation(location);
                occurrence.setPublic(isPublic);
                try {
                    currentCal.addEvent(occurrence, true); // Assuming autoDecline true
                    addedCount++;
                } catch (CalendarConflictException e) {
                    // Notify about the conflict but continue trying to add others
                    notifyError("Skipping recurring instance due to conflict: " + occurrence.getEventName() + " at " + occurrence.getStart() + " - " + e.getMessage());
                }
            }
            // Notify about the overall operation
             onModelEvent(new ModelEvent(ModelEvent.EventType.EVENT_ADDED, // Or a different type?
                 "Added " + addedCount + " of " + occurrences.size() + " recurring event instances.", addedCount));

        } catch (IllegalStateException e) {
            notifyError("No calendar selected to add recurring event to.");
            throw e; // Re-throw
        } catch (Exception e) { // Catch errors from generator or other issues
            notifyError("Error adding recurring event: " + e.getMessage());
            throw e; // Re-throw
        }
    }

    @Override
    public boolean editSingleEvent(String property, String originalName, LocalDateTime originalStart, LocalDateTime originalEnd, String newValue) throws Exception {
        try {
            // Delegate to model, which throws specific exceptions
            boolean success = calendarManager.getCurrentCalendar().editSingleEvent(property, originalName, originalStart, originalEnd, newValue);
            // Success notification handled by model event
            return success;
        } catch (IllegalStateException e) {
            notifyError("No calendar selected to edit event in.");
            throw e; // Re-throw
        } catch (EventNotFoundException | InvalidDataException | CalendarConflictException e) {
             notifyError("Error editing single event: " + e.getMessage());
             throw e; // Re-throw specific exceptions
        } catch (Exception e) { // Catch unexpected errors
             notifyError("Unexpected error editing single event: " + e.getMessage());
             throw e;
        }
    }

    @Override
    public boolean deleteEvent(String eventName, LocalDateTime start, LocalDateTime end) throws Exception {
        try {
             // Delegate to model, which throws specific exceptions
            boolean success = calendarManager.getCurrentCalendar().deleteEvent(eventName, start, end);
            // Success notification handled by model event
            return success;
        } catch (IllegalStateException e) {
            notifyError("No calendar selected to delete event from.");
            throw e; // Re-throw
        } catch (EventNotFoundException e) {
             notifyError("Error deleting event: " + e.getMessage());
             throw e; // Re-throw specific exception
        } catch (Exception e) { // Catch unexpected errors
             notifyError("Unexpected error deleting event: " + e.getMessage());
             throw e;
        }
    }

    // --- Import/Export using Utility Classes ---

    @Override
    public int importFromGoogleCSV(String filePath) throws IOException, ImportException, CalendarConflictException {
        int importedCount = 0;
        List<ICalendarEvent> importedEvents;

        try (Reader reader = new FileReader(filePath)) {
            // Use the importer utility
            importedEvents = googleImporter.importEvents(reader);
        } catch (FileNotFoundException e) {
            throw new IOException("Import file not found: " + filePath, e);
        } catch (IOException | ImportException e) {
            // Re-throw IO or specific Import exceptions
            throw e;
        } catch (Exception e) {
            // Wrap unexpected errors from importer
            throw new ImportException("Unexpected error during import: " + e.getMessage(), e);
        }

        // Get the current calendar (throws IllegalStateException if none active)
        CalendarManager currentCal = calendarManager.getCurrentCalendar();
        int conflictCount = 0;

        // Add imported events to the model one by one
        for (ICalendarEvent event : importedEvents) {
            try {
                currentCal.addEvent(event, true); // Model handles conflict check and notification
                importedCount++;
            } catch (CalendarConflictException e) {
                // Handle conflict - notify and skip
                conflictCount++;
                notifyError("Skipping imported event due to conflict: " + event.getEventName() + " - " + e.getMessage());
                // Optionally re-throw if import should fail on first conflict?
                // throw e;
            } catch (Exception e) {
                 // Handle other potential errors from addEvent
                 notifyError("Error adding imported event '" + event.getEventName() + "': " + e.getMessage());
                 // Decide whether to continue or fail the whole import
            }
        }

        // Notify overall import status (different from model's internal notification)
         onModelEvent(new ModelEvent(ModelEvent.EventType.IMPORT_COMPLETED,
             "Controller import finished. Added: " + importedCount + ", Conflicts/Skipped: " + conflictCount, importedCount));

        return importedCount;
    }

    @Override
    public void exportToGoogleCSV(String filePath) throws IOException, ExportException {
        List<ICalendarEvent> eventsToExport;
        try {
            // Get events from the current calendar
            eventsToExport = calendarManager.getCurrentCalendar().getAllEvents();
        } catch (IllegalStateException e) {
            // Re-throw specific exception if no calendar is active
            throw e;
        }

        try (Writer writer = new FileWriter(filePath)) {
            // Use the exporter utility
            googleExporter.export(eventsToExport, writer);
            // Notify success (exporter doesn't notify)
             onModelEvent(new ModelEvent(ModelEvent.EventType.EXPORT_COMPLETED,
                 "Exported to Google CSV: " + filePath, filePath));
        } catch (IOException | ExportException e) {
            // Re-throw IO or specific Export exceptions
            throw e;
        } catch (Exception e) {
             // Wrap unexpected errors from exporter
            throw new ExportException("Unexpected error during export: " + e.getMessage(), e);
        }
    }
}
