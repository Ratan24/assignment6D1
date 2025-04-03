package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import model.MultiCalendarManager;

/**
 * Dialog for importing/exporting calendar data to CSV files
 */
public class FileOperationDialog extends JDialog {

  private enum OperationType {
    IMPORT,
    EXPORT
  }

  private CalendarGUI parentGui; // Added reference to parent
  private MultiCalendarManager calendarManager;
  private OperationType operationType;
  private JFileChooser fileChooser;
  private JButton executeButton;
  private JButton cancelButton;

  /**
   * Constructor
   *
   * @param parent The parent frame
   * @param calendarManager The calendar manager
   * @param type The operation type (import or export)
   */
  public FileOperationDialog(CalendarGUI parent, MultiCalendarManager calendarManager, OperationType type) {
    super(parent, type == OperationType.IMPORT ? "Import from CSV" : "Export to CSV", true);
    this.parentGui = parent; // Store parent
    this.calendarManager = calendarManager;
    this.operationType = type;

    // Initialize components
    initializeComponents();

    // Layout
    layoutComponents();

    // Register listeners
    registerListeners();

    // Final setup
    setSize(600, 400);
    setLocationRelativeTo(parent);
  }

  /**
   * Initialize dialog components
   */
  private void initializeComponents() {
    fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
      }

      @Override
      public String getDescription() {
        return "CSV Files (*.csv)";
      }
    });

    // Set current directory to user directory
    fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

    executeButton = new JButton(operationType == OperationType.IMPORT ? "Import" : "Export");
    cancelButton = new JButton("Cancel");
  }

  /**
   * Layout the components in the dialog
   */
  private void layoutComponents() {
    JPanel contentPanel = new JPanel(new BorderLayout());

    // Add file chooser
    contentPanel.add(fileChooser, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(executeButton);
    buttonPanel.add(cancelButton);

    contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    setContentPane(contentPanel);
  }

  /**
   * Register event listeners
   */
  private void registerListeners() {
    executeButton.addActionListener(e -> {
      File selectedFile = fileChooser.getSelectedFile();
      if (selectedFile != null) {
        if (operationType == OperationType.EXPORT) {
          // Make sure the file has a .csv extension
          String path = selectedFile.getAbsolutePath();
          if (!path.toLowerCase().endsWith(".csv")) {
            path += ".csv";
            selectedFile = new File(path);
          }
        }

        // Perform the operation
        if (performOperation(selectedFile)) {
          dispose();
        }
      } else {
        JOptionPane.showMessageDialog(
            this,
            "Please select a file",
            "File Required",
            JOptionPane.WARNING_MESSAGE
        );
      }
    });

    cancelButton.addActionListener(e -> dispose());

    // Close dialog when ESC key is pressed
    getRootPane().registerKeyboardAction(
        e -> dispose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW
    );
  }

  /**
   * Perform the import or export operation
   *
   * @param file The file to import from or export to
   * @return true if the operation succeeded, false otherwise
   */
  private boolean performOperation(File file) {
    try {
      if (operationType == OperationType.EXPORT) {
        // Export calendar to Google-compatible CSV
        calendarManager.getCurrentCalendar().exportToGoogleCSV(file.getAbsolutePath());

        JOptionPane.showMessageDialog(
            this,
            "Calendar exported successfully to " + file.getAbsolutePath(),
            "Export Successful",
            JOptionPane.INFORMATION_MESSAGE
        );

        return true;
      } else { // Import
        int importedCount = calendarManager.getCurrentCalendar().importFromGoogleCSV(file.getAbsolutePath());

        JOptionPane.showMessageDialog(
            this,
            "Import complete.\nSuccessfully imported: " + importedCount + " events.",
            "Import Successful",
            JOptionPane.INFORMATION_MESSAGE
        );
        // Trigger view refresh in the parent GUI
        if (parentGui != null) {
            parentGui.updateView();
        }
        return true;
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          this,
          "Error: " + e.getMessage(),
          "Operation Failed",
          JOptionPane.ERROR_MESSAGE
      );

      return false;
    }
  }

  /**
   * Static method to show the import dialog
   *
   * @param parent The parent GUI frame
   * @param calendarManager The calendar manager
   */
  public static void showImportDialog(CalendarGUI parent, MultiCalendarManager calendarManager) {
    FileOperationDialog dialog = new FileOperationDialog(
        parent, calendarManager, OperationType.IMPORT
    );
    dialog.setVisible(true);
  }

  /**
   * Static method to show the export dialog
   *
   * @param parent The parent GUI frame
   * @param calendarManager The calendar manager
   */
  public static void showExportDialog(CalendarGUI parent, MultiCalendarManager calendarManager) {
    FileOperationDialog dialog = new FileOperationDialog(
        parent, calendarManager, OperationType.EXPORT
    );
    dialog.setVisible(true);
  }
}
