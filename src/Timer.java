import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

class Timer extends Thread
  {
  // Our programable timer. This is the OLD version that used to make
	// interrupts to inform about the end of a CPU slice. It's supposed to be
	// programable. But has some weaknesses (bugs) that make it not fare.
	// IN 2006-1, you are asked to use simple versions that just place
	// an interrupt at each time interval and the kernel itself must 
	// count these timer ticks and test for a the time slice end.
  private IntController hint;
  private GlobalSynch synch;
  private int counter = 0;
  private int slice = 5;
  public Timer(IntController i, GlobalSynch gs)
    {
    hint = i;
    synch = gs;
    }
  // For the services below, time is expressed in tenths of seconds
  public void setSlice(int t) { slice = t; } 
  public void setTime(int t) { counter = t; } 
  public int getTime() { return counter; }
  // This is the thread that keeps track of time and generates the
  // interrupt when a slice has ended, but can be reset any time
  // with any "time-to-alarm"
  public void run()
    {
    while (true)
      {
      counter = slice;
      while (counter > 0)
	{
        synch.mysleep(2);
	--counter;
	System.err.println("tick "+counter);
	}
      System.err.println("timer INT");
      hint.set(2);
      }
    }
  }

