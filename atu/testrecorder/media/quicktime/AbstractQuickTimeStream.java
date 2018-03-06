package atu.testrecorder.media.quicktime;

import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Codec;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ImageOutputStreamAdapter;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import javax.imageio.stream.ImageOutputStream;











































public class AbstractQuickTimeStream
{
  protected ImageOutputStream out;
  protected long streamOffset;
  protected WideDataAtom mdatAtom;
  protected long mdatOffset;
  protected CompositeAtom moovAtom;
  protected Date creationTime;
  protected long movieTimeScale = 600L;
  

  protected ArrayList<Track> tracks = new ArrayList();
  

  public AbstractQuickTimeStream() {}
  

  protected static enum States
  {
    REALIZED,  STARTED,  FINISHED,  CLOSED;
  }
  


  protected States state = States.REALIZED;
  




























  protected static enum MediaType
  {
    VIDEO,  AUDIO;
  }
  







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
  




  protected abstract class Atom
  {
    protected String type;
    



    protected long offset;
    




    public Atom(String type)
      throws IOException
    {
      this.type = type;
      offset = getRelativeStreamPosition();
    }
    



    public abstract void finish()
      throws IOException;
    



    public abstract long size();
  }
  



  protected class CompositeAtom
    extends AbstractQuickTimeStream.DataAtom
  {
    protected LinkedList<AbstractQuickTimeStream.Atom> children;
    


    public CompositeAtom(String type)
      throws IOException
    {
      super(type);
      children = new LinkedList();
    }
    
    public void add(AbstractQuickTimeStream.Atom child) throws IOException {
      if (children.size() > 0) {
        ((AbstractQuickTimeStream.Atom)children.getLast()).finish();
      }
      children.add(child);
    }
    




    public void finish()
      throws IOException
    {
      if (!finished) {
        if (size() > 4294967295L) {
          throw new IOException("CompositeAtom \"" + type + "\" is too large: " + size());
        }
        
        long pointer = getRelativeStreamPosition();
        seekRelative(offset);
        

        DataAtomOutputStream headerData = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
        headerData.writeInt((int)size());
        headerData.writeType(type);
        for (AbstractQuickTimeStream.Atom child : children) {
          child.finish();
        }
        seekRelative(pointer);
        finished = true;
      }
    }
    
    public long size()
    {
      long length = 8L + data.size();
      for (AbstractQuickTimeStream.Atom child : children) {
        length += child.size();
      }
      return length;
    }
  }
  


