package view;

/**
 * Interface for handling output operations.
 * Provides methods for displaying text to the user.
 */
public interface IOutputHandler {

  /**
   * Prints the given string followed by a line separator.
   *
   * @param s The string to print
   */
  void println(String s);

}