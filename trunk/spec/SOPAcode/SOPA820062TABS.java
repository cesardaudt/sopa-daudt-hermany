///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 20/06/2002
//
// SOPA820062TABS - All hardware components for the 2006-2 edition
//
// This versions uses TABS to organize the output with a simple code.  
//
// Many simplifications were made and the messages were ajusted.
//
// The timer is still an old version that needs to be simplified.
//
///////////////////////////////////////////////////////////////////

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class SOPA820062TABS
  {
  public static void main(String args[])
    {
    // The program models a complete computer with most HW components
    // The kernel, which is the software component, might have been
    // created here also, but Processor has a refernce to it and it
    // has a reference to the processor, so I decided that all software
    // is under the processor environment: kernel inside processor.
    
    IntController i = new IntController();
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
    Memory m	= new Memory(i,1024);
    Timer t	= new Timer(i);
    Disk d	= new Disk(i,m,1024,"disk.txt");
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
    line = new JTextField(30);
    line.setEditable(true);
    c.add(line);
    ml = new ConsoleListener();
    line.addActionListener(ml);
    setSize(400,80);
    setVisible(true);
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
		// store text and call an interruption
    l = e.getActionCommand();
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
  // Interruptions from memory are exceptions that need to be handled right
  // now, and have priority over other ints. So, memory interrupt has its
  // own indicator, and the others compete among them using the Semaphore.

  private Semaphore semhi;
  private int number[] = new int[2];
  private final int memoryInterruptNumber = 3;
	private String tabs = "\t\t\t";
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
		System.err.println(tabs+"INT "+n);
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

class Memory
  {
  // This is the memory system component. 
  private IntController hint;
  private int[] memoryWord;
  private int memorySize;
  // MMU: base and limit registers
  private int limitRegister;            // specified in logical addresses
  private int baseRegister;             // add base to get physical address
  // constructor
  public Memory(IntController i,int s)
    {
    // remember size and create memory
    hint = i;
    memorySize = s;
    memoryWord = new int[s];
    // Initialize with the dummy program
    init(0,'J','P','A',0);
    }
  // Access methods for the MMU: these are accessed by the kernel.
  // They do not check memory limits. It is interpreted as kernel's
  // fault to set these registers with values out of range. The result
  // is that the JVM will terminate with an 'out of range' exception
  // if a process uses a memory space that the kernel set wrongly.
  // This is correct: the memory interruption must be set in our
  // simulated machine only if the kernel was right and the process itself
  // tries to access an address which is out of its logical space.
  public void setLimitRegister(int val) { limitRegister = val; };
  public void setBaseRegister(int val) { baseRegister = val; };
  // Here goes some specifi methods for the kernel to access memory
  // bypassing the MMU (do not add base register or test limits)
  public int superRead(int address) { return memoryWord[address]; }
  public void superWrite(int address, int data) { memoryWord[address] = data; }
  // Access methods for the Memory itself
  public synchronized void init(int add, int a, int b, int c, int d)
    {
    memoryWord[add] = (a << 24)+(b<<16)+(c<<8)+d;
    }
  public synchronized int read(int address)
    {
    if (address >= limitRegister)
      {
      hint.set(3);
      return 0;
      }
    else
      return memoryWord[baseRegister + address];
    }
  public synchronized void write(int address, int data)
    {
    if (address >= limitRegister)
      hint.set(3);
    else
      memoryWord[baseRegister + address] = data;
    }
  }

class Timer extends Thread
  {
  // Our programable timer. This is the OLD version that used to make
	// interrupts to inform about the end of a CPU slice. It's supposed to be
	// programable. But has some weaknesses (bugs) that make it not fare.
	// IN 2006-2, you are asked to use simple versions that just place
	// an interrupt at each time interval and the kernel itself must 
	// count these timer ticks and test for a the time slice end.
	// Advice: Make the timer slower to avoid too many kernel calls!
  private IntController hint;
	  private int counter = 0;
  private int slice = 5;
  public Timer(IntController i)
    {
    hint = i;
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
        try { sleep(150); }
        catch (InterruptedException e){}
				--counter;
				System.err.println("tick "+counter);
				}
      System.err.println("timer INT");
      hint.set(2);
      }
    }
  }

