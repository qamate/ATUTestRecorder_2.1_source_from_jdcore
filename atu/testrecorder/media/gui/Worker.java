package atu.testrecorder.media.gui;

import javax.swing.SwingUtilities;




















public abstract class Worker<T>
  implements Runnable
{
  private T value;
  private Throwable error;
  
  public Worker() {}
  
  public final void run()
  {
    try
    {
      setValue(construct());
    } catch (Throwable e) {
      setError(e);
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          failed(getError());
          finished();
        }
      });
      return;
    }
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        done(getValue());
        finished();
      }
    });
  }
  







  protected abstract T construct()
    throws Exception;
  







  protected void done(T value) {}
  







  protected void failed(Throwable error)
  {
    error.printStackTrace();
  }
  






  protected void finished() {}
  






  public synchronized T getValue()
  {
    return value;
  }
  


  private synchronized void setValue(T x)
  {
    value = x;
  }
  



  protected synchronized Throwable getError()
  {
    return error;
  }
  


  private synchronized void setError(Throwable x)
  {
    error = x;
  }
  


  public void start()
  {
    new Thread(this).start();
  }
}
