package controller;

/**
 * This is the interface of IcalendarController to handle interactive and headlessMode.
 */
public interface ICalendarController {

  void runInteractiveMode();

  void runHeadlessMode(String fileName);

}
