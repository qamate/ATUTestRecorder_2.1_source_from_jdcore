package atu.testrecorder.media.avi;

import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Codec;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ImageOutputStreamAdapter;
import java.awt.Rectangle;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.imageio.stream.ImageOutputStream;














































public abstract class AbstractAVIStream
{
  protected ImageOutputStream out;
  protected long streamOffset;
  public AbstractAVIStream() {}
  
  protected static enum MediaType
  {
    AUDIO("auds"), 
    MIDI("mids"), 
    TEXT("txts"), 
    VIDEO("vids");
    
    protected String fccType;
    
    private MediaType(String fourCC) {
      fccType = fourCC;
    }
  }
  
  protected ArrayList<Track> tracks = new ArrayList();
  









  protected long getRelativeStreamPosition()
    throws IOException
  {
    return out.getStreamPosition() - streamOffset;
  }
  




  protected void seekRelative(long newPosition)
    throws IOException
  {
    out.seek(newPosition + streamOffset);
  }
  



  protected static class Sample
  {
    String chunkType;
    


    long offset;
    


    long length;
    

    int duration;
    

    boolean isSync;
    


    public Sample(String chunkId, int duration, long offset, long length, boolean isSync)
    {
      chunkType = chunkId;
      this.duration = duration;
      this.offset = offset;
      this.length = length;
      this.isSync = isSync;
    }
  }
  






  protected abstract class Track
  {
    final AbstractAVIStream.MediaType mediaType;
    





    protected long timeScale = 1L;
    




    protected long frameRate = 30L;
    



    protected LinkedList<AbstractAVIStream.Sample> samples;
    



    protected int syncInterval = 30;
    protected String twoCC;
    protected String fourCC;
    
    public Track(int trackIndex, AbstractAVIStream.MediaType mediaType, String fourCC) {
      this.mediaType = mediaType;
      twoCC = ("00" + Integer.toString(trackIndex));
      twoCC = twoCC.substring(twoCC.length() - 2);
      this.fourCC = fourCC;
    }
    



    AbstractAVIStream.FixedSizeDataChunk strhChunk;
    


    AbstractAVIStream.FixedSizeDataChunk strfChunk;
  }
  



  protected class VideoTrack
    extends AbstractAVIStream.Track
  {
    protected VideoFormat videoFormat;
    

    protected float videoQuality = 0.97F;
    
    protected IndexColorModel palette;
    
    protected IndexColorModel previousPalette;
    
    protected Object previousData;
    protected Codec codec;
    protected Buffer outputBuffer;
    protected Rectangle rcFrame;
    
    public VideoTrack(int trackIndex, String fourCC)
    {
      super(trackIndex, AbstractAVIStream.MediaType.VIDEO, fourCC);
    }
  }
  




  protected abstract class Chunk
  {
    protected String chunkType;
    



    protected long offset;
    



    public Chunk(String chunkType)
      throws IOException
    {
      this.chunkType = chunkType;
      offset = getRelativeStreamPosition();
    }
    



    public abstract void finish()
      throws IOException;
    



    public abstract long size();
  }
  



  protected class CompositeChunk
    extends AbstractAVIStream.Chunk
  {
    protected String compositeType;
    

    protected LinkedList<AbstractAVIStream.Chunk> children;
    

    protected boolean finished;
    


    public CompositeChunk(String compositeType, String chunkType)
      throws IOException
    {
      super(chunkType);
      this.compositeType = compositeType;
      
      out.writeLong(0L);
      out.writeInt(0);
      children = new LinkedList();
    }
    
    public void add(AbstractAVIStream.Chunk child) throws IOException {
      if (children.size() > 0) {
        ((AbstractAVIStream.Chunk)children.getLast()).finish();
      }
      children.add(child);
    }
    




    public void finish()
      throws IOException
    {
      if (!finished) {
        if (size() > 4294967295L) {
          throw new IOException("CompositeChunk \"" + chunkType + "\" is too large: " + size());
        }
        
        long pointer = getRelativeStreamPosition();
        seekRelative(offset);
        

        DataChunkOutputStream headerData = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
        headerData.writeType(compositeType);
        headerData.writeUInt(size() - 8L);
        headerData.writeType(chunkType);
        for (AbstractAVIStream.Chunk child : children) {
          child.finish();
        }
        seekRelative(pointer);
        if (size() % 2L == 1L) {
          out.writeByte(0);
        }
        finished = true;
      }
    }
    
    public long size()
    {
      long length = 12L;
      for (AbstractAVIStream.Chunk child : children) {
        length += child.size() + child.size() % 2L;
      }
      return length;
    }
  }
  


  protected class DataChunk
    extends AbstractAVIStream.Chunk
  {
    protected DataChunkOutputStream data;
    

    protected boolean finished;
    

    public DataChunk(String name)
      throws IOException
    {
      super(name);
      out.writeLong(0L);
      data = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
    }
    
    public DataChunkOutputStream getOutputStream() {
      if (finished) {
        throw new IllegalStateException("DataChunk is finished");
      }
      return data;
    }
    



    public long getOffset()
    {
      return offset;
    }
    
    public void finish() throws IOException
    {
      if (!finished) {
        long sizeBefore = size();
        
        if (size() > 4294967295L) {
          throw new IOException("DataChunk \"" + chunkType + "\" is too large: " + size());
        }
        
        long pointer = getRelativeStreamPosition();
        seekRelative(offset);
        

        DataChunkOutputStream headerData = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
        headerData.writeType(chunkType);
        headerData.writeUInt(size() - 8L);
        seekRelative(pointer);
        if (size() % 2L == 1L) {
          out.writeByte(0);
        }
        finished = true;
        long sizeAfter = size();
        if (sizeBefore != sizeAfter) {
          System.err.println("size mismatch " + sizeBefore + ".." + sizeAfter);
        }
      }
    }
    
    public long size()
    {
      return 8L + data.size();
    }
  }
  


  protected class FixedSizeDataChunk
    extends AbstractAVIStream.Chunk
  {
    protected DataChunkOutputStream data;
    
    protected boolean finished;
    
    protected long fixedSize;
    

    public FixedSizeDataChunk(String chunkType, long fixedSize)
      throws IOException
    {
      super(chunkType);
      this.fixedSize = fixedSize;
      data = new DataChunkOutputStream(new ImageOutputStreamAdapter(out), false);
      data.writeType(chunkType);
      data.writeUInt(fixedSize);
      data.clearCount();
      

      byte[] buf = new byte[(int)Math.min(512L, fixedSize)];
      long written = 0L;
      while (written < fixedSize) {
        data.write(buf, 0, (int)Math.min(buf.length, fixedSize - written));
        written += Math.min(buf.length, fixedSize - written);
      }
      if (fixedSize % 2L == 1L) {
        out.writeByte(0);
      }
      seekToStartOfData();
    }
    


    public DataChunkOutputStream getOutputStream()
    {
      return data;
    }
    



    public long getOffset()
    {
      return offset;
    }
    
    public void seekToStartOfData() throws IOException {
      seekRelative(offset + 8L);
      data.clearCount();
    }
    
    public void seekToEndOfChunk() throws IOException {
      seekRelative(offset + 8L + fixedSize + fixedSize % 2L);
    }
    
    public void finish() throws IOException
    {
      if (!finished) {
        finished = true;
      }
    }
    
    public long size()
    {
      return 8L + fixedSize;
    }
  }
}
