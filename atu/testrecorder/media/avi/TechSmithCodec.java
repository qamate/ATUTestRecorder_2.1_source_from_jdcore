package atu.testrecorder.media.avi;

import atu.testrecorder.media.AbstractVideoCodec;
import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Format;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ByteArrayImageOutputStream;
import atu.testrecorder.media.io.SeekableByteArrayOutputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;




























































































public class TechSmithCodec
  extends AbstractVideoCodec
{
  private ByteArrayImageOutputStream temp = new ByteArrayImageOutputStream(ByteOrder.LITTLE_ENDIAN);
  private Object previousPixels;
  
  public TechSmithCodec() {}
  
  public Format setInputFormat(Format f) { if ((f instanceof VideoFormat)) {
      VideoFormat vf = (VideoFormat)f;
      if (BufferedImage.class.isAssignableFrom(vf.getDataClass())) {
        return super.setInputFormat(new VideoFormat("image", vf.getDataClass(), vf.getWidth(), vf.getHeight(), vf.getDepth()));
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
      } else {
        depth = 24;
      }
      return super.setOutputFormat(new VideoFormat("tscc", [B.class, vf.getWidth(), vf.getHeight(), depth));
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
    SeekableByteArrayOutputStream tmp; if ((data instanceof byte[])) {
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
    int offset = x + y * scanlineStride;
    try
    {
      switch (vf.getDepth()) {
      case 8: 
        byte[] pixels = getIndexed8(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        
        if (((flags & 0x10) != 0) || 
          (previousPixels == null))
        {
          writeKey8(tmp, pixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
          flags = 16;
        } else {
          writeDelta8(tmp, pixels, (byte[])previousPixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
          flags = 0;
        }
        if (previousPixels == null) {
          previousPixels = pixels.clone();
        } else {
          System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
        }
        break;
      
      case 16: 
        short[] pixels = getRGB15(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        
        if (((flags & 0x10) != 0) || 
          (previousPixels == null))
        {
          writeKey16(tmp, pixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
          flags = 16;
        } else {
          writeDelta16(tmp, pixels, (short[])previousPixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
          flags = 0;
        }
        if (previousPixels == null) {
          previousPixels = pixels.clone();
        } else {
          System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
        }
        break;
      
      case 24: 
        int[] pixels = getRGB24(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        
        if (((flags & 0x10) != 0) || 
          (previousPixels == null))
        {
          writeKey24(tmp, pixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
          flags = 16;
        } else {
          writeDelta24(tmp, pixels, (int[])previousPixels, vf.getWidth(), vf.getHeight(), offset, scanlineStride);
          flags = 0;
        }
        if (previousPixels == null) {
          previousPixels = pixels.clone();
        } else {
          System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
        }
        break;
      
      default: 
        flags = 2;
        return;
      }
      
      
      data = tmp.getBuffer();
      offset = 0;
      length = ((int)tmp.getStreamPosition());
      return;
    } catch (IOException ex) {
      ex.printStackTrace();
      flags = 2;
    }
  }
  








  public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    temp.clear();
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
            temp.write(0);
            temp.write(literalCount);
            temp.write(data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            if (literalCount < 3) {
              for (; literalCount > 0; literalCount--) {
                temp.write(1);
                temp.write(data[(xy - literalCount)]);
              }
            } else {
              temp.write(0);
              temp.write(literalCount);
              temp.write(data, xy - literalCount, literalCount);
              if (literalCount % 2 == 1) {
                temp.write(0);
              }
              literalCount = 0;
            }
          }
          temp.write(repeatCount);
          temp.write(v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        if (literalCount < 3) {
          for (; literalCount > 0; literalCount--) {
            temp.write(1);
            temp.write(data[(xy - literalCount)]);
          }
        } else {
          temp.write(0);
          temp.write(literalCount);
          temp.write(data, xy - literalCount, literalCount);
          if (literalCount % 2 == 1) {
            temp.write(0);
          }
        }
        literalCount = 0;
      }
      
      temp.write(0);
      temp.write(0);
    }
    temp.write(0);
    temp.write(1);
    

    DeflaterOutputStream defl = new DeflaterOutputStream(out);
    temp.toOutputStream(defl);
    defl.finish();
  }
  









  public void writeDelta8(OutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    temp.clear();
    
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
          temp.write(0);
          temp.write(2);
          temp.write(Math.min(255, skipCount));
          temp.write(Math.min(255, verticalOffset));
          skipCount -= Math.min(255, skipCount);
          verticalOffset -= Math.min(255, verticalOffset);
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
                temp.write(1);
                temp.write(data[(xy - literalCount)]);
                literalCount--;
              } else {
                int literalRun = Math.min(254, literalCount);
                temp.write(0);
                temp.write(literalRun);
                temp.write(data, xy - literalCount, literalRun);
                if (literalRun % 2 == 1) {
                  temp.write(0);
                }
                literalCount -= literalRun;
              }
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              while (skipCount > 0) {
                temp.write(0);
                temp.write(2);
                temp.write(Math.min(255, skipCount));
                temp.write(0);
                xy += Math.min(255, skipCount);
                skipCount -= Math.min(255, skipCount);
              }
              xy--;
            } else {
              temp.write(repeatCount);
              temp.write(v);
              xy += repeatCount - 1;
            }
          }
        }
        

        while (literalCount > 0) {
          if (literalCount < 3) {
            temp.write(1);
            temp.write(data[(xy - literalCount)]);
            literalCount--;
          } else {
            int literalRun = Math.min(254, literalCount);
            temp.write(0);
            temp.write(literalRun);
            temp.write(data, xy - literalCount, literalRun);
            if (literalRun % 2 == 1) {
              temp.write(0);
            }
            literalCount -= literalRun;
          }
        }
        
        temp.write(0);
        temp.write(0);
      } }
    temp.write(0);
    temp.write(1);
    

    if (temp.length() == 2L) {
      temp.toOutputStream(out);
    } else {
      DeflaterOutputStream defl = new DeflaterOutputStream(out);
      temp.toOutputStream(defl);
      defl.finish();
    }
  }
  







  public void writeKey16(OutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    temp.clear();
    int ymax = offset + height * scanlineStride;
    int upsideDown = ymax - scanlineStride + offset;
    

    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = upsideDown - y;
      int xymax = xy + width;
      
      int literalCount = 0;
      int repeatCount = 0;
      for (; xy < xymax; xy++)
      {
        short v = data[xy];
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
            temp.write(0);
            temp.write(literalCount);
            temp.writeShorts(data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            if (literalCount < 3) {
              for (; literalCount > 0; literalCount--) {
                temp.write(1);
                temp.writeShort(data[(xy - literalCount)]);
              }
            } else {
              temp.write(0);
              temp.write(literalCount);
              temp.writeShorts(data, xy - literalCount, literalCount);
              


              literalCount = 0;
            }
          }
          temp.write(repeatCount);
          temp.writeShort(v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        if (literalCount < 3) {
          for (; literalCount > 0; literalCount--) {
            temp.write(1);
            temp.writeShort(data[(xy - literalCount)]);
          }
        } else {
          temp.write(0);
          temp.write(literalCount);
          temp.writeShorts(data, xy - literalCount, literalCount);
        }
        


        literalCount = 0;
      }
      
      temp.write(0);
      temp.write(0);
    }
    temp.write(0);
    temp.write(1);
    

    DeflaterOutputStream defl = new DeflaterOutputStream(out);
    temp.toOutputStream(defl);
    defl.finish();
  }
  










  public void writeDelta16(OutputStream out, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    temp.clear();
    
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
          temp.write(0);
          temp.write(2);
          temp.write(Math.min(255, skipCount));
          temp.write(Math.min(255, verticalOffset));
          skipCount -= Math.min(255, skipCount);
          verticalOffset -= Math.min(255, verticalOffset);
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
          

          short v = data[xy];
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
                temp.write(1);
                temp.writeShort(data[(xy - literalCount)]);
                literalCount--;
              } else {
                int literalRun = Math.min(254, literalCount);
                temp.write(0);
                temp.write(literalRun);
                temp.writeShorts(data, xy - literalCount, literalRun);
                


                literalCount -= literalRun;
              }
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              while (skipCount > 0) {
                temp.write(0);
                temp.write(2);
                temp.write(Math.min(255, skipCount));
                temp.write(0);
                xy += Math.min(255, skipCount);
                skipCount -= Math.min(255, skipCount);
              }
              xy--;
            } else {
              temp.write(repeatCount);
              temp.writeShort(v);
              xy += repeatCount - 1;
            }
          }
        }
        

        while (literalCount > 0) {
          if (literalCount < 3) {
            temp.write(1);
            temp.writeShort(data[(xy - literalCount)]);
            literalCount--;
          } else {
            int literalRun = Math.min(254, literalCount);
            temp.write(0);
            temp.write(literalRun);
            temp.writeShorts(data, xy - literalCount, literalRun);
            


            literalCount -= literalRun;
          }
        }
        
        temp.write(0);
        temp.write(0);
      }
    }
    temp.write(0);
    temp.write(1);
    
    if (temp.length() == 2L) {
      temp.toOutputStream(out);
    } else {
      DeflaterOutputStream defl = new DeflaterOutputStream(out);
      temp.toOutputStream(defl);
      defl.finish();
    }
  }
  







  public void writeKey24(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    temp.clear();
    int ymax = offset + height * scanlineStride;
    int upsideDown = ymax - scanlineStride + offset;
    

    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = upsideDown - y;
      int xymax = xy + width;
      
      int literalCount = 0;
      int repeatCount = 0;
      for (; xy < xymax; xy++)
      {
        int v = data[xy];
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
            temp.write(0);
            temp.write(literalCount);
            writeInts24LE(temp, data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            if (literalCount < 3) {
              for (; literalCount > 0; literalCount--) {
                temp.write(1);
                writeInt24LE(temp, data[(xy - literalCount)]);
              }
            } else {
              temp.write(0);
              temp.write(literalCount);
              writeInts24LE(temp, data, xy - literalCount, literalCount);
              


              literalCount = 0;
            }
          }
          temp.write(repeatCount);
          writeInt24LE(temp, v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        if (literalCount < 3) {
          for (; literalCount > 0; literalCount--) {
            temp.write(1);
            writeInt24LE(temp, data[(xy - literalCount)]);
          }
        } else {
          temp.write(0);
          temp.write(literalCount);
          writeInts24LE(temp, data, xy - literalCount, literalCount);
        }
        


        literalCount = 0;
      }
      
      temp.write(0);
      temp.write(0);
    }
    temp.write(0);
    temp.write(1);
    

    DeflaterOutputStream defl = new DeflaterOutputStream(out);
    temp.toOutputStream(defl);
    defl.finish();
  }
  










  public void writeDelta24(OutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    temp.clear();
    
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
          temp.write(0);
          temp.write(2);
          temp.write(Math.min(255, skipCount));
          temp.write(Math.min(255, verticalOffset));
          skipCount -= Math.min(255, skipCount);
          verticalOffset -= Math.min(255, verticalOffset);
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
          

          int v = data[xy];
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
                temp.write(1);
                writeInt24LE(temp, data[(xy - literalCount)]);
                literalCount--;
              } else {
                int literalRun = Math.min(254, literalCount);
                temp.write(0);
                temp.write(literalRun);
                writeInts24LE(temp, data, xy - literalCount, literalRun);
                


                literalCount -= literalRun;
              }
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              while (skipCount > 0) {
                temp.write(0);
                temp.write(2);
                temp.write(Math.min(255, skipCount));
                temp.write(0);
                xy += Math.min(255, skipCount);
                skipCount -= Math.min(255, skipCount);
              }
              xy--;
            } else {
              temp.write(repeatCount);
              writeInt24LE(temp, v);
              xy += repeatCount - 1;
            }
          }
        }
        

        while (literalCount > 0) {
          if (literalCount < 3) {
            temp.write(1);
            writeInt24LE(temp, data[(xy - literalCount)]);
            literalCount--;
          } else {
            int literalRun = Math.min(254, literalCount);
            temp.write(0);
            temp.write(literalRun);
            writeInts24LE(temp, data, xy - literalCount, literalRun);
            


            literalCount -= literalRun;
          }
        }
        
        temp.write(0);
        temp.write(0);
      }
    }
    temp.write(0);
    temp.write(1);
    
    if (temp.length() == 2L) {
      temp.toOutputStream(out);
    } else {
      DeflaterOutputStream defl = new DeflaterOutputStream(out);
      temp.toOutputStream(defl);
      defl.finish();
    }
  }
  
  public static void main(String[] args) {
    byte[] data = {
      8, 2, 3, 4, 4, 3, 7, 7, 7, 8, 
      8, 1, 1, 1, 1, 2, 7, 7, 7, 8, 
      8, 0, 2, 0, 0, 0, 7, 7, 7, 8, 
      8, 2, 2, 3, 4, 4, 7, 7, 7, 8, 
      8, 1, 4, 4, 4, 5, 7, 7, 7, 8 };
    

    byte[] prev = {
      8, 3, 3, 3, 3, 3, 7, 7, 7, 8, 
      8, 1, 1, 1, 1, 1, 7, 7, 7, 8, 
      8, 5, 5, 5, 5, 0, 7, 7, 7, 8, 
      8, 2, 2, 0, 0, 0, 7, 7, 7, 8, 
      8, 2, 0, 0, 0, 5, 7, 7, 7, 8 };
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataChunkOutputStream out = new DataChunkOutputStream(buf);
    TechSmithCodec enc = new TechSmithCodec();
    try
    {
      enc.writeDelta8(out, data, prev, 1, 8, 10, 5);
      
      out.close();
      
      byte[] result = buf.toByteArray();
      System.out.println("size:" + result.length);
      System.out.println(Arrays.toString(result));
      System.out.print("0x [");
      
      for (int i = 0; i < result.length; i++) {
        if (i != 0) {
          System.out.print(',');
        }
        String hex = "00" + Integer.toHexString(result[i]);
        System.out.print(hex.substring(hex.length() - 2));
      }
      System.out.println(']');
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