class Disk extends Thread
  {
  // Our disc component has a semaphore to implemente its dependency on
  // a call from the processor. The semaphore is private, and we offer 
  // a method roda that unlocks it. Does it need to be synchronized???
  // It needs a semaphore to avoid busy waiting, but...
  private IntController hint;
  private Memory mem;
  private Semaphore sem;
  private String fileName;
  private int[] diskImage;
  private int diskSize;
  // Here go the disk interface registers
  private int address;
  private int writeData;
  private int[] readData;
  private int readSize;
  private int operation;
  private int errorCode;
  // and some codes to get the meaning of the intterface
  // you can use the codes inside the kernel, like: dis.OPERATION_READ
  public final int OPERATION_READ = 0;
  public final int OPERATION_WRITE = 1;
  public final int OPERATION_LOAD = 2;
  public final int ERRORCODE_SUCCESS = 0;
  public final int ERRORCODE_SOMETHING_WRONG = 1;
  public final int ERRORCODE_ADDRESS_OUT_OF_RANGE = 2;
  public final int ERRORCODE_MISSING_EOF = 3;
  public final int BUFFER_SIZE = 128;
  public final int END_OF_FILE = 0xFFFFFFFF;
	private String tabs = "\t\t\t\t";
  // Constructor
  public Disk(IntController i, Memory m, int s, String name)
    {
    hint = i;
    mem = m;
    sem = new Semaphore(0);
    // remember size and create disk memory
    diskSize = s;
    fileName = name;
    diskImage = new int[s];
    readData = new int[BUFFER_SIZE];
    readSize = 0;
    }
		
  // Methods that the kernel (in CPU) should call: "roda" activates the disk
  // The last parameter, data, is only for the 'write' operation
  public void roda(int op, int add, int data)
    {
    address = add;
    writeData = data;
    readSize = 0;
    operation = op;
    errorCode = ERRORCODE_SUCCESS;
    sem.V();
    }
  // After disk traps an interruption, kernel retrieve its results
  public int getError() { return errorCode; }
  public int getSize() { return readSize; }
  public int getData(int buffer_position) { return readData[buffer_position]; }
		
  // The thread that is the disk itself
  public void run()
    {
    try { load(fileName); } catch (IOException e){}
    while (true)
      {
      // wait for some request comming from the processor
      sem.P();
      // Processor requested: now I have something to do...
      for (int i=0; i < 20; ++i)
        {
        // sleep just one quantum which is one disc turn here
        try { sleep(50); }
        catch (InterruptedException e){}
        System.err.println(tabs+"turn");
        }
      // After so many turns the disk should do its task!!!
			
      if (address < 0 || address >= diskSize)
        errorCode = ERRORCODE_ADDRESS_OUT_OF_RANGE;
      else
        {
        errorCode = ERRORCODE_SUCCESS;
				
        switch(operation)
          {
        case OPERATION_READ:
          System.err.println(tabs+"READ");
          readSize = 1;
          readData[0] = diskImage[address];
          break;
        case OPERATION_WRITE:
          System.err.println(tabs+"WRITE");
          diskImage[address] = writeData;
          break;
        case OPERATION_LOAD:
          System.err.println(tabs+"LOAD");
          int diskIndex = address;
          int bufferIndex = 0;
					System.err.print(tabs);
          while (diskImage[diskIndex] != END_OF_FILE)
            {
            System.err.print(".");
            readData[bufferIndex] = diskImage[diskIndex];
            ++diskIndex;
            ++bufferIndex;
            if (bufferIndex >= BUFFER_SIZE || diskIndex >= diskSize)
              {
              errorCode = ERRORCODE_MISSING_EOF;
              break;
              }
            }
					System.err.println(" ");
          readSize = bufferIndex;
          break;
          }
        }		
      // Here goes the code that generates an interrupt
      hint.set(5);
      }
    }
		
  // this is to read disk initial image from a hosted text file
  private void load(String filename) throws IOException
    {
    FileReader f = new FileReader(filename);
    StreamTokenizer tokemon = new StreamTokenizer(f);
    int bytes[] = new int[4];
    int tok = tokemon.nextToken();
    for (int i=0; tok != StreamTokenizer.TT_EOF && (i < diskSize); ++i)
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
      diskImage[i] = ((bytes[0]&255)<<24) | ((bytes[1]&255)<<16) | 
                     ((bytes[2]&255)<<8) | (bytes[3]&255);
      System.out.println("Parsed "+bytes[0]+" "+bytes[1]+" "
                                +bytes[2]+" "+bytes[3]+" = "+diskImage[i]);
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
  private Disk dis;
  // CPU internal components
  private int PC;	// Program Counter
  private int[] IR;	// Instruction Register
  private int[] reg;	// general purpose registers
  private int[] flag;   // flags Z E L
  private final int Z = 0;
  private final int E = 1;
  private final int L = 2;
  // Access methods
  public int getPC() { return PC; }
  public void setPC(int i) { PC = i; }
  public int[] getReg() { return reg; }
  public void setReg(int[] r) { reg = r; }
  public int[] getFlag() { return flag; }
  public void setFlag(int[] f) { flag = f; }
  // Kernel is like a software in ROM
  private Kernel kernel;
	private String tabs = "\t"; 
  public Processor(IntController i, Memory m, ConsoleListener c, 
                  Timer t, Disk d)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    PC = 0;
    IR = new int[4];
    reg = new int[16];
    flag = new int[3];
    kernel = new Kernel(i,m,c,t,d,this);
    }
  public void run()
    {
    while (true)
      {
      // sleep a tenth of a second
        try { sleep(100); }
        catch (InterruptedException e){}
      // read from memory in the address indicated by PC
      int RD = mem.read(PC++);
      // break the 32bit word into 4 separate bytes
      IR[0] = RD>>>24;
      IR[1] = (RD>>>16) & 255;
      IR[2] = (RD>>>8) & 255;
      IR[3] = RD & 255;
      // print CPU status to check if it is ok
      System.err.print(tabs+"PC="+PC+" ");
			
      // Execute basic instructions of the architecture
			
      execute_basic_instructions();
			
      // Check for Hardware Interrupt and if so call the kernel
      int thisInt = hint.get();
      if ( thisInt != 0)
        {
        // Call the kernel passing the interrupt number
        kernel.run(thisInt);
        // Kernel handled the last interrupt
        hint.reset(thisInt);
        }
      }
    }
		
  public void execute_basic_instructions()
    {
    if ((IR[0]=='L') && (IR[1]=='M'))
      {
      System.err.println("L M "+IR[2]+" "+IR[3]);
      reg[IR[2]] = mem.read(IR[3]);
      }
    else
    if ((IR[0]=='L') && (IR[1]=='C'))
      {
      System.err.println("L C "+IR[2]+" "+IR[3]);
      reg[IR[2]] = IR[3];
      }
    else
    if ((IR[0]=='W') && (IR[1]=='M'))
      {
      System.err.println("W M "+IR[2]+" "+IR[3]);
      mem.write(IR[3],reg[IR[2]]);
      }
    else
    if ((IR[0]=='S') && (IR[1]=='U'))
      {
      System.err.println("S U "+IR[2]+" "+IR[3]);
      reg[IR[2]] = reg[IR[2]] - reg[IR[3]];
      }
    else
    if ((IR[0]=='A') && (IR[1]=='D'))
      {
      System.err.println("A D "+IR[2]+" "+IR[3]);
      reg[IR[2]] = reg[IR[2]] + reg[IR[3]];
      }
    else
    if ((IR[0]=='D') && (IR[1]=='E') && (IR[2]=='C'))
      {
      System.err.println("D E C "+IR[2]);
      reg[IR[3]] = reg[IR[3]] - 1;
      }
    else
    if ((IR[0]=='I') && (IR[1]=='N') && (IR[2]=='C'))
      {
      System.err.println("I N C "+IR[2]);
      reg[IR[3]] = reg[IR[3]] + 1;
      }
    else
    if ((IR[0]=='C') && (IR[1]=='P'))
      {
      System.err.println("C P "+IR[2]+" "+IR[3]);
      if (reg[IR[2]] == 0) flag[Z] = 1; else flag[Z] = 0;
      if (reg[IR[2]] == reg[IR[3]]) flag[E] = 1; else flag[E] = 0;
      if (reg[IR[2]] < reg[IR[3]]) flag[L] = 1; else flag[L] = 0;
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='A'))
      {
      System.err.println("J P A "+IR[3]);
      PC = IR[3];
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='Z'))
      {
      System.err.println("J P Z "+IR[3]);
      if (flag[Z] == 1)
        PC = IR[3];
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='E'))
      {
      System.err.println("J P E "+IR[3]);
      if (flag[E] == 1)
        PC = IR[3];
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='L'))
      {
      System.err.println("J P L "+IR[3]);
      if (flag[L] == 1)
        PC = IR[3];
      }
    else
    if (IR[0]=='I'&&IR[1]=='N'&&IR[2]=='T')
      {
      System.err.println("I N T "+IR[3]);
      kernel.run(IR[3]);
      }
    else
      System.err.println("? ? ? ? ");
    }
  }

