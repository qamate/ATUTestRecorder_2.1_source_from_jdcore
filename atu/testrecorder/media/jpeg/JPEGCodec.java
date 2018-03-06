package atu.testrecorder.media.jpeg;

import atu.testrecorder.media.AbstractVideoCodec;
import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Format;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ByteArrayImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;























public class JPEGCodec
  extends AbstractVideoCodec
{
  public JPEGCodec() {}
  
  public Format setInputFormat(Format f)
  {
    if ((f instanceof VideoFormat)) {
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
      return super.setOutputFormat(new VideoFormat("MJPG", [B.class, vf.getWidth(), vf.getHeight(), 24));
    }
    return super.setOutputFormat(null);
  }
  
  public void process(Buffer in, Buffer out)
  {
    if ((flags & 0x2) != 0) {
      flags = 2;
      return;
    }
    BufferedImage image = getBufferedImage(in);
    if (image == null) {
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
    try
    {
      ImageWriter iw = (ImageWriter)ImageIO.getImageWritersByMIMEType("image/jpeg").next();
      ImageWriteParam iwParam = iw.getDefaultWriteParam();
      iwParam.setCompressionMode(2);
      iwParam.setCompressionQuality(quality);
      iw.setOutput(tmp);
      IIOImage img = new IIOImage(image, null, null);
      iw.write(null, img, iwParam);
      iw.dispose();
      
      flags = 16;
      data = tmp.getBuffer();
      offset = 0;
      length = ((int)tmp.getStreamPosition());
    } catch (IOException ex) {
      ex.printStackTrace();
      flags = 2;
      return;
    }
  }
}
