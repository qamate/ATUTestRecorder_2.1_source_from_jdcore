package atu.testrecorder.media;



public class VideoFormat
  extends Format
{
  private final int width;
  

  private final int height;
  

  private final int depth;
  

  private final Class dataClass;
  

  private final String encoding;
  

  private final String compressorName;
  

  public static final String IMAGE = "image";
  

  public static final String QT_CINEPAK = "cvid";
  

  public static final String QT_JPEG = "jpeg";
  

  public static final String QT_JPEG_COMPRESSOR_NAME = "Photo - JPEG";
  

  public static final String QT_PNG = "png ";
  
  public static final String QT_PNG_COMPRESSOR_NAME = "PNG";
  
  public static final String QT_ANIMATION = "rle ";
  
  public static final String QT_ANIMATION_COMPRESSOR_NAME = "Animation";
  
  public static final String QT_RAW = "raw ";
  
  public static final String QT_RAW_COMPRESSOR_NAME = "NONE";
  
  public static final String AVI_DIB = "DIB ";
  
  public static final String AVI_RLE = "RLE ";
  
  public static final String AVI_TECHSMITH_SCREEN_CAPTURE = "tscc";
  
  public static final String AVI_MJPG = "MJPG";
  
  public static final String AVI_PNG = "png ";
  

  public VideoFormat(String encoding, Class dataClass, int width, int height, int depth)
  {
    this(encoding, encoding, dataClass, width, height, depth);
  }
  
  public VideoFormat(String encoding, String compressorName, Class dataClass, int width, int height, int depth) {
    this.encoding = encoding;
    this.compressorName = compressorName;
    this.dataClass = dataClass;
    this.width = width;
    this.height = height;
    this.depth = depth;
  }
  
  public VideoFormat(String encoding, String compressorName) {
    this(encoding, compressorName, null, -1, -1, -1);
  }
  
  public VideoFormat(String encoding) {
    this(encoding, encoding, null, -1, -1, -1);
  }
  
  public int getDepth() {
    return depth;
  }
  
  public int getHeight() {
    return height;
  }
  
  public int getWidth() {
    return width;
  }
  
  public Class getDataClass()
  {
    return dataClass;
  }
  
  public String getEncoding() {
    return encoding;
  }
  
  public String getCompressorName() { return compressorName; }
}
