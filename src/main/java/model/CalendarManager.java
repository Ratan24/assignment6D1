package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter; // Import formatter
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Import Comparator
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages a single calendar's events and properties.
 * Implements ICalendarManager and handles event storage, retrieval, editing,
 * conflict detection, and timezone management for one calendar.
 * Also implements IModelEventListener to potentially receive events (though not used internally yet)
 * and includes listener management to notify observers of changes within this specific calendar.
 */
public class CalendarManager implements ICalendarManager, IModelEventListener { // Implement listener

    private String calendarName;
    private ZoneId timeZone;
    private final List<ICalendarEvent> events;
    private final List<IModelEventListener> listeners = new ArrayList<>(); // Listener list

    // Formatter for parsing date/time strings in editEventProperty
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");


    /**
     * Constructs a CalendarManager.
     *
     * @param name     The name of the calendar.
     * @param timezone The timezone ID (e.g., "America/New_York").
     * @throws InvalidDataException If the timezone ID is invalid.
     */
    public CalendarManager(String name, String timezone) throws InvalidDataException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("Calendar name cannot be empty.");
        }
        this.calendarName = name;
        try {
            this.timeZone = ZoneId.of(timezone);
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            throw new InvalidDataException("Invalid timezone ID: " + timezone, e);
        }
        this.events = new ArrayList<>();
    }

    // --- Listener Management ---

    /**
     * Adds a listener to be notified of model events specific to this calendar.
     * @param listener The listener to add.
     */
    @Override // This method IS declared in ICalendarManager
    public void addModelEventListener(IModelEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     * @param listener The listener to remove.
     */
    @Override // This method IS declared in ICalendarManager
    public void removeModelEventListener(IModelEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners about a model event.
     * @param event The event to fire.
     */
    protected void fireModelEvent(ModelEvent event) {
        // Create a copy to avoid ConcurrentModificationException if a listener modifies the list
        List<IModelEventListener> listenersCopy = new ArrayList<>(listeners);
        for (IModelEventListener listener : listenersCopy) {
            try {
                listener.onModelEvent(event);
            } catch (Exception e) {
                // Log or handle listener exceptions appropriately
                System.err.println("Error notifying listener " + listener.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace(); // Consider a more robust logging mechanism
            }
        }
    }

    // --- Getters/Setters specific to CalendarManager (Not in ICalendarManager) ---

    // @Override // Removed - Not in ICalendarManager
    public String getCalendarName() {
        return calendarName;
    }

    // @Override // Removed - Not in ICalendarManager
    public ZoneId getTimeZone() {
        return timeZone;
    }

    // @Override // Removed - Not in ICalendarManager
    public void setCalendarName(String newName) throws InvalidDataException {
        if (newName == null || newName.trim().isEmpty()) {
            throw new InvalidDataException("Calendar name cannot be empty.");
        }
        String oldName = this.calendarName;
        this.calendarName = newName;
        fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_EDITED,
            "Calendar '" + oldName + "' renamed to: " + newName, this));
    }

    // @Override // Removed - Not in ICalendarManager
    public void setTimeZone(String timezoneId) throws InvalidDataException {
        try {
            ZoneId newZone = ZoneId.of(timezoneId);
            this.timeZone = newZone;
             fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_EDITED,
                 "Calendar '" + calendarName + "' timezone updated to: " + timezoneId, this));
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            throw new InvalidDataException("Invalid timezone ID: " + timezoneId, e);
        }
    }

    // --- ICalendarManager Implementation ---

    @Override
    public void addEvent(ICalendarEvent event, boolean autoDecline) throws CalendarConflictException {
        Objects.requireNonNull(event, "Event cannot be null");
        if (hasConflict(event)) {
            throw new CalendarConflictException("Conflict detected with event: " + getConflictingEvent(event));
        }
        events.add(event);
        // Collections.sort(events); // Use Comparator sort
        events.sort(Comparator.comparing(ICalendarEvent::getStart)); // Keep sorted
        fireModelEvent(new ModelEvent(ModelEvent.EventType.EVENT_ADDED, "Event added: " + event.getEventName(), event));
    }

    @Override
    public boolean editSingleEvent(String property, String originalName, LocalDateTime originalStart, LocalDateTime originalEnd, String newValue)
            throws EventNotFoundException, InvalidDataException, CalendarConflictException {
        ICalendarEvent eventToEdit = findEvent(originalName, originalStart, originalEnd);
        if (eventToEdit == null) {
            throw new EventNotFoundException("Event not found for editing: " + originalName + " at " + originalStart);
        }

        // Create a temporary copy to check for conflicts before applying changes
        CalendarEvent tempEvent = new CalendarEvent(eventToEdit.getEventName(), eventToEdit.getStart(), eventToEdit.getEnd(), eventToEdit.isAllDay());
        tempEvent.setDescription(eventToEdit.getDescription());
        tempEvent.setLocation(eventToEdit.getLocation());
        tempEvent.setPublic(eventToEdit.isPublic());

        boolean changed = updateEventProperty(tempEvent, property, newValue);

        if (changed) {
            // Check for conflicts *only if time changed*
             if (property.equalsIgnoreCase("start") || property.equalsIgnoreCase("end")) {
                 // Temporarily remove original event to check conflict with the modified version
                 events.remove(eventToEdit);
                 if (hasConflict(tempEvent)) {
                     events.add(eventToEdit); // Add original back
                     // Collections.sort(events); // Use Comparator sort
                     events.sort(Comparator.comparing(ICalendarEvent::getStart));
                     throw new CalendarConflictException("Editing event creates conflict with: " + getConflictingEvent(tempEvent));
                 }
                 events.add(eventToEdit); // Add original back before applying changes
                 // Collections.sort(events); // Use Comparator sort
                 events.sort(Comparator.comparing(ICalendarEvent::getStart));
             }
            // Apply changes to the actual event
            updateEventProperty(eventToEdit, property, newValue);
            // Collections.sort(events); // Use Comparator sort
            events.sort(Comparator.comparing(ICalendarEvent::getStart)); // Re-sort if start time changed
            fireModelEvent(new ModelEvent(ModelEvent.EventType.EVENT_EDITED, "Event edited: " + eventToEdit.getEventName(), eventToEdit));
        }
        return changed; // Indicate if any change was actually made
    }


    @Override
    public int editEventsByStart(String property, String eventName, LocalDateTime startTime, String newValue)
            throws InvalidDataException, CalendarConflictException {
        int updatedCount = 0;
        List<ICalendarEvent> toUpdate = events.stream()
                .filter(e -> e.getEventName().equalsIgnoreCase(eventName) && !e.getStart().isBefore(startTime))
                .collect(Collectors.toList());

        if (toUpdate.isEmpty()) {
            return 0; // Or throw EventNotFoundException? Current behavior is return 0.
        }

        // Need careful conflict checking if time properties are changed
        if (property.equalsIgnoreCase("start") || property.equalsIgnoreCase("end")) {
             throw new InvalidDataException("Bulk editing of start/end times is not supported due to complex conflict checking.");
        }

        for (ICalendarEvent event : toUpdate) {
             boolean changed = updateEventProperty(event, property, newValue);
             if (changed) {
                 updatedCount++;
                 fireModelEvent(new ModelEvent(ModelEvent.EventType.EVENT_EDITED, "Event edited (bulk): " + event.getEventName(), event));
             }
        }
        if (updatedCount > 0 && property.equalsIgnoreCase("name")) {
             // Collections.sort(events); // Use Comparator sort
             events.sort(Comparator.comparing(ICalendarEvent::getStart)); // Re-sort if name changed
        }
        return updatedCount;
    }

    @Override
    public int editEventsByName(String property, String eventName, String newValue)
            throws InvalidDataException, CalendarConflictException {
        int updatedCount = 0;
        List<ICalendarEvent> toUpdate = events.stream()
                .filter(e -> e.getEventName().equalsIgnoreCase(eventName))
                .collect(Collectors.toList());

        if (toUpdate.isEmpty()) {
            return 0; // Or throw EventNotFoundException? Current behavior is return 0.
        }

        // Need careful conflict checking if time properties are changed
        if (property.equalsIgnoreCase("start") || property.equalsIgnoreCase("end")) {
             throw new InvalidDataException("Bulk editing of start/end times is not supported due to complex conflict checking.");
        }

        for (ICalendarEvent event : toUpdate) {
             boolean changed = updateEventProperty(event, property, newValue);
              if (changed) {
                 updatedCount++;
                 fireModelEvent(new ModelEvent(ModelEvent.EventType.EVENT_EDITED, "Event edited (bulk): " + event.getEventName(), event));
             }
        }
         if (updatedCount > 0 && property.equalsIgnoreCase("name")) {
             // Collections.sort(events); // Use Comparator sort
             events.sort(Comparator.comparing(ICalendarEvent::getStart)); // Re-sort if name changed
        }
        return updatedCount;
    }

    @Override
    public boolean deleteEvent(String eventName, LocalDateTime start, LocalDateTime end) throws EventNotFoundException {
        ICalendarEvent eventToRemove = findEvent(eventName, start, end);
        if (eventToRemove == null) {
            throw new EventNotFoundException("Event not found for deletion: " + eventName + " at " + start);
        }
        boolean removed = events.remove(eventToRemove);
        if (removed) {
             fireModelEvent(new ModelEvent(ModelEvent.EventType.EVENT_DELETED, "Event deleted: " + eventName, eventToRemove));
        }
        return removed;
    }

    @Override
    public List<ICalendarEvent> getEventsOn(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay(); // Exclusive end

        return events.stream()
                .filter(event -> !event.getStart().isAfter(endOfDay.minusSeconds(1)) && // Starts before or exactly at end of day
                                 !event.getEnd().isBefore(startOfDay) && // Ends after or exactly at start of day
                                 !event.getEnd().isEqual(startOfDay)) // Handle 0-duration events at midnight
                .collect(Collectors.toList());
    }


    @Override
    public List<ICalendarEvent> getEventsInRange(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start, "Start datetime cannot be null");
        Objects.requireNonNull(end, "End datetime cannot be null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start datetime must be before end datetime.");
        }
        return events.stream()
                .filter(event -> event.getStart().isBefore(end) && event.getEnd().isAfter(start))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBusyAt(LocalDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return events.stream()
                .anyMatch(event -> !dateTime.isBefore(event.getStart()) && dateTime.isBefore(event.getEnd()));
    }

    @Override
    public List<ICalendarEvent> getAllEvents() {
        // Return an immutable copy to prevent external modification
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    // --- Helper Methods ---

    private boolean hasConflict(ICalendarEvent newEvent) {
        return events.stream().anyMatch(existingEvent -> existingEvent.conflictsWith(newEvent));
    }

    private ICalendarEvent getConflictingEvent(ICalendarEvent newEvent) {
        return events.stream()
                .filter(existingEvent -> existingEvent.conflictsWith(newEvent))
                .findFirst()
                .orElse(null); // Should not happen if hasConflict is true
    }

    private ICalendarEvent findEvent(String name, LocalDateTime start, LocalDateTime end) {
        return events.stream()
                .filter(e -> e.getEventName().equalsIgnoreCase(name) &&
                             e.getStart().equals(start) &&
                             e.getEnd().equals(end))
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates a specific property of an event object.
     *
     * @param event    The event to update (must be mutable, e.g., CalendarEvent).
     * @param property The name of the property to update (case-insensitive).
     * @param newValue The new value for the property.
     * @return true if a property was successfully updated, false otherwise.
     * @throws InvalidDataException If the property name is invalid or the value cannot be parsed.
     */
     private boolean updateEventProperty(ICalendarEvent event, String property, String newValue) throws InvalidDataException {
         if (!(event instanceof CalendarEvent)) {
             // Cannot modify immutable ICalendarEvent implementations directly
             throw new InvalidDataException("Cannot modify event of type: " + event.getClass().getName());
         }
         CalendarEvent mutableEvent = (CalendarEvent) event;
         boolean changed = false;

         switch (property.toLowerCase()) {
             case "name":
             case "subject": // Allow subject as alias for name
                 if (!mutableEvent.getEventName().equals(newValue)) {
                     mutableEvent.setEventName(newValue);
                     changed = true;
                 }
                 break;
             case "description":
                 if (!Objects.equals(mutableEvent.getDescription(), newValue)) {
                     mutableEvent.setDescription(newValue);
                     changed = true;
                 }
                 break;
             case "location":
                  if (!Objects.equals(mutableEvent.getLocation(), newValue)) {
                     mutableEvent.setLocation(newValue);
                     changed = true;
                 }
                 break;
             case "public":
                 try {
                     boolean isPublic = Boolean.parseBoolean(newValue);
                     if (mutableEvent.isPublic() != isPublic) {
                         mutableEvent.setPublic(isPublic);
                         changed = true;
                     }
                 } catch (Exception e) {
                     throw new InvalidDataException("Invalid boolean value for public: " + newValue, e);
                 }
                 break;
             case "start":
                 try {
                     LocalDateTime newStart = LocalDateTime.parse(newValue, dateTimeFormatter); // Use defined formatter
                     if (!mutableEvent.getStart().equals(newStart)) {
                         mutableEvent.setStart(newStart);
                         changed = true;
                     }
                 } catch (DateTimeParseException e) {
                     throw new InvalidDataException("Invalid start date/time format: " + newValue, e);
                 }
                 break;
             case "end":
                  try {
                     LocalDateTime newEnd = LocalDateTime.parse(newValue, dateTimeFormatter); // Use defined formatter
                     if (!mutableEvent.getEnd().equals(newEnd)) {
                         mutableEvent.setEnd(newEnd);
                         changed = true;
                     }
                 } catch (DateTimeParseException e) {
                     throw new InvalidDataException("Invalid end date/time format: " + newValue, e);
                 }
                 break;
             // Add cases for other editable properties if needed
             default:
                 throw new InvalidDataException("Attempted to edit unknown property: " + property);
         }
         // Ensure start is not after end if times were changed
         if (mutableEvent.getStart().isAfter(mutableEvent.getEnd())) {
             throw new InvalidDataException("Event start time cannot be after end time.");
         }
         return changed;
     }

     // --- IModelEventListener Implementation ---
     // This manager doesn't listen to other models, so this is a no-op.
     // It's implemented to satisfy the interface if needed elsewhere,
     // but its primary role is *firing* events, not receiving them.
     @Override
     public void onModelEvent(ModelEvent event) {
         // No action needed in CalendarManager when receiving external events.
     }
}
