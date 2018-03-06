package atu.testrecorder.media.avi;

import atu.testrecorder.media.AbstractVideoCodec;
import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Format;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ByteArrayImageOutputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import javax.imageio.stream.ImageOutputStream;




















































































public class RunLengthCodec
  extends AbstractVideoCodec
{
  private byte[] previousPixels;
  
  public RunLengthCodec() {}
  
  public Format setInputFormat(Format f)
  {
    if ((f instanceof VideoFormat)) {
      VideoFormat vf = (VideoFormat)f;
      if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
        return super.setInputFormat(new VideoFormat("image", vf.getDataClass(), vf.getWidth(), vf.getHeight(), 8));
      }
    }
    return super.setInputFormat(null);
  }
  
  public Format setOutputFormat(Format f)
  {
    if ((f instanceof VideoFormat)) {
      VideoFormat vf = (VideoFormat)f;
      return super.setOutputFormat(new VideoFormat("RLE ", [B.class, vf.getWidth(), vf.getHeight(), 8));
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
    ByteArrayImageOutputStream tmp;
    ByteArrayImageOutputStream tmp; if ((data instanceof byte[])) {
      tmp = new ByteArrayImageOutputStream((byte[])data);
    } else {
      tmp = new ByteArrayImageOutputStream();
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
    int offset = x + y * scanlineStride;
    try
    {
      byte[] pixels = getIndexed8(in);
      if (pixels == null) {
        throw new UnsupportedOperationException("Can not process buffer " + in);
      }
      
      if (((flags & 0x10) != 0) || 
        (previousPixels == null))
      {
        writeKey8(tmp, pixels, width, height, offset, scanlineStride);
        flags = 16;
      } else {
        writeDelta8(tmp, pixels, previousPixels, width, height, offset, scanlineStride);
        flags = 0;
      }
      data = tmp.getBuffer();
      offset = 0;
      length = ((int)tmp.getStreamPosition());
      
      if (previousPixels == null) {
        previousPixels = ((byte[])pixels.clone());
      } else {
        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
      }
      return;
    } catch (IOException ex) {
      ex.printStackTrace();
      flags = 2;
    }
  }
  







  public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    ByteArrayImageOutputStream buf = new ByteArrayImageOutputStream(data.length);
    writeKey8(buf, data, width, height, offset, scanlineStride);
    buf.toOutputStream(out);
  }
  







  public void writeKey8(ImageOutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
    
    int ymax = offset + height * scanlineStride;
    int upsideDown = ymax - scanlineStride + offset;
    

    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = upsideDown - y;
      int xymax = xy + width;
      
      int literalCount = 0;
      int repeatCount = 0;
      for (; xy < xymax; xy++)
      {
        byte v = data[xy];
        for (repeatCount = 0; (xy < xymax) && (repeatCount < 255); repeatCount++) {
          if (data[xy] != v) {
            break;
          }
          xy++;
        }
        


        xy -= repeatCount;
        if (repeatCount < 3) {
          literalCount++;
          if (literalCount == 254) {
            out.write(0);
            out.write(literalCount);
            out.write(data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            if (literalCount < 3) {
              for (; literalCount > 0; literalCount--) {
                out.write(1);
                out.write(data[(xy - literalCount)]);
              }
            } else {
              out.write(0);
              out.write(literalCount);
              out.write(data, xy - literalCount, literalCount);
              if (literalCount % 2 == 1) {
                out.write(0);
              }
              literalCount = 0;
            }
          }
          out.write(repeatCount);
          out.write(v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        if (literalCount < 3) {
          for (; literalCount > 0; literalCount--) {
            out.write(1);
            out.write(data[(xy - literalCount)]);
          }
        } else {
          out.write(0);
          out.write(literalCount);
          out.write(data, xy - literalCount, literalCount);
          if (literalCount % 2 == 1) {
            out.write(0);
          }
        }
        literalCount = 0;
      }
      
      out.write(0);
      out.write(0);
    }
    out.write(0);
    out.write(1);
  }
  






  public void writeDelta8(OutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    ByteArrayImageOutputStream buf = new ByteArrayImageOutputStream(data.length);
    writeDelta8(buf, data, prev, width, height, offset, scanlineStride);
    buf.toOutputStream(out);
  }
  








  public void writeDelta8(ImageOutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
    
    int ymax = offset + height * scanlineStride;
    int upsideDown = ymax - scanlineStride + offset;
    

    int verticalOffset = 0;
    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = upsideDown - y;
      int xymax = xy + width;
      

      for (int skipCount = 0; 
          xy < xymax; skipCount++) {
        if (data[xy] != prev[xy]) {
          break;
        }
        xy++;
      }
      


      if (skipCount == width)
      {
        verticalOffset++;
      }
      else
      {
        while ((verticalOffset > 0) || (skipCount > 0)) {
          if ((verticalOffset == 1) && (skipCount == 0)) {
            out.write(0);
            out.write(0);
            verticalOffset = 0;
          } else {
            out.write(0);
            out.write(2);
            out.write(Math.min(255, skipCount));
            out.write(Math.min(255, verticalOffset));
            skipCount -= Math.min(255, skipCount);
            verticalOffset -= Math.min(255, verticalOffset);
          }
        }
        

        int literalCount = 0;
        int repeatCount = 0;
        for (; xy < xymax; xy++)
        {
          for (skipCount = 0; xy < xymax; skipCount++) {
            if (data[xy] != prev[xy]) {
              break;
            }
            xy++;
          }
          


          xy -= skipCount;
          

          byte v = data[xy];
          for (repeatCount = 0; (xy < xymax) && (repeatCount < 255); repeatCount++) {
            if (data[xy] != v) {
              break;
            }
            xy++;
          }
          


          xy -= repeatCount;
          
          if ((skipCount < 4) && (xy + skipCount < xymax) && (repeatCount < 3)) {
            literalCount++;
          } else {
            while (literalCount > 0) {
              if (literalCount < 3) {
                out.write(1);
                out.write(data[(xy - literalCount)]);
                literalCount--;
              } else {
                int literalRun = Math.min(254, literalCount);
                out.write(0);
                out.write(literalRun);
                out.write(data, xy - literalCount, literalRun);
                if (literalRun % 2 == 1) {
                  out.write(0);
                }
                literalCount -= literalRun;
              }
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              while (skipCount > 0) {
                out.write(0);
                out.write(2);
                out.write(Math.min(255, skipCount));
                out.write(0);
                xy += Math.min(255, skipCount);
                skipCount -= Math.min(255, skipCount);
              }
              xy--;
            } else {
              out.write(repeatCount);
              out.write(v);
              xy += repeatCount - 1;
            }
          }
        }
        

        while (literalCount > 0) {
          if (literalCount < 3) {
            out.write(1);
            out.write(data[(xy - literalCount)]);
            literalCount--;
          } else {
            int literalRun = Math.min(254, literalCount);
            out.write(0);
            out.write(literalRun);
            out.write(data, xy - literalCount, literalRun);
            if (literalRun % 2 == 1) {
              out.write(0);
            }
            literalCount -= literalRun;
          }
        }
        
        out.write(0);
        out.write(0);
      }
    }
    out.write(0);
    out.write(1);
  }
}
