package atu.testrecorder.media;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;







public abstract class AbstractVideoCodec
  extends AbstractCodec
{
  private BufferedImage imgConverter;
  
  public AbstractVideoCodec() {}
  
  protected byte[] getIndexed8(Buffer buf)
  {
    if ((data instanceof byte[])) {
      return (byte[])data;
    }
    if ((data instanceof BufferedImage)) {
      BufferedImage image = (BufferedImage)data;
      if ((image.getRaster().getDataBuffer() instanceof DataBufferByte)) {
        return ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
      }
    }
    return null;
  }
  

  protected short[] getRGB15(Buffer buf)
  {
    if ((data instanceof int[])) {
      return (short[])data;
    }
    if ((data instanceof BufferedImage)) {
      BufferedImage image = (BufferedImage)data;
      if ((image.getColorModel() instanceof DirectColorModel)) {
        DirectColorModel dcm = (DirectColorModel)image.getColorModel();
        if ((image.getRaster().getDataBuffer() instanceof DataBufferShort))
        {
          return ((DataBufferShort)image.getRaster().getDataBuffer()).getData();
        }
        if ((image.getRaster().getDataBuffer() instanceof DataBufferUShort))
        {
          return ((DataBufferUShort)image.getRaster().getDataBuffer()).getData();
        }
      }
      if (imgConverter == null) {
        int width = ((VideoFormat)outputFormat).getWidth();
        int height = ((VideoFormat)outputFormat).getHeight();
        imgConverter = new BufferedImage(width, height, 9);
      }
      Graphics2D g = imgConverter.createGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      return ((DataBufferShort)imgConverter.getRaster().getDataBuffer()).getData();
    }
    return null;
  }
  
  protected int[] getRGB24(Buffer buf)
  {
    if ((data instanceof int[])) {
      return (int[])data;
    }
    if ((data instanceof BufferedImage)) {
      BufferedImage image = (BufferedImage)data;
      if ((image.getColorModel() instanceof DirectColorModel)) {
        DirectColorModel dcm = (DirectColorModel)image.getColorModel();
        if ((dcm.getBlueMask() == 255) && (dcm.getGreenMask() == 65280) && (dcm.getRedMask() == 16711680) && 
          ((image.getRaster().getDataBuffer() instanceof DataBufferInt))) {
          return ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        }
      }
      
      VideoFormat vf = (VideoFormat)outputFormat;
      return image.getRGB(0, 0, vf.getWidth(), vf.getHeight(), null, 0, vf.getWidth());
    }
    return null;
  }
  
  protected int[] getARGB32(Buffer buf) {
    if ((data instanceof int[])) {
      return (int[])data;
    }
    if ((data instanceof BufferedImage)) {
      BufferedImage image = (BufferedImage)data;
      if ((image.getColorModel() instanceof DirectColorModel)) {
        DirectColorModel dcm = (DirectColorModel)image.getColorModel();
        if ((dcm.getBlueMask() == 255) && (dcm.getGreenMask() == 65280) && (dcm.getRedMask() == 16711680) && 
          ((image.getRaster().getDataBuffer() instanceof DataBufferInt))) {
          return ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        }
      }
      
      VideoFormat vf = (VideoFormat)outputFormat;
      return image.getRGB(0, 0, vf.getWidth(), vf.getHeight(), null, 0, vf.getWidth());
    }
    return null;
  }
  
  protected BufferedImage getBufferedImage(Buffer buf)
  {
    if ((data instanceof BufferedImage)) {
      return (BufferedImage)data;
    }
    return null;
  }
  
  private byte[] byteBuf = new byte[4];
  
  protected void writeInt24(ImageOutputStream out, int v) throws IOException { byteBuf[0] = ((byte)(v >>> 16));
    byteBuf[1] = ((byte)(v >>> 8));
    byteBuf[2] = ((byte)(v >>> 0));
    out.write(byteBuf, 0, 3);
  }
  
  protected void writeInt24LE(ImageOutputStream out, int v) throws IOException { byteBuf[2] = ((byte)(v >>> 16));
    byteBuf[1] = ((byte)(v >>> 8));
    byteBuf[0] = ((byte)(v >>> 0));
    out.write(byteBuf, 0, 3);
  }
  
  protected void writeInts24(ImageOutputStream out, int[] i, int off, int len) throws IOException
  {
    if ((off < 0) || (len < 0) || (off + len > i.length) || (off + len < 0)) {
      throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > i.length!");
    }
    
    byte[] b = new byte[len * 3];
    int boff = 0;
    for (int j = 0; j < len; j++) {
      int v = i[(off + j)];
      
      b[(boff++)] = ((byte)(v >>> 16));
      b[(boff++)] = ((byte)(v >>> 8));
      b[(boff++)] = ((byte)(v >>> 0));
    }
    
    out.write(b, 0, len * 3);
  }
  
  protected void writeInts24LE(ImageOutputStream out, int[] i, int off, int len) throws IOException {
    if ((off < 0) || (len < 0) || (off + len > i.length) || (off + len < 0)) {
      throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > i.length!");
    }
    
    byte[] b = new byte[len * 3];
    int boff = 0;
    for (int j = 0; j < len; j++) {
      int v = i[(off + j)];
      b[(boff++)] = ((byte)(v >>> 0));
      b[(boff++)] = ((byte)(v >>> 8));
      b[(boff++)] = ((byte)(v >>> 16));
    }
    

    out.write(b, 0, len * 3);
  }
}
