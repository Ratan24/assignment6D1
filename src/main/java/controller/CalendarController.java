package controller;

import model.ICalendarManager;
import model.MultiCalendarManager;
import view.OutputHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * CalendarController implements ICalendarController.
 * It now uses a MultiCalendarManager to support multiple calendars.
 */
public class CalendarController implements ICalendarController {

  private final MultiCalendarManager multiCal;

  // The currently selected calendar.
  public CalendarController(MultiCalendarManager multiCal) {
    this.multiCal = multiCal;
  }

  /**
   * Interactive mode: continuously read user commands.
   */
  @Override
  public void runInteractiveMode() {
    Scanner scanner = new Scanner(System.in);
    OutputHandler.getInstance().println("Calendar App Interactive Mode. Type 'exit' to quit.");
    while (true) {
      OutputHandler.getInstance().println("> ");
      String userCommand = scanner.nextLine();
      if (userCommand.equalsIgnoreCase("exit")) {
        OutputHandler.getInstance().println("Exiting.");
        break;
      }
      try {
        // Delegate command processing to CommandParser.
        CommandParser.processCommand(userCommand, multiCal);
      } catch (Exception e) {
        OutputHandler.getInstance().println("Error: " + e.getMessage());
      }
    }
    scanner.close();
  }

  /**
   * Headless mode: read commands from a file.
   */
  @Override
  public void runHeadlessMode(String fileName) {
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String command;
      while ((command = br.readLine()) != null) {
        OutputHandler.getInstance().println("> " + command);
        if (command.equalsIgnoreCase("exit")) {
          OutputHandler.getInstance().println("Exiting.");
          break;
        }
        CommandParser.processCommand(command, multiCal);
      }
    } catch (IOException e) {
      OutputHandler.getInstance().println("Error reading file: " + e.getMessage());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Command error: " + e.getMessage());
    }
  }
}