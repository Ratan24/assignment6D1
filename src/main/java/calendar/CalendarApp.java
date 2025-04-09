package calendar;

// Import the enhanced controller and its interface
import controller.EnhancedCalendarController;
import controller.IEnhancedCalendarController;
// Keep original controller for non-GUI modes for now
import controller.CalendarController;
import model.MultiCalendarManager;
import view.CalendarGUI;
import view.OutputHandler; // Still needed for non-GUI modes via original controller

/**
 * Main application class for the Calendar application.
 * Handles command-line arguments and launches the appropriate mode:
 * - GUI mode (default)
 * - Interactive text mode
 * - Headless script mode
 */
public class CalendarApp {

  /**
   * Main method
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    try {
      // Create model
      MultiCalendarManager calendarManager = new MultiCalendarManager();

      // Determine mode and create appropriate controller
      if (args.length == 0 || (args.length >= 2 && args[0].equalsIgnoreCase("--mode") && args[1].equalsIgnoreCase("gui"))) {
        // GUI Mode: Use Enhanced Controller
        EnhancedCalendarController enhancedController = new EnhancedCalendarController(calendarManager);
        enhancedController.initialize(); // Initialize (registers listener)
        launchGUIMode(enhancedController); // Pass enhanced controller

      } else if (args.length >= 2 && args[0].equalsIgnoreCase("--mode")) {
        // Non-GUI Modes: Use Original Controller (for now)
        // TODO: Consider refactoring original controller or CommandParser later
        //       to also avoid direct OutputHandler use if strict MVC is desired everywhere.
        CalendarController originalController = new CalendarController(calendarManager);
        String mode = args[1].toLowerCase();
        switch (mode) {
          case "interactive":
            originalController.runInteractiveMode();
            break;
          case "headless":
            if (args.length < 3) {
              OutputHandler.getInstance().println("Headless mode requires a command file."); // Original controller might still use this
              printUsage();
            } else {
              originalController.runHeadlessMode(args[2]);
            }
            break;
          // case "gui": // Already handled above
          //   launchGUIMode(controller);
          //   break;
          default:
            OutputHandler.getInstance().println("Invalid mode: " + mode); // Keep for invalid non-GUI modes
            printUsage();
            break;
        }
      } else {
        // Invalid arguments
        printUsage();
      }
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error: " + e.getMessage());
    }
  }

  /**
   * Launch the GUI mode
   * Launch the GUI mode using the enhanced controller.
   *
   * @param controller The enhanced controller instance.
   */
  private static void launchGUIMode(IEnhancedCalendarController controller) { // Accept enhanced interface
    // Pass the enhanced controller to the GUI's launch method
    CalendarGUI.launchGUI(controller);
  }

  /**
   * Print usage instructions
   */
  private static void printUsage() {
    OutputHandler.getInstance().println("Usage:");
    OutputHandler.getInstance().println("  java -jar Calendar.jar");
    OutputHandler.getInstance().println("    Launches the GUI mode");
    OutputHandler.getInstance().println("  java -jar Calendar.jar --mode interactive");
    OutputHandler.getInstance().println("    Launches in interactive text mode");
    OutputHandler.getInstance().println("  java -jar Calendar.jar --mode headless <script-file>");
    OutputHandler.getInstance().println("    Executes commands from the script file in headless mode");
  }
}
