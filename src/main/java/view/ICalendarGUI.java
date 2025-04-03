package view;

/**
 * Interface for GUI views of the calendar application.
 * Defines the contract for any GUI implementation.
 */
public interface ICalendarGUI {

  /**
   * Initialize and display the GUI
   */
  void display();

  /**
   * Show an error message to the user
   *
   * @param message The error message to display
   */
  void showError(String message);

  /**
   * Update the calendar view to reflect changes in the model
   */
  void updateView();

  /**
   * Close the GUI
   */
  void close();
}