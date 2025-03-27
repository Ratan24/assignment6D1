package tests;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;
import controller.CalendarController;
import model.MultiCalendarManager;
import view.OutputHandler;

public class CalendarControllerInteractiveTest {

  private ByteArrayOutputStream outContent;
  private PrintStream originalOut;
  private TestInputStream testIn;

  // Custom InputStream that tracks whether close() was called.
  private static class TestInputStream extends InputStream {
    private final ByteArrayInputStream bais;
    private boolean closed = false;
    public TestInputStream(String input) {
      bais = new ByteArrayInputStream(input.getBytes());
    }
    @Override
    public int read() throws IOException {
      return bais.read();
    }
    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }
    public boolean isClosed() {
      return closed;
    }
  }

  @Before
  public void setUp() throws Exception {
    // Capture System.out
    originalOut = System.out;
    outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    // Prepare test input:
    // 1. Create a calendar (so a current calendar exists)
    // 2. Send an invalid command (which should produce an error "Invalid command: invalid command")
    // 3. Exit.
    String simulatedInput =
            "create calendar --name Work --timezone America/New_York\n" +
                    "invalid command\n" +
                    "exit\n";
    testIn = new TestInputStream(simulatedInput);
    System.setIn(testIn);
  }

  @After
  public void tearDown() throws Exception {
    System.setOut(originalOut);
    System.setIn(System.in);
    outContent.reset();
  }

  @Test
  public void testRunInteractiveMode() {
    // Create a MultiCalendarManager and a CalendarController.
    MultiCalendarManager multiCal = new MultiCalendarManager();
    CalendarController controller = new CalendarController(multiCal);

    // Execute interactive mode; it will read our test input.
    controller.runInteractiveMode();

    String output = outContent.toString();
    // Debug output (optional):
    // System.err.println("Captured output: " + output);

    // Check that the welcome message is printed.
    assertTrue("Welcome message missing",
            output.contains("Calendar App Interactive Mode. Type 'exit' to quit."));
    // Check that the prompt is printed.
    assertTrue("Prompt missing", output.contains("> "));
    // Check that the calendar was created.
    assertTrue("Calendar creation message missing",
            output.contains("Calendar created: Work (America/New_York)"));
    // Check that the error message for the invalid command is printed.
    // Note: The error message is printed in the catch block as "Error: " + e.getMessage()
    // and since the invalid command is processed with an existing calendar, the exception message is:
    // "Invalid command: invalid command"
    assertTrue("Error message missing", output.contains("Invalid command: invalid command"));
    // Check that the "Exiting." message is printed.
    assertTrue("Exiting message missing", output.contains("Exiting."));
    // Verify that the custom InputStream was closed.
    assertTrue("Expected System.in to be closed after interactive mode", testIn.isClosed());
  }
}
