package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.ICalendarEvent;
// import model.MultiCalendarManager; // Removed model import
import controller.ICalendarController; // Added controller import

/**
 * Dialog for displaying and managing events on a specific day
 */
public class DayEventsDialog extends JDialog {

  private LocalDate date;
  // private MultiCalendarManager calendarManager; // Removed model reference
  private ICalendarController controller; // Added controller reference
  private JList<ICalendarEvent> eventsList;
  private DefaultListModel<ICalendarEvent> eventsModel;
  private JButton addEventButton;
  private JButton editEventButton;
  private JButton deleteEventButton;
  private JButton closeButton;
  private ColorManager colorManager; // Added for coloring

  /**
   * Constructor
   *
   * @param parent The parent frame
   * @param date The date to show events for
   * @param controller The controller
   */
  public DayEventsDialog(Frame parent, LocalDate date, ICalendarController controller) { // Accept controller
    super(parent, "Events on " + date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), true);
    this.date = date;
    // this.calendarManager = calendarManager; // Removed model assignment
    this.controller = controller; // Store controller
    this.colorManager = new ColorManager(); // Initialize ColorManager

    // Initialize components
    initializeComponents();

    // Layout
    layoutComponents();

    // Register listeners
    registerListeners();

    // Load events
    loadEvents();

    // Final setup
    setSize(500, 350);
    setLocationRelativeTo(parent);
  }

  /**
   * Initialize dialog components
   */
  private void initializeComponents() {
    eventsModel = new DefaultListModel<>();
    eventsList = new JList<>(eventsModel);
    // Pass controller and colorManager to the renderer
    eventsList.setCellRenderer(new EventCellRenderer(controller, colorManager));
    // Allow multiple selections for deletion
    eventsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    addEventButton = new JButton("Add Event");
    editEventButton = new JButton("Edit");
    deleteEventButton = new JButton("Delete");
    closeButton = new JButton("Close");

    // Initially disable edit/delete buttons until an event is selected
    editEventButton.setEnabled(false);
    deleteEventButton.setEnabled(false);
  }

  /**
   * Layout the components in the dialog
   */
  private void layoutComponents() {
    JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Events list with header
    JPanel listPanel = new JPanel(new BorderLayout());
    listPanel.add(new JLabel("Events:"), BorderLayout.NORTH);

    JScrollPane scrollPane = new JScrollPane(eventsList);
    scrollPane.setBorder(BorderFactory.createEtchedBorder());
    listPanel.add(scrollPane, BorderLayout.CENTER);

    contentPanel.add(listPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(addEventButton);
    buttonPanel.add(editEventButton);
    buttonPanel.add(deleteEventButton);
    buttonPanel.add(closeButton);

    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(contentPanel);
  }

  /**
   * Register event listeners
   */
  private void registerListeners() {
    // Close button
    closeButton.addActionListener(e -> dispose());

    // Selection listener for the events list
    eventsList.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) { // Only react when selection is stable
            int[] selectedIndices = eventsList.getSelectedIndices();
            boolean singleSelection = selectedIndices.length == 1;
            boolean anySelection = selectedIndices.length > 0;

            editEventButton.setEnabled(singleSelection); // Enable edit only for single selection
            deleteEventButton.setEnabled(anySelection); // Enable delete if any are selected
        }
    });

    // Add event button
    addEventButton.addActionListener(e -> {
      createNewEvent();
    });

    // Edit event button
    editEventButton.addActionListener(e -> {
      ICalendarEvent selected = eventsList.getSelectedValue();
      if (selected != null) {
        editEvent(selected);
      }
    });

    // Delete event button
    deleteEventButton.addActionListener(e -> {
        List<ICalendarEvent> selectedEvents = eventsList.getSelectedValuesList();
        if (!selectedEvents.isEmpty()) {
            confirmDeleteMultipleEvents(selectedEvents);
        }
    });

    // Double-click on event to edit
    eventsList.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int index = eventsList.locationToIndex(e.getPoint());
          if (index >= 0) {
            ICalendarEvent selected = eventsModel.getElementAt(index);
            editEvent(selected);
          }
        }
      }
    });

    // Close dialog when ESC key is pressed
    getRootPane().registerKeyboardAction(
        e -> dispose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW
    );
  }

  /**
   * Load events for the selected day
   */
  private void loadEvents() {
    eventsModel.clear();

    try {
      // Get events from controller
      List<ICalendarEvent> events = controller.getEventsOn(date);

      // Sort events by start time
      events.sort((e1, e2) -> {
        // All-day events come first
        if (e1.isAllDay() && !e2.isAllDay()) return -1;
        if (!e1.isAllDay() && e2.isAllDay()) return 1;

        // Then sort by start time
        return e1.getStart().compareTo(e2.getStart());
      });

      // Add sorted events to the model
      for (ICalendarEvent event : events) {
        eventsModel.addElement(event);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          this,
          "Error loading events: " + e.getMessage(),
          "Error",
          JOptionPane.ERROR_MESSAGE
      );
    }
  }

  /**
   * Create a new event using the EventDialog
   */
  private void createNewEvent() {
    // Pass controller to EventDialog
    boolean saved = EventDialog.showDialog(this, controller, date, null);
    if (saved) {
      loadEvents(); // Refresh the list if saved
    }
  }

  /**
   * Edit an existing event using the EventDialog
   *
   * @param event The event to edit
   */
  private void editEvent(ICalendarEvent event) {
    // Pass controller to EventDialog
    boolean saved = EventDialog.showDialog(this, controller, date, event);
    if (saved) {
      loadEvents(); // Refresh the list if saved
    }
  }

  /**
   * Confirm and delete an event
   *
   * @param eventsToDelete The list of events to delete
   */
  private void confirmDeleteMultipleEvents(List<ICalendarEvent> eventsToDelete) {
    String message;
    if (eventsToDelete.size() == 1) {
        message = "Are you sure you want to delete event: " + eventsToDelete.get(0).getEventName() + "?";
    } else {
        message = "Are you sure you want to delete the selected " + eventsToDelete.size() + " events?";
    }

    int result = JOptionPane.showConfirmDialog(
        this,
        message,
        "Confirm Deletion",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (result == JOptionPane.YES_OPTION) {
        int deletedCount = 0;
        int failedCount = 0;
        StringBuilder errors = new StringBuilder("Errors occurred while deleting:\n");

        for (ICalendarEvent event : eventsToDelete) {
            try {
                // Call the controller's deleteEvent method for each event
                boolean deleted = controller.deleteEvent(
                    event.getEventName(), event.getStart(), event.getEnd()
                );
                if (deleted) {
                    deletedCount++;
                } else {
                    // Should ideally not happen if controller throws EventNotFoundException
                    failedCount++;
                    errors.append("- Could not delete '").append(event.getEventName()).append("'\n");
                }
            } catch (Exception e) { // Catch exceptions from controller.deleteEvent
                failedCount++;
                errors.append("- Error deleting '").append(event.getEventName()).append("': ").append(e.getMessage()).append("\n");
            }
        }

        // Show summary message
        String summaryMessage = deletedCount + " event(s) deleted.";
        if (failedCount > 0) {
            summaryMessage += "\n" + failedCount + " event(s) failed to delete.";
            JOptionPane.showMessageDialog(
                this,
                summaryMessage + "\n\n" + errors.toString(),
                "Deletion Result",
                JOptionPane.WARNING_MESSAGE
            );
        } else {
             // Optionally show success message only if needed (list refresh should happen via events)
             // JOptionPane.showMessageDialog(this, summaryMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
        }

        // Refresh the list regardless of partial success/failure
        loadEvents();
    }
  }

  /**
   * Custom cell renderer for the events list
   */
  private class EventCellRenderer extends DefaultListCellRenderer {
      private final ICalendarController cellController;
      private final ColorManager cellColorManager;

      public EventCellRenderer(ICalendarController controller, ColorManager colorManager) {
          this.cellController = controller;
          this.cellColorManager = colorManager;
      }

      @Override
      public Component getListCellRendererComponent(
          JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {

          // Get default label styling
          JLabel label = (JLabel) super.getListCellRendererComponent(
              list, value, index, isSelected, cellHasFocus);

          if (value instanceof ICalendarEvent) {
              ICalendarEvent event = (ICalendarEvent) value;
              String displayText;

              // Format display text based on event type
              if (event.isAllDay()) {
                  if (event.getStart().toLocalDate().isBefore(date)) {
                     displayText = event.getEventName() + " (All Day, Continues)";
                  } else {
                     displayText = event.getEventName() + " (All Day)";
                  }
              } else {
                  LocalDateTime eventStart = event.getStart();
                  LocalDateTime eventEnd = event.getEnd();
                  LocalDate eventStartDate = eventStart.toLocalDate();
                  LocalDate eventEndDate = eventEnd.toLocalDate();
                  DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                  DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm");

                  boolean startsBefore = eventStartDate.isBefore(date);
                  boolean endsAfter = eventEndDate.isAfter(date);
                  boolean endsOnMidnightNextDay = eventEnd.toLocalTime().equals(LocalTime.MIDNIGHT) && eventEndDate.equals(date.plusDays(1));

                  if (startsBefore && (endsAfter || endsOnMidnightNextDay)) {
                      displayText = String.format("%s (Continues from %s until %s)",
                          event.getEventName(), eventStart.format(dateTimeFormatter), eventEnd.format(dateTimeFormatter));
                  } else if (startsBefore && eventEndDate.equals(date)) {
                      displayText = String.format("%s (Ends %s, from %s)",
                          event.getEventName(), eventEnd.format(timeFormatter), eventStart.format(dateTimeFormatter));
                  } else if (eventStartDate.equals(date) && (endsAfter || endsOnMidnightNextDay)) {
                      displayText = String.format("%s (Starts %s, until %s)",
                          event.getEventName(), eventStart.format(timeFormatter), eventEnd.format(dateTimeFormatter));
                  } else if (eventStartDate.equals(date) && eventEndDate.equals(date)) {
                      String timeStr = eventStart.format(timeFormatter) + " - " + eventEnd.format(timeFormatter);
                      displayText = event.getEventName() + " (" + timeStr + ")";
                  } else {
                      displayText = String.format("%s (%s - %s)",
                          event.getEventName(), eventStart.format(dateTimeFormatter), eventEnd.format(dateTimeFormatter));
                  }
              }
              label.setText(displayText);

              // Apply color based on the current calendar
              try {
                  String currentCalendarName = cellController.getCurrentCalendarName();
                  if (currentCalendarName != null) {
                      // Correct method name: getColorForCalendar
                      Color calendarColor = cellColorManager.getColorForCalendar(currentCalendarName);
                      // Use color for foreground or background (foreground is usually better for lists)
                      if (!isSelected) { // Don't override selection color
                          label.setForeground(calendarColor);
                      }
                      // Example: Set a border color instead/as well
                      // label.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, calendarColor));
                  }
              } catch (Exception e) {
                  // Ignore errors getting color, use default rendering
                  System.err.println("Error getting calendar color for renderer: " + e.getMessage());
              }
          }
          return label;
      }
  }

  /**
   * Static method to show the dialog
   *
   * @param parent The parent frame
   * @param date The date to show events for
   * @param controller The controller
   */
  public static void showDialog(Frame parent, LocalDate date, ICalendarController controller) { // Accept controller
    DayEventsDialog dialog = new DayEventsDialog(parent, date, controller);
    dialog.setVisible(true);
  }
}
