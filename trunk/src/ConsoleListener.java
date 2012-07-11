import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

class ConsoleListener implements ActionListener 
  {
  // Console is an intelligent terminal that reads an entire command
  // line and then generates an interrupt. It should provide a method
  // for the kernel to read the command line.

  private IntController hint;
  private GlobalSynch synch;
  private Semaphore sem;
  private SlaveListener sl;
  private String l;
  public void setInterruptController(IntController i)
    {
    hint = i;
    sem = new Semaphore(0);
    sl = new SlaveListener(i);
    sl.start();
    }
  public void setGlobalSynch(GlobalSynch gs)
    {
    synch = gs;
    }
  public void actionPerformed(ActionEvent e)
    {
    l = e.getActionCommand();
    // Here goes the code that generates an interrupt
    sl.setInterrupt();
    }
    
  public String getLine()
    {
    return l;
    } 
  }