  protected class DataAtom
    extends AbstractQuickTimeStream.Atom
  {
    protected DataAtomOutputStream data;
    

    protected boolean finished;
    

    public DataAtom(String name)
      throws IOException
    {
      super(name);
      out.writeLong(0L);
      data = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
    }
    
    public DataAtomOutputStream getOutputStream() {
      if (finished) {
        throw new IllegalStateException("DataAtom is finished");
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
          throw new IOException("DataAtom \"" + type + "\" is too large: " + size());
        }
        
        long pointer = getRelativeStreamPosition();
        seekRelative(offset);
        

        DataAtomOutputStream headerData = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
        headerData.writeUInt(size());
        headerData.writeType(type);
        seekRelative(pointer);
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
  


  protected class WideDataAtom
    extends AbstractQuickTimeStream.Atom
  {
    protected DataAtomOutputStream data;
    

    protected boolean finished;
    


    public WideDataAtom(String type)
      throws IOException
    {
      super(type);
      out.writeLong(0L);
      out.writeLong(0L);
      data = new DataAtomOutputStream(new ImageOutputStreamAdapter(out))
      {
        public void flush()
          throws IOException
        {}
      };
    }
    
    public DataAtomOutputStream getOutputStream()
    {
      if (finished) {
        throw new IllegalStateException("Atom is finished");
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
        long pointer = getRelativeStreamPosition();
        seekRelative(offset);
        

        DataAtomOutputStream headerData = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
        long finishedSize = size();
        if (finishedSize <= 4294967295L) {
          headerData.writeUInt(8L);
          headerData.writeType("wide");
          headerData.writeUInt(finishedSize - 8L);
          headerData.writeType(type);
        } else {
          headerData.writeInt(1);
          headerData.writeType(type);
          headerData.writeLong(finishedSize - 8L);
        }
        
        seekRelative(pointer);
        finished = true;
      }
    }
    
    public long size()
    {
      return 16L + data.size();
    }
  }
  

  protected static abstract class Group
  {
    protected AbstractQuickTimeStream.Sample firstSample;
    
    protected AbstractQuickTimeStream.Sample lastSample;
    protected long sampleCount;
    protected static final long maxSampleCount = 2147483647L;
    
    protected Group(AbstractQuickTimeStream.Sample firstSample)
    {
      this.firstSample = (this.lastSample = firstSample);
      sampleCount = 1L;
    }
    
    protected Group(AbstractQuickTimeStream.Sample firstSample, AbstractQuickTimeStream.Sample lastSample, long sampleCount) {
      this.firstSample = firstSample;
      this.lastSample = lastSample;
      this.sampleCount = sampleCount;
      if (sampleCount > 2147483647L) {
        throw new IllegalArgumentException("Capacity exceeded");
      }
    }
    
    protected Group(Group group) {
      firstSample = firstSample;
      lastSample = lastSample;
      sampleCount = sampleCount;
    }
    





    protected boolean maybeAddSample(AbstractQuickTimeStream.Sample sample)
    {
      if (sampleCount < 2147483647L) {
        lastSample = sample;
        sampleCount += 1L;
        return true;
      }
      return false;
    }
    





    protected boolean maybeAddChunk(AbstractQuickTimeStream.Chunk chunk)
    {
      if (sampleCount + sampleCount <= 2147483647L) {
        lastSample = lastSample;
        sampleCount += sampleCount;
        return true;
      }
      return false;
    }
    
    public long getSampleCount() {
      return sampleCount;
    }
  }
  




  protected static class Sample
  {
    long offset;
    



    long length;
    



    long duration;
    




    public Sample(long duration, long offset, long length)
    {
      this.duration = duration;
      this.offset = offset;
      this.length = length;
    }
  }
  

  protected static class TimeToSampleGroup
    extends AbstractQuickTimeStream.Group
  {
    public TimeToSampleGroup(AbstractQuickTimeStream.Sample firstSample)
    {
      super();
    }
    
    public TimeToSampleGroup(AbstractQuickTimeStream.Group group) {
      super();
    }
    







    public boolean maybeAddSample(AbstractQuickTimeStream.Sample sample)
    {
      if (firstSample.duration == duration) {
        return super.maybeAddSample(sample);
      }
      return false;
    }
    
    public boolean maybeAddChunk(AbstractQuickTimeStream.Chunk chunk)
    {
      if (firstSample.duration == firstSample.duration) {
        return super.maybeAddChunk(chunk);
      }
      return false;
    }
    
    public long getSampleDuration()
    {
      return firstSample.duration;
    }
  }
  

  protected static class SampleSizeGroup
    extends AbstractQuickTimeStream.Group
  {
    public SampleSizeGroup(AbstractQuickTimeStream.Sample firstSample)
    {
      super();
    }
    
    public SampleSizeGroup(AbstractQuickTimeStream.Group group) {
      super();
    }
    







    public boolean maybeAddSample(AbstractQuickTimeStream.Sample sample)
    {
      if (firstSample.length == length) {
        return super.maybeAddSample(sample);
      }
      return false;
    }
    
    public boolean maybeAddChunk(AbstractQuickTimeStream.Chunk chunk)
    {
      if (firstSample.length == firstSample.length) {
        return super.maybeAddChunk(chunk);
      }
      return false;
    }
    
    public long getSampleLength()
    {
      return firstSample.length;
    }
  }
  




  protected static class Chunk
    extends AbstractQuickTimeStream.Group
  {
    protected int sampleDescriptionId;
    




    public Chunk(AbstractQuickTimeStream.Sample firstSample, int sampleDescriptionId)
    {
      super();
      this.sampleDescriptionId = sampleDescriptionId;
    }
    





    public Chunk(AbstractQuickTimeStream.Sample firstSample, AbstractQuickTimeStream.Sample lastSample, int sampleCount, int sampleDescriptionId)
    {
      super(lastSample, sampleCount);
      this.sampleDescriptionId = sampleDescriptionId;
    }
    







    public boolean maybeAddSample(AbstractQuickTimeStream.Sample sample, int sampleDescriptionId)
    {
      if ((sampleDescriptionId == this.sampleDescriptionId) && 
        (lastSample.offset + lastSample.length == offset)) {
        return super.maybeAddSample(sample);
      }
      return false;
    }
    
    public boolean maybeAddChunk(Chunk chunk)
    {
      if ((sampleDescriptionId == sampleDescriptionId) && 
        (lastSample.offset + lastSample.length == firstSample.offset)) {
        return super.maybeAddChunk(chunk);
      }
      return false;
    }
    
    public long getChunkOffset()
    {
      return firstSample.offset;
    }
  }
  




  protected abstract class Track
  {
    protected final AbstractQuickTimeStream.MediaType mediaType;
    



    protected long mediaTimeScale = 600L;
    

    protected String mediaCompressionType;
    

    protected String mediaCompressorName;
    
    protected ArrayList<AbstractQuickTimeStream.Chunk> chunks = new ArrayList();
    


    protected ArrayList<AbstractQuickTimeStream.TimeToSampleGroup> timeToSamples = new ArrayList();
    


    protected ArrayList<AbstractQuickTimeStream.SampleSizeGroup> sampleSizes = new ArrayList();
    



    protected ArrayList<Long> syncSamples = null;
    
    protected long sampleCount = 0L;
    
    protected long mediaDuration = 0L;
    

    protected AbstractQuickTimeStream.Edit[] editList;
    
    protected int syncInterval;
    
    protected Codec codec;
    
    protected Buffer outputBuffer;
    

    public Track(AbstractQuickTimeStream.MediaType mediaType)
    {
      this.mediaType = mediaType;
    }
    
    public void addSample(AbstractQuickTimeStream.Sample sample, int sampleDescriptionId, boolean isSyncSample) {
      mediaDuration += duration;
      sampleCount += 1L;
      if (isSyncSample) {
        if (syncSamples != null) {
          syncSamples.add(Long.valueOf(sampleCount));
        }
      }
      else if (syncSamples == null) {
        syncSamples = new ArrayList();
        for (long i = 1L; i < sampleCount; i += 1L) {
          syncSamples.add(Long.valueOf(i));
        }
      }
      
      if ((timeToSamples.isEmpty()) || 
        (!((AbstractQuickTimeStream.TimeToSampleGroup)timeToSamples.get(timeToSamples.size() - 1)).maybeAddSample(sample))) {
        timeToSamples.add(new AbstractQuickTimeStream.TimeToSampleGroup(sample));
      }
      if ((sampleSizes.isEmpty()) || 
        (!((AbstractQuickTimeStream.SampleSizeGroup)sampleSizes.get(sampleSizes.size() - 1)).maybeAddSample(sample))) {
        sampleSizes.add(new AbstractQuickTimeStream.SampleSizeGroup(sample));
      }
      if ((chunks.isEmpty()) || 
        (!((AbstractQuickTimeStream.Chunk)chunks.get(chunks.size() - 1)).maybeAddSample(sample, sampleDescriptionId))) {
        chunks.add(new AbstractQuickTimeStream.Chunk(sample, sampleDescriptionId));
      }
    }
    
    public void addChunk(AbstractQuickTimeStream.Chunk chunk, boolean isSyncSample) {
      mediaDuration += firstSample.duration * sampleCount;
      sampleCount += sampleCount;
      if ((timeToSamples.isEmpty()) || 
        (!((AbstractQuickTimeStream.TimeToSampleGroup)timeToSamples.get(timeToSamples.size() - 1)).maybeAddChunk(chunk))) {
        timeToSamples.add(new AbstractQuickTimeStream.TimeToSampleGroup(chunk));
      }
      if ((sampleSizes.isEmpty()) || 
        (!((AbstractQuickTimeStream.SampleSizeGroup)sampleSizes.get(sampleSizes.size() - 1)).maybeAddChunk(chunk))) {
        sampleSizes.add(new AbstractQuickTimeStream.SampleSizeGroup(chunk));
      }
      if ((chunks.isEmpty()) || 
        (!((AbstractQuickTimeStream.Chunk)chunks.get(chunks.size() - 1)).maybeAddChunk(chunk))) {
        chunks.add(chunk);
      }
    }
    
    public boolean isEmpty() {
      return sampleCount == 0L;
    }
    
    public long getSampleCount() {
      return sampleCount;
    }
    



    public long getTrackDuration(long movieTimeScale)
    {
      if ((editList == null) || (editList.length == 0)) {
        return mediaDuration * movieTimeScale / mediaTimeScale;
      }
      long duration = 0L;
      for (int i = 0; i < editList.length; i++) {
        duration += editList[i].trackDuration;
      }
      return duration;
    }
    



    protected void writeTrackAtoms(int trackIndex, AbstractQuickTimeStream.CompositeAtom moovAtom, Date modificationTime)
      throws IOException
    {
      AbstractQuickTimeStream.CompositeAtom trakAtom = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "trak");
      moovAtom.add(trakAtom);
      

























      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "tkhd");
      trakAtom.add(leaf);
      DataAtomOutputStream d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(15);
      















      d.writeMacTimestamp(creationTime);
      




      d.writeMacTimestamp(modificationTime);
      




      d.writeInt(trackIndex + 1);
      


      d.writeInt(0);
      

      d.writeUInt(getTrackDuration(movieTimeScale));
      






      d.writeLong(0L);
      

      d.writeShort(0);
      




      d.writeShort(0);
      





      d.writeFixed8D8(mediaType == AbstractQuickTimeStream.MediaType.AUDIO ? 1 : 0);
      


      d.writeShort(0);
      

      d.writeFixed16D16(1.0D);
      d.writeFixed16D16(0.0D);
      d.writeFixed2D30(0.0D);
      d.writeFixed16D16(0.0D);
      d.writeFixed16D16(1.0D);
      d.writeFixed2D30(0.0D);
      d.writeFixed16D16(0.0D);
      d.writeFixed16D16(0.0D);
      d.writeFixed2D30(1.0D);
      



      d.writeFixed16D16(mediaType == AbstractQuickTimeStream.MediaType.VIDEO ? videoWidth : 0);
      

      d.writeFixed16D16(mediaType == AbstractQuickTimeStream.MediaType.VIDEO ? videoHeight : 0);
      


      AbstractQuickTimeStream.CompositeAtom edtsAtom = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "edts");
      trakAtom.add(edtsAtom);
      















      leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "elst");
      edtsAtom.add(leaf);
      d = leaf.getOutputStream();
      
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      
      AbstractQuickTimeStream.Edit[] elist = editList;
      if ((elist == null) || (elist.length == 0)) {
        d.writeUInt(1L);
        d.writeUInt(getTrackDuration(movieTimeScale));
        d.writeUInt(0L);
        d.writeFixed16D16(1.0D);
      } else {
        d.writeUInt(elist.length);
        for (int i = 0; i < elist.length; i++) {
          d.writeUInt(trackDuration);
          d.writeUInt(mediaTime);
          d.writeUInt(mediaRate);
        }
      }
      


