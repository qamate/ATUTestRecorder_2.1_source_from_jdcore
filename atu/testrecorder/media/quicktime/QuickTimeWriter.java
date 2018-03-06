package atu.testrecorder.media.quicktime;

import atu.testrecorder.media.Buffer;
import atu.testrecorder.media.Codec;
import atu.testrecorder.media.MovieWriter;
import atu.testrecorder.media.VideoFormat;
import atu.testrecorder.media.io.ImageOutputStreamAdapter;
import atu.testrecorder.media.jpeg.JPEGCodec;
import atu.testrecorder.media.png.PNGCodec;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;




































































































public class QuickTimeWriter
  extends AbstractQuickTimeStream
  implements MovieWriter
{
  public static final VideoFormat VIDEO_RAW = new VideoFormat("raw ", "NONE");
  public static final VideoFormat VIDEO_ANIMATION = new VideoFormat("rle ", "Animation");
  public static final VideoFormat VIDEO_JPEG = new VideoFormat("jpeg", "Photo - JPEG");
  public static final VideoFormat VIDEO_PNG = new VideoFormat("png ", "PNG");
  



  public QuickTimeWriter(File file)
    throws IOException
  {
    if (file.exists()) {
      file.delete();
    }
    out = new FileImageOutputStream(file);
    streamOffset = 0L;
  }
  



  public QuickTimeWriter(ImageOutputStream out)
    throws IOException
  {
    this.out = out;
    streamOffset = out.getStreamPosition();
  }
  







  public void setMovieTimeScale(long timeScale)
  {
    if ((timeScale < 1L) || (timeScale > 8589934592L)) {
      throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
    }
    movieTimeScale = timeScale;
  }
  





  public long getMovieTimeScale()
  {
    return movieTimeScale;
  }
  






  public long getMediaTimeScale(int track)
  {
    return tracks.get(track)).mediaTimeScale;
  }
  





  public long getMediaDuration(int track)
  {
    return tracks.get(track)).mediaDuration;
  }
  









  public long getUneditedTrackDuration(int track)
  {
    AbstractQuickTimeStream.Track t = (AbstractQuickTimeStream.Track)tracks.get(track);
    return mediaDuration * mediaTimeScale / movieTimeScale;
  }
  











  public long getTrackDuration(int track)
  {
    return ((AbstractQuickTimeStream.Track)tracks.get(track)).getTrackDuration(movieTimeScale);
  }
  




  public long getMovieDuration()
  {
    long duration = 0L;
    for (AbstractQuickTimeStream.Track t : tracks) {
      duration = Math.max(duration, t.getTrackDuration(movieTimeScale));
    }
    return duration;
  }
  





  public void setVideoColorTable(int track, IndexColorModel icm)
  {
    AbstractQuickTimeStream.VideoTrack t = (AbstractQuickTimeStream.VideoTrack)tracks.get(track);
    videoColorTable = icm;
  }
  






  public IndexColorModel getVideoColorTable(int track)
  {
    AbstractQuickTimeStream.VideoTrack t = (AbstractQuickTimeStream.VideoTrack)tracks.get(track);
    return videoColorTable;
  }
  






  public void setEditList(int track, AbstractQuickTimeStream.Edit[] editList)
  {
    if ((editList != null) && (editList.length > 0) && (length1mediaTime == -1)) {
      throw new IllegalArgumentException("Edit list must not end with empty edit.");
    }
    tracks.get(track)).editList = editList;
  }
  















  public int addVideoTrack(VideoFormat format, long timeScale, int width, int height)
    throws IOException
  {
    return addVideoTrack(format.getEncoding(), format.getCompressorName(), timeScale, width, height, 24, 30);
  }
  















  public int addVideoTrack(VideoFormat format, long timeScale, int width, int height, int depth, int syncInterval)
    throws IOException
  {
    return addVideoTrack(format.getEncoding(), format.getCompressorName(), timeScale, width, height, depth, syncInterval);
  }
  



















  public int addVideoTrack(String compressionType, String compressorName, long timeScale, int width, int height, int depth, int syncInterval)
    throws IOException
  {
    ensureStarted();
    if ((compressionType == null) || (compressionType.length() != 4)) {
      throw new IllegalArgumentException("compressionType must be 4 characters long:" + compressionType);
    }
    if ((compressorName == null) || (compressorName.length() < 1) || (compressorName.length() > 32)) {
      throw new IllegalArgumentException("compressorName must be between 1 and 32 characters long:" + compressionType);
    }
    if ((timeScale < 1L) || (timeScale > 8589934592L)) {
      throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
    }
    if ((width < 1) || (height < 1)) {
      throw new IllegalArgumentException("Width and height must be greater than 0, width:" + width + " height:" + height);
    }
    
    AbstractQuickTimeStream.VideoTrack t = new AbstractQuickTimeStream.VideoTrack(this);
    mediaCompressionType = compressionType;
    mediaCompressorName = compressorName;
    mediaTimeScale = timeScale;
    videoWidth = width;
    videoHeight = height;
    videoDepth = depth;
    syncInterval = syncInterval;
    videoFormat = new VideoFormat(compressionType, compressorName, [B.class, width, height, depth);
    createCodec(t);
    tracks.add(t);
    return tracks.size() - 1;
  }
  
  private void createCodec(AbstractQuickTimeStream.VideoTrack vt) {
    String enc = videoFormat.getEncoding();
    if (enc.equals("jpeg")) {
      codec = new JPEGCodec();
    } else if (enc.equals("png ")) {
      codec = new PNGCodec();
    } else if (enc.equals("raw ")) {
      codec = new RawCodec();
    } else if (enc.equals("rle ")) {
      codec = new AnimationCodec();
    }
    codec.setInputFormat(new VideoFormat("image", BufferedImage.class, videoWidth, videoHeight, videoDepth));
    codec.setOutputFormat(new VideoFormat(videoFormat.getEncoding(), videoFormat.getCompressorName(), [B.class, videoWidth, videoHeight, videoDepth));
    codec.setQuality(videoQuality);
  }
  
  public Codec getCodec(int track)
  {
    return tracks.get(track)).codec;
  }
  
  public void setCodec(int track, Codec codec)
  {
    tracks.get(track)).codec = codec;
  }
  







  public int addAudioTrack(AudioFormat format)
    throws IOException
  {
    ensureStarted();
    
    double sampleRate = format.getSampleRate();
    long timeScale = (int)Math.floor(sampleRate);
    int sampleSizeInBits = format.getSampleSizeInBits();
    int numberOfChannels = format.getChannels();
    boolean bigEndian = format.isBigEndian();
    int frameDuration = (int)(format.getSampleRate() / format.getFrameRate());
    int frameSize = format.getFrameSize();
    boolean isCompressed = (format.getProperty("vbr") != null) && (((Boolean)format.getProperty("vbr")).booleanValue());
    String qtAudioFormat;
    if (format.getEncoding().equals(AudioFormat.Encoding.ALAW)) {
      String qtAudioFormat = "alaw";
      if (sampleSizeInBits != 8) {
        throw new IllegalArgumentException("Sample size of 8 for ALAW required:" + sampleSizeInBits);
      }
    } else if (format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) { String qtAudioFormat;
      String qtAudioFormat; String qtAudioFormat; String qtAudioFormat; switch (sampleSizeInBits) {
      case 16: 
        qtAudioFormat = bigEndian ? "twos" : "sowt";
        break;
      case 24: 
        qtAudioFormat = "in24";
        break;
      case 32: 
        qtAudioFormat = "in32";
        break;
      default: 
        throw new IllegalArgumentException("Sample size of 16, 24 or 32 for PCM_SIGNED required:" + sampleSizeInBits);break; }
    } else { String qtAudioFormat;
      if (format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
        if (sampleSizeInBits != 8) {
          throw new IllegalArgumentException("Sample size of 8 PCM_UNSIGNED required:" + sampleSizeInBits);
        }
        qtAudioFormat = "raw "; } else { String qtAudioFormat;
        if (format.getEncoding().equals(AudioFormat.Encoding.ULAW)) {
          if (sampleSizeInBits != 8) {
            throw new IllegalArgumentException("Sample size of 8 for ULAW required:" + sampleSizeInBits);
          }
          qtAudioFormat = "ulaw"; } else { String qtAudioFormat;
          if (format.getEncoding().toString().equals("MP3")) {
            qtAudioFormat = ".mp3";
          } else {
            qtAudioFormat = format.getEncoding().toString();
            if (qtAudioFormat.length() != 4)
              throw new IllegalArgumentException("Unsupported encoding:" + format.getEncoding());
          }
        }
      } }
    return addAudioTrack(qtAudioFormat, timeScale, sampleRate, 
      numberOfChannels, sampleSizeInBits, 
      isCompressed, frameDuration, frameSize);
  }
  
























  public int addAudioTrack(String compressionType, long timeScale, double sampleRate, int numberOfChannels, int sampleSizeInBits, boolean isCompressed, int frameDuration, int frameSize)
    throws IOException
  {
    ensureStarted();
    if ((compressionType == null) || (compressionType.length() != 4)) {
      throw new IllegalArgumentException("audioFormat must be 4 characters long:" + compressionType);
    }
    if ((timeScale < 1L) || (timeScale > 8589934592L)) {
      throw new IllegalArgumentException("timeScale must be between 1 and 2^32:" + timeScale);
    }
    if (timeScale != (int)Math.floor(sampleRate)) {
      throw new IllegalArgumentException("timeScale: " + timeScale + " must match integer portion of sampleRate: " + sampleRate);
    }
    if ((numberOfChannels != 1) && (numberOfChannels != 2)) {
      throw new IllegalArgumentException("numberOfChannels must be 1 or 2: " + numberOfChannels);
    }
    if ((sampleSizeInBits != 8) && (sampleSizeInBits != 16)) {
      throw new IllegalArgumentException("sampleSize must be 8 or 16: " + numberOfChannels);
    }
    
    AbstractQuickTimeStream.AudioTrack t = new AbstractQuickTimeStream.AudioTrack(this);
    mediaCompressionType = compressionType;
    mediaTimeScale = timeScale;
    soundSampleRate = sampleRate;
    soundCompressionId = (isCompressed ? -2 : -1);
    soundNumberOfChannels = numberOfChannels;
    soundSampleSize = sampleSizeInBits;
    soundSamplesPerPacket = frameDuration;
    if (isCompressed) {
      soundBytesPerPacket = frameSize;
      soundBytesPerFrame = (frameSize * numberOfChannels);
    } else {
      soundBytesPerPacket = (frameSize / numberOfChannels);
      soundBytesPerFrame = frameSize;
    }
    soundBytesPerSample = (sampleSizeInBits / 8);
    tracks.add(t);
    return tracks.size() - 1;
  }
  
















  public void setCompressionQuality(int track, float newValue)
  {
    AbstractQuickTimeStream.VideoTrack vt = (AbstractQuickTimeStream.VideoTrack)tracks.get(track);
    videoQuality = newValue;
    if (codec != null) {
      codec.setQuality(newValue);
    }
  }
  




  public float getCompressionQuality(int track)
  {
    return tracks.get(track)).videoQuality;
  }
  






  public void setSyncInterval(int track, int i)
  {
    tracks.get(track)).syncInterval = i;
  }
  
  public int getSyncInterval(int track)
  {
    return tracks.get(track)).syncInterval;
  }
  




  protected void ensureStarted()
    throws IOException
  {
    ensureOpen();
    if (state == AbstractQuickTimeStream.States.FINISHED) {
      throw new IOException("Can not write into finished movie.");
    }
    if (state != AbstractQuickTimeStream.States.STARTED) {
      creationTime = new Date();
      writeProlog();
      mdatAtom = new AbstractQuickTimeStream.WideDataAtom(this, "mdat");
      state = AbstractQuickTimeStream.States.STARTED;
    }
  }
  













  public void writeFrame(int track, BufferedImage image, long duration)
    throws IOException
  {
    if (duration <= 0L) {
      throw new IllegalArgumentException("Duration must be greater 0.");
    }
    AbstractQuickTimeStream.VideoTrack vt = (AbstractQuickTimeStream.VideoTrack)tracks.get(track);
    if (mediaType != AbstractQuickTimeStream.MediaType.VIDEO) {
      throw new IllegalArgumentException("Track " + track + " is not a video track");
    }
    if (codec == null) {
      throw new UnsupportedOperationException("No codec for this video format.");
    }
    ensureStarted();
    

    if (videoWidth == -1) {
      videoWidth = image.getWidth();
      videoHeight = image.getHeight();

    }
    else if ((videoWidth != image.getWidth()) || (videoHeight != image.getHeight())) {
      throw new IllegalArgumentException("Dimensions of frame[" + ((AbstractQuickTimeStream.Track)tracks.get(track)).getSampleCount() + 
        "] (width=" + image.getWidth() + ", height=" + image.getHeight() + 
        ") differs from video dimension (width=" + 
        videoWidth + ", height=" + videoHeight + ") in track " + track + ".");
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
    OutputStream mdatOut = mdatAtom.getOutputStream();
    mdatOut.write((byte[])outputBuffer.data, outputBuffer.offset, outputBuffer.length);
    
    long length = getRelativeStreamPosition() - offset;
    vt.addSample(new AbstractQuickTimeStream.Sample(duration, offset, length), 1, isSync);
  }
  















  public void writeSample(int track, File file, long duration)
    throws IOException
  {
    writeSample(track, file, duration, true);
  }
  















  public void writeSample(int track, File file, long duration, boolean isSync)
    throws IOException
  {
    ensureStarted();
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      writeSample(track, in, duration, isSync);
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }
  











  public void writeSample(int track, InputStream in, long duration)
    throws IOException
  {
    writeSample(track, in, duration, true);
  }
  












  public void writeSample(int track, InputStream in, long duration, boolean isSync)
    throws IOException
  {
    ensureStarted();
    if (duration <= 0L) {
      throw new IllegalArgumentException("duration must be greater 0");
    }
    AbstractQuickTimeStream.Track t = (AbstractQuickTimeStream.Track)tracks.get(track);
    ensureOpen();
    ensureStarted();
    long offset = getRelativeStreamPosition();
    OutputStream mdatOut = mdatAtom.getOutputStream();
    byte[] buf = new byte['က'];
    int len;
    while ((len = in.read(buf)) != -1) { int len;
      mdatOut.write(buf, 0, len);
    }
    long length = getRelativeStreamPosition() - offset;
    t.addSample(new AbstractQuickTimeStream.Sample(duration, offset, length), 1, isSync);
  }
  











  public void writeSample(int track, byte[] data, long duration)
    throws IOException
  {
    writeSample(track, data, 0, data.length, duration, true);
  }
  












  public void writeSample(int track, byte[] data, long duration, boolean isSync)
    throws IOException
  {
    ensureStarted();
    writeSample(track, data, 0, data.length, duration, isSync);
  }
  













  public void writeSample(int track, byte[] data, int off, int len, long duration)
    throws IOException
  {
    ensureStarted();
    writeSample(track, data, off, len, duration, true);
  }
  














  public void writeSample(int track, byte[] data, int off, int len, long duration, boolean isSync)
    throws IOException
  {
    ensureStarted();
    if (duration <= 0L) {
      throw new IllegalArgumentException("duration must be greater 0");
    }
    AbstractQuickTimeStream.Track t = (AbstractQuickTimeStream.Track)tracks.get(track);
    ensureOpen();
    ensureStarted();
    long offset = getRelativeStreamPosition();
    OutputStream mdatOut = mdatAtom.getOutputStream();
    mdatOut.write(data, off, len);
    t.addSample(new AbstractQuickTimeStream.Sample(duration, offset, len), 1, isSync);
  }
  















  public void writeSamples(int track, int sampleCount, byte[] data, long sampleDuration)
    throws IOException
  {
    writeSamples(track, sampleCount, data, 0, data.length, sampleDuration, true);
  }
  















  public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration)
    throws IOException
  {
    writeSamples(track, sampleCount, data, off, len, sampleDuration, true);
  }
  



















  public void writeSamples(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration, boolean isSync)
    throws IOException
  {
    ensureStarted();
    if (sampleDuration <= 0L) {
      throw new IllegalArgumentException("sampleDuration must be greater 0, sampleDuration=" + sampleDuration);
    }
    if (sampleCount <= 0) {
      throw new IllegalArgumentException("sampleCount must be greater 0, sampleCount=" + sampleCount);
    }
    if (len % sampleCount != 0) {
      throw new IllegalArgumentException("len must be divisable by sampleCount len=" + len + " sampleCount=" + sampleCount);
    }
    AbstractQuickTimeStream.Track t = (AbstractQuickTimeStream.Track)tracks.get(track);
    ensureOpen();
    ensureStarted();
    long offset = getRelativeStreamPosition();
    OutputStream mdatOut = mdatAtom.getOutputStream();
    mdatOut.write(data, off, len);
    

    int sampleLength = len / sampleCount;
    AbstractQuickTimeStream.Sample first = new AbstractQuickTimeStream.Sample(sampleDuration, offset, sampleLength);
    AbstractQuickTimeStream.Sample last = new AbstractQuickTimeStream.Sample(sampleDuration, offset + sampleLength * (sampleCount - 1), sampleLength);
    t.addChunk(new AbstractQuickTimeStream.Chunk(first, last, sampleCount, 1), isSync);
  }
  

  public boolean isVFRSupported()
  {
    return true;
  }
  







  public boolean isDataLimitReached()
  {
    try
    {
      long maxMediaDuration = 0L;
      for (AbstractQuickTimeStream.Track t : tracks) {
        maxMediaDuration = Math.max(mediaDuration, maxMediaDuration);
      }
      

      return (getRelativeStreamPosition() > 2305843009213693952L) || (maxMediaDuration > 2305843009213693952L);
    } catch (IOException ex) {}
    return true;
  }
  




  public void close()
    throws IOException
  {
    try
    {
      if (state == AbstractQuickTimeStream.States.STARTED) {
        finish();
      }
    } finally {
      if (state != AbstractQuickTimeStream.States.CLOSED) {
        out.close();
        state = AbstractQuickTimeStream.States.CLOSED;
      }
    }
  }
  







  public void finish()
    throws IOException
  {
    ensureOpen();
    if (state != AbstractQuickTimeStream.States.FINISHED) {
      int i = 0; for (int n = tracks.size(); i < n; i++) {}
      
      mdatAtom.finish();
      writeEpilog();
      state = AbstractQuickTimeStream.States.FINISHED;
    }
  }
  








  protected void ensureOpen()
    throws IOException
  {
    if (state == AbstractQuickTimeStream.States.CLOSED) {
      throw new IOException("Stream closed");
    }
  }
  









  private void writeProlog()
    throws IOException
  {
    AbstractQuickTimeStream.DataAtom ftypAtom = new AbstractQuickTimeStream.DataAtom(this, "ftyp");
    DataAtomOutputStream d = ftypAtom.getOutputStream();
    d.writeType("qt  ");
    d.writeBCD4(2005);
    d.writeBCD2(3);
    d.writeBCD2(0);
    d.writeType("qt  ");
    d.writeInt(0);
    d.writeInt(0);
    d.writeInt(0);
    ftypAtom.finish();
  }
  
  private void writeEpilog() throws IOException {
    Date modificationTime = new Date();
    long duration = getMovieDuration();
    



    moovAtom = new AbstractQuickTimeStream.CompositeAtom(this, "moov");
    

























    AbstractQuickTimeStream.DataAtom leaf = new AbstractQuickTimeStream.DataAtom(this, "mvhd");
    moovAtom.add(leaf);
    DataAtomOutputStream d = leaf.getOutputStream();
    d.writeByte(0);
    

    d.writeByte(0);
    d.writeByte(0);
    d.writeByte(0);
    

    d.writeMacTimestamp(creationTime);
    




    d.writeMacTimestamp(modificationTime);
    




    d.writeUInt(movieTimeScale);
    




    d.writeUInt(duration);
    




    d.writeFixed16D16(1.0D);
    


    d.writeShort(256);
    


    d.write(new byte[10]);
    

    d.writeFixed16D16(1.0D);
    d.writeFixed16D16(0.0D);
    d.writeFixed2D30(0.0D);
    d.writeFixed16D16(0.0D);
    d.writeFixed16D16(1.0D);
    d.writeFixed2D30(0.0D);
    d.writeFixed16D16(0.0D);
    d.writeFixed16D16(0.0D);
    d.writeFixed2D30(1.0D);
    




    d.writeInt(0);
    

    d.writeInt(0);
    

    d.writeInt(0);
    

    d.writeInt(0);
    

    d.writeInt(0);
    

    d.writeInt(0);
    

    d.writeUInt(tracks.size() + 1);
    



    int i = 0; for (int n = tracks.size(); i < n; i++) {
      AbstractQuickTimeStream.Track t = (AbstractQuickTimeStream.Track)tracks.get(i);
      
      t.writeTrackAtoms(i, moovAtom, modificationTime);
    }
    
    moovAtom.finish();
  }
  







  public void toWebOptimizedMovie(File outputFile, boolean compressHeader)
    throws IOException
  {
    finish();
    long originalMdatOffset = mdatAtom.getOffset();
    AbstractQuickTimeStream.CompositeAtom originalMoovAtom = moovAtom;
    mdatOffset = 0L;
    
    ImageOutputStream originalOut = out;
    try {
      out = null;
      
      if (compressHeader) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int maxIteration = 5;
        long compressionHeadersSize = 48L;
        long headerSize = 0L;
        long freeSize = 0L;
        for (;;) {
          mdatOffset = (compressionHeadersSize + headerSize + freeSize);
          buf.reset();
          DeflaterOutputStream deflater = new DeflaterOutputStream(buf);
          out = new MemoryCacheImageOutputStream(deflater);
          writeEpilog();
          out.close();
          deflater.close();
          
          if (buf.size() <= headerSize + freeSize) break; maxIteration--; if (maxIteration <= 0) break;
          if (headerSize != 0L) {
            freeSize = Math.max(freeSize, buf.size() - headerSize - freeSize);
          }
          headerSize = buf.size();
        }
        freeSize = headerSize + freeSize - buf.size();
        headerSize = buf.size();
        



        if ((maxIteration < 0) || (buf.size() == 0)) {
          compressHeader = false;
          System.err.println("WARNING QuickTimeWriter failed to compress header.");
        } else {
          out = new FileImageOutputStream(outputFile);
          writeProlog();
          


          DataAtomOutputStream daos = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
          daos.writeUInt(headerSize + 40L);
          daos.writeType("moov");
          
          daos.writeUInt(headerSize + 32L);
          daos.writeType("cmov");
          
          daos.writeUInt(12L);
          daos.writeType("dcom");
          daos.writeType("zlib");
          
          daos.writeUInt(headerSize + 12L);
          daos.writeType("cmvd");
          daos.writeUInt(originalMoovAtom.size());
          
          daos.write(buf.toByteArray());
          

          daos.writeUInt(freeSize + 8L);
          daos.writeType("free");
          for (int i = 0; i < freeSize; i++) {
            daos.write(0);
          }
        }
      }
      
      if (!compressHeader) {
        out = new FileImageOutputStream(outputFile);
        mdatOffset = moovAtom.size();
        writeProlog();
        writeEpilog();
      }
      

      byte[] buf = new byte['က'];
      originalOut.seek(originalMdatOffset);
      long count = 0L; for (long n = mdatAtom.size(); count < n;) {
        int read = originalOut.read(buf, 0, (int)Math.min(buf.length, n - count));
        out.write(buf, 0, read);
        count += read;
      }
      out.close();
    } finally {
      mdatOffset = 0L;
      moovAtom = originalMoovAtom;
      out = originalOut;
    }
  }
}
