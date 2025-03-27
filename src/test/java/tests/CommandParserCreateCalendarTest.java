package tests;

import static org.junit.Assert.*;
import org.junit.*;
import model.MultiCalendarManager;
import controller.CommandParser;

public class CommandParserCreateCalendarTest {

  private MultiCalendarManager multiCal;

  @Before
  public void setUp() throws Exception {
    multiCal = new MultiCalendarManager();
  }

  @Test
  public void testProcessCreateCalendar_Valid() throws Exception {
    // Valid command: expected tokens are:
    // [create, calendar, --name, Work, --timezone, America/New_York]
    String command = "create calendar --name Work --timezone America/New_York";
    CommandParser.processCreateCalendar(command, multiCal);
    // The current calendar should be created and its name should be "Work"
    assertNotNull("Expected current calendar to be created", multiCal.getCurrentCalendar());
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
  }

  @Test(expected = Exception.class)
  public void testProcessCreateCalendar_TooFewTokens() throws Exception {
    // Provide a command with too few tokens (only 4 tokens), which should fail
    // Expected error: "Invalid create calendar command format."
    String command = "create calendar --name Work";
    CommandParser.processCreateCalendar(command, multiCal);
  }

  @Test
  public void testProcessCreateCalendar_MissingTimezoneValue() {
    // Provide a command with 5 tokens: "create calendar --name Work --timezone"
    // Since no timezone value is provided, calName will be set ("Work") but tz remains null.
    // This should cause an exception with message "Calendar name and timezone must be provided."
    String command = "create calendar --name Work --timezone";
    try {
      CommandParser.processCreateCalendar(command, multiCal);
      fail("Expected exception due to missing timezone value");
    } catch (Exception e) {
      assertTrue("Expected error about missing calendar name and timezone",
              e.getMessage().contains("Calendar name and timezone must be provided."));
    }
  }

  @Test
  public void testProcessCreateCalendar_ExtraTokens() throws Exception {
    // Supply a command with extra tokens between parameters.
    // For example: "create calendar --name Work extraToken --timezone America/New_York"
    // The parser should still correctly pick up calName as "Work" and tz as "America/New_York".
    String command = "create calendar --name Work extraToken --timezone America/New_York";
    CommandParser.processCreateCalendar(command, multiCal);
    assertNotNull("Expected current calendar to be created", multiCal.getCurrentCalendar());
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
  }
}
