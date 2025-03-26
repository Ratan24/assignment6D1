package calendar;

import controller.CalendarController;
import model.CalendarManager;
import view.OutputHandler;

public class CalendarApp {

  public static void main(String[] args) {
    if (args.length < 2) {
      OutputHandler.getInstance().println("Usage: --mode interactive OR --mode headless <commandFile.txt>");
      return;
    }

    CalendarManager calendar = new CalendarManager();
    CalendarController controller = new CalendarController(calendar);

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
  }

}
