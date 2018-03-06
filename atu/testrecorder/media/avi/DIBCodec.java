package atu.testrecorder.media.avi;

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
































public class DIBCodec
  extends AbstractVideoCodec
{
  public DIBCodec() {}
  
  public Format setInputFormat(Format f)
  {
    if ((f instanceof VideoFormat)) {
      VideoFormat vf = (VideoFormat)f;
      int depth = vf.getDepth();
      if (depth <= 4) {
        depth = 4;
      } else if (depth <= 8) {
        depth = 8;
      } else {
        depth = 24;
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
      if (depth <= 4) {
        depth = 4;
      } else if (depth <= 8) {
        depth = 8;
      } else {
        depth = 24;
      }
      return super.setOutputFormat(new VideoFormat("DIB ", [B.class, vf.getWidth(), vf.getHeight(), depth));
    }
    return super.setOutputFormat(null);
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
      case 4: 
        byte[] pixels = getIndexed8(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        writeKey4(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
        break;
      
      case 8: 
        byte[] pixels = getIndexed8(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        writeKey8(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
        break;
      
      case 24: 
        int[] pixels = getRGB24(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        writeKey24(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
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
  









  public void writeKey4(OutputStream out, byte[] pixels, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    byte[] bytes = new byte[width];
    for (int y = (height - 1) * scanlineStride; y >= 0; y -= scanlineStride) {
      int x = offset;int xx = 0; for (int n = offset + width; x < n; xx++) {
        bytes[xx] = ((byte)((pixels[(y + x)] & 0xF) << 4 | pixels[(y + x + 1)] & 0xF));x += 2;
      }
      out.write(bytes);
    }
  }
  









  public void writeKey8(OutputStream out, byte[] pixels, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    for (int y = (height - 1) * scanlineStride; y >= 0; y -= scanlineStride) {
      out.write(pixels, y + offset, width);
    }
  }
  







  public void writeKey24(OutputStream out, int[] pixels, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    int w3 = width * 3;
    byte[] bytes = new byte[w3];
    for (int xy = (height - 1) * scanlineStride + offset; xy >= offset; xy -= scanlineStride) {
      int x = 0; for (int xp = 0; x < w3; xp++) {
        int p = pixels[(xy + xp)];
        bytes[x] = ((byte)p);
        bytes[(x + 1)] = ((byte)(p >> 8));
        bytes[(x + 2)] = ((byte)(p >> 16));x += 3;
      }
      


      out.write(bytes);
    }
  }
}