      AbstractQuickTimeStream.CompositeAtom mdiaAtom = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "mdia");
      trakAtom.add(mdiaAtom);
      











      leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "mdhd");
      mdiaAtom.add(leaf);
      d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeMacTimestamp(creationTime);
      




      d.writeMacTimestamp(modificationTime);
      




      d.writeUInt(mediaTimeScale);
      



      d.writeUInt(mediaDuration);
      

      d.writeShort(0);
      



      d.writeShort(0);
      



      leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "hdlr");
      mdiaAtom.add(leaf);
      










      d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeType("mhlr");
      



      d.writeType(mediaType == AbstractQuickTimeStream.MediaType.VIDEO ? "vide" : "soun");
      






      if (mediaType == AbstractQuickTimeStream.MediaType.AUDIO) {
        d.writeType("appl");
      } else {
        d.writeUInt(0L);
      }
      


      d.writeUInt(mediaType == AbstractQuickTimeStream.MediaType.AUDIO ? 268435456L : 0L);
      

      d.writeUInt(mediaType == AbstractQuickTimeStream.MediaType.AUDIO ? 65941 : 0);
      

      d.writePString(mediaType == AbstractQuickTimeStream.MediaType.AUDIO ? "Apple Sound Media Handler" : "");
      




      writeMediaInformationAtoms(mdiaAtom);
    }
    

    protected void writeMediaInformationAtoms(AbstractQuickTimeStream.CompositeAtom mdiaAtom)
      throws IOException
    {
      AbstractQuickTimeStream.CompositeAtom minfAtom = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "minf");
      mdiaAtom.add(minfAtom);
      

      writeMediaInformationHeaderAtom(minfAtom);
      





      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "hdlr");
      minfAtom.add(leaf);
      










      DataAtomOutputStream d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeType("dhlr");
      



      d.writeType("alis");
      





      if (mediaType == AbstractQuickTimeStream.MediaType.AUDIO) {
        d.writeType("appl");
      } else {
        d.writeUInt(0L);
      }
      


      d.writeUInt(mediaType == AbstractQuickTimeStream.MediaType.AUDIO ? 268435457L : 0L);
      

      d.writeInt(mediaType == AbstractQuickTimeStream.MediaType.AUDIO ? 65967 : 0);
      

      d.writePString("Apple Alias Data Handler");
      




      AbstractQuickTimeStream.CompositeAtom dinfAtom = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "dinf");
      minfAtom.add(dinfAtom);
      



      leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "dref");
      dinfAtom.add(leaf);
      




















      d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeInt(1);
      

      d.writeInt(12);
      


      d.writeType("alis");
      



      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(1);
      










      writeSampleTableAtoms(minfAtom);
    }
    

    protected abstract void writeMediaInformationHeaderAtom(AbstractQuickTimeStream.CompositeAtom paramCompositeAtom)
      throws IOException;
    
    protected abstract void writeSampleDescriptionAtom(AbstractQuickTimeStream.CompositeAtom paramCompositeAtom)
      throws IOException;
    
    protected void writeSampleTableAtoms(AbstractQuickTimeStream.CompositeAtom minfAtom)
      throws IOException
    {
      AbstractQuickTimeStream.CompositeAtom stblAtom = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "stbl");
      minfAtom.add(stblAtom);
      

      writeSampleDescriptionAtom(stblAtom);
      






      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "stts");
      stblAtom.add(leaf);
      












      DataAtomOutputStream d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeUInt(timeToSamples.size());
      


      for (AbstractQuickTimeStream.TimeToSampleGroup tts : timeToSamples) {
        d.writeUInt(tts.getSampleCount());
        


        d.writeUInt(tts.getSampleDuration());
      }
      





      leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "stsc");
      stblAtom.add(leaf);
      













      d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      int entryCount = 0;
      long previousSampleCount = -1L;
      long previousSampleDescriptionId = -1L;
      for (AbstractQuickTimeStream.Chunk c : chunks) {
        if ((sampleCount != previousSampleCount) || 
          (sampleDescriptionId != previousSampleDescriptionId)) {
          previousSampleCount = sampleCount;
          previousSampleDescriptionId = sampleDescriptionId;
          entryCount++;
        }
      }
      
      d.writeInt(entryCount);
      

      int firstChunk = 1;
      previousSampleCount = -1L;
      previousSampleDescriptionId = -1L;
      for (AbstractQuickTimeStream.Chunk c : chunks) {
        if ((sampleCount != previousSampleCount) || 
          (sampleDescriptionId != previousSampleDescriptionId)) {
          previousSampleCount = sampleCount;
          previousSampleDescriptionId = sampleDescriptionId;
          
          d.writeUInt(firstChunk);
          

          d.writeUInt(sampleCount);
          

          d.writeInt(sampleDescriptionId);
        }
        




        firstChunk++;
      }
      

      if (syncSamples != null) {
        leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "stss");
        stblAtom.add(leaf);
        











        d = leaf.getOutputStream();
        d.write(0);
        

        d.write(0);
        d.write(0);
        d.write(0);
        

        d.writeUInt(syncSamples.size());
        


        for (Long number : syncSamples) {
          d.writeUInt(number.longValue());
        }
      }
      









      leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "stsz");
      stblAtom.add(leaf);
      












      d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      int sampleUnit = (mediaType == AbstractQuickTimeStream.MediaType.AUDIO) && 
        (soundCompressionId != -2) ? 
        soundSampleSize / 8 * soundNumberOfChannels : 
        1;
      if (sampleSizes.size() == 1) {
        d.writeUInt(((AbstractQuickTimeStream.SampleSizeGroup)sampleSizes.get(0)).getSampleLength() / sampleUnit);
        




        d.writeUInt(((AbstractQuickTimeStream.SampleSizeGroup)sampleSizes.get(0)).getSampleCount());

      }
      else
      {
        d.writeUInt(0L);
        





        long count = 0L;
        for (AbstractQuickTimeStream.SampleSizeGroup s : sampleSizes) {
          count += sampleCount;
        }
        d.writeUInt(count);
        
        AbstractQuickTimeStream.SampleSizeGroup s;
        int i;
        for (??? = sampleSizes.iterator(); ???.hasNext(); 
            
            i < sampleCount)
        {
          s = (AbstractQuickTimeStream.SampleSizeGroup)???.next();
          long sampleSize = s.getSampleLength() / sampleUnit;
          i = 0; continue;
          d.writeUInt(sampleSize);i++;
        }
      }
      












      if ((chunks.isEmpty()) || (((AbstractQuickTimeStream.Chunk)chunks.get(chunks.size() - 1)).getChunkOffset() <= 4294967295L))
      {
        leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "stco");
        stblAtom.add(leaf);
        











        d = leaf.getOutputStream();
        d.write(0);
        

        d.write(0);
        d.write(0);
        d.write(0);
        

        d.writeUInt(chunks.size());
        

        for (AbstractQuickTimeStream.Chunk c : chunks) {
          d.writeUInt(c.getChunkOffset() + mdatOffset);
        }
        

      }
      else
      {

        leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "co64");
        stblAtom.add(leaf);
        











        d = leaf.getOutputStream();
        d.write(0);
        

        d.write(0);
        d.write(0);
        d.write(0);
        

        d.writeUInt(chunks.size());
        


        for (AbstractQuickTimeStream.Chunk c : chunks) {
          d.writeLong(c.getChunkOffset());
        }
      }
    }
  }
  






  protected class VideoTrack
    extends AbstractQuickTimeStream.Track
  {
    protected VideoFormat videoFormat;
    





    protected float videoQuality = 0.97F;
    



    protected int videoWidth = -1;
    



    protected int videoHeight = -1;
    



    protected int videoDepth = -1;
    
    protected IndexColorModel videoColorTable;
    

    public VideoTrack()
    {
      super(AbstractQuickTimeStream.MediaType.VIDEO);
    }
    



    protected void writeMediaInformationHeaderAtom(AbstractQuickTimeStream.CompositeAtom minfAtom)
      throws IOException
    {
      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "vmhd");
      minfAtom.add(leaf);
      







      DataAtomOutputStream d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(1);
      






      d.writeShort(64);
      






      d.writeUShort(0);
      d.writeUShort(0);
      d.writeUShort(0);
    }
    












    protected void writeSampleDescriptionAtom(AbstractQuickTimeStream.CompositeAtom stblAtom)
      throws IOException
    {
      AbstractQuickTimeStream.CompositeAtom leaf = new AbstractQuickTimeStream.CompositeAtom(AbstractQuickTimeStream.this, "stsd");
      stblAtom.add(leaf);
      















      DataAtomOutputStream d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeInt(1);
      


      d.writeInt(86);
      
      d.writeType(mediaCompressionType);
      




      d.write(new byte[6]);
      

      d.writeShort(1);
      









      d.writeShort(0);
      



      d.writeShort(0);
      

      d.writeType("java");
      



      d.writeInt(0);
      


      d.writeInt(512);
      


      d.writeUShort(videoWidth);
      


      d.writeUShort(videoHeight);
      

      d.writeFixed16D16(72.0D);
      


      d.writeFixed16D16(72.0D);
      


      d.writeInt(0);
      

      d.writeShort(1);
      


      d.writePString(mediaCompressorName, 32);
      


      d.writeShort(videoDepth);
      






      d.writeShort(videoColorTable == null ? -1 : 0);
      








      if (videoColorTable != null) {
        writeColorTableAtom(leaf);
      }
    }
    










    protected void writeColorTableAtom(AbstractQuickTimeStream.CompositeAtom stblAtom)
      throws IOException
    {
      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "ctab");
      stblAtom.add(leaf);
      
      DataAtomOutputStream d = leaf.getOutputStream();
      
      d.writeUInt(0L);
      d.writeUShort(32768);
      d.writeUShort(videoColorTable.getMapSize() - 1);
      



      int i = 0; for (int n = videoColorTable.getMapSize(); i < n; i++)
      {


        d.writeUShort(0);
        d.writeUShort(videoColorTable.getRed(i) << 8 | videoColorTable.getRed(i));
        d.writeUShort(videoColorTable.getGreen(i) << 8 | videoColorTable.getGreen(i));
        d.writeUShort(videoColorTable.getBlue(i) << 8 | videoColorTable.getBlue(i));
      }
    }
  }
  





  protected class AudioTrack
    extends AbstractQuickTimeStream.Track
  {
    protected int soundNumberOfChannels;
    




    protected int soundSampleSize;
    




    protected int soundCompressionId;
    




    protected long soundSamplesPerPacket;
    




    protected int soundBytesPerPacket;
    




    protected int soundBytesPerFrame;
    




    protected int soundBytesPerSample;
    




    protected double soundSampleRate;
    



    protected byte[] stsdExtensions = new byte[0];
    
    public AudioTrack() {
      super(AbstractQuickTimeStream.MediaType.AUDIO);
    }
    



    protected void writeMediaInformationHeaderAtom(AbstractQuickTimeStream.CompositeAtom minfAtom)
      throws IOException
    {
      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "smhd");
      minfAtom.add(leaf);
      





      DataAtomOutputStream d = leaf.getOutputStream();
      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeFixed8D8(0.0F);
      











      d.writeUShort(0);
    }
    













    protected void writeSampleDescriptionAtom(AbstractQuickTimeStream.CompositeAtom stblAtom)
      throws IOException
    {
      AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(AbstractQuickTimeStream.this, "stsd");
      stblAtom.add(leaf);
      



























      DataAtomOutputStream d = leaf.getOutputStream();
      


      d.write(0);
      

      d.write(0);
      d.write(0);
      d.write(0);
      

      d.writeInt(1);
      




      d.writeUInt(52 + stsdExtensions.length);
      

      d.writeType(mediaCompressionType);
      



      d.write(new byte[6]);
      

      d.writeUShort(1);
      







      d.writeUShort(1);
      

      d.writeUShort(0);
      

      d.writeUInt(0L);
      

      d.writeUShort(soundNumberOfChannels);
      



      d.writeUShort(soundSampleSize);
      




      d.writeUShort(soundCompressionId);
      




      d.writeUShort(0);
      

      d.writeFixed16D16(soundSampleRate);
      








      d.writeUInt(soundSamplesPerPacket);
      







      d.writeUInt(soundBytesPerPacket);
      









      d.writeUInt(soundBytesPerFrame);
      






      d.writeUInt(soundBytesPerSample);
      







      d.write(stsdExtensions);
    }
  }
  







  public static class Edit
  {
    public int trackDuration;
    






    public int mediaTime;
    






    public int mediaRate;
    







    public Edit(int trackDuration, int mediaTime, double mediaRate)
    {
      if (trackDuration < 0) {
        throw new IllegalArgumentException("trackDuration must not be < 0:" + trackDuration);
      }
      if (mediaTime < -1) {
        throw new IllegalArgumentException("mediaTime must not be < -1:" + mediaTime);
      }
      if (mediaRate <= 0.0D) {
        throw new IllegalArgumentException("mediaRate must not be <= 0:" + mediaRate);
      }
      this.trackDuration = trackDuration;
      this.mediaTime = mediaTime;
      this.mediaRate = ((int)(mediaRate * 65536.0D));
    }
    












    public Edit(int trackDuration, int mediaTime, int mediaRate)
    {
      if (trackDuration < 0) {
        throw new IllegalArgumentException("trackDuration must not be < 0:" + trackDuration);
      }
      if (mediaTime < -1) {
        throw new IllegalArgumentException("mediaTime must not be < -1:" + mediaTime);
      }
      if (mediaRate <= 0) {
        throw new IllegalArgumentException("mediaRate must not be <= 0:" + mediaRate);
      }
      this.trackDuration = trackDuration;
      this.mediaTime = mediaTime;
      this.mediaRate = mediaRate;
    }
  }
}
