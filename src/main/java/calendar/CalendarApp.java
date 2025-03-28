package calendar;

import controller.CalendarController;
import model.MultiCalendarManager;
import view.OutputHandler;

/**
 * Main application class for the Calendar application. Handles command-line arguments and
 * initializes the application.
 */
public class CalendarApp {

  /**
   * Entry point of the application. Supports interactive and headless modes.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        OutputHandler.getInstance().println("Usage: --mode interactive OR --mode headless"
            + " <commandFile.txt>");
        return;
      }

      MultiCalendarManager multiCal = new MultiCalendarManager();
      CalendarController controller = new CalendarController(multiCal);

      if (args[0].equalsIgnoreCase("--mode")) {
        if (args[1].equalsIgnoreCase("interactive")) {
          controller.runInteractiveMode();
        } else if (args[1].equalsIgnoreCase("headless")) {
          if (args.length < 3) {
            OutputHandler.getInstance().println("Headless mode requires a command file.");
            return;
          }
          controller.runHeadlessMode(args[2]);
        } else {
          OutputHandler.getInstance().println("Invalid mode. Use interactive or headless.");
        }
      }
    } catch (Exception e) {
      OutputHandler.getInstance().println("Error: " + e.getMessage());
    }
  }
}