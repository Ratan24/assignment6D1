package calendar;

import controller.CalendarController;
import model.MultiCalendarManager;
import view.CalendarGUI;
import view.OutputHandler;

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
      // Create model and controller
      MultiCalendarManager calendarManager = new MultiCalendarManager();
      CalendarController controller = new CalendarController(calendarManager);

      // Process command-line arguments
      if (args.length == 0) {
        // Default to GUI mode if no arguments
        launchGUIMode(calendarManager, controller);
      } else if (args.length >= 2 && args[0].equalsIgnoreCase("--mode")) {
        // Process mode
        String mode = args[1].toLowerCase();
        switch (mode) {
          case "interactive":
            controller.runInteractiveMode();
            break;
          case "headless":
            if (args.length < 3) {
              OutputHandler.getInstance().println("Headless mode requires a command file.");
              printUsage();
            } else {
              controller.runHeadlessMode(args[2]);
            }
            break;
          case "gui":
            launchGUIMode(calendarManager, controller);
            break;
          default:
            OutputHandler.getInstance().println("Invalid mode: " + mode);
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
   *
   * @param calendarManager The model
   * @param controller The controller
   */
  private static void launchGUIMode(MultiCalendarManager calendarManager, CalendarController controller) {
    CalendarGUI.launchGUI(calendarManager, controller);
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