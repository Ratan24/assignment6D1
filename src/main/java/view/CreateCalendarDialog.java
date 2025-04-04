package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// import model.MultiCalendarManager; // Removed model import
import controller.ICalendarController; // Added controller import

/**
 * Dialog for creating a new calendar with a specific timezone
 */
public class CreateCalendarDialog extends JDialog {

  private JTextField calendarNameField;
  private JComboBox<String> timezoneComboBox;
  private JButton createButton;
  private JButton cancelButton;
  private boolean approved = false;

  /**
   * Constructor
   *
   * @param parent The parent frame
   */
  public CreateCalendarDialog(Frame parent) {
    super(parent, "Create New Calendar", true);

    // Initialize components
    initializeComponents();

    // Layout
    layoutComponents();

    // Register listeners
    registerListeners();

    // Final setup
    pack();
    setLocationRelativeTo(parent);
  }

  /**
   * Initialize dialog components
   */
  private void initializeComponents() {
    calendarNameField = new JTextField(20);

    // Get all available time zones
    List<String> availableZones = new ArrayList<>(ZoneId.getAvailableZoneIds());
    Collections.sort(availableZones);

    timezoneComboBox = new JComboBox<>(availableZones.toArray(new String[0]));

    // Try to select the system default timezone
    String defaultZone = ZoneId.systemDefault().getId();
    timezoneComboBox.setSelectedItem(defaultZone);

    createButton = new JButton("Create");
    cancelButton = new JButton("Cancel");
  }

  /**
   * Layout the components in the dialog
   */
  private void layoutComponents() {
    JPanel contentPanel = new JPanel(new BorderLayout());

    // Form panel
    JPanel formPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Calendar name
    gbc.gridx = 0;
    gbc.gridy = 0;
    formPanel.add(new JLabel("Calendar Name:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    formPanel.add(calendarNameField, gbc);

    // Timezone
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0.0;
    formPanel.add(new JLabel("Timezone:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    formPanel.add(timezoneComboBox, gbc);

    contentPanel.add(formPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(createButton);
    buttonPanel.add(cancelButton);

    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(contentPanel);
  }

  /**
   * Register event listeners
   */
  private void registerListeners() {
    createButton.addActionListener(e -> {
      if (validateInputs()) {
        approved = true;
        dispose();
      }
    });

    cancelButton.addActionListener(e -> {
      approved = false;
      dispose();
    });

    // Close dialog when ESC key is pressed
    getRootPane().registerKeyboardAction(
        e -> {
          approved = false;
          dispose();
        },
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW
    );
  }

  /**
   * Validate user inputs before creating calendar
   *
   * @return true if inputs are valid, false otherwise
   */
  private boolean validateInputs() {
    String name = getCalendarName();
    if (name.trim().isEmpty()) {
      JOptionPane.showMessageDialog(
          this,
          "Please enter a calendar name",
          "Input Error",
          JOptionPane.ERROR_MESSAGE
      );
      return false;
    }

    return true;
  }

  /**
   * Get the entered calendar name
   *
   * @return The calendar name
   */
  public String getCalendarName() {
    return calendarNameField.getText();
  }

  /**
   * Get the selected timezone
   *
   * @return The timezone ID
   */
  public String getTimezone() {
    return (String) timezoneComboBox.getSelectedItem();
  }

  /**
   * Check if the dialog was approved
   *
   * @return true if approved, false if cancelled
   */
  public boolean isApproved() {
    return approved;
  }

  /**
   * Static method to show the dialog and create a calendar if approved
   *
   * @param parent The parent frame
   * @param controller The controller to use for creating the calendar
   * @return true if a calendar was created, false otherwise
   */
  public static boolean showDialog(Frame parent, ICalendarController controller) { // Accept controller
    CreateCalendarDialog dialog = new CreateCalendarDialog(parent);
    dialog.setVisible(true);

    if (dialog.isApproved()) {
      try {
        // Use controller to create calendar
        controller.createCalendar(
            dialog.getCalendarName(),
            dialog.getTimezone()
        );
        return true;
      } catch (Exception e) {
        JOptionPane.showMessageDialog(
            parent,
            "Error creating calendar: " + e.getMessage(),
            "Calendar Creation Error",
            JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
    }

    return false;
  }
}
