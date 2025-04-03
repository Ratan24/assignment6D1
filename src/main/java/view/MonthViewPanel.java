package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime; // Added import
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import model.ICalendarEvent;
import model.MultiCalendarManager;

/**
 * Panel that displays a month view of the calendar
 */
public class MonthViewPanel extends JPanel {

  private YearMonth yearMonth;
  private JPanel daysPanel;
  private JPanel headerPanel;
  private CalendarGUI parent;
  private ColorManager colorManager;

  /**
   * Constructor
   *
   * @param yearMonth The year and month to display
   * @param parent The parent GUI
   */
  public MonthViewPanel(YearMonth yearMonth, CalendarGUI parent) {
    this.yearMonth = yearMonth;
    this.parent = parent;
    this.colorManager = new ColorManager();

    setLayout(new BorderLayout());

    // Create day of week header
    headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    // Create days grid
    daysPanel = new JPanel();
    daysPanel.setLayout(new GridLayout(0, 7));
    add(daysPanel, BorderLayout.CENTER);

    // Initial update
    updateDaysPanel();
  }

  /**
   * Creates the header panel with day of week names
   *
   * @return A panel with day names
   */
  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 7));

    // Add day names
    for (int i = 0; i < 7; i++) {
      // Get day of week (1 = Monday, 7 = Sunday in ISO)
      DayOfWeek day = DayOfWeek.of(((i + 1) % 7) + 1);
      JLabel label = new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
      label.setHorizontalAlignment(JLabel.CENTER);
      label.setFont(label.getFont().deriveFont(Font.BOLD));
      panel.add(label);
    }

    return panel;
  }

  /**
   * Updates the days panel with the current month
   */
  private void updateDaysPanel() {
    daysPanel.removeAll();

    // Get the first day of the month
    LocalDate firstOfMonth = yearMonth.atDay(1);

    // Get the day of week (0 = Monday, 6 = Sunday with the ISO calendar)
    int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue() % 7;

    // Add empty cells for days before the start of the month
    for (int i = 0; i < dayOfWeekValue; i++) {
      daysPanel.add(new JPanel());
    }

    // Add cells for each day in the month
    for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
      LocalDate date = yearMonth.atDay(day);
      DayPanel dayPanel = new DayPanel(date);
      daysPanel.add(dayPanel);
    }

    // Add empty cells at the end to fill the grid if needed
    int totalCells = 7 * 6; // 6 rows
    int emptyCellsAtEnd = totalCells - dayOfWeekValue - yearMonth.lengthOfMonth();
    for (int i = 0; i < emptyCellsAtEnd; i++) {
      daysPanel.add(new JPanel());
    }

    // Refresh the panel
    daysPanel.revalidate();
    daysPanel.repaint();
  }

  /**
   * Set a new year and month to display
   *
   * @param yearMonth The year and month to display
   */
  public void setYearMonth(YearMonth yearMonth) {
    this.yearMonth = yearMonth;
    updateDaysPanel();
  }

  /**
   * Load events from the calendar manager
   *
   * @param calendarManager The calendar manager
   */
  public void loadEvents(MultiCalendarManager calendarManager) {
    // Clear events from all day panels first
    for (Component comp : daysPanel.getComponents()) {
      if (comp instanceof DayPanel) {
        ((DayPanel) comp).clearEvents();
      }
    }

    // Get color for current calendar
    Color calendarColor = Color.BLUE; // Default color
    try {
      String calendarName = calendarManager.getCurrentCalendar().getCalendarName();
      calendarColor = colorManager.getColorForCalendar(calendarName);
    } catch (Exception e) {
      System.err.println("Error getting calendar color: " + e.getMessage());
      // Use default color if there's an error getting the specific one
    }

    // Define the start and end of the month view
    LocalDate monthStart = yearMonth.atDay(1);
    LocalDate monthEnd = yearMonth.atEndOfMonth();
    LocalDateTime viewStart = monthStart.atStartOfDay();
    // Include the entire last day
    LocalDateTime viewEnd = monthEnd.plusDays(1).atStartOfDay();

    try {
      // Get all events that overlap with the current month view
      List<ICalendarEvent> eventsForMonth = calendarManager.getCurrentCalendar()
          .getEventsInRange(viewStart, viewEnd);

      // Go through all day panels again to add events
      for (Component comp : daysPanel.getComponents()) {
        if (comp instanceof DayPanel) {
          DayPanel dayPanel = (DayPanel) comp;
          LocalDate panelDate = dayPanel.getDate();

          // Check each event to see if it falls on this panel's date
          for (ICalendarEvent event : eventsForMonth) {
            LocalDate eventStartDate = event.getStart().toLocalDate();
            // For non-all-day events, the end date might be the same day or later.
            // For all-day events, the logical end is often considered the start of the next day.
            // We need to include events ending *on* this day.
            LocalDate eventEndDate = event.isAllDay() ?
                event.getStart().toLocalDate() : // All-day events typically span just the start date visually in simple views
                event.getEnd().toLocalDate();
            // Adjust end date if event ends exactly at midnight, it belongs to the previous day.
            if (!event.isAllDay() && event.getEnd().toLocalTime().equals(java.time.LocalTime.MIDNIGHT) && !event.getStart().toLocalDate().equals(event.getEnd().toLocalDate())) {
                 eventEndDate = eventEndDate.minusDays(1);
            }


            // Check if the panelDate is within the event's date range (inclusive)
            if (!panelDate.isBefore(eventStartDate) && !panelDate.isAfter(eventEndDate)) {
              dayPanel.addEvent(event.getEventName(), calendarColor);
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error loading events for month view: " + e.getMessage());
      // Optionally show an error message to the user
    }

    // Refresh the panel
    daysPanel.revalidate();
    daysPanel.repaint();
  }

  /**
   * Inner class representing a single day cell in the calendar
   */
  private class DayPanel extends JPanel {

    private LocalDate date;
    private JLabel dateLabel;
    private JPanel eventsPanel;

    /**
     * Constructor
     *
     * @param date The date this panel represents
     */
    public DayPanel(LocalDate date) {
      this.date = date;
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

      // Date number label
      dateLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
      dateLabel.setHorizontalAlignment(JLabel.RIGHT);
      if (date.equals(LocalDate.now())) {
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
        dateLabel.setForeground(Color.RED);
        // Highlight today's cell
        setBackground(new Color(255, 255, 230));
      } else {
        setBackground(Color.WHITE);
      }
      add(dateLabel, BorderLayout.NORTH);

      // Panel for events
      eventsPanel = new JPanel();
      eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
      eventsPanel.setOpaque(false);
      JScrollPane scrollPane = new JScrollPane(eventsPanel);
      scrollPane.setBorder(null);
      scrollPane.setOpaque(false);
      scrollPane.getViewport().setOpaque(false);
      add(scrollPane, BorderLayout.CENTER);

      // Make the day selectable
      setPreferredSize(new Dimension(100, 80));

      // Add a mouse listener to handle clicks
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          parent.showEventsForDay(date);
        }
      });
    }

    /**
     * Add an event to this day cell
     *
     * @param eventName The name of the event
     * @param color The color to indicate the calendar the event belongs to
     */
    public void addEvent(String eventName, Color color) {
      JLabel eventLabel = new JLabel("• " + eventName);
      eventLabel.setForeground(color);
      // For long event names, show truncated text with tooltip
      if (eventName.length() > 15) {
        eventLabel.setText("• " + eventName.substring(0, 12) + "...");
        eventLabel.setToolTipText(eventName);
      }
      eventsPanel.add(eventLabel);
    }

    /**
     * Clear all events from this day
     */
    public void clearEvents() {
      eventsPanel.removeAll();
    }

    /**
     * Get the date of this day
     *
     * @return The date
     */
    public LocalDate getDate() {
      return date;
    }
  }
}
