package controller;

import model.ICalendarManager;
import view.OutputHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * CalendarController implements ICalendarController, providing
 * both interactive (console-based) and headless (file-based) modes
 * for executing user commands on an ICalendarManager.
 */
public class CalendarController implements ICalendarController {

  private final ICalendarManager calendar;

  /**
   * Constructs the controller with a given ICalendarManager implementation.
   */
  public CalendarController(ICalendarManager calendar) {
    this.calendar = calendar;
  }

  /**
   * In interactive mode, we repeatedly prompt the user for commands
   * until they type "exit". Each command is then passed to CommandParser
   * for processing.
   */
  @Override
  public void runInteractiveMode() {
    Scanner consoleScanner = new Scanner(System.in);
    OutputHandler.getInstance().println("Calendar App Interactive Mode. Type 'exit' to quit.");

    while (true) {
      OutputHandler.getInstance().println("> ");
      String userCommand = consoleScanner.nextLine();

      if (userCommand.equalsIgnoreCase("exit")) {
        OutputHandler.getInstance().println("Exiting.");
        break;
      }

      try {
        CommandParser.processCommand(userCommand, calendar);
      } catch (Exception e) {
        OutputHandler.getInstance().println("Error: " + e.getMessage());
      }
    }

    consoleScanner.close();
  }

  /**
   * In headless mode, we read commands line-by-line from a file
   * until we reach 'exit' or end-of-file. Each command is passed to
   * CommandParser for execution on the ICalendarManager.
   */
  @Override
  public void runHeadlessMode(String fileName) {
    try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
      String commandLine;

      while ((commandLine = fileReader.readLine()) != null) {
        OutputHandler.getInstance().println("> " + commandLine);

        if (commandLine.equalsIgnoreCase("exit")) {
          OutputHandler.getInstance().println("Exiting.");
          break;
        }

        CommandParser.processCommand(commandLine, calendar);
      }
    } catch (IOException e) {
      OutputHandler.getInstance().println("Error reading file: " + e.getMessage());
    } catch (Exception e) {
      OutputHandler.getInstance().println("Command error: " + e.getMessage());
    }
  }
}
