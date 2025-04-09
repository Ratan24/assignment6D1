package model;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages multiple calendars, allowing creation, switching, editing,
 * and copying events between them. Implements IMultiCalendar and IModelEventListener
 * to handle and forward events.
 */
public class MultiCalendarManager implements IMultiCalendar, IModelEventListener { // Implement listener

    private final Map<String, CalendarManager> calendars;
    private CalendarManager currentManager; // The currently active calendar
    private final List<IModelEventListener> listeners = new ArrayList<>(); // Listeners for MultiCalendar events

    public MultiCalendarManager() {
        this.calendars = new HashMap<>();
        this.currentManager = null;
    }

    // --- Listener Management (for MultiCalendarManager itself) ---

    // @Override // Removed - Not in IMultiCalendar
    public void addModelEventListener(IModelEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // @Override // Removed - Not in IMultiCalendar
    public void removeModelEventListener(IModelEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies listeners registered with the MultiCalendarManager.
     * @param event The event to fire.
     */
    protected void fireModelEvent(ModelEvent event) {
        // Create a copy to avoid ConcurrentModificationException
        List<IModelEventListener> listenersCopy = new ArrayList<>(listeners);
        for (IModelEventListener listener : listenersCopy) {
             try {
                listener.onModelEvent(event);
            } catch (Exception e) {
                System.err.println("Error notifying listener " + listener.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // --- IModelEventListener Implementation (to listen to active CalendarManager) ---

    /**
     * Receives events from the currently active CalendarManager and forwards them
     * to the listeners registered with this MultiCalendarManager.
     * @param event The event received from the active CalendarManager.
     */
    @Override
    public void onModelEvent(ModelEvent event) {
        // Forward the event to MultiCalendarManager's listeners
        fireModelEvent(event);
    }


    // --- IMultiCalendar Implementation ---

    @Override
    public void createCalendar(String name, String timezone) throws InvalidDataException {
        if (calendars.containsKey(name)) {
            throw new InvalidDataException("Calendar with name '" + name + "' already exists.");
        }
        CalendarManager newCal = new CalendarManager(name, timezone);
        calendars.put(name, newCal);
        // If this is the first calendar, make it active
        if (currentManager == null) {
            useCalendar(name); // This will fire CALENDAR_SWITCHED
        }
        // Fire CALENDAR_CREATED event *after* potentially switching
        fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_CREATED, "Calendar created: " + name + " (" + timezone + ")", newCal));
    }

    @Override
    public void useCalendar(String name) throws InvalidDataException {
        CalendarManager managerToUse = calendars.get(name);
        if (managerToUse == null) {
            throw new InvalidDataException("Calendar not found: " + name);
        }
        if (managerToUse != currentManager) {
            // Unregister from old manager if exists
            if (currentManager != null) {
                currentManager.removeModelEventListener(this);
            }
            // Set new manager and register self as listener
            currentManager = managerToUse;
            currentManager.addModelEventListener(this); // Listen to events from the active calendar

            fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_SWITCHED, "Using calendar: " + name, currentManager));
        } else {
             // Optionally fire event even if switching to the same calendar?
             // fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_SWITCHED, "Using calendar: " + name, currentManager));
             // For now, only fire if it actually changed.
        }
    }

    @Override
    public void editCalendar(String oldName, String property, String newValue) throws InvalidDataException {
        CalendarManager managerToEdit = calendars.get(oldName);
        if (managerToEdit == null) {
            throw new InvalidDataException("Calendar not found: " + oldName);
        }

        if (property.equalsIgnoreCase("name")) {
            if (newValue == null || newValue.trim().isEmpty()) {
                throw new InvalidDataException("New calendar name cannot be empty.");
            }
            if (calendars.containsKey(newValue) && !newValue.equalsIgnoreCase(oldName)) {
                throw new InvalidDataException("Calendar name '" + newValue + "' already exists.");
            }
            // Update map key and internal name
            calendars.remove(oldName);
            managerToEdit.setCalendarName(newValue); // This fires CALENDAR_EDITED from CalendarManager
            calendars.put(newValue, managerToEdit);
            // No need to fire again from here, CalendarManager already did.
        } else if (property.equalsIgnoreCase("timezone")) {
            managerToEdit.setTimeZone(newValue); // This fires CALENDAR_EDITED from CalendarManager
             // If the edited calendar is the current one, notify about the timezone change specifically?
             if (managerToEdit == currentManager) {
                  fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_EDITED, "Active calendar timezone updated to: " + newValue, currentManager));
             }
        } else {
            throw new InvalidDataException("Invalid property to edit for calendar: " + property);
        }
    }