class Kernel
  {
  // Access to hardware components, including the processor
  private IntController hint;
  private Memory mem;
  private ConsoleListener con;
  private Timer tim;
  private Disk dis;
  private Processor pro;
  // Data used by the kernel
  private ProcessList readyList;
  private ProcessList diskList;
	private String tabs = "\t\t\t\t\t";
  // In the constructor goes initialization code
  public Kernel(IntController i, Memory m, ConsoleListener c, 
                Timer t, Disk d, Processor p)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    pro = p;
    readyList = new ProcessList ("Ready List");
    diskList = new ProcessList ("Disk List");
    // Creates the dummy process
    readyList.pushBack( new ProcessDescriptor(0) );
    readyList.getBack().setPC(0);
		}
  // Each time the kernel runs it have access to all hardware components
  public void run(int interruptNumber)
    {
    ProcessDescriptor aux = null;
    // This is the entry point: must check what happened
    System.err.println(tabs+"Kernel called for int "+interruptNumber);
    // save context
    readyList.getFront().setPC(pro.getPC());
    readyList.getFront().setReg(pro.getReg());
    switch(interruptNumber)
      {
    case 2: // HW INT timer
      aux = readyList.popFront();
      readyList.pushBack(aux);
      System.err.println(tabs+"CPU now runs: "+readyList.getFront().getPID());
      break;
    case 5: // HW INT disk 
      aux = diskList.popFront();
      readyList.pushBack(aux);
      break;
    case 15: // HW INT console
      System.err.println(tabs+"Operator typed " + con.getLine());
			break;
    case 36: // SW INT read
      aux = readyList.popFront();
      diskList.pushBack(aux);
      dis.roda(0,0,0);
      break;
    default:
      System.err.println(tabs+"Unknown...");
      }
    // restore context
    pro.setPC(readyList.getFront().getPC());
    pro.setReg(readyList.getFront().getReg());
		readyList.print();
		diskList.print();
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
	
// This list implementation (and the 'next filed' in ProcessDescriptor) was
// programmed in a class to be faster than searching Java's standard lists,
// and it matches the names of the C++ STL. It is all we need now...

class ProcessList
{
  private String myName = "No name";
	private String tabs = "\t\t\t\t\t";
  private ProcessDescriptor first = null;
  private ProcessDescriptor last = null;
  public ProcessDescriptor getFront() { return first; }
  public ProcessDescriptor getBack() { return last; }
  
  public ProcessList(String name)
    { 
	  myName=name ;
    }
  
  public void print()
    { 
		System.err.print(tabs+myName+": ");
	  ProcessDescriptor n;
		for (n = first; n!= null; n = n.getNext())
		  System.err.print(n.getPID()+" ");
		System.err.println("");
    }
		
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

