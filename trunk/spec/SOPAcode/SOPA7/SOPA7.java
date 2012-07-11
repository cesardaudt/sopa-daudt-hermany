///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 20/06/2002
//
// SOPA7 - disk load and All hardware components for 2004's edition
//
///////////////////////////////////////////////////////////////////

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SOPA7
  {
  public static void main(String args[])
    {
    // The program models a complete computer with most HW components
    // The kernel, which is the software component, might have been
    // created here also, but Processor has a refernce to it and it
    // has a reference to the processor, so I decided that all software
    // is under the processor environment: kernel inside processor.
    IntController i	= new IntController();
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
    Memory m	= new Memory(1024);
    Timer t	= new Timer(i);
    Disco d	= new Disco(i,m,1024);
    Processor p	= new Processor(i,m,c,t,d);
    // start all threads
    p.start();
    t.start();
    d.start();
    }
  }

class MyWin extends JFrame
  {
  private ConsoleListener ml;
  private JTextField line;
  public MyWin()
    {
    super("Console");
    Container c = getContentPane();
    c.setLayout(new FlowLayout());
    line = new JTextField(60);
    line.setEditable(true);
    c.add(line);
    ml = new ConsoleListener();
    line.addActionListener(ml);
    setSize(800,80);
    show();
    }
  public ConsoleListener getListener() { return ml; }
  }


class ConsoleListener implements ActionListener
  {
  // Console is an intelligent terminal that reads an entire command
  // line and then generates an interrupt. It should provide a method
  // for the kernel to read the command line.

  private IntController hint;
  private String l;
  public void setInterruptController(IntController i)
    {
    hint = i;
    }
  public void actionPerformed(ActionEvent e)
    {
    l = e.getActionCommand();
    // Here goes the code that generates an interrupt    
    hint.set(15);
    }
  public String getLine()
    {
    return l;
    } 
  }

class Semaphore 
  {
  // This class was found on the Internet and had some bugs fixed.
  // It implements a semaphore using the Java built in monitors.
  // Note how the primitives wait and notify are used inside the
  // monitor, and make the process executing on it leave the
  // monitor until another event happens.
  int value;
  public  Semaphore(int initialValue)
    {
    value = initialValue;
    }
  public synchronized void P() 
    {
    while (value <= 0 ) 
      {
      try { wait(); }
      catch(InterruptedException e){}
      }
    value--;
    }
  public synchronized void V() 
    {
    value++;
    notify();
    }
  }

class IntController
  {
  // The interrupt controler component has a private semaphore to maintain 
  // interrupt requests coming from all other components. 

  private Semaphore semhi;
  private int number;
  public IntController()		{ semhi = new Semaphore(1);    }
  public void set(int n)		{ semhi.P(); number = n;    }
  public int get()				{ return number;   }
  public void reset()			{ number = 0; semhi.V(); }
  }

class Memory
  {
  // This is the memory system component. We should include here the
  // MMU (memory management unit) with its registers and access methods.
  private int[] word;
  private int size;
  public Memory(int s)
    {
    // remember size and create memory
    size = s;
    word = new int[s];
    // Initialize with a program
    init(0,'X','M',0,10);
    init(1,'X','D',0,10);
    init(2,'S','M',0,10);
    init(3,'J','P','A',0);
    init(30,'L','D',0,10);
    init(31,'L','D',0,10);
    init(32,'I','N','T',36);
    init(33,'J','P','A',30);
    }
  public synchronized void init(int add, int a, int b, int c, int d)
    {
    word[add] = (a << 24)+(b<<16)+(c<<8)+d;
    }
  public synchronized int read(int address)
    {
    return word[address];
    }
  public synchronized void write(int address, int data)
    {
    word[address] = data;
    }
  }

class Timer extends Thread
  {
  // Our programable timer. Need to implement its programability.
  // I guess it is one-time programable only. Resetting it once
  // running might be difficult to implement.
  private IntController hint;
  public Timer(IntController i)
    {
    hint = i;
    }
  public void run()
    {
    while (true)
      {
      try 
        {
        sleep(500); // half second
        // Here goes the code that generates an interrupt
        hint.set(2);
        }
      catch (InterruptedException e){}
      System.err.println("timer");
      }
    }
  }

class Disco extends Thread
  {
  // Our disc component has a semaphore to implemente its dependency on
  // a call from the processor. The semaphore is private, and we offer 
  // a method roda that unlocks it. Does it need to be synchronized???
  // It needs a semaphore to avoid busy waiting...
  private IntController hint;
  private Memory mem;
  private Semaphore sem;
  private int[] data;
  private int size;
  public Disco(IntController i,Memory m, int s)
    {
    hint = i;
    mem = m;
    sem = new Semaphore(0);
    // remember size and create disk memory
    size = s;
    data = new int[s];
    }
  public void run()
    {
    try {load("disk.txt");} catch (IOException e){}
    while (true)
      {
      // wait for some request comming from the processor
      sem.P();
      // Processor requested: now I have something to do...
      for (int i=0; i < 20; ++i)
        {
        // sleep just 50 ms which is one disc turn here
        try {sleep(50);} catch (InterruptedException e){}
        System.err.println("disk made a turn");
        }
      // Here goes the code that generates an interrupt
      hint.set(5);
      }
    }
  public void roda()
    {
    sem.V();
    }

  public void load(String filename) throws IOException
    {
    FileReader f = new FileReader(filename);
    StreamTokenizer tokemon = new StreamTokenizer(f);
    int bytes[] = new int[4];
    int tok = tokemon.nextToken();
    for (int i=0; tok != StreamTokenizer.TT_EOF && i<size; ++i)
      {
      for (int j=0; tok != StreamTokenizer.TT_EOF && j<4; ++j)
        {
        if (tokemon.ttype == StreamTokenizer.TT_NUMBER )
          bytes[j] = (int) tokemon.nval;
        else
        if (tokemon.ttype == StreamTokenizer.TT_WORD )
          bytes[j] = (int) tokemon.sval.charAt(0); 
        else
          System.out.println("Unexpected token at disk image!"); 
        tok = tokemon.nextToken();      
        }
      data[i] = ((bytes[0]&255)<<24) | ((bytes[1]&255)<<16) | 
		((bytes[2]&255)<<8) | (bytes[3]&255);
      System.out.print("Parsed "+bytes[0]+" "+bytes[1]+" "+bytes[2]+" "+bytes[3]);
      System.out.println(" = "+data[i]);
      }
    }
  }

class Processor extends Thread
  {
  // Access to hardware components
  private IntController hint;
  private Memory mem;
  private ConsoleListener con;
  private Timer tim;
  private Disco dis;
  // CPU internal components
  private int PC;			// Program Counter
  private int[] IR;			// Instruction Register
  private int[] reg;
  // Access methods
  public int getPC() { return PC; }
  public void setPC(int i) { PC = i; }
  public int[] getReg() { return reg; }
  public void setReg(int[] r) { reg = r; }
  // Kernel is like a software in ROM
  private Kernel kernel;
  public Processor(IntController i, Memory m, ConsoleListener c, Timer t, Disco d)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    PC = 0;
    IR = new int[4];
    reg = new int[16];
    kernel = new Kernel(i,m,c,t,d,this);
    }
  public void run()
    {
    while (true)
      {
      // sleep a tenth of a second
      try {sleep(100);} catch (InterruptedException e){}
      // read from memory in the address indicated by PC
      int RD = mem.read(PC);
      ++PC;
      // break the 32bit word into 4 separate bytes
      IR[0] = RD>>>24;
      IR[1] = (RD>>>16) & 255;
      IR[2] = (RD>>>8) & 255;
      IR[3] = RD & 255;
      // print CPU status to check if it´s ok
      System.err.print("processor: PC="+PC);
      System.err.print(" IR="+IR[0]+" "+IR[1]+" "+IR[2]+" "+IR[3]);
      if (IR[0]=='I'&&IR[1]=='N'&&IR[2]=='T')
        {
        System.err.println(" SW INT... ");
	    kernel.run(IR[3]);
	    }
      else
      if (IR[0]=='J'&&IR[1]=='P'&&IR[2]=='A')
        {
        System.err.println(" JUMPING... ");
	    PC = IR[3];
	    }
      else
        System.err.println(" ??? ");
      // advance PC to next instruction

      // Check for Hardware Interrupt and if so call the kernel
      if (hint.get() != 0)
    {
    // Call the kernel passing the interrupt number
    kernel.run(hint.get());
    // This goes here because only HW interrupts are reset in the controller
    // But reseting the interrupt controller might be a task for the Kernel 
    // in a real system.
    hint.reset();
    }
      }
    }
  }

class Kernel
  {
  // Access to hardware components, including the processor
  private IntController hint;
  private Memory mem;
  private ConsoleListener con;
  private Timer tim;
  private Disco dis;
  private Processor pro;
  // Data used by the kernel
  private ProcessList readyList;
  private ProcessList diskList;
  private int running = 0;
  // In the constructor goes initialization code
  public Kernel(IntController i, Memory m, ConsoleListener c, Timer t, Disco d, Processor p)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    pro = p;
    readyList = new ProcessList ();
    diskList = new ProcessList ();
    ProcessDescriptor aux = new ProcessDescriptor(456) ;
    readyList.pushBack( aux );
    aux.setPC(0);
    readyList.getBack().setPC(0);
    readyList.pushBack( new ProcessDescriptor(457) );
    readyList.getBack().setPC(30);
    }
  // Each time the kernel runs it have access to all hardware components
  public void run(int interruptNumber)
    {
    ProcessDescriptor aux = null;
    // This is the entry point: must check what happened
    System.err.println("Kernel called for int "+interruptNumber);
    // save context
    readyList.getFront().setPC(pro.getPC());
    readyList.getFront().setReg(pro.getReg());
    switch(interruptNumber)
      {
    case 2:
        aux = readyList.popFront();
        readyList.pushBack(aux);
        System.err.println("CPU runs: "+readyList.getFront().getPID());
	break;
    case 5:
	aux = diskList.popFront();
        readyList.pushBack(aux);
        break;
    case 15: // console
        System.err.println("Operator typed " + con.getLine());
	break;
    case 36:
	aux = readyList.popFront();
        diskList.pushBack(aux);
        dis.roda();
        break;
    default:
	System.err.println("Unknown...");
      }
    // restore context
    pro.setPC(readyList.getFront().getPC());
    pro.setReg(readyList.getFront().getReg());
    }
  }

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

class ProcessList
  {
  private ProcessDescriptor first = null;
  private ProcessDescriptor last = null;
  public ProcessDescriptor getFront() { return first; }
  public ProcessDescriptor getBack() { return last; }
  public ProcessDescriptor popFront() 
    { 
    ProcessDescriptor n;
    if(first!=null)
      {
      n = first;
      first=first.getNext();
      if (last == n)
        last = null;
      n.setNext(null);
      return n;
      }
   return null;
    }
  public void pushBack(ProcessDescriptor n) 
    { 
    n.setNext(null);
    if (last!=null)
      last.setNext(n);
    else
      first = n; 
    last = n;
    }
  }
