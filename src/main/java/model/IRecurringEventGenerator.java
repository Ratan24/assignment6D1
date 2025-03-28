package model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for generating recurring calendar events.
 */

public interface IRecurringEventGenerator {

  /**
   * Generates recurring events based on the provided repeatPart.
   */
  List<ICalendarEvent> generateRecurringEventsInterface(
      String eventName,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime,
      String repeatPart,
      boolean isAllDay
  ) throws Exception;
}
