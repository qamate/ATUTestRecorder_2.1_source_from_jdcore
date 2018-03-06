package atu.testrecorder.media.avi;

import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Codec;
import atu.testrecorder.media.MovieWriter;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.jpeg.JPEGCodec;
import atu.testrecorder.media.png.PNGCodec;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;


































public class AVIWriter
  extends AbstractAVIStream
  implements MovieWriter
{
  public static final VideoFormat VIDEO_RAW = new VideoFormat("DIB ");
  public static final VideoFormat VIDEO_JPEG = new VideoFormat("MJPG");
  public static final VideoFormat VIDEO_PNG = new VideoFormat("png ");
  public static final VideoFormat VIDEO_SCREEN_CAPTURE = new VideoFormat("tscc");
  



  private static enum States
  {
    STARTED,  FINISHED,  CLOSED;
  }
  


  private States state = States.FINISHED;
  


  private AbstractAVIStream.CompositeChunk aviChunk;
  


  private AbstractAVIStream.CompositeChunk moviChunk;
  


  AbstractAVIStream.FixedSizeDataChunk avihChunk;
  



  public AVIWriter(File file)
    throws IOException
  {
    if (file.exists()) {
      file.delete();
    }
    out = new FileImageOutputStream(file);
    streamOffset = 0L;
  }
  



  public AVIWriter(ImageOutputStream out)
    throws IOException
  {
    this.out = out;
    streamOffset = out.getStreamPosition();
  }
  


















  public int addVideoTrack(VideoFormat format, long timeScale, long frameRate, int width, int height, int depth, int syncInterval)
    throws IOException
  {
    return addVideoTrack(format.getEncoding(), timeScale, frameRate, width, height, depth, syncInterval);
  }
  












  public int addVideoTrack(VideoFormat format, long timeScale, long frameRate, int syncInterval)
    throws IOException
  {
    return addVideoTrack(format.getEncoding(), timeScale, frameRate, format.getWidth(), format.getHeight(), format.getDepth(), syncInterval);
  }
  












  public int addVideoTrack(VideoFormat format, long timeScale, long frameRate, int width, int height)
    throws IOException
  {
    return addVideoTrack(format.getEncoding(), timeScale, frameRate, width, height, 24, 24);
  }
  















  public int addVideoTrack(String fourCC, long timeScale, long frameRate, int width, int height, int depth, int syncInterval)
    throws IOException
  {
    AbstractAVIStream.VideoTrack vt = new AbstractAVIStream.VideoTrack(this, tracks.size(), fourCC);
    videoFormat = new VideoFormat(fourCC, [B.class, width, height, depth);
    timeScale = timeScale;
    frameRate = frameRate;
    syncInterval = syncInterval;
    rcFrame = new Rectangle(0, 0, width, height);
    
    samples = new LinkedList();
    
    if (videoFormat.getDepth() == 4) {
      byte[] gray = new byte[16];
      for (int i = 0; i < gray.length; i++) {
        gray[i] = ((byte)(i << 4 | i));
      }
      palette = new IndexColorModel(4, 16, gray, gray, gray);
    } else if (videoFormat.getDepth() == 8) {
      byte[] gray = new byte['Ā'];
      for (int i = 0; i < gray.length; i++) {
        gray[i] = ((byte)i);
      }
      palette = new IndexColorModel(8, 256, gray, gray, gray);
    }
    createCodec(vt);
    
    tracks.add(vt);
    return tracks.size() - 1;
  }
  
  public void setPalette(int track, IndexColorModel palette)
  {
    tracks.get(track)).palette = palette;
  }
  














  public void setCompressionQuality(int track, float newValue)
  {
    AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tracks.get(track);
    videoQuality = newValue;
    if (codec != null) {
      codec.setQuality(newValue);
    }
  }
  




  public float getVideoCompressionQuality(int track)
  {
    return tracks.get(track)).videoQuality;
  }
  











  public void setVideoDimension(int track, int width, int height)
  {
    if ((width < 1) || (height < 1)) {
      throw new IllegalArgumentException("width and height must be greater zero.");
    }
    AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tracks.get(track);
    videoFormat = new VideoFormat(videoFormat.getEncoding(), [B.class, width, height, videoFormat.getDepth());
  }
  




  public Dimension getVideoDimension(int track)
  {
    AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tracks.get(track);
    VideoFormat fmt = videoFormat;
    return new Dimension(fmt.getWidth(), fmt.getHeight());
  }
  




  private void ensureStarted()
    throws IOException
  {
    if (state != States.STARTED) {
      writeProlog();
      state = States.STARTED;
    }
  }
  
















  public void writeFrame(int track, BufferedImage image, long duration)
    throws IOException
  {
    ensureStarted();
    
    AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tracks.get(track);
    if (codec == null) {
      throw new UnsupportedOperationException("No codec for this video format.");
    }
    

    VideoFormat fmt = videoFormat;
    if ((fmt.getWidth() != image.getWidth()) || (fmt.getHeight() != image.getHeight())) {
      throw new IllegalArgumentException("Dimensions of image[" + samples.size() + 
        "] (width=" + image.getWidth() + ", height=" + image.getHeight() + 
        ") differs from image[0] (width=" + 
        fmt.getWidth() + ", height=" + fmt.getHeight());
    }
    



    long offset = getRelativeStreamPosition();
    switch (fmt.getDepth()) {
    case 4: 
      IndexColorModel imgPalette = (IndexColorModel)image.getColorModel();
      int[] imgRGBs = new int[16];
      imgPalette.getRGBs(imgRGBs);
      int[] previousRGBs = new int[16];
      if (previousPalette == null) {
        previousPalette = palette;
      }
      previousPalette.getRGBs(previousRGBs);
      if (!Arrays.equals(imgRGBs, previousRGBs)) {
        previousPalette = imgPalette;
        AbstractAVIStream.DataChunk paletteChangeChunk = new AbstractAVIStream.DataChunk(this, twoCC + "pc");
        










        int first = 0;
        int last = imgPalette.getMapSize() - 1;
        














        DataChunkOutputStream pOut = paletteChangeChunk.getOutputStream();
        pOut.writeByte(first);
        pOut.writeByte(last - first + 1);
        pOut.writeShort(0);
        
        for (int i = first; i <= last; i++) {
          pOut.writeByte(imgRGBs[i] >>> 16 & 0xFF);
          pOut.writeByte(imgRGBs[i] >>> 8 & 0xFF);
          pOut.writeByte(imgRGBs[i] & 0xFF);
          pOut.writeByte(0);
        }
        
        moviChunk.add(paletteChangeChunk);
        paletteChangeChunk.finish();
        long length = getRelativeStreamPosition() - offset;
        samples.add(new AbstractAVIStream.Sample(chunkType, 0, offset, length - 8L, false));
        offset = getRelativeStreamPosition();
      }
      break;
    
    case 8: 
      IndexColorModel imgPalette = (IndexColorModel)image.getColorModel();
      int[] imgRGBs = new int['Ā'];
      imgPalette.getRGBs(imgRGBs);
      int[] previousRGBs = new int['Ā'];
      if (previousPalette == null) {
        previousPalette = palette;
      }
      previousPalette.getRGBs(previousRGBs);
      if (!Arrays.equals(imgRGBs, previousRGBs)) {
        previousPalette = imgPalette;
        AbstractAVIStream.DataChunk paletteChangeChunk = new AbstractAVIStream.DataChunk(this, twoCC + "pc");
        










        int first = 0;
        int last = imgPalette.getMapSize() - 1;
        














        DataChunkOutputStream pOut = paletteChangeChunk.getOutputStream();
        pOut.writeByte(first);
        pOut.writeByte(last - first + 1);
        pOut.writeShort(0);
        
        for (int i = first; i <= last; i++) {
          pOut.writeByte(imgRGBs[i] >>> 16 & 0xFF);
          pOut.writeByte(imgRGBs[i] >>> 8 & 0xFF);
          pOut.writeByte(imgRGBs[i] & 0xFF);
          pOut.writeByte(0);
        }
        
        moviChunk.add(paletteChangeChunk);
        paletteChangeChunk.finish();
        long length = getRelativeStreamPosition() - offset;
        samples.add(new AbstractAVIStream.Sample(chunkType, 0, offset, length - 8L, false));
        offset = getRelativeStreamPosition();
      }
      

      break;
    }
    
    

    if (outputBuffer == null) {
      outputBuffer = new Buffer();
    }
    
    boolean isSync = syncInterval != 0;
    
    Buffer inputBuffer = new Buffer();
    flags = (isSync ? 16 : 0);
    data = image;
    codec.process(inputBuffer, outputBuffer);
    if (outputBuffer.flags == 2) {
      return;
    }
    
    isSync = (outputBuffer.flags & 0x10) != 0;
    
    long offset = getRelativeStreamPosition();
    
    AbstractAVIStream.DataChunk videoFrameChunk = new AbstractAVIStream.DataChunk(this, 
      twoCC + "dc");
    moviChunk.add(videoFrameChunk);
    videoFrameChunk.getOutputStream().write((byte[])outputBuffer.data, outputBuffer.offset, outputBuffer.length);
    videoFrameChunk.finish();
    long length = getRelativeStreamPosition() - offset;
    
    samples.add(new AbstractAVIStream.Sample(chunkType, (int)frameRate, offset, length - 8L, isSync));
    if (getRelativeStreamPosition() > 4294967296L) {
      throw new IOException("AVI file is larger than 4 GB");
    }
  }
  
  private void createCodec(AbstractAVIStream.VideoTrack vt)
  {
    VideoFormat fmt = videoFormat;
    String enc = fmt.getEncoding();
    if (enc.equals("MJPG")) {
      codec = new JPEGCodec();
    } else if (enc.equals("png ")) {
      codec = new PNGCodec();
    } else if (enc.equals("DIB ")) {
      codec = new DIBCodec();
    } else if (enc.equals("RLE ")) {
      codec = new RunLengthCodec();
    } else if (enc.equals("tscc")) {
      codec = new TechSmithCodec();
    }
    
    codec.setInputFormat(new VideoFormat(enc, BufferedImage.class, fmt.getWidth(), fmt.getHeight(), fmt.getDepth()));
    codec.setOutputFormat(new VideoFormat(enc, [B.class, fmt.getWidth(), fmt.getHeight(), fmt.getDepth()));
    codec.setQuality(videoQuality);
  }
  














  public void writeFrame(int track, File file)
    throws IOException
  {
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      writeFrame(track, in);
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }
  














  public void writeFrame(int track, InputStream in)
    throws IOException
  {
    ensureStarted();
    
    AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tracks.get(track);
    
    AbstractAVIStream.DataChunk videoFrameChunk = new AbstractAVIStream.DataChunk(this, 
      twoCC + "dc");
    moviChunk.add(videoFrameChunk);
    OutputStream mdatOut = videoFrameChunk.getOutputStream();
    long offset = getRelativeStreamPosition();
    byte[] buf = new byte['Ȁ'];
    int len;
    while ((len = in.read(buf)) != -1) { int len;
      mdatOut.write(buf, 0, len);
    }
    long length = getRelativeStreamPosition() - offset;
    videoFrameChunk.finish();
    samples.add(new AbstractAVIStream.Sample(chunkType, (int)frameRate, offset, length - 8L, true));
    if (getRelativeStreamPosition() > 4294967296L) {
      throw new IOException("AVI file is larger than 4 GB");
    }
  }
  















  public void writeSample(int track, byte[] data, int off, int len, long duration, boolean isSync)
    throws IOException
  {
    ensureStarted();
    AbstractAVIStream.Track t = (AbstractAVIStream.Track)tracks.get(track);
    AbstractAVIStream.DataChunk dc;
    if ((t instanceof AbstractAVIStream.VideoTrack)) {
      AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)t;
      dc = new AbstractAVIStream.DataChunk(this, 
        twoCC + "dc");
    } else {
      throw new UnsupportedOperationException("Not yet implemented"); }
    AbstractAVIStream.DataChunk dc;
    moviChunk.add(dc);
    OutputStream mdatOut = dc.getOutputStream();
    long offset = getRelativeStreamPosition();
    mdatOut.write(data, off, len);
    long length = getRelativeStreamPosition() - offset;
    dc.finish();
    samples.add(new AbstractAVIStream.Sample(chunkType, (int)frameRate, offset, length - 8L, true));
    if (getRelativeStreamPosition() > 4294967296L) {
      throw new IOException("AVI file is larger than 4 GB");
    }
  }
  



















  public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration, boolean isSync)
    throws IOException
  {
    for (int i = 0; i < sampleCount; i++) {
      writeSample(track, data, off, len / sampleCount, sampleDuration, isSync);
      off += len / sampleCount;
    }
  }
  




  public void close()
    throws IOException
  {
    if (state == States.STARTED) {
      finish();
    }
    if (state != States.CLOSED) {
      out.close();
      state = States.CLOSED;
    }
  }
  







  public void finish()
    throws IOException
  {
    ensureOpen();
    if (state != States.FINISHED) {
      for (AbstractAVIStream.Track tr : tracks) {
        if ((tr instanceof AbstractAVIStream.VideoTrack)) {
          AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tr;
          VideoFormat fmt = videoFormat;
          if ((fmt.getWidth() == -1) || (fmt.getHeight() == -1)) {
            throw new IllegalStateException("image width and height must be specified");
          }
        }
      }
      moviChunk.finish();
      writeEpilog();
      state = States.FINISHED;
    }
  }
  

  private void ensureOpen()
    throws IOException
  {
    if (state == States.CLOSED) {
      throw new IOException("Stream closed");
    }
  }
  

  public boolean isVFRSupported()
  {
    return false;
  }
  





  public boolean isDataLimitReached()
  {
    try
    {
      return getRelativeStreamPosition() > 1932735283L;
    } catch (IOException ex) {}
    return true;
  }
  











  private void writeProlog()
    throws IOException
  {
    aviChunk = new AbstractAVIStream.CompositeChunk(this, "RIFF", "AVI ");
    AbstractAVIStream.CompositeChunk hdrlChunk = new AbstractAVIStream.CompositeChunk(this, "LIST", "hdrl");
    

    aviChunk.add(hdrlChunk);
    avihChunk = new AbstractAVIStream.FixedSizeDataChunk(this, "avih", 56L);
    avihChunk.seekToEndOfChunk();
    hdrlChunk.add(avihChunk);
    
    AbstractAVIStream.CompositeChunk strlChunk = new AbstractAVIStream.CompositeChunk(this, "LIST", "strl");
    hdrlChunk.add(strlChunk);
    

    for (AbstractAVIStream.Track tr : tracks) {
      if ((tr instanceof AbstractAVIStream.VideoTrack)) {
        AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tr;
        strhChunk = new AbstractAVIStream.FixedSizeDataChunk(this, "strh", 56L);
        strhChunk.seekToEndOfChunk();
        strlChunk.add(strhChunk);
        strfChunk = new AbstractAVIStream.FixedSizeDataChunk(this, "strf", palette == null ? 40 : 40 + palette.getMapSize() * 4);
        strfChunk.seekToEndOfChunk();
        strlChunk.add(strfChunk);
      } else {
        throw new UnsupportedOperationException("Track type not implemented yet.");
      }
    }
    
    moviChunk = new AbstractAVIStream.CompositeChunk(this, "LIST", "movi");
    aviChunk.add(moviChunk);
  }
  

  private void writeEpilog()
    throws IOException
  {
    long largestBufferSize = 0L;
    
    long duration = 0L;
    AbstractAVIStream.Sample s; for (AbstractAVIStream.Track tr : tracks) {
      if ((tr instanceof AbstractAVIStream.VideoTrack))
      {
        AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tr;
        
        long trackDuration = 0L;
        for (AbstractAVIStream.Sample s : samples) {
          trackDuration += duration;
        }
        duration = Math.max(duration, trackDuration);
        for (??? = samples.iterator(); ???.hasNext();) { s = (AbstractAVIStream.Sample)???.next();
          if (length > largestBufferSize) {
            largestBufferSize = length;
          }
        }
      }
    }
    
















    AbstractAVIStream.DataChunk idx1Chunk = new AbstractAVIStream.DataChunk(this, "idx1");
    aviChunk.add(idx1Chunk);
    DataChunkOutputStream d = idx1Chunk.getOutputStream();
    long moviListOffset = moviChunk.offset + 8L;
    
    for (AbstractAVIStream.Track tr : tracks) {
      if ((tr instanceof AbstractAVIStream.VideoTrack))
      {
        AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tr;
        for (AbstractAVIStream.Sample f : samples)
        {
          d.writeType(chunkType);
          









          d.writeUInt((chunkType.endsWith("pc") ? 256 : 0) | 
            (isSync ? 16 : 0));
          









          d.writeUInt(offset - moviListOffset);
          




          d.writeUInt(length);
        }
      }
      else {
        throw new UnsupportedOperationException("Track type not yet implemented.");
      }
    }
    idx1Chunk.finish();
    



















    avihChunk.seekToStartOfData();
    d = avihChunk.getOutputStream();
    

    AbstractAVIStream.Track tt = (AbstractAVIStream.Track)tracks.get(0);
    
    d.writeUInt(1000000L * timeScale / frameRate);
    


    d.writeUInt(0L);
    




    d.writeUInt(0L);
    


    d.writeUInt(48L);
    





















    long dwTotalFrames = 0L;
    for (AbstractAVIStream.Track t : tracks) {
      dwTotalFrames += samples.size();
    }
    d.writeUInt(dwTotalFrames);
    

    d.writeUInt(0L);
    











    d.writeUInt(1L);
    


    d.writeUInt(largestBufferSize);
    






    AbstractAVIStream.VideoTrack vt = null;
    for (AbstractAVIStream.Track t : tracks) {
      if ((t instanceof AbstractAVIStream.VideoTrack)) {
        vt = (AbstractAVIStream.VideoTrack)t;
        break;
      }
    }
    Object fmt = videoFormat;
    d.writeUInt(vt == null ? 0 : ((VideoFormat)fmt).getWidth());
    

    d.writeUInt(vt == null ? 0 : ((VideoFormat)fmt).getHeight());
    

    d.writeUInt(0L);
    d.writeUInt(0L);
    d.writeUInt(0L);
    d.writeUInt(0L);
    

    for (fmt = tracks.iterator(); ((Iterator)fmt).hasNext();) { AbstractAVIStream.Track tr = (AbstractAVIStream.Track)((Iterator)fmt).next();
      




























      strhChunk.seekToStartOfData();
      d = strhChunk.getOutputStream();
      d.writeType(mediaType.fccType);
      








      d.writeType(fourCC);
      




      if (((tr instanceof AbstractAVIStream.VideoTrack)) && (videoFormat.getDepth() <= 8)) {
        d.writeUInt(65536L);
      } else {
        d.writeUInt(0L);
      }
      













      d.writeUShort(0);
      



      d.writeUShort(0);
      

      d.writeUInt(0L);
      






      d.writeUInt(timeScale);
      





      d.writeUInt(frameRate);
      

      d.writeUInt(0L);
      




      d.writeUInt(samples.size());
      


      long dwSuggestedBufferSize = 0L;
      for (AbstractAVIStream.Sample s : samples) {
        if (length > dwSuggestedBufferSize) {
          dwSuggestedBufferSize = length;
        }
      }
      d.writeUInt(dwSuggestedBufferSize);
      




      d.writeInt(-1);
      





      d.writeUInt(0L);
      









      d.writeUShort((tr instanceof AbstractAVIStream.VideoTrack) ? rcFrame.x : 0);
      d.writeUShort((tr instanceof AbstractAVIStream.VideoTrack) ? rcFrame.y : 0);
      d.writeUShort((tr instanceof AbstractAVIStream.VideoTrack) ? rcFrame.x + rcFrame.width : 0);
      d.writeUShort((tr instanceof AbstractAVIStream.VideoTrack) ? rcFrame.y + rcFrame.height : 0);
      









      AbstractAVIStream.VideoTrack vt = (AbstractAVIStream.VideoTrack)tr;
      

















      strfChunk.seekToStartOfData();
      d = strfChunk.getOutputStream();
      d.writeUInt(40L);
      



      d.writeInt(videoFormat.getWidth());
      

      d.writeInt(videoFormat.getHeight());
      












      d.writeShort(1);
      


      d.writeShort(videoFormat.getDepth());
      




      String enc = videoFormat.getEncoding();
      if (enc.equals("DIB ")) {
        d.writeInt(0);
      } else if (enc.equals("RLE ")) {
        if (videoFormat.getDepth() == 8) {
          d.writeInt(1);
        } else if (videoFormat.getDepth() == 4) {
          d.writeInt(2);
        } else {
          throw new UnsupportedOperationException("RLE only supports 4-bit and 8-bit images");
        }
      } else {
        d.writeType(videoFormat.getEncoding());
      }
      


















      if (enc.equals("DIB ")) {
        d.writeInt(0);
      } else {
        VideoFormat fmt = videoFormat;
        if (fmt.getDepth() == 4) {
          d.writeInt(fmt.getWidth() * fmt.getHeight() / 2);
        } else {
          int bytesPerPixel = Math.max(1, fmt.getDepth() / 8);
          d.writeInt(fmt.getWidth() * fmt.getHeight() * bytesPerPixel);
        }
      }
      



      d.writeInt(0);
      


      d.writeInt(0);
      


      d.writeInt(palette == null ? 0 : palette.getMapSize());
      


      d.writeInt(0);
      



      if (palette != null) {
        int i = 0; for (int n = palette.getMapSize(); i < n; i++)
        {







          d.write(palette.getBlue(i));
          d.write(palette.getGreen(i));
          d.write(palette.getRed(i));
          d.write(0);
        }
      }
    }
    

    aviChunk.finish();
  }
}
