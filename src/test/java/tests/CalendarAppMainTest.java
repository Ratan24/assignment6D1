package tests;

import calendar.CalendarApp;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Test file for CalendarApp main method.
 */
public class CalendarAppMainTest {

  // Utility method to capture system output during a runnable's execution.
  private String captureOutput(Runnable runnable) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(baos));
    try {
      runnable.run();
    } finally {
      System.setOut(originalOut);
    }
    return baos.toString();
  }

  @Test
  public void testMainNoArguments() {
    String output = captureOutput(() -> CalendarApp.main(new String[]{}));
    // Expect usage instructions printed.
    assertTrue("Should print usage instructions",
            output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  @Test
  public void testMainOneArgument() {
    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode"}));
    assertTrue("Should print usage instructions",
            output.contains("Usage: --mode interactive OR --mode headless <commandFile.txt>"));
  }

  @Test
  public void testMainFirstArgNotMode() {
    // When the first argument is not "--mode", no command is executed.
    String output = captureOutput(() -> CalendarApp.main(new String[]{"wrong", "interactive"}));
    // In the updated design, if the first argument is not "--mode", nothing is printed.
    assertEquals("No output expected if first arg is not '--mode'", "", output.trim());
  }

  @Test
  public void testMainInteractiveMode() {
    // Simulate interactive input: create an event, then "exit".
    String simulatedInput = "create event InteractiveTest on 2025-03-05\nexit\n";
    InputStream originalIn = System.in;
    System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode", "interactive"}));

    System.setIn(originalIn);
    // Expect to see at least a prompt and "Exiting."
    assertTrue("Interactive mode should prompt for input", output.contains(">"));
    assertTrue("Interactive mode should print 'Exiting.'", output.contains("Exiting."));
  }

//  @Test
//  public void testMainHeadlessMode_ValidFile() throws Exception {
//    // Create a temporary file with valid commands and an exit.
//    File temp = File.createTempFile("commands", ".txt");
//    try (PrintWriter writer = new PrintWriter(temp)) {
//      writer.println("create event HeadlessTest on 2025-03-06");
//      writer.println("exit");
//    }
//    String output = captureOutput(() ->
//            CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()})
//    );
//    // Check that an all‑day event is created (adjust expected text as per your OutputHandler output).
//    assertTrue("Headless mode should create an all-day event",
//            output.contains("All-day event created:"));
//    temp.delete();
//  }

  @Test
  public void testMainHeadlessMode_MissingFileArg() {
    String output = captureOutput(() -> CalendarApp.main(new String[]{"--mode", "headless"}));
    assertTrue("Should print message about missing command file",
            output.contains("Headless mode requires a command file."));
  }

  @Test
  public void testMainHeadlessMode_NonExistentFile() {
    String output = captureOutput(() ->
            CalendarApp.main(new String[]{"--mode", "headless", "nonexistent_file.txt"})
    );
    assertTrue("Should print error reading file", output.contains("Error reading file:"));
  }

  @Test
  public void testMainHeadlessMode_InvalidCommand() throws Exception {
    // Create a temporary file with an invalid command.
    File temp = File.createTempFile("invalidCommands", ".txt");
    try (PrintWriter writer = new PrintWriter(temp)) {
      writer.println("invalid command");
      writer.println("exit");
    }
    String output = captureOutput(() ->
            CalendarApp.main(new String[]{"--mode", "headless", temp.getAbsolutePath()})
    );
    assertTrue("Should print command error", output.contains("Command error:"));
    temp.delete();
  }

  @Test
  public void testMainInvalidMode() {
    String output = captureOutput(() ->
            CalendarApp.main(new String[]{"--mode", "foobar"})
    );
    assertTrue("Should indicate invalid mode",
            output.contains("Invalid mode. Use interactive or headless."));
  }
}
