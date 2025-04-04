package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Added for potential future use if controller returns Map

// Removed model imports, will use controller
// import model.MultiCalendarManager;
// import model.CalendarManager;
// import model.ICalendarEvent;
import controller.ICalendarController; // Use interface

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
  // private MultiCalendarManager calendarManager; // Removed model reference
  private ICalendarController controller; // Use interface
  private ColorManager colorManager;

  // State
  private YearMonth currentYearMonth;

  /**
   * Constructor for the GUI
   *
   * @param controller The controller for handling user actions and data access
   */
  public CalendarGUI(ICalendarController controller) { // Accept interface
    super("Calendar Application");
    // this.calendarManager = calendarManager; // Remove model reference
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
      // Ask controller to handle default creation if needed
      controller.createDefaultCalendarIfNeeded();
      updateCalendarSelector(); // Update selector after potential creation
    } catch (Exception e) {
      showError("Error during initial calendar setup: " + e.getMessage());
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

    // Calendar panel (month view) - Pass controller instead of this (parent) for data access?
    // For now, keep passing 'this' and let MonthViewPanel call back to GUI, which calls controller.
    // Alternative: Pass controller directly: new MonthViewPanel(currentYearMonth, controller);
    monthPanel = new MonthViewPanel(currentYearMonth, this); // Keep passing parent GUI for now

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
      // Avoid triggering on initial population or removal
      if (selected != null && e.getActionCommand().equals("comboBoxChanged")) {
          try {
              controller.switchCalendar(selected);
              updateView(); // Update view after switching
              setStatus("Using calendar: " + selected);
          } catch (Exception ex) {
              showError("Error switching calendar: " + ex.getMessage());
              // Optionally, re-select the previously active calendar if switch fails
              updateCalendarSelector();
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
    String currentCalendarName = null;
    try {
      // Get names from controller
      List<String> calendarNames = controller.getAvailableCalendarNames();
      currentCalendarName = controller.getCurrentCalendarName(); // Get current name

      for (String calendarName : calendarNames) {
        calendarSelector.addItem(calendarName);
      }

      // Select the current calendar if possible
      if (currentCalendarName != null) {
        calendarSelector.setSelectedItem(currentCalendarName);
      } else if (!calendarNames.isEmpty()) {
        // If no current one is set but list isn't empty, select the first one
        calendarSelector.setSelectedIndex(0);
        // Optionally, tell controller to use this one? Depends on desired behavior.
        // controller.switchCalendar(calendarNames.get(0));
      }

    } catch (Exception e) {
      showError("Error loading calendars: " + e.getMessage());
    } finally {
      // Re-add the action listeners
      for (ActionListener listener : listeners) {
        calendarSelector.addActionListener(listener);
      }
      // Ensure the selected item reflects the actual current calendar after listeners are added
      if (currentCalendarName != null) {
          calendarSelector.setSelectedItem(currentCalendarName);
      }
    }
  }

  /**
   * Get a list of available calendar names
   *
   * @return List of calendar names (changed from array)
   */
  private List<String> getAvailableCalendars() {
    try {
      // Get names directly from controller
      return controller.getAvailableCalendarNames();
    } catch (Exception e) {
      showError("Error retrieving calendars: " + e.getMessage());
      return new ArrayList<>(); // Return empty list on error
    }
  }

  /**
   * Show a dialog to create a new calendar
   */
  private void showCreateCalendarDialog() {
    // Pass controller to the dialog
    boolean created = CreateCalendarDialog.showDialog(this, controller);
    if (created) {
      updateCalendarSelector(); // Refresh dropdown
      // updateView(); // View might not need full update, selector handles current
      setStatus("Calendar created successfully");
    }
  }

  /**
   * Show a dialog to edit the current calendar's name and timezone
   */
  private void showEditCalendarDialog() {
    try {
      // Get current details from controller
      String currentName = controller.getCurrentCalendarName();
      String currentTimezone = controller.getCurrentCalendarTimezone();

      if (currentName == null) {
          showError("No calendar selected to edit.");
          return;
      }

      // Create components for the dialog
      JTextField nameField = new JTextField(currentName, 20);
      // Get timezones from controller
      JComboBox<String> timezoneComboBox = new JComboBox<>(controller.getAvailableTimezones());
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
          // Use controller to edit
          controller.editCalendar(currentName, "name", newName);
          // Important: Update currentName for potential timezone edit below
          currentName = newName; // Keep track locally for next potential edit call
          changed = true;
          setStatus("Calendar renamed to: " + newName);
        }

        // Edit timezone if changed
        if (newTimezone != null && !newTimezone.equals(currentTimezone)) {
          // Use controller to edit (using potentially updated currentName)
          controller.editCalendar(currentName, "timezone", newTimezone);
          changed = true;
          setStatus("Calendar timezone updated to: " + newTimezone);
        }

        if (changed) {
          updateCalendarSelector(); // Update dropdown if name changed
          // updateView(); // View update might be needed if timezone affects display
        }
      }
    } catch (IllegalStateException e) { // Should be caught earlier now
       showError("No calendar selected to edit.");
    } catch (Exception e) {
      showError("Error editing calendar: " + e.getMessage());
    }
  }

  /**
   * Helper to get sorted available timezone IDs - Now handled by Controller
   * @return Array of timezone IDs
   */
  // private String[] getAvailableTimezones() { ... } // Removed

  /**
   * Show the import dialog
   */
  private void showImportDialog() {
    try {
      // Pass controller to dialog
      FileOperationDialog.showImportDialog(this, controller);
      // Dialog should trigger updateView if needed (already does)
      // updateView(); // Removed, handled by dialog callback/parent reference
    } catch (Exception e) {
      showError("Error showing import dialog: " + e.getMessage());
    }
  }

  /**
   * Show the export dialog
   */
  private void showExportDialog() {
    try {
      // Pass controller to dialog
      FileOperationDialog.showExportDialog(this, controller);
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
      // Pass controller to dialog
      DayEventsDialog.showDialog(this, date, controller);
      // Dialog should trigger updateView if needed (already does via parent ref)
      // updateView(); // Removed, handled by dialog callback/parent reference
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
      // Pass controller to month panel to load events
      monthPanel.loadEvents(controller);
    } catch (Exception e) {
      showError("Error loading events for month view: " + e.getMessage());
      // Optionally clear events in panel: monthPanel.clearEvents();
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
   * @param controller The controller for the application
   */
  public static void launchGUI(ICalendarController controller) { // Accept interface
    SwingUtilities.invokeLater(() -> {
      // Pass only the controller to the GUI constructor
      CalendarGUI gui = new CalendarGUI(controller);
      gui.display();
    });
  }
}
