package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import model.MultiCalendarManager;
import model.CalendarManager;
import model.ICalendarEvent;
import controller.CalendarController;

/**
 * Main GUI implementation for the Calendar application.
 * Follows MVC architecture by serving as the View component.
 */
public class CalendarGUI extends JFrame implements ICalendarGUI {

  // Components
  private JPanel topPanel;
  private MonthViewPanel monthPanel;
  private JPanel statusPanel;
  private JLabel statusLabel;

  // Controls
  private JLabel monthYearLabel;
  private JComboBox<String> calendarSelector;
  private JButton prevMonthButton;
  private JButton nextMonthButton;
  private JButton createCalendarButton;

  // Model and controller references
  private MultiCalendarManager calendarManager;
  private CalendarController controller;
  private ColorManager colorManager;

  // State
  private YearMonth currentYearMonth;

  /**
   * Constructor for the GUI
   *
   * @param calendarManager The model for the calendar application
   * @param controller The controller for handling user actions
   */
  public CalendarGUI(MultiCalendarManager calendarManager, CalendarController controller) {
    super("Calendar Application");
    this.calendarManager = calendarManager;
    this.controller = controller;
    this.currentYearMonth = YearMonth.now();
    this.colorManager = new ColorManager();

    initializeFrame();
    initializeComponents();
    initializeMenuBar();
    layoutComponents();
    registerListeners();

    // Try to create a default calendar if none exists
    createDefaultCalendarIfNeeded();

    // Initialize with current month
    updateView();
  }

  /**
   * Create a default calendar if no calendars exist
   */
  private void createDefaultCalendarIfNeeded() {
    try {
      // If no calendars exist, create a default one
      if (getAvailableCalendars().length == 0) {
        String defaultName = "My Calendar";
        String defaultTimezone = java.time.ZoneId.systemDefault().getId();
        calendarManager.createCalendar(defaultName, defaultTimezone);
        updateCalendarSelector();
      }
    } catch (Exception e) {
      showError("Error creating default calendar: " + e.getMessage());
    }
  }

