package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import model.MultiCalendarManager;
import controller.CommandParser;

/**
 * Tests the calendar editing functionality in CommandParser, ensuring it correctly processes edit
 * commands with various parameter combinations and properly handles error conditions.
 */
public class CommandParserEditCalendarTest {

  private MultiCalendarManager multiCal;

  @Before
  public void setUp() throws Exception {
    multiCal = new MultiCalendarManager();
    multiCal.createCalendar("Work", "America/New_York");
  }

  @Test(expected = Exception.class)
  public void testEditCalendar_TooFewTokens() throws Exception {
    String command = "edit calendar --name Work";
    CommandParser.processEditCalendar(command, multiCal);
  }

  @Test
  public void testEditCalendar_MissingNewValue() {
    String command = "edit calendar --name Work --property timezone";
    try {
      CommandParser.processEditCalendar(command, multiCal);
      fail("Expected exception due to missing new value for property.");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Invalid edit calendar command parameters."));
    }
  }

  @Test
  public void testEditCalendar_ValidChange() throws Exception {
    String command = "edit calendar --name Work --property timezone America/Los_Angeles";
    CommandParser.processEditCalendar(command, multiCal);
    assertEquals("America/Los_Angeles", multiCal.getCurrentCalendar().getTimeZone().toString());
  }

  @Test
  public void testEditCalendar_ExtraTokens_Ignored() throws Exception {
    String command = "edit calendar --name Work extraToken --property timezone " +
        "America/Los_Angeles extraExtra";
    CommandParser.processEditCalendar(command, multiCal);
    assertEquals("Work", multiCal.getCurrentCalendar().getCalendarName());
    assertEquals("America/Los_Angeles", multiCal.getCurrentCalendar().getTimeZone().toString());
  }
}
