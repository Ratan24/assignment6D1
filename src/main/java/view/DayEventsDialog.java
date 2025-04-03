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
import model.MultiCalendarManager;

/**
 * Dialog for displaying and managing events on a specific day
 */
public class DayEventsDialog extends JDialog {

  private LocalDate date;
  private MultiCalendarManager calendarManager;
  private JList<ICalendarEvent> eventsList;
  private DefaultListModel<ICalendarEvent> eventsModel;
  private JButton addEventButton;
  private JButton editEventButton;
  private JButton deleteEventButton;
  private JButton closeButton;

  /**
   * Constructor
   *
   * @param parent The parent frame
   * @param date The date to show events for
   * @param calendarManager The calendar manager
   */
  public DayEventsDialog(Frame parent, LocalDate date, MultiCalendarManager calendarManager) {
    super(parent, "Events on " + date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), true);
    this.date = date;
    this.calendarManager = calendarManager;

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
    eventsList.setCellRenderer(new EventCellRenderer());
    eventsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
      boolean hasSelection = !eventsList.isSelectionEmpty();
      editEventButton.setEnabled(hasSelection);
      deleteEventButton.setEnabled(hasSelection);
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
      ICalendarEvent selected = eventsList.getSelectedValue();
      if (selected != null) {
        confirmDeleteEvent(selected);
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
      List<ICalendarEvent> events = calendarManager.getCurrentCalendar().getEventsOn(date);

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
    boolean saved = EventDialog.showDialog(this, calendarManager, date, null);
    if (saved) {
      loadEvents(); // Refresh the list if saved
    }
  }

  /**
   * Edit an existing event
   *
   * Edit an existing event using the EventDialog
   *
   * @param event The event to edit
   */
  private void editEvent(ICalendarEvent event) {
    boolean saved = EventDialog.showDialog(this, calendarManager, date, event);
    if (saved) {
      loadEvents(); // Refresh the list if saved
    }
  }

  /**
   * Confirm and delete an event
   *
   * @param event The event to delete
   */
  private void confirmDeleteEvent(ICalendarEvent event) {
    int result = JOptionPane.showConfirmDialog(
        this,
        "Are you sure you want to delete event: " + event.getEventName() + "?",
        "Confirm Deletion",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (result == JOptionPane.YES_OPTION) {
      try {
        // Call the model's deleteEvent method
        boolean deleted = calendarManager.getCurrentCalendar().deleteEvent(
            event.getEventName(), event.getStart(), event.getEnd()
        );

        if (deleted) {
          JOptionPane.showMessageDialog(
              this,
              "Event deleted successfully.",
              "Success",
              JOptionPane.INFORMATION_MESSAGE
          );
          // After deleting, reload events
          loadEvents();
        } else {
           JOptionPane.showMessageDialog(
              this,
              "Event could not be found or deleted.",
              "Deletion Failed",
              JOptionPane.WARNING_MESSAGE
          );
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(
            this,
            "Error deleting event: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
      }
    }
  }

  /**
   * Custom cell renderer for the events list
   */
  private class EventCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {

      JLabel label = (JLabel) super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);

      if (value instanceof ICalendarEvent) {
        ICalendarEvent event = (ICalendarEvent) value;

        // Format display based on event type
        if (event.isAllDay()) {
          // For all-day events spanning multiple days, indicate the range if needed,
          // but for a single day view, "All Day" is usually sufficient.
          // If the event starts *before* this dialog's date, we could add "(Continues)"
          if (event.getStart().toLocalDate().isBefore(date)) {
             label.setText(event.getEventName() + " (All Day, Continues)");
          } else {
             label.setText(event.getEventName() + " (All Day)");
          }
        } else {
          LocalDateTime eventStart = event.getStart();
          LocalDateTime eventEnd = event.getEnd();
          LocalDate eventStartDate = eventStart.toLocalDate();
          LocalDate eventEndDate = eventEnd.toLocalDate();
          DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
          DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm");

          // Check if the event spans multiple days relative to the dialog's date
          boolean startsBefore = eventStartDate.isBefore(date);
          boolean endsAfter = eventEndDate.isAfter(date);
          // Handle case where event ends exactly at midnight of the next day
          boolean endsOnMidnightNextDay = eventEnd.toLocalTime().equals(LocalTime.MIDNIGHT) && eventEndDate.equals(date.plusDays(1));


          if (startsBefore && (endsAfter || endsOnMidnightNextDay)) {
            // Spans the entire current day
            label.setText(String.format("%s (Continues from %s until %s)",
                event.getEventName(),
                eventStart.format(dateTimeFormatter),
                eventEnd.format(dateTimeFormatter)));
          } else if (startsBefore && eventEndDate.equals(date)) {
            // Starts before today, ends today
             label.setText(String.format("%s (Ends %s, from %s)",
                event.getEventName(),
                eventEnd.format(timeFormatter),
                eventStart.format(dateTimeFormatter)));
          } else if (eventStartDate.equals(date) && (endsAfter || endsOnMidnightNextDay)) {
            // Starts today, ends after today
             label.setText(String.format("%s (Starts %s, until %s)",
                event.getEventName(),
                eventStart.format(timeFormatter),
                eventEnd.format(dateTimeFormatter)));
          } else if (eventStartDate.equals(date) && eventEndDate.equals(date)) {
            // Starts and ends today (the original case)
            String timeStr = eventStart.format(timeFormatter) + " - " + eventEnd.format(timeFormatter);
            label.setText(event.getEventName() + " (" + timeStr + ")");
          } else {
             // Should not happen if getEventsOn is correct, but fallback
             label.setText(String.format("%s (%s - %s)",
                event.getEventName(),
                eventStart.format(dateTimeFormatter),
                eventEnd.format(dateTimeFormatter)));
          }
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
   * @param calendarManager The calendar manager
   */
  public static void showDialog(Frame parent, LocalDate date, MultiCalendarManager calendarManager) {
    DayEventsDialog dialog = new DayEventsDialog(parent, date, calendarManager);
    dialog.setVisible(true);
  }
}
