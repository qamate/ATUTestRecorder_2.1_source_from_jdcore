package atu.testrecorder;

import atu.testrecorder.exceptions.ATUTestRecorderException;
import atu.testrecorder.media.MovieWriter;
import atu.testrecorder.media.avi.AVIWriter;
import atu.testrecorder.media.color.Colors;
import atu.testrecorder.media.image.Images;
import atu.testrecorder.media.quicktime.QuickTimeWriter;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;














public class ATUTestRecorder
{
  private CursorEnum cursor;
  private String format;
  
  private static enum CursorEnum
  {
    BLACK,  WHITE,  NONE;
  }
  




  private int depth = 24;
  
  private MovieWriter w;
  
  private long startTime;
  
  private long time;
  
  private float screenRate = 15.0F;
  
  private float mouseRate = 30.0F;
  
  private int aviSyncInterval = (int)(Math.max(screenRate, mouseRate) * 60.0F);
  




  private int qtSyncInterval = (int)Math.max(screenRate, mouseRate);
  



  private long maxFrameDuration = 1000L;
  

  private Robot robot;
  

  private Rectangle rect;
  

  private BufferedImage screenCapture;
  
  private List<MouseCapture> mouseCaptures;
  
  private BufferedImage videoImg;
  
  private Graphics2D videoGraphics;
  
  private ScheduledThreadPoolExecutor screenTimer;
  
  private ScheduledThreadPoolExecutor mouseTimer;
  
  private BufferedImage cursorImg;
  
  private Point cursorOffset = new Point(-8, -5);
  
  private final Object sync = new Object();
  
  private float audioRate;
  
  private Thread audioRunner;
  
  private AudioFormat audioFormat;
  private String recordingName = "";
  private String recordingRootDirectiry = "";
  





  public ATUTestRecorder(Boolean isAudioRecordingEnabled)
    throws ATUTestRecorderException
  {
    try
    {
      recorder(recordingRootDirectiry, recordingName, 
        isAudioRecordingEnabled);
    } catch (AWTException e) {
      throw new ATUTestRecorderException(e.getMessage());
    } catch (IOException e) {
      throw new ATUTestRecorderException(e.getMessage());
    }
  }
  








  public ATUTestRecorder(String recordingName, Boolean isAudioRecordingEnabled)
    throws ATUTestRecorderException
  {
    this.recordingName = recordingName;
    recordingRootDirectiry = "";
    try {
      recorder(recordingRootDirectiry, recordingName, 
        isAudioRecordingEnabled);
    } catch (AWTException e) {
      throw new ATUTestRecorderException(e.getMessage());
    } catch (IOException e) {
      throw new ATUTestRecorderException(e.getMessage());
    }
  }
  










  public ATUTestRecorder(String recordingRootDirectiry, String recordingName, Boolean isAudioRecordingEnabled)
    throws ATUTestRecorderException
  {
    this.recordingName = recordingName;
    this.recordingRootDirectiry = recordingRootDirectiry;
    try {
      recorder(recordingRootDirectiry, recordingName, 
        isAudioRecordingEnabled);
    } catch (AWTException e) {
      throw new ATUTestRecorderException(e.getMessage());
    } catch (IOException e) {
      throw new ATUTestRecorderException(e.getMessage());
    }
  }
  
  private void recorder(String recordingRootDirectiry, String recordingName, Boolean isAudioRecordingEnabled)
    throws IOException, AWTException
  {
    Window window = new Window(null);
    GraphicsConfiguration cfg = window.getGraphicsConfiguration();
    





    format = "QuickTime";
    depth = 24;
    cursor = CursorEnum.WHITE;
    screenRate = 15.0F;
    mouseRate = 30.0F;
    if (isAudioRecordingEnabled.booleanValue()) {
      audioRate = 44100.0F;
    } else {
      audioRate = 0.0F;
    }
    
    aviSyncInterval = ((int)(Math.max(screenRate, mouseRate) * 60.0F));
    




    qtSyncInterval = ((int)Math.max(screenRate, mouseRate));
    
    rect = cfg.getBounds();
    
    robot = new Robot(cfg.getDevice());
    
    if (depth == 24) {
      videoImg = new BufferedImage(rect.width, rect.height, 
        1);
    } else if (depth == 16) {
      videoImg = new BufferedImage(rect.width, rect.height, 
        9);
    } else if (depth == 8) {
      videoImg = new BufferedImage(rect.width, rect.height, 
        13, Colors.createMacColors());
    }
    else {
      throw new IOException("Unsupported color depth " + depth);
    }
    
    videoGraphics = videoImg.createGraphics();
    videoGraphics.setRenderingHint(RenderingHints.KEY_DITHERING, 
      RenderingHints.VALUE_DITHER_DISABLE);
    videoGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, 
      RenderingHints.VALUE_COLOR_RENDER_SPEED);
    videoGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, 
      RenderingHints.VALUE_RENDER_SPEED);
    mouseCaptures = 
      Collections.synchronizedList(new LinkedList());
    if (cursor == CursorEnum.BLACK) {
      cursorImg = Images.toBufferedImage(Images.createImage(
        ATUTestRecorder.class, 
        "/atu/testrecorder/media/images/Cursor.black.png"));
    } else {
      cursorImg = Images.toBufferedImage(Images.createImage(
        ATUTestRecorder.class, 
        "/atu/testrecorder/media/images/Cursor.white.png"));
    }
    
    createMovieWriter(recordingRootDirectiry, recordingName);
  }
  










  protected void createMovieWriter(String recordingRootDirectiry, String recordingName)
    throws IOException
  {
    if (recordingRootDirectiry == null) {
      recordingRootDirectiry = "";
    }
    File recordingRootDir = new File(recordingRootDirectiry);
    
    if (recordingName == null)
      recordingName = "";
    File folder;
    File folder;
    if (recordingRootDirectiry.trim() == "") {
      folder = new File("." + File.separator);
    }
    else {
      if (!recordingRootDir.exists())
        throw new IOException("Directory \"" + recordingRootDir + 
          "\" does not exist.");
      if (!recordingRootDir.isDirectory()) {
        throw new IOException("\"" + recordingRootDir + 
          "\" is not a directory.");
      }
      
      folder = new File(recordingRootDirectiry + File.separator);
    }
    
    if (!folder.exists()) {
      folder.mkdirs();
    } else if (!folder.isDirectory()) {
      throw new IOException("\"" + folder + "\" is not a directory.");
    }
    
    SimpleDateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd 'at' HH.mm.ss");
    if (format.equals("AVI"))
    {
      AVIWriter aviw;
      


      w = 
        (aviw = new AVIWriter(new File(folder, 
        "ScreenRecording " + dateFormat.format(new Date()) + 
        ".avi")));
      aviw.addVideoTrack(AVIWriter.VIDEO_SCREEN_CAPTURE, 1L, 
        (int)mouseRate, rect.width, rect.height, depth, 
        aviSyncInterval);
      if (depth == 8) {
        aviw.setPalette(0, (IndexColorModel)videoImg.getColorModel());
      }
    } else if (format.equals("QuickTime"))
    {
      QuickTimeWriter qtw;
      

      w = 
        (qtw = new QuickTimeWriter(new File(folder, recordingName + ".mov")));
      qtw.addVideoTrack(QuickTimeWriter.VIDEO_ANIMATION, 1000L, 
        rect.width, rect.height, depth, qtSyncInterval);
      if (audioRate > 0.0F) {
        audioFormat = new AudioFormat(audioRate, 16, 1, true, true);
        qtw.addAudioTrack(audioFormat);
      }
      if (depth == 8) {
        qtw.setVideoColorTable(0, 
          (IndexColorModel)videoImg.getColorModel());
      }
    } else {
      throw new IOException("Unsupported format " + format);
    }
  }
  




  public void start()
    throws ATUTestRecorderException
  {
    startTime = (this.time = System.currentTimeMillis());
    screenTimer = new ScheduledThreadPoolExecutor(1);
    screenTimer.scheduleAtFixedRate(new Runnable()
    {
      public void run()
      {
        try {
          ATUTestRecorder.this.grabScreen();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }, (int)(1000.0F / screenRate), (int)(1000.0F / screenRate), 
      TimeUnit.MILLISECONDS);
    mouseTimer = new ScheduledThreadPoolExecutor(1);
    mouseTimer.scheduleAtFixedRate(new Runnable()
    {
      public void run()
      {
        ATUTestRecorder.this.grabMouse();
      }
    }, (int)(1000.0F / mouseRate), (int)(1000.0F / mouseRate), 
      TimeUnit.MILLISECONDS);
    
    if ((audioRate > 0.0F) && ((w instanceof QuickTimeWriter))) {
      try {
        startAudio();
      } catch (LineUnavailableException e) {
        throw new ATUTestRecorderException(e.getMessage());
      }
    }
  }
  



  private void startAudio()
    throws LineUnavailableException
  {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, 
      audioFormat);
    

    final TargetDataLine line = (TargetDataLine)AudioSystem.getLine(info);
    line.open(audioFormat);
    line.start();
    int bufferSize;
    final int bufferSize; if (audioFormat.getFrameSize() != -1) {
      bufferSize = (int)audioFormat.getSampleRate() * 
        audioFormat.getFrameSize();
    } else {
      bufferSize = (int)audioFormat.getSampleRate();
    }
    audioRunner = new Thread()
    {
      public void run()
      {
        byte[] buffer = new byte[bufferSize];
        try
        {
          while (audioRunner == this) {
            int count = line.read(buffer, 0, buffer.length);
            if (count > 0) {
              synchronized (sync) {
                int sampleCount = count * 8 / 
                  audioFormat.getSampleSizeInBits();
                w.writeSamples(1, sampleCount, buffer, 0, 
                  count, 1L, true);
              }
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
          return;
        } finally {
          line.close();
        }
      }
    };
    audioRunner.start();
  }
  


  public void stop()
    throws ATUTestRecorderException
  {
    try
    {
      Thread.sleep(2000L);
    } catch (InterruptedException e1) {
      throw new ATUTestRecorderException(e1.getMessage());
    }
    mouseTimer.shutdown();
    screenTimer.shutdown();
    Thread T = audioRunner;
    audioRunner = null;
    try
    {
      mouseTimer.awaitTermination((int)(1000.0F / mouseRate), 
        TimeUnit.MILLISECONDS);
      screenTimer.awaitTermination((int)(1000.0F / screenRate), 
        TimeUnit.MILLISECONDS);
      
      if (T != null)
      {
        T.join();
      }
    } catch (InterruptedException e) {
      throw new ATUTestRecorderException(e.getMessage());
    }
    
    synchronized (sync) {
      try {
        w.close();
      } catch (IOException e) {
        throw new ATUTestRecorderException(e.getMessage());
      }
      w = null;
    }
    videoGraphics.dispose();
    videoImg.flush();
  }
  





  private void grabScreen()
    throws IOException
  {
    screenCapture = robot.createScreenCapture(new Rectangle(0, 0, 
      rect.width, rect.height));
    long now = System.currentTimeMillis();
    videoGraphics.drawImage(screenCapture, 0, 0, null);
    

    boolean hasMouseCapture = false;
    if (cursor != CursorEnum.NONE)
    {
      Point previous = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
      while ((!mouseCaptures.isEmpty()) && (mouseCaptures.get(0)).time < now)) {
        MouseCapture pc = (MouseCapture)mouseCaptures.remove(0);
        if (time > time) {
          hasMouseCapture = true;
          Point p = p;
          x -= rect.x;
          y -= rect.y;
          synchronized (sync) {
            if ((!w.isVFRSupported()) || (x != x) || 
              (y != y) || 
              (time - time > maxFrameDuration)) {
              x = x;
              y = y;
              

              videoGraphics.drawImage(cursorImg, x + 
                cursorOffset.x, y + cursorOffset.y, 
                null);
              if (w == null) {
                return;
              }
              try {
                w.writeFrame(0, videoImg, 
                  (int)(time - time));
              } catch (Throwable t) {
                throw new IllegalStateException("ATU TestRecorder Error");
              }
              time = time;
              
              videoGraphics.drawImage(screenCapture, 
                x + cursorOffset.x, y + cursorOffset.y, 
                x + cursorOffset.x + cursorImg.getWidth() - 
                1, y + cursorOffset.y + 
                cursorImg.getHeight() - 1, 
                x + cursorOffset.x, y + cursorOffset.y, 
                x + cursorOffset.x + cursorImg.getWidth() - 
                1, y + cursorOffset.y + 
                cursorImg.getHeight() - 1, 
                null);
            }
          }
        }
      }
    }
    
























    if (!hasMouseCapture) {
      if (cursor != CursorEnum.NONE) {
        PointerInfo info = MouseInfo.getPointerInfo();
        Point p = info.getLocation();
        videoGraphics.drawImage(cursorImg, x + cursorOffset.x, x + 
          cursorOffset.y, null);
      }
      synchronized (sync) {
        w.writeFrame(0, videoImg, (int)(now - time));
      }
      time = now;
    }
  }
  
  private void grabMouse()
  {
    long now = System.currentTimeMillis();
    PointerInfo info = MouseInfo.getPointerInfo();
    
    mouseCaptures.add(new MouseCapture(now, info.getLocation()));
  }
  
  private static class MouseCapture
  {
    public long time;
    public Point p;
    
    public MouseCapture(long time, Point p)
    {
      this.time = time;
      this.p = p;
    }
  }
}