    // @Override // Removed - Not in IMultiCalendar
    public CalendarManager getCurrentCalendar() throws IllegalStateException {
        if (currentManager == null) {
            throw new IllegalStateException("No calendar is currently in use.");
        }
        return currentManager;
    }

    // @Override // Removed - Not in IMultiCalendar
    public List<CalendarManager> getAllCalendars() {
        return new ArrayList<>(calendars.values()); // Return a copy
    }

    // --- Event Copying Methods ---

    @Override
    public void copyEvent(String eventName, LocalDateTime sourceDateTime, String targetCalendarName, LocalDateTime targetStartDateTime) throws Exception {
        CalendarManager sourceCal = getCurrentCalendar(); // Assume copy from current
        CalendarManager targetCal = calendars.get(targetCalendarName);
        if (targetCal == null) {
            throw new InvalidDataException("Target calendar not found: " + targetCalendarName);
        }

        // Find the specific event instance in the source calendar
        ICalendarEvent sourceEvent = sourceCal.getAllEvents().stream()
            .filter(e -> e.getEventName().equalsIgnoreCase(eventName) && e.getStart().equals(sourceDateTime))
            .findFirst()
            .orElseThrow(() -> new EventNotFoundException("Source event '" + eventName + "' at " + sourceDateTime + " not found."));

        // Create a copy with the new start time
        long durationSeconds = java.time.Duration.between(sourceEvent.getStart(), sourceEvent.getEnd()).getSeconds();
        LocalDateTime targetEndDateTime = targetStartDateTime.plusSeconds(durationSeconds);

        CalendarEvent copiedEvent = new CalendarEvent(
            sourceEvent.getEventName(),
            targetStartDateTime,
            targetEndDateTime,
            sourceEvent.isAllDay()
        );
        // Copy other properties
        copiedEvent.setDescription(sourceEvent.getDescription());
        copiedEvent.setLocation(sourceEvent.getLocation());
        copiedEvent.setPublic(sourceEvent.isPublic());

        // Add to target calendar (will handle conflicts and notify via EVENT_ADDED)
        targetCal.addEvent(copiedEvent, true); // Assuming autoDecline true for copies
        // Fire a general event indicating the target calendar was modified
        fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_EDITED, "Event '" + eventName + "' copied to " + targetCalendarName, copiedEvent));
    }

    @Override
    public void copyEventsOn(LocalDate sourceDate, String targetCalendarName, LocalDate targetStartDate) throws Exception {
        CalendarManager sourceCal = getCurrentCalendar();
        CalendarManager targetCal = calendars.get(targetCalendarName);
        if (targetCal == null) {
            throw new InvalidDataException("Target calendar not found: " + targetCalendarName);
        }

        List<ICalendarEvent> eventsToCopy = sourceCal.getEventsOn(sourceDate);
        if (eventsToCopy.isEmpty()) {
            // Nothing to copy, maybe notify? Use ERROR type for potential user feedback
            fireModelEvent(new ModelEvent(ModelEvent.EventType.ERROR, "No events found on " + sourceDate + " to copy.", null));
            return;
        }

        int copiedCount = 0;
        int conflictCount = 0;
        for (ICalendarEvent sourceEvent : eventsToCopy) {
            LocalDateTime newStart = targetStartDate.atTime(sourceEvent.getStart().toLocalTime());
            long durationSeconds = java.time.Duration.between(sourceEvent.getStart(), sourceEvent.getEnd()).getSeconds();
            LocalDateTime newEnd = newStart.plusSeconds(durationSeconds);

            CalendarEvent copiedEvent = new CalendarEvent(
                sourceEvent.getEventName(), newStart, newEnd, sourceEvent.isAllDay()
            );
            copiedEvent.setDescription(sourceEvent.getDescription());
            copiedEvent.setLocation(sourceEvent.getLocation());
            copiedEvent.setPublic(sourceEvent.isPublic());

            try {
                targetCal.addEvent(copiedEvent, true); // Fires EVENT_ADDED
                copiedCount++;
            } catch (CalendarConflictException e) {
                conflictCount++;
                // Fire ERROR event for skipped copy
                fireModelEvent(new ModelEvent(ModelEvent.EventType.ERROR, "Skipping copied event due to conflict: " + copiedEvent.getEventName() + " - " + e.getMessage(), null));
            }
        }
         // Fire general CALENDAR_EDITED event for the target calendar
         fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_EDITED,
             "Copied " + copiedCount + " events from " + sourceDate + " to " + targetCalendarName + " starting " + targetStartDate + ". Conflicts: " + conflictCount, copiedCount));
    }

    @Override
    public void copyEventsBetween(LocalDate sourceStartDate, LocalDate sourceEndDate, String targetCalendarName, LocalDate targetStartDate) throws Exception {
         CalendarManager sourceCal = getCurrentCalendar();
         CalendarManager targetCal = calendars.get(targetCalendarName);
         if (targetCal == null) {
             throw new InvalidDataException("Target calendar not found: " + targetCalendarName);
         }

         LocalDateTime sourceRangeStart = sourceStartDate.atStartOfDay();
         LocalDateTime sourceRangeEnd = sourceEndDate.plusDays(1).atStartOfDay(); // Exclusive end

         List<ICalendarEvent> eventsToCopy = sourceCal.getEventsInRange(sourceRangeStart, sourceRangeEnd);
          if (eventsToCopy.isEmpty()) {
            // Use ERROR type for potential user feedback
            fireModelEvent(new ModelEvent(ModelEvent.EventType.ERROR, "No events found between " + sourceStartDate + " and " + sourceEndDate + " to copy.", null));
            return;
        }

         int copiedCount = 0;
         int conflictCount = 0;
         long dayOffset = java.time.temporal.ChronoUnit.DAYS.between(sourceStartDate, targetStartDate);

         for (ICalendarEvent sourceEvent : eventsToCopy) {
             LocalDateTime newStart = sourceEvent.getStart().plusDays(dayOffset);
             LocalDateTime newEnd = sourceEvent.getEnd().plusDays(dayOffset);

             CalendarEvent copiedEvent = new CalendarEvent(
                 sourceEvent.getEventName(), newStart, newEnd, sourceEvent.isAllDay()
             );
             copiedEvent.setDescription(sourceEvent.getDescription());
             copiedEvent.setLocation(sourceEvent.getLocation());
             copiedEvent.setPublic(sourceEvent.isPublic());

             try {
                 targetCal.addEvent(copiedEvent, true); // Fires EVENT_ADDED
                 copiedCount++;
             } catch (CalendarConflictException e) {
                 conflictCount++;
                 // Fire ERROR event for skipped copy
                 fireModelEvent(new ModelEvent(ModelEvent.EventType.ERROR, "Skipping copied event due to conflict: " + copiedEvent.getEventName() + " - " + e.getMessage(), null));
             }
         }
          // Fire general CALENDAR_EDITED event for the target calendar
          fireModelEvent(new ModelEvent(ModelEvent.EventType.CALENDAR_EDITED,
              "Copied " + copiedCount + " events from range " + sourceStartDate + "-" + sourceEndDate + " to " + targetCalendarName + " starting " + targetStartDate + ". Conflicts: " + conflictCount, copiedCount));
    }
}
