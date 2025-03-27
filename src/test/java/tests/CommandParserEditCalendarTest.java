package tests;

import static org.junit.Assert.*;
import org.junit.*;
import model.MultiCalendarManager;
import controller.CommandParser;

public class CommandParserEditCalendarTest {

  private MultiCalendarManager multiCal;

  @Before
  public void setUp() throws Exception {
    multiCal = new MultiCalendarManager();
    // For a valid edit, first create a calendar.
    // Using a valid timezone so that subsequent edits work.
    multiCal.createCalendar("Work", "America/New_York");
  }

  @Test(expected = Exception.class)
  public void testEditCalendar_TooFewTokens() throws Exception {
    // Provide a command with fewer than 6 tokens.
    // For example: "edit calendar --name Work" (only 3 tokens)
    String command = "edit calendar --name Work";
    CommandParser.processEditCalendar(command, multiCal);
  }

  @Test
  public void testEditCalendar_MissingNewValue() {
    // Supply a command where "--property" is given but no new value follows.
    // Expected tokens: ["edit", "calendar", "--name", "Work", "--property", "timezone"]
    String command = "edit calendar --name Work --property timezone";
    try {
      CommandParser.processEditCalendar(command, multiCal);
      fail("Expected exception due to missing new value for property.");
    } catch (Exception e) {
      // Expect the exception message thrown by processEditCalendar
      assertTrue(e.getMessage().contains("Invalid edit calendar command parameters."));
    }
  }

  @Test
  public void testEditCalendar_ValidChange() throws Exception {
    // Supply a valid command:
    // Expected tokens:
    // ["edit", "calendar", "--name", "Work", "--property", "timezone", "America/Los_Angeles"]
    String command = "edit calendar --name Work --property timezone America/Los_Angeles";
    CommandParser.processEditCalendar(command, multiCal);
    // The MultiCalendarManager.editCalendar method prints a confirmation message.
    // For example, it should print "Calendar timezone updated to: America/Los_Angeles".
    // Since our test does not capture output here, we can instead verify that the underlying
    // calendar's timezone is updated.
    assertEquals("America/Los_Angeles", multiCal.getCurrentCalendar().getTimeZone().toString());
  }

  @Test
  public void testEditCalendar_ExtraTokens_Ignored() throws Exception {
    // Extra tokens should not confuse the parser.
    // For example: "edit calendar --name Work extraToken --property timezone America/Los_Angeles extraExtra"
    // The parser starts at index 2 and checks for parameter flags.
    String command = "edit calendar --name Work extraToken --property timezone America/Los_Angeles extraExtra";
    // Even if extra tokens appear, the parser should pick up the first occurrence of "--name" and "--property".
    CommandParser.processEditCalendar(command, multiCal);
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
    assertEquals("America/Los_Angeles", multiCal.getCurrentCalendar().getTimeZone().toString());
  }
}
