package view;

import javax.annotation.processing.Generated;

public class OutputHandler implements IOutputHandler {
  private static OutputHandler instance = new OutputHandler();

  private OutputHandler() { }

  public static OutputHandler getInstance() {
    return instance;
  }

  @Override
  @Generated("Excluded from mutation testing")
  public void println(String s) {
    System.out.println(s);
  }
}
