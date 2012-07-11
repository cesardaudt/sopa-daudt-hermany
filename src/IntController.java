import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

class IntController
  {
  // The interrupt controler component has a private semaphore to maintain 
  // interrupt requests coming from all other components. 
  // Interruptions from memory are exceptions that need to be handled right
  // now, and have priority over other ints. So, memory interrupt has its
  // own indicator, and the others compete among them using the Semaphore.

  private Semaphore semhi;
  private int number[] = new int[2];
  private final int memoryInterruptNumber = 3;
  public IntController()		
    {
    semhi = new Semaphore(1);
    }
		
  public void set(int n)
    { 
    if (n == memoryInterruptNumber)
      number[0] = n;
    else
      {
      semhi.P();
      number[1] = n;
      }
    }
		
  public int get()
    { 
    if (number[0]>0)
      return number[0];
    else
      return number[1];
    }
		
  public void reset(int n)
    {
    if (n == memoryInterruptNumber)
      number[0] = 0;
    else
      {
      number[1] = 0;
      semhi.V();
      }
    }
  }
