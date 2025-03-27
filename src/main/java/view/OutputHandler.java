package view;

import javax.annotation.processing.Generated;

/**
 * Singleton implementation of IOutputHandler interface.
 * Provides standard output functionality through System.out.
 */
public class OutputHandler implements IOutputHandler {
  private static OutputHandler instance = new OutputHandler();

  /**
   * Private constructor to enforce singleton pattern.
   */
  private OutputHandler() { }

  /**
   * Returns the singleton instance of OutputHandler.
   *
   * @return The singleton OutputHandler instance
   */
  public static OutputHandler getInstance() {
    return instance;
  }

  /**
   * Prints the given string followed by a line separator to standard output.
   *
   * @param s The string to print
   */
  @Override
  @Generated("Excluded from mutation testing")
  public void println(String s) {
    System.out.println(s);
  }
}