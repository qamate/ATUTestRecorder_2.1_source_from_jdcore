package atu.testrecorder.media.io;

import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.stream.ImageOutputStream;




























public class ImageOutputStreamAdapter
  extends OutputStream
{
  protected ImageOutputStream out;
  
  public ImageOutputStreamAdapter(ImageOutputStream out)
  {
    this.out = out;
  }
  











  public void write(int b)
    throws IOException
  {
    out.write(b);
  }
  















  public void write(byte[] b)
    throws IOException
  {
    write(b, 0, b.length);
  }
  



















  public void write(byte[] b, int off, int len)
    throws IOException
  {
    out.write(b, off, len);
  }
  









  public void flush()
    throws IOException
  {
    out.flush();
  }
  










  public void close()
    throws IOException
  {
    try
    {
      flush();
    } finally {
      out.close();
    }
  }
}