  /**
   * Initialize the main frame settings
   */
  private void initializeFrame() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(800, 600);
    setLayout(new BorderLayout());
    setLocationRelativeTo(null);
  }

  /**
   * Initialize all UI components
   */
  private void initializeComponents() {
    // Top panel components
    topPanel = new JPanel();
    monthYearLabel = new JLabel();
    prevMonthButton = new JButton("<");
    nextMonthButton = new JButton(">");
    calendarSelector = new JComboBox<>();
    createCalendarButton = new JButton("New Calendar");

    // Calendar panel (month view)
    monthPanel = new MonthViewPanel(currentYearMonth, this);

    // Status panel
    statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
    statusLabel = new JLabel("Ready");
    statusPanel.add(statusLabel, BorderLayout.WEST);
  }

  /**
   * Initialize the menu bar
   */
  private void initializeMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    // File menu
    JMenu fileMenu = new JMenu("File");

    JMenuItem importMenuItem = new JMenuItem("Import from CSV...");
    importMenuItem.addActionListener(e -> showImportDialog());

    JMenuItem exportMenuItem = new JMenuItem("Export to CSV...");
    exportMenuItem.addActionListener(e -> showExportDialog());

    JMenuItem exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.addActionListener(e -> System.exit(0));

    fileMenu.add(importMenuItem);
    fileMenu.add(exportMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);

    // Calendar menu
    JMenu calendarMenu = new JMenu("Calendar");

    JMenuItem createCalendarMenuItem = new JMenuItem("Create New Calendar...");
    createCalendarMenuItem.addActionListener(e -> showCreateCalendarDialog());

    JMenuItem editCalendarMenuItem = new JMenuItem("Edit Current Calendar...");
    editCalendarMenuItem.addActionListener(e -> showEditCalendarDialog());

    calendarMenu.add(createCalendarMenuItem);
    calendarMenu.add(editCalendarMenuItem);

    // Help menu
    JMenu helpMenu = new JMenu("Help");

    JMenuItem aboutMenuItem = new JMenuItem("About...");
    aboutMenuItem.addActionListener(e -> showAboutDialog());

    helpMenu.add(aboutMenuItem);

    // Add menus to menu bar
    menuBar.add(fileMenu);
    menuBar.add(calendarMenu);
    menuBar.add(helpMenu);

    // Set the menu bar
    setJMenuBar(menuBar);
  }

  /**
   * Layout components in the frame
   */
  private void layoutComponents() {
    // Top panel layout
    topPanel.setLayout(new FlowLayout());
    topPanel.add(prevMonthButton);
    topPanel.add(monthYearLabel);
    topPanel.add(nextMonthButton);
    topPanel.add(new JLabel("Calendar:"));
    topPanel.add(calendarSelector);
    topPanel.add(createCalendarButton);

    // Add panels to frame
    add(topPanel, BorderLayout.NORTH);
    add(monthPanel, BorderLayout.CENTER);
    add(statusPanel, BorderLayout.SOUTH);
  }

  /**
   * Register event listeners for user interactions
   */
  private void registerListeners() {
    // Month navigation
    prevMonthButton.addActionListener(e -> {
      currentYearMonth = currentYearMonth.minusMonths(1);
      updateView();
    });

    nextMonthButton.addActionListener(e -> {
      currentYearMonth = currentYearMonth.plusMonths(1);
      updateView();
    });

    // Calendar selection
    calendarSelector.addActionListener(e -> {
      String selected = (String) calendarSelector.getSelectedItem();
      if (selected != null) {
        try {
          calendarManager.useCalendar(selected);
          updateView();
          setStatus("Using calendar: " + selected);
        } catch (Exception ex) {
          showError("Error switching calendar: " + ex.getMessage());
        }
      }
    });

    // Create new calendar
    createCalendarButton.addActionListener(e -> showCreateCalendarDialog());
  }

  /**
   * Update the calendar selector dropdown with available calendars
   */
  private void updateCalendarSelector() {
    // Remove action listener to prevent unnecessary events
    ActionListener[] listeners = calendarSelector.getActionListeners();
    for (ActionListener listener : listeners) {
      calendarSelector.removeActionListener(listener);
    }

    // Clear and repopulate the dropdown
    calendarSelector.removeAllItems();

    try {
      for (String calendarName : getAvailableCalendars()) {
        calendarSelector.addItem(calendarName);
      }

      // Select the current calendar if possible
      try {
        String currentCalendarName = calendarManager.getCurrentCalendar().getCalendarName();
        calendarSelector.setSelectedItem(currentCalendarName);
      } catch (IllegalStateException e) {
        // No current calendar, just leave the selection as is
      }
    } catch (Exception e) {
      showError("Error loading calendars: " + e.getMessage());
    } finally {
      // Re-add the action listeners
      for (ActionListener listener : listeners) {
        calendarSelector.addActionListener(listener);
      }
    }
  }

  /**
   * Get a list of available calendar names
   *
   * @return Array of calendar names
   */
  private String[] getAvailableCalendars() {
    List<String> calendarNames = new ArrayList<>();
    try {
      for (CalendarManager calendar : calendarManager.getAllCalendars()) {
        calendarNames.add(calendar.getCalendarName());
      }
    } catch (Exception e) {
      showError("Error retrieving calendars: " + e.getMessage());
    }
    return calendarNames.toArray(new String[0]);
  }

  /**
   * Show a dialog to create a new calendar
   */
  private void showCreateCalendarDialog() {
    boolean created = CreateCalendarDialog.showDialog(this, calendarManager);
    if (created) {
      updateCalendarSelector();
      updateView();
      setStatus("Calendar created successfully");
    }
  }

  /**
   * Show a dialog to edit the current calendar's name and timezone
   */
  private void showEditCalendarDialog() {
    try {
      CalendarManager currentCal = calendarManager.getCurrentCalendar();
      String currentName = currentCal.getCalendarName();
      String currentTimezone = currentCal.getTimeZone().getId();

      // Create components for the dialog
      JTextField nameField = new JTextField(currentName, 20);
      JComboBox<String> timezoneComboBox = new JComboBox<>(getAvailableTimezones());
      timezoneComboBox.setSelectedItem(currentTimezone);

      JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
      panel.add(new JLabel("Calendar Name:"));
      panel.add(nameField);
      panel.add(new JLabel("Timezone:"));
      panel.add(timezoneComboBox);

      int result = JOptionPane.showConfirmDialog(
          this,
          panel,
          "Edit Calendar Properties",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
      );

      if (result == JOptionPane.OK_OPTION) {
        String newName = nameField.getText().trim();
        String newTimezone = (String) timezoneComboBox.getSelectedItem();
        boolean changed = false;

        // Edit name if changed
        if (!newName.isEmpty() && !newName.equals(currentName)) {
          calendarManager.editCalendar(currentName, "name", newName);
          // Important: Update currentName for potential timezone edit below
          currentName = newName;
          changed = true;
          setStatus("Calendar renamed to: " + newName);
        }

        // Edit timezone if changed
        if (newTimezone != null && !newTimezone.equals(currentTimezone)) {
          // Use the potentially updated currentName
          calendarManager.editCalendar(currentName, "timezone", newTimezone);
          changed = true;
          setStatus("Calendar timezone updated to: " + newTimezone);
        }

        if (changed) {
          updateCalendarSelector(); // Update dropdown if name changed
          // updateView(); // Consider if a full view update is needed for timezone change
        }
      }
    } catch (IllegalStateException e) {
       showError("No calendar selected to edit.");
    } catch (Exception e) {
      showError("Error editing calendar: " + e.getMessage());
    }
  }

  /**
   * Helper to get sorted available timezone IDs
   * @return Array of timezone IDs
   */
  private String[] getAvailableTimezones() {
      java.util.List<String> availableZones = new java.util.ArrayList<>(java.time.ZoneId.getAvailableZoneIds());
      java.util.Collections.sort(availableZones);
      return availableZones.toArray(new String[0]);
  }

  /**
   * Show the import dialog
   */
  private void showImportDialog() {
    try {
      FileOperationDialog.showImportDialog(this, calendarManager);
      updateView();
    } catch (Exception e) {
      showError("Error showing import dialog: " + e.getMessage());
    }
  }

  /**
   * Show the export dialog
   */
  private void showExportDialog() {
    try {
      FileOperationDialog.showExportDialog(this, calendarManager);
    } catch (Exception e) {
      showError("Error showing export dialog: " + e.getMessage());
    }
  }

  /**
   * Show the about dialog
   */
  private void showAboutDialog() {
    JOptionPane.showMessageDialog(
        this,
        "Calendar Application\n\n" +
            "A Java Swing-based calendar application\n" +
            "Created for Object-Oriented Design course",
        "About Calendar Application",
        JOptionPane.INFORMATION_MESSAGE
    );
  }

  /**
   * Show events for a specific day
   *
   * @param date The date to show events for
   */
  public void showEventsForDay(LocalDate date) {
    try {
      DayEventsDialog.showDialog(this, date, calendarManager);
      // After dialog closes, refresh the calendar view
      updateView();
    } catch (Exception e) {
      showError("Error showing events: " + e.getMessage());
    }
  }

  /**
   * Set the status bar message
   *
   * @param message The message to display
   */
  private void setStatus(String message) {
    statusLabel.setText(message);
  }

  // ICalendarGUI implementation

  @Override
  public void display() {
    SwingUtilities.invokeLater(() -> {
      setVisible(true);
    });
  }

  @Override
  public void showError(String message) {
    JOptionPane.showMessageDialog(
        this,
        message,
        "Error",
        JOptionPane.ERROR_MESSAGE
    );
  }

  @Override
  public void updateView() {
    // Update month/year label
    monthYearLabel.setText(currentYearMonth.getMonth().toString() + " " + currentYearMonth.getYear());

    // Update calendar panel
    monthPanel.setYearMonth(currentYearMonth);

    try {
      // Load events for each day
      monthPanel.loadEvents(calendarManager);
    } catch (Exception e) {
      // Just continue without showing events
    }

    // Update calendar selector
    updateCalendarSelector();

    // Refresh the view
    validate();
    repaint();
  }

  @Override
  public void close() {
    dispose();
  }

  /**
   * Static method to launch the GUI
   *
   * @param calendarManager The model for the calendar application
   * @param controller The controller for the application
   */
  public static void launchGUI(MultiCalendarManager calendarManager, CalendarController controller) {
    SwingUtilities.invokeLater(() -> {
      CalendarGUI gui = new CalendarGUI(calendarManager, controller);
      gui.display();
    });
  }
}
