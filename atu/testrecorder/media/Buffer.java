package atu.testrecorder.media;





public class Buffer
{
  public static final int FLAG_DISCARD = 2;
  



  public static final int FLAG_KEY_FRAME = 16;
  


  public int flags;
  


  public Object data;
  


  public int offset;
  


  public int length;
  


  public long duration;
  


  public long timeScale;
  


  public long timeStamp;
  


  public Format format;
  


  public int sampleCount = 1;
  
  public Buffer() {}
}
