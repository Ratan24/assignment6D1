package tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import org.junit.Before;
import org.junit.Test;

import model.MultiCalendarManager;
import controller.CommandParser;

/**
 * Tests the calendar creation functionality in CommandParser, verifying correct parsing of command
 * parameters and appropriate error handling for various invalid input scenarios.
 */
public class CommandParserCreateCalendarTest {

  private MultiCalendarManager multiCal;

  @Before
  public void setUp() throws Exception {
    multiCal = new MultiCalendarManager();
  }

  @Test
  public void testProcessCreateCalendar_Valid() throws Exception {
    String command = "create calendar --name Work --timezone America/New_York";
    CommandParser.processCreateCalendar(command, multiCal);
    assertNotNull("Expected current calendar to be created", multiCal.getCurrentCalendar());
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
  }

  @Test(expected = Exception.class)
  public void testProcessCreateCalendar_TooFewTokens() throws Exception {
    String command = "create calendar --name Work";
    CommandParser.processCreateCalendar(command, multiCal);
  }

  @Test
  public void testProcessCreateCalendar_MissingTimezoneValue() {
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
    String command = "create calendar --name Work extraToken --timezone America/New_York";
    CommandParser.processCreateCalendar(command, multiCal);
    assertNotNull("Expected current calendar to be created", multiCal.getCurrentCalendar());
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
  }
}