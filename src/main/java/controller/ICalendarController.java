package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import model.ICalendarEvent; // Added import

/**
 * Controller interface for the Calendar application, mediating between View and Model.
 * Handles user actions from the GUI and interacts with the Model.
 */
public interface ICalendarController {

    // --- Application Modes (Existing) ---
    /**
     * Launches the GUI for interactive use.
     */
    void runInteractiveMode();

    /**
     * Runs the application in headless mode using commands from a file.
     * @param fileName The path to the command file.
     */
    void runHeadlessMode(String fileName);

    // --- Calendar Management ---
    /**
     * Gets the names of all available calendars.
     * @return A list of calendar names.
     */
    List<String> getAvailableCalendarNames();

    /**
     * Gets the name of the currently active calendar.
     * @return The current calendar name, or null if none is active.
     */
    String getCurrentCalendarName();

    /**
     * Gets the timezone ID of the currently active calendar.
     * @return The timezone ID string, or null if none is active.
     */
    String getCurrentCalendarTimezone();

    /**
     * Creates a new calendar.
     * @param name The name for the new calendar.
     * @param timezone The timezone ID for the new calendar.
     * @throws Exception If creation fails (e.g., name conflict).
     */
    void createCalendar(String name, String timezone) throws Exception;

    /**
     * Edits a property (name or timezone) of an existing calendar.
     * @param oldName The current name of the calendar to edit.
     * @param property The property to change ("name" or "timezone").
     * @param newValue The new value for the property.
     * @throws Exception If editing fails (e.g., calendar not found, invalid value).
     */
    void editCalendar(String oldName, String property, String newValue) throws Exception;

    /**
     * Switches the active calendar.
     * @param name The name of the calendar to switch to.
     * @throws Exception If the calendar name is not found.
     */
    void switchCalendar(String name) throws Exception;

    /**
     * Creates a default calendar if no calendars exist upon startup.
     * @throws Exception If default calendar creation fails.
     */
    void createDefaultCalendarIfNeeded() throws Exception;

    /**
     * Gets a sorted list of available system timezone IDs.
     * @return An array of timezone ID strings.
     */
    String[] getAvailableTimezones();

    // --- Event Retrieval ---
    /**
     * Gets all events within a specified date/time range for the current calendar.
     * @param start The start of the range (inclusive).
     * @param end The end of the range (exclusive).
     * @return A list of events within the range.
     */
    List<ICalendarEvent> getEventsInRange(LocalDateTime start, LocalDateTime end);

    /**
     * Gets all events occurring on a specific date for the current calendar.
     * @param date The date to retrieve events for.
     * @return A list of events on that date.
     */
    List<ICalendarEvent> getEventsOn(LocalDate date);

    // --- Event Management ---
    /**
     * Adds a single (non-recurring) event to the current calendar.
     * @param event The event object to add.
     * @param autoDecline True to automatically decline conflicting events, false otherwise.
     * @throws Exception If adding the event fails (e.g., conflict and autoDecline is false).
     */
    void addEvent(ICalendarEvent event, boolean autoDecline) throws Exception;

    /**
     * Creates and adds a single event from individual data fields.
     * @param name Event name.
     * @param start Start date/time.
     * @param end End date/time.
     * @param isAllDay True if the event is an all-day event.
     * @param description Event description.
     * @param location Event location.
     * @param isPublic Event visibility.
     * @throws Exception If creating or adding the event fails.
     */
    void createNewSingleEvent(String name, LocalDateTime start, LocalDateTime end, boolean isAllDay, String description, String location, boolean isPublic) throws Exception;

    /**
     * Creates and adds a series of recurring events based on the provided rules.
     * @param name Event name.
     * @param start Start date/time of the first occurrence.
     * @param end End date/time of the first occurrence.
     * @param recurrenceRule The RRULE string defining recurrence.
     * @param isAllDay True if the event is an all-day event.
     * @param description Event description.
     * @param location Event location.
     * @param isPublic Event visibility.
     * @throws Exception If generating or adding recurring events fails.
     */
    void addRecurringEvent(String name, LocalDateTime start, LocalDateTime end, String recurrenceRule, boolean isAllDay, String description, String location, boolean isPublic) throws Exception;

    /**
     * Edits a property of a single event instance.
     * @param property The property to edit (e.g., "name", "start", "end", "description").
     * @param originalName The original name of the event to identify it.
     * @param originalStart The original start time to identify the event.
     * @param originalEnd The original end time to identify the event.
     * @param newValue The new value for the property.
     * @return True if the edit was successful, false otherwise.
     * @throws Exception If editing fails.
     */
    boolean editSingleEvent(String property, String originalName, LocalDateTime originalStart, LocalDateTime originalEnd, String newValue) throws Exception;

    /**
     * Deletes a single event instance.
     * @param eventName The name of the event to delete.
     * @param start The start time of the event instance to delete.
     * @param end The end time of the event instance to delete.
     * @return True if the event was found and deleted, false otherwise.
     * @throws Exception If deletion fails.
     */
    boolean deleteEvent(String eventName, LocalDateTime start, LocalDateTime end) throws Exception;

    // --- Import/Export ---
    /**
     * Imports events from a Google Calendar CSV file into the current calendar.
     * @param filePath The path to the CSV file.
     * @return The number of events successfully imported.
     * @throws Exception If importing fails.
     */
    int importFromGoogleCSV(String filePath) throws Exception;

    /**
     * Exports the current calendar's events to a Google Calendar compatible CSV file.
     * @param filePath The path where the CSV file should be saved.
     * @throws Exception If exporting fails.
     */
    void exportToGoogleCSV(String filePath) throws Exception;
}
