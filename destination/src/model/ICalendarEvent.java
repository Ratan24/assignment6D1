package model;

import java.time.LocalDateTime;

public interface ICalendarEvent {

  String getEventName();
  void setEventName(String eventName);

  LocalDateTime getStart();
  void setStart(LocalDateTime start);

  LocalDateTime getEnd();
  void setEnd(LocalDateTime end);

  boolean isAllDay();
  void setAllDay(boolean isAllDay);

  String getDescription();
  void setDescription(String description);

  String getLocation();
  void setLocation(String location);

  boolean isPublic();
  void setPublic(boolean isPublic);

  boolean conflictsWith(ICalendarEvent other);

}
