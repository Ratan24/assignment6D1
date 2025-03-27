package calendar;

import controller.CalendarController;
import model.MultiCalendarManager;
import view.OutputHandler;

public class CalendarApp {
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        OutputHandler.getInstance().println("Usage: --mode interactive OR --mode headless" +
                " <commandFile.txt>");
        return;
      }

      // Create a MultiCalendarManager instance.
      MultiCalendarManager multiCal = new MultiCalendarManager();
      // Initialize the controller with the multi-calendar manager.
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
