package atu.testrecorder.media.quicktime;

import atu.testrecorder.media.AbstractVideoCodec;
import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Format;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.SeekableByteArrayOutputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;































public class RawCodec
  extends AbstractVideoCodec
{
  public RawCodec() {}
  
  public Format setInputFormat(Format f)
  {
    if ((f instanceof VideoFormat)) {
      VideoFormat vf = (VideoFormat)f;
      int depth = vf.getDepth();
      if (depth <= 8) {
        depth = 8;
      } else if (depth <= 16) {
        depth = 16;
      } else if (depth <= 24) {
        depth = 24;
      } else {
        depth = 32;
      }
      if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
        return super.setInputFormat(new VideoFormat("image", vf.getDataClass(), vf.getWidth(), vf.getHeight(), depth));
      }
    }
    return super.setInputFormat(null);
  }
  
  public Format setOutputFormat(Format f)
  {
    if ((f instanceof VideoFormat)) {
      VideoFormat vf = (VideoFormat)f;
      int depth = vf.getDepth();
      if (depth <= 8) {
        depth = 8;
      } else if (depth <= 16) {
        depth = 16;
      } else if (depth <= 24) {
        depth = 24;
      } else {
        depth = 32;
      }
      return super.setOutputFormat(new VideoFormat("raw ", 
        "NONE", [B.class, vf.getWidth(), vf.getHeight(), depth));
    }
    return super.setOutputFormat(null);
  }
  










  public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    int xy = offset; for (int ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
      out.write(data, xy, width);
    }
  }
  










  public void writeKey16(OutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    byte[] bytes = new byte[width * 2];
    int xy = offset; for (int ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
      int x = 0; for (int i = 0; x < width; i += 2) {
        int pixel = data[(xy + x)];
        bytes[i] = ((byte)(pixel >> 8));
        bytes[(i + 1)] = ((byte)pixel);x++;
      }
      

      out.write(bytes, 0, bytes.length);
    }
  }
  










  public void writeKey24(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    byte[] bytes = new byte[width * 3];
    int xy = offset; for (int ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
      int x = 0; for (int i = 0; x < width; i += 3) {
        int pixel = data[(xy + x)];
        bytes[i] = ((byte)(pixel >> 16));
        bytes[(i + 1)] = ((byte)(pixel >> 8));
        bytes[(i + 2)] = ((byte)pixel);x++;
      }
      


      out.write(bytes, 0, bytes.length);
    }
  }
  










  public void writeKey32(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    byte[] bytes = new byte[width * 4];
    int xy = offset; for (int ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
      int x = 0; for (int i = 0; x < width; i += 4) {
        int pixel = data[(xy + x)];
        bytes[i] = ((byte)(pixel >> 24));
        bytes[(i + 1)] = ((byte)(pixel >> 16));
        bytes[(i + 2)] = ((byte)(pixel >> 8));
        bytes[(i + 3)] = ((byte)pixel);x++;
      }
      



      out.write(bytes, 0, bytes.length);
    }
  }
  









  public void writeKey24(OutputStream out, BufferedImage image)
    throws IOException
  {
    int width = image.getWidth();
    int height = image.getHeight();
    WritableRaster raster = image.getRaster();
    int[] rgb = new int[width * 3];
    byte[] bytes = new byte[width * 3];
    for (int y = 0; y < height; y++)
    {
      rgb = raster.getPixels(0, y, width, 1, rgb);
      int k = 0; for (int n = width * 3; k < n; k++) {
        bytes[k] = ((byte)rgb[k]);
      }
      out.write(bytes);
    }
  }
  

  public void process(Buffer in, Buffer out)
  {
    if ((flags & 0x2) != 0) {
      flags = 2;
      return;
    }
    format = outputFormat;
    SeekableByteArrayOutputStream tmp;
    SeekableByteArrayOutputStream tmp;
    if ((data instanceof byte[])) {
      tmp = new SeekableByteArrayOutputStream((byte[])data);
    } else {
      tmp = new SeekableByteArrayOutputStream();
    }
    VideoFormat vf = (VideoFormat)outputFormat;
    
    Rectangle r;
    
    int scanlineStride;
    if ((data instanceof BufferedImage)) {
      BufferedImage image = (BufferedImage)data;
      WritableRaster raster = image.getRaster();
      int scanlineStride = raster.getSampleModel().getWidth();
      Rectangle r = raster.getBounds();
      x -= raster.getSampleModelTranslateX();
      y -= raster.getSampleModelTranslateY();
    } else {
      r = new Rectangle(0, 0, vf.getWidth(), vf.getHeight());
      scanlineStride = vf.getWidth();
    }
    try
    {
      switch (vf.getDepth()) {
      case 8: 
        writeKey8(tmp, getIndexed8(in), width, height, x + y * scanlineStride, scanlineStride);
        break;
      
      case 16: 
        writeKey16(tmp, getRGB15(in), width, height, x + y * scanlineStride, scanlineStride);
        break;
      
      case 24: 
        writeKey24(tmp, getRGB24(in), width, height, x + y * scanlineStride, scanlineStride);
        break;
      
      case 32: 
        writeKey24(tmp, getARGB32(in), width, height, x + y * scanlineStride, scanlineStride);
        break;
      
      default: 
        flags = 2;
        return;
      }
      
      
      flags = 16;
      data = tmp.getBuffer();
      offset = 0;
      length = ((int)tmp.getStreamPosition());
      return;
    } catch (IOException ex) {
      ex.printStackTrace();
      flags = 2;
    }
  }
}
