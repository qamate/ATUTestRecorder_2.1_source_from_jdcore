package atu.testrecorder.media;




public abstract class AbstractCodec
  implements Codec
{
  protected Format inputFormat;
  


  protected Format outputFormat;
  


  protected float quality = 1.0F;
  
  public AbstractCodec() {}
  
  public Format setInputFormat(Format f) {
    inputFormat = f;
    return f;
  }
  
  public Format setOutputFormat(Format f)
  {
    outputFormat = f;
    return f;
  }
  
  public void setQuality(float newValue)
  {
    quality = newValue;
  }
  
  public float getQuality()
  {
    return quality;
  }
}
