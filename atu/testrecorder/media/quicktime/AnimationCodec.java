package atu.testrecorder.media.quicktime;

import atu.testrecorder.media.AbstractVideoCodec;
import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Format;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ByteArrayImageOutputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;






































































































































public class AnimationCodec
  extends AbstractVideoCodec
{
  private Object previousPixels;
  private short[] test;
  
  public AnimationCodec() {}
  
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
        return super.setInputFormat(new VideoFormat("rle ", "Animation", vf.getDataClass(), vf.getWidth(), vf.getHeight(), depth));
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
      return super.setOutputFormat(new VideoFormat("rle ", "Animation", [B.class, vf.getWidth(), vf.getHeight(), depth));
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
    try
    {
      switch (vf.getDepth()) {
      case 8: 
        byte[] pixels = getIndexed8(in);
        if (pixels == null) {
          throw new UnsupportedOperationException("Unable to process buffer " + in);
        }
        
        if (((flags & 0x10) != 0) || 
          (previousPixels == null))
        {
          encodeKey8(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
          flags = 16;
        } else {
          encodeDelta8(tmp, pixels, (byte[])previousPixels, width, height, x + y * scanlineStride, scanlineStride);
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
          throw new UnsupportedOperationException("Unable to process buffer " + in);
        }
        

        if (((flags & 0x10) != 0) || 
          (previousPixels == null)) {
          encodeKey16(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
          flags = 16;
        } else {
          encodeDelta16(tmp, pixels, (short[])previousPixels, width, height, x + y * scanlineStride, scanlineStride);
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
          throw new UnsupportedOperationException("Unable to process buffer " + in);
        }
        

        if (((flags & 0x10) != 0) || 
          (previousPixels == null)) {
          encodeKey24(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
          flags = 16;
        } else {
          encodeDelta24(tmp, pixels, (int[])previousPixels, width, height, x + y * scanlineStride, scanlineStride);
          flags = 0;
        }
        if (previousPixels == null) {
          previousPixels = pixels.clone();
        } else {
          System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
        }
        break;
      
      case 32: 
        int[] pixels = getARGB32(in);
        if (pixels == null) {
          flags = 2;
          return;
        }
        

        if (((flags & 0x10) != 0) || 
          (previousPixels == null)) {
          encodeKey32(tmp, pixels, width, height, x + y * scanlineStride, scanlineStride);
          flags = 16;
        } else {
          encodeDelta32(tmp, pixels, (int[])previousPixels, width, height, x + y * scanlineStride, scanlineStride);
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
  









  public void encodeKey8(ImageOutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    if ((width % 4 != 0) || (offset % 4 != 0) || (scanlineStride % 4 != 0)) {
      throw new UnsupportedOperationException("Conversion is not fully implemented yet.");
    }
    

    int[] ints = new int[data.length / 4];
    int i = 0; for (int j = 0; i < data.length; j++) {
      ints[j] = ((data[i] & 0xFF) << 24 | (data[(i + 1)] & 0xFF) << 16 | (data[(i + 2)] & 0xFF) << 8 | data[(i + 3)] & 0xFF);i += 4;
    }
    encodeKey32(out, ints, width / 4, height, offset / 4, scanlineStride / 4);
  }
  









  public void encodeDelta8(ImageOutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    if ((width % 4 != 0) || (offset % 4 != 0) || (scanlineStride % 4 != 0)) {
      throw new UnsupportedOperationException("Conversion is not fully implemented yet.");
    }
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    

    int[] ints = new int[data.length / 4];
    int i = 0; for (int j = 0; i < data.length; j++) {
      ints[j] = ((data[i] & 0xFF) << 24 | (data[(i + 1)] & 0xFF) << 16 | (data[(i + 2)] & 0xFF) << 8 | data[(i + 3)] & 0xFF);i += 4;
    }
    
    int[] pints = new int[prev.length / 4];
    int i = 0; for (int j = 0; i < prev.length; j++) {
      pints[j] = ((prev[i] & 0xFF) << 24 | (prev[(i + 1)] & 0xFF) << 16 | (prev[(i + 2)] & 0xFF) << 8 | prev[(i + 3)] & 0xFF);i += 4;
    }
    encodeDelta32(out, ints, pints, width / 4, height, offset / 4, scanlineStride / 4);
  }
  








  public void encodeKey16(ImageOutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    long headerPos = out.getStreamPosition();
    

    out.writeInt(0);
    out.writeShort(0);
    

    int ymax = offset + height * scanlineStride;
    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = y;
      int xymax = y + width;
      
      out.write(1);
      
      int literalCount = 0;
      int repeatCount = 0;
      for (; xy < xymax; xy++)
      {
        short v = data[xy];
        for (repeatCount = 0; (xy < xymax) && (repeatCount < 127); repeatCount++) {
          if (data[xy] != v) {
            break;
          }
          xy++;
        }
        


        xy -= repeatCount;
        
        if (repeatCount < 2) {
          literalCount++;
          if (literalCount == 127) {
            out.write(literalCount);
            out.writeShorts(data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            out.write(literalCount);
            out.writeShorts(data, xy - literalCount, literalCount);
            literalCount = 0;
          }
          out.write(-repeatCount);
          out.writeShort(v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        out.write(literalCount);
        out.writeShorts(data, xy - literalCount, literalCount);
        literalCount = 0;
      }
      
      out.write(-1);
    }
    


    long pos = out.getStreamPosition();
    out.seek(headerPos);
    out.writeInt((int)(pos - headerPos));
    out.seek(pos);
  }
  









  public void encodeDelta16(ImageOutputStream out, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    


    int ymax = offset + height * scanlineStride;
    
    for (int ymin = offset; ymin < ymax; ymin += scanlineStride) {
      int xy = ymin;
      int xymax = ymin + width;
      for (; xy < xymax; xy++) {
        if (data[xy] != prev[xy]) {
          break;
        }
      }
    }
    

    if (ymin == ymax)
    {
      out.writeInt(4);
      return;
    }
    for (; 
        

        ymax > ymin; ymax -= scanlineStride) {
      int xy = ymax - scanlineStride;
      int xymax = ymax - scanlineStride + width;
      for (; xy < xymax; xy++) {
        if (data[xy] != prev[xy]) {
          break;
        }
      }
    }
    


    long headerPos = out.getStreamPosition();
    out.writeInt(0);
    
    if ((ymin == offset) && (ymax == offset + height * scanlineStride))
    {
      out.writeShort(0);
    }
    else {
      out.writeShort(8);
      out.writeShort((ymin - offset) / scanlineStride);
      out.writeShort(0);
      out.writeShort((ymax - ymin + 1 - offset) / scanlineStride);
      out.writeShort(0);
    }
    

    for (int y = ymin; y < ymax; y += scanlineStride) {
      int xy = y;
      int xymax = y + width;
      

      for (int skipCount = 0; 
          xy < xymax; skipCount++) {
        if (data[xy] != prev[xy]) {
          break;
        }
        xy++;
      }
      


      if (skipCount == width)
      {
        out.write(1);
        out.write(-1);
      }
      else {
        out.write(Math.min(255, skipCount + 1));
        skipCount -= Math.min(254, skipCount);
        while (skipCount > 0) {
          out.write(0);
          out.write(Math.min(255, skipCount + 1));
          skipCount -= Math.min(254, skipCount);
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
          for (repeatCount = 0; (xy < xymax) && (repeatCount < 127); repeatCount++) {
            if (data[xy] != v) {
              break;
            }
            xy++;
          }
          


          xy -= repeatCount;
          
          if ((skipCount < 2) && (xy + skipCount < xymax) && (repeatCount < 2)) {
            literalCount++;
            if (literalCount == 127) {
              out.write(literalCount);
              out.writeShorts(data, xy - literalCount + 1, literalCount);
              literalCount = 0;
            }
          } else {
            if (literalCount > 0) {
              out.write(literalCount);
              out.writeShorts(data, xy - literalCount, literalCount);
              literalCount = 0;
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              xy += skipCount - 1;
              while (skipCount > 0) {
                out.write(0);
                out.write(Math.min(255, skipCount + 1));
                skipCount -= Math.min(254, skipCount);
              }
            } else {
              out.write(-repeatCount);
              out.writeShort(v);
              xy += repeatCount - 1;
            }
          }
        }
        

        if (literalCount > 0) {
          out.write(literalCount);
          out.writeShorts(data, xy - literalCount, literalCount);
          literalCount = 0;
        }
        
        out.write(-1);
      }
    }
    
    long pos = out.getStreamPosition();
    out.seek(headerPos);
    out.writeInt((int)(pos - headerPos));
    out.seek(pos);
  }
  








  public void encodeKey24(ImageOutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    long headerPos = out.getStreamPosition();
    

    out.writeInt(0);
    out.writeShort(0);
    

    int ymax = offset + height * scanlineStride;
    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = y;
      int xymax = y + width;
      
      out.write(1);
      
      int literalCount = 0;
      int repeatCount = 0;
      for (; xy < xymax; xy++)
      {
        int v = data[xy];
        for (repeatCount = 0; (xy < xymax) && (repeatCount < 127); repeatCount++) {
          if (data[xy] != v) {
            break;
          }
          xy++;
        }
        


        xy -= repeatCount;
        
        if (repeatCount < 2) {
          literalCount++;
          if (literalCount > 126) {
            out.write(literalCount);
            writeInts24(out, data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            out.write(literalCount);
            writeInts24(out, data, xy - literalCount, literalCount);
            literalCount = 0;
          }
          out.write(-repeatCount);
          writeInt24(out, v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        out.write(literalCount);
        writeInts24(out, data, xy - literalCount, literalCount);
        literalCount = 0;
      }
      
      out.write(-1);
    }
    


    long pos = out.getStreamPosition();
    out.seek(headerPos);
    out.writeInt((int)(pos - headerPos));
    out.seek(pos);
  }
  









  public void encodeDelta24(ImageOutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    


    int ymax = offset + height * scanlineStride;
    
    for (int ymin = offset; ymin < ymax; ymin += scanlineStride) {
      int xy = ymin;
      int xymax = ymin + width;
      for (; xy < xymax; xy++) {
        if (data[xy] != prev[xy]) {
          break;
        }
      }
    }
    

    if (ymin == ymax)
    {
      out.writeInt(4);
      return;
    }
    for (; 
        

        ymax > ymin; ymax -= scanlineStride) {
      int xy = ymax - scanlineStride;
      int xymax = ymax - scanlineStride + width;
      for (; xy < xymax; xy++) {
        if (data[xy] != prev[xy]) {
          break;
        }
      }
    }
    


    long headerPos = out.getStreamPosition();
    out.writeInt(0);
    
    if ((ymin == offset) && (ymax == offset + height * scanlineStride))
    {
      out.writeShort(0);
    }
    else {
      out.writeShort(8);
      out.writeShort((ymin - offset) / scanlineStride);
      out.writeShort(0);
      out.writeShort((ymax - ymin + 1 - offset) / scanlineStride);
      out.writeShort(0);
    }
    


    for (int y = ymin; y < ymax; y += scanlineStride) {
      int xy = y;
      int xymax = y + width;
      

      for (int skipCount = 0; 
          xy < xymax; skipCount++) {
        if (data[xy] != prev[xy]) {
          break;
        }
        xy++;
      }
      


      if (skipCount == width)
      {
        out.write(1);
        out.write(-1);
      }
      else {
        out.write(Math.min(255, skipCount + 1));
        skipCount -= Math.min(254, skipCount);
        while (skipCount > 0) {
          out.write(0);
          out.write(Math.min(255, skipCount + 1));
          skipCount -= Math.min(254, skipCount);
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
          for (repeatCount = 0; (xy < xymax) && (repeatCount < 127); repeatCount++) {
            if (data[xy] != v) {
              break;
            }
            xy++;
          }
          


          xy -= repeatCount;
          
          if ((skipCount < 1) && (xy + skipCount < xymax) && (repeatCount < 2)) {
            literalCount++;
            if (literalCount == 127) {
              out.write(literalCount);
              writeInts24(out, data, xy - literalCount + 1, literalCount);
              literalCount = 0;
            }
          } else {
            if (literalCount > 0) {
              out.write(literalCount);
              writeInts24(out, data, xy - literalCount, literalCount);
              literalCount = 0;
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              xy += skipCount - 1;
              while (skipCount > 0) {
                out.write(0);
                out.write(Math.min(255, skipCount + 1));
                skipCount -= Math.min(254, skipCount);
              }
            } else {
              out.write(-repeatCount);
              writeInt24(out, v);
              xy += repeatCount - 1;
            }
          }
        }
        

        if (literalCount > 0) {
          out.write(literalCount);
          writeInts24(out, data, xy - literalCount, literalCount);
          literalCount = 0;
        }
        
        out.write(-1);
      }
    }
    

    long pos = out.getStreamPosition();
    out.seek(headerPos);
    out.writeInt((int)(pos - headerPos));
    out.seek(pos);
  }
  








  public void encodeKey32(ImageOutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    long headerPos = out.getStreamPosition();
    

    out.writeInt(0);
    out.writeShort(0);
    

    int ymax = offset + height * scanlineStride;
    for (int y = offset; y < ymax; y += scanlineStride) {
      int xy = y;
      int xymax = y + width;
      
      out.write(1);
      
      int literalCount = 0;
      int repeatCount = 0;
      for (; xy < xymax; xy++)
      {
        int v = data[xy];
        for (repeatCount = 0; (xy < xymax) && (repeatCount < 127); repeatCount++) {
          if (data[xy] != v) {
            break;
          }
          xy++;
        }
        


        xy -= repeatCount;
        
        if (repeatCount < 2) {
          literalCount++;
          if (literalCount > 126) {
            out.write(literalCount);
            out.writeInts(data, xy - literalCount + 1, literalCount);
            literalCount = 0;
          }
        } else {
          if (literalCount > 0) {
            out.write(literalCount);
            out.writeInts(data, xy - literalCount, literalCount);
            literalCount = 0;
          }
          out.write(-repeatCount);
          out.writeInt(v);
          xy += repeatCount - 1;
        }
      }
      

      if (literalCount > 0) {
        out.write(literalCount);
        out.writeInts(data, xy - literalCount, literalCount);
        literalCount = 0;
      }
      
      out.write(-1);
    }
    


    long pos = out.getStreamPosition();
    out.seek(headerPos);
    out.writeInt((int)(pos - headerPos));
    out.seek(pos);
  }
  









  public void encodeDelta32(ImageOutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    out.setByteOrder(ByteOrder.BIG_ENDIAN);
    


    int ymax = offset + height * scanlineStride;
    
    for (int ymin = offset; ymin < ymax; ymin += scanlineStride) {
      int xy = ymin;
      int xymax = ymin + width;
      for (; xy < xymax; xy++) {
        if (data[xy] != prev[xy]) {
          break;
        }
      }
    }
    

    if (ymin == ymax)
    {
      out.writeInt(4);
      return;
    }
    for (; 
        

        ymax > ymin; ymax -= scanlineStride) {
      int xy = ymax - scanlineStride;
      int xymax = ymax - scanlineStride + width;
      for (; xy < xymax; xy++) {
        if (data[xy] != prev[xy]) {
          break;
        }
      }
    }
    


    long headerPos = out.getStreamPosition();
    out.writeInt(0);
    
    if ((ymin == offset) && (ymax == offset + height * scanlineStride))
    {
      out.writeShort(0);
    }
    else {
      out.writeShort(8);
      out.writeShort((ymin - offset) / scanlineStride);
      out.writeShort(0);
      out.writeShort((ymax - ymin + 1 - offset) / scanlineStride);
      out.writeShort(0);
    }
    


    for (int y = ymin; y < ymax; y += scanlineStride) {
      int xy = y;
      int xymax = y + width;
      

      for (int skipCount = 0; 
          xy < xymax; skipCount++) {
        if (data[xy] != prev[xy]) {
          break;
        }
        xy++;
      }
      


      if (skipCount == width)
      {
        out.write(1);
        out.write(-1);
      }
      else {
        out.write(Math.min(255, skipCount + 1));
        if (skipCount > 254) {
          skipCount -= 254;
          while (skipCount > 254) {
            out.write(0);
            out.write(255);
            skipCount -= 254;
          }
          out.write(0);
          out.write(skipCount + 1);
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
          for (repeatCount = 0; (xy < xymax) && (repeatCount < 127); repeatCount++) {
            if (data[xy] != v) {
              break;
            }
            xy++;
          }
          


          xy -= repeatCount;
          
          if ((skipCount < 1) && (xy + skipCount < xymax) && (repeatCount < 2)) {
            literalCount++;
            if (literalCount == 127) {
              out.write(literalCount);
              out.writeInts(data, xy - literalCount + 1, literalCount);
              literalCount = 0;
            }
          } else {
            if (literalCount > 0) {
              out.write(literalCount);
              out.writeInts(data, xy - literalCount, literalCount);
              literalCount = 0;
            }
            if (xy + skipCount == xymax)
            {

              xy += skipCount - 1;
            } else if (skipCount >= repeatCount) {
              while (skipCount > 254) {
                out.write(0);
                out.write(255);
                xy += 254;
                skipCount -= 254;
              }
              out.write(0);
              out.write(skipCount + 1);
              xy += skipCount - 1;
            } else {
              out.write(-repeatCount);
              out.writeInt(v);
              xy += repeatCount - 1;
            }
          }
        }
        

        if (literalCount > 0) {
          out.write(literalCount);
          out.writeInts(data, xy - literalCount, literalCount);
          literalCount = 0;
        }
        
        out.write(-1);
      }
    }
    

    long pos = out.getStreamPosition();
    out.seek(headerPos);
    out.writeInt((int)(pos - headerPos));
    out.seek(pos);
  }
  










  public void decodeDelta16(ImageInputStream in, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
    throws IOException
  {
    in.setByteOrder(ByteOrder.BIG_ENDIAN);
    


    long chunkSize = in.readUnsignedInt();
    if (chunkSize <= 8L) {
      return;
    }
    if (in.length() != chunkSize) {
      throw new IOException("Illegal chunk size:" + chunkSize + " expected:" + in.length());
    }
    


    int header = in.readUnsignedShort();
    
    int numberOfLines;
    if (header == 0)
    {
      int startingLine = 0;
      numberOfLines = height;
    } else if (header == 8)
    {
      int startingLine = in.readUnsignedShort();
      int reserved1 = in.readUnsignedShort();
      if (reserved1 != 0) {
        throw new IOException("Illegal value in reserved1 0x" + Integer.toHexString(reserved1));
      }
      int numberOfLines = in.readUnsignedShort();
      int reserved2 = in.readUnsignedShort();
      if (reserved2 != 0) {
        throw new IOException("Illegal value in reserved2 0x" + Integer.toHexString(reserved2));
      }
    } else {
      throw new IOException("Unknown header 0x" + Integer.toHexString(header));
    }
    int numberOfLines;
    int startingLine;
    if ((startingLine > height) || (numberOfLines == 0)) {
      return;
    }
    if (startingLine + numberOfLines - 1 > height) {
      throw new IOException("Illegal startingLine or numberOfLines, startingLine=" + startingLine + ", numberOfLines=" + numberOfLines);
    }
    


    for (int l = 0; l < numberOfLines; l++)
    {
      int i = offset + (startingLine + l) * scanlineStride;
      

      int skipCode = in.readUnsignedByte() - 1;
      if (skipCode == -1) {
        break;
      }
      if (skipCode > 0)
      {
        if (data == prev) {
          i += skipCode;
        } else {
          for (int j = 0; j < skipCode; j++) {
            data[i] = prev[i];
            i++;
          }
        }
      }
      
      for (;;)
      {
        int opCode = in.readByte();
        if (opCode == 0) {
          int skipCode = in.readUnsignedByte() - 1;
          if (skipCode > 0)
          {
            if (prev != data) {
              System.arraycopy(prev, i, data, i, skipCode);
            }
            i += skipCode;
          }
        } else if (opCode > 0)
        {
          try {
            in.readFully(data, i, opCode);
          }
          catch (EOFException e)
          {
            System.exit(5);
            return;
          }
          i += opCode;
        } else { if (opCode == -1) {
            break;
          }
          if (opCode < -1)
          {
            short d = in.readShort();
            int end = i - opCode;
            while (i < end)
              data[(i++)] = d;
          }
        }
      }
      assert (i <= offset + (startingLine + l + 1) * scanlineStride);
    }
    assert (in.getStreamPosition() == in.length());
  }
}
