import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class SOPA
  {
  public static void main(String args[])
    {
    // The program models a complete computer with most HW components
    // The kernel, which is the software component, might have been
    // created here also, but Processor has a refernce to it and it
    // has a reference to the processor, so I decided that all software
    // is under the processor environment: kernel inside processor.
    
    GlobalSynch gs = new GlobalSynch(50);  // quantum of 50ms
    IntController i = new IntController();
    // Create interface
    SopaInterface.initViewer(gs);
    // Create window console
    MyWin mw = new MyWin();
    mw.addWindowListener
      (
      new WindowAdapter()
        {
        public void windowClosing (WindowEvent e)
          { System.exit(0); }
        }
      );
    ConsoleListener c = mw.getListener();
    c.setInterruptController(i);
    c.setGlobalSynch(gs);
    Memory m	= new Memory(i,1024);
    Timer t	= new Timer(i,gs);
    Disk d	= new Disk(i,gs,m,1024,"disk.txt");
    Processor p	= new Processor(i,gs,m,c,t,d);
    // start all threads
    p.start();
    t.start();
    d.start();
    gs.start();
    }
  }
