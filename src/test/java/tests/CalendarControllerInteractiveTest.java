package tests;



import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import controller.CalendarController;
import model.MultiCalendarManager;


/**
 * Tests the interactive mode functionality of CalendarController. Verifies command processing,
 * output messages, and resource management using simulated user input.
 */
public class CalendarControllerInteractiveTest {

  private ByteArrayOutputStream outContent;
  private PrintStream originalOut;
  private TestInputStream testIn;

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
    originalOut = System.out;
    outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
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
    MultiCalendarManager multiCal = new MultiCalendarManager();
    CalendarController controller = new CalendarController(multiCal);

    controller.runInteractiveMode();

    String output = outContent.toString();

    assertTrue("Welcome message missing",
        output.contains("Calendar App Interactive Mode. Type 'exit' to quit."));
    assertTrue("Prompt missing", output.contains("> "));
    assertTrue("Calendar creation message missing",
        output.contains("Calendar created: Work (America/New_York)"));
    assertTrue("Error message missing", output.contains("Invalid command: invalid command"));
    assertTrue("Exiting message missing", output.contains("Exiting."));
    assertTrue("Expected System.in to be closed after interactive mode", testIn.isClosed());
  }
}