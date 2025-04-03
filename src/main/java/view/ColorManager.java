package view;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manage colors for different calendars.
 * Ensures each calendar has a consistent color throughout the UI.
 */
public class ColorManager {

  private static final Color[] PREDEFINED_COLORS = {
      new Color(66, 133, 244),  // Blue
      new Color(219, 68, 55),   // Red
      new Color(15, 157, 88),   // Green
      new Color(244, 180, 0),   // Yellow
      new Color(171, 71, 188),  // Purple
      new Color(255, 112, 67),  // Orange
      new Color(0, 121, 107),   // Teal
      new Color(3, 155, 229)    // Light Blue
  };

  private Map<String, Color> calendarColors = new HashMap<>();
  private int nextColorIndex = 0;

  /**
   * Get the color for a specific calendar
   *
   * @param calendarName The name of the calendar
   * @return The color assigned to the calendar
   */
  public Color getColorForCalendar(String calendarName) {
    if (!calendarColors.containsKey(calendarName)) {
      // Assign a new color from our predefined set
      Color newColor = PREDEFINED_COLORS[nextColorIndex % PREDEFINED_COLORS.length];
      calendarColors.put(calendarName, newColor);
      nextColorIndex++;
    }

    return calendarColors.get(calendarName);
  }

  /**
   * Clear all assigned colors
   */
  public void clear() {
    calendarColors.clear();
    nextColorIndex = 0;
  }
}