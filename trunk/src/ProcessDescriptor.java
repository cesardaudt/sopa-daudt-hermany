import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

class ProcessDescriptor
  {
  private int PID;
  private int PC;
  private int[] reg;
  private ProcessDescriptor next;
  public int getPID() { return PID; }
  public int getPC() { return PC; }
  public void setPC(int i) { PC = i; }
  public int[] getReg() { return reg; }
  public void setReg(int[] r) { reg = r; }
  public ProcessDescriptor getNext() { return next; }
  public void setNext(ProcessDescriptor n) { next = n; }
  public ProcessDescriptor(int pid) 
    {
    PID = pid;
    PC = 0;
    reg = new int[16];
    }
  
  }
