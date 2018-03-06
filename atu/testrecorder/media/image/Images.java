package atu.testrecorder.media.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.PrintStream;
import java.net.URL;
import javax.swing.ImageIcon;

















public class Images
{
  private Images() {}
  
  public static Image createImage(Class baseClass, String location)
  {
    URL resource = baseClass.getResource(location);
    if (resource == null) {
      System.err.println("Warning: Images.createImage no resource found for " + baseClass + " " + location);
      return null;
    }
    return createImage(resource);
  }
  
  public static Image createImage(URL resource) { Image image = Toolkit.getDefaultToolkit().createImage(resource);
    





    return image;
  }
  



























  public static BufferedImage toBufferedImage(RenderedImage rImg)
  {
    BufferedImage image;
    


























    BufferedImage image;
    

























    if ((rImg instanceof BufferedImage)) {
      image = (BufferedImage)rImg;
    } else {
      Raster r = rImg.getData();
      WritableRaster wr = WritableRaster.createWritableRaster(
        r.getSampleModel(), null);
      rImg.copyData(wr);
      image = new BufferedImage(
        rImg.getColorModel(), 
        wr, 
        rImg.getColorModel().isAlphaPremultiplied(), 
        null);
    }
    
    return image;
  }
  
  public static BufferedImage toBufferedImage(Image image) {
    if ((image instanceof BufferedImage)) {
      return (BufferedImage)image;
    }
    

    image = new ImageIcon(image).getImage();
    

    BufferedImage bimage = null;
    
    if (System.getProperty("java.version").startsWith("1.4.1_"))
    {




      bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), 2);
    }
    else
    {
      boolean hasAlpha;
      try {
        hasAlpha = hasAlpha(image);
      }
      catch (IllegalAccessError e) {
        boolean hasAlpha;
        hasAlpha = true;
      }
      

      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      try
      {
        int transparency = 1;
        if (hasAlpha) {
          transparency = 3;
        }
        

        GraphicsDevice gs = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        bimage = gc.createCompatibleImage(
          image.getWidth(null), image.getHeight(null), transparency);
      }
      catch (Exception localException) {}
      


      if (bimage == null)
      {
        int type = 1;
        if (hasAlpha) {
          type = 2;
        }
        bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
      }
    }
    

    Graphics g = bimage.createGraphics();
    

    g.drawImage(image, 0, 0, null);
    g.dispose();
    
    return bimage;
  }
  



































  public static boolean hasAlpha(Image image)
  {
    if ((image instanceof BufferedImage)) {
      BufferedImage bimage = (BufferedImage)image;
      return bimage.getColorModel().hasAlpha();
    }
    


    PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
    try {
      pg.grabPixels();
    }
    catch (InterruptedException localInterruptedException) {}
    

    ColorModel cm = pg.getColorModel();
    return cm.hasAlpha();
  }
  


  public static BufferedImage[] split(Image image, int count, boolean isHorizontal)
  {
    BufferedImage src = toBufferedImage(image);
    if (count == 1) {
      return new BufferedImage[] { src };
    }
    
    BufferedImage[] parts = new BufferedImage[count];
    for (int i = 0; i < count; i++) {
      if (isHorizontal) {
        parts[i] = src.getSubimage(
          src.getWidth() / count * i, 0, 
          src.getWidth() / count, src.getHeight());
      }
      else {
        parts[i] = src.getSubimage(
          0, src.getHeight() / count * i, 
          src.getWidth(), src.getHeight() / count);
      }
    }
    
    return parts;
  }
  
  public static BufferedImage toIntImage(BufferedImage img) {
    if ((img.getRaster().getDataBuffer() instanceof DataBufferInt)) {
      return img;
    }
    BufferedImage intImg = new BufferedImage(img.getWidth(), img.getHeight(), 1);
    Graphics2D g = intImg.createGraphics();
    g.drawImage(img, 0, 0, null);
    g.dispose();
    return intImg;
  }
}
