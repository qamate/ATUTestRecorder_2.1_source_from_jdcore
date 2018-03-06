package atu.testrecorder.exceptions;


public class ATUTestRecorderException
  extends Exception
{
  private String message;
  

  public ATUTestRecorderException() {}
  
  public ATUTestRecorderException(String message)
  {
    this.message = message;
  }
  
  public String toString() {
    return "[ATU Test Recorder Exception] " + message;
  }
}
