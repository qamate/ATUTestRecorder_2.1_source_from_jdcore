package atu.testrecorder.media;

public abstract interface Codec
{
  public abstract Format setInputFormat(Format paramFormat);
  
  public abstract Format setOutputFormat(Format paramFormat);
  
  public abstract void process(Buffer paramBuffer1, Buffer paramBuffer2);
  
  public abstract void setQuality(float paramFloat);
  
  public abstract float getQuality();
}
