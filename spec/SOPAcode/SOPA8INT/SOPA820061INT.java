///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 20/06/2002
//
// SOPA820061 - All hardware components for the 2006-1 edition
//
// This code was updated to include synchronization between threads
// so that we can implement step by step execution as well as an
// interface with Play, Pause and Step-by-step commands. 
//
//  Please, consider that some testing and tuning may be required.
//
///////////////////////////////////////////////////////////////////

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class SOPA820061INT
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
  
class GlobalSynch extends Thread
  {
  // This is a master clock for the simulation. Instead of running concurrent
  // threads with the normal sleep from Java, we use instead this GlobalSynch
  // sleep system that can be controlled and excecuted step by step.
  private int quantum;
  private boolean stepMode;
  private Semaphore lock;
  public GlobalSynch(int q)
    {
    quantum = q;
    stepMode = false; 
    lock = new Semaphore(1);
    }
  public synchronized void mysleep(int n)
    {
    for (int i=0; i<n; ++i)
      try { wait(); }
      catch (InterruptedException e){}
    }
  public synchronized void mywakeup()
    {
    notifyAll();
    }
  public void run()
    {
    while (true)
      {
      lock.P();
      if (stepMode == false)
        lock.V();
      try { sleep(quantum); }
      catch (InterruptedException e){}
      mywakeup();
      }
    }
  public synchronized void advance()
    {
    if (stepMode == true)
      lock.V();
    }
  public synchronized void pause()
    {
    if (stepMode == false)
      {
      stepMode = true;
      lock.P();
      }
    }
  public synchronized void play()
    {
    if (stepMode == true)
      {
      stepMode = false;
      lock.V();
      }
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
  
class SlaveListener extends Thread
  {
  private IntController hint;
  private Semaphore sem;
  public SlaveListener(IntController i)
    {
    hint = i;
    sem = new Semaphore(0);
    }
  public void setInterrupt()
    {
    sem.V();
    }
  
  public void run()
    {
    while(true)
      {
      sem.P();
      hint.set(15);
      }
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

class Disk extends Thread
  {
  // Our disc component has a semaphore to implemente its dependency on
  // a call from the processor. The semaphore is private, and we offer 
  // a method roda that unlocks it. Does it need to be synchronized???
  // It needs a semaphore to avoid busy waiting, but...
  private IntController hint;
  private GlobalSynch synch;
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
  // Constructor
  public Disk(IntController i, GlobalSynch gs, Memory m, int s, String name)
    {
    hint = i;
    synch = gs;
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
        synch.mysleep(1);
        System.err.println("disk made a turn");
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
          System.err.println("OPERATION_READ");
          readSize = 1;
          readData[0] = diskImage[address];
          break;
        case OPERATION_WRITE:
          System.err.println("OPERATION_WRITE");
          diskImage[address] = writeData;
          break;
        case OPERATION_LOAD:
          System.err.println("OPERATION_LOAD");
          int diskIndex = address;
          int bufferIndex = 0;
          while (diskImage[diskIndex] != END_OF_FILE)
            {
            System.err.println(".");
            readData[bufferIndex] = diskImage[diskIndex];
            ++diskIndex;
            ++bufferIndex;
            if (bufferIndex >= BUFFER_SIZE || diskIndex >= diskSize)
              {
              errorCode = ERRORCODE_MISSING_EOF;
              break;
              }
            }
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
  private GlobalSynch synch;
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
  public Processor(IntController i, GlobalSynch gs, Memory m, ConsoleListener c, 
                  Timer t, Disk d)
    {
    hint = i;
    synch = gs;
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
      synch.mysleep(2);
      // read from memory in the address indicated by PC
      int RD = mem.read(PC++);
      // break the 32bit word into 4 separate bytes
      IR[0] = RD>>>24;
      IR[1] = (RD>>>16) & 255;
      IR[2] = (RD>>>8) & 255;
      IR[3] = RD & 255;
      // print CPU status to check if it is ok
      System.err.print("processor: PC="+PC);
      System.err.print(" IR="+IR[0]+" "+IR[1]+" "+IR[2]+" "+IR[3]+" ");
			
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
      System.err.println(" [L M r m] ");
      reg[IR[2]] = mem.read(IR[3]);
      }
    else
    if ((IR[0]=='L') && (IR[1]=='C'))
      {
      System.err.println(" [L C r c] ");
      reg[IR[2]] = IR[3];
      }
    else
    if ((IR[0]=='W') && (IR[1]=='M'))
      {
      System.err.println(" [W M r m] ");
      mem.write(IR[3],reg[IR[2]]);
      }
    else
    if ((IR[0]=='S') && (IR[1]=='U'))
      {
      System.err.println(" [S U r1 r2] ");
      reg[IR[2]] = reg[IR[2]] - reg[IR[3]];
      }
    else
    if ((IR[0]=='A') && (IR[1]=='D'))
      {
      System.err.println(" [A D r1 r2] ");
      reg[IR[2]] = reg[IR[2]] + reg[IR[3]];
      }
    else
    if ((IR[0]=='D') && (IR[1]=='E') && (IR[2]=='C'))
      {
      System.err.println(" [D E C r1] ");
      reg[IR[3]] = reg[IR[3]] - 1;
      }
    else
    if ((IR[0]=='I') && (IR[1]=='N') && (IR[2]=='C'))
      {
      System.err.println(" [I N C r1] ");
      reg[IR[3]] = reg[IR[3]] + 1;
      }
    else
    if ((IR[0]=='C') && (IR[1]=='P'))
      {
      System.err.println(" [C P r1 r2] ");
      if (reg[IR[2]] == 0) flag[Z] = 1; else flag[Z] = 0;
      if (reg[IR[2]] == reg[IR[3]]) flag[E] = 1; else flag[E] = 0;
      if (reg[IR[2]] < reg[IR[3]]) flag[L] = 1; else flag[L] = 0;
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='A'))
      {
      System.err.println(" [J P A m] ");
      PC = IR[3];
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='Z'))
      {
      System.err.println(" [J P Z m] ");
      if (flag[Z] == 1)
        PC = IR[3];
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='E'))
      {
      System.err.println(" [J P E m] ");
      if (flag[E] == 1)
        PC = IR[3];
      }
    else
    if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='L'))
      {
      System.err.println(" [J P L m] ");
      if (flag[L] == 1)
        PC = IR[3];
      }
    else
    if (IR[0]=='I'&&IR[1]=='N'&&IR[2]=='T')
      {
      System.err.println(" [I N T n] ");
      kernel.run(IR[3]);
      }
    else
      System.err.println(" [? ? ? ?] ");
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
    readyList = new ProcessList ("Ready");
    diskList = new ProcessList ("Disk");
    // Creates the dummy process
    readyList.pushBack( new ProcessDescriptor(0) );
    readyList.getBack().setPC(0);
    }
  // Each time the kernel runs it have access to all hardware components
  public void run(int interruptNumber)
    {
    // Calls the interface
    // You need to inform the PIDs from the ready and disk Lists
    //CÉSAR: NÃO DEVIA SER diskList.getFront().getPID() no segundo parâmetro?
    SopaInterface.updateDisplay(readyList.getFront().getPID(), 
          readyList.getFront().getPID(), interruptNumber);
    
    ProcessDescriptor aux = null;
    // This is the entry point: must check what happened
    System.err.println("Kernel called for int "+interruptNumber);
    // save context
    readyList.getFront().setPC(pro.getPC());
    readyList.getFront().setReg(pro.getReg());
    switch(interruptNumber)
      {
    case 2: // HW INT timer
      aux = readyList.popFront();
      readyList.pushBack(aux);
      System.err.println("CPU now runs: "+readyList.getFront().getPID());
      break;
    case 5: // HW INT disk 
      aux = diskList.popFront();
      readyList.pushBack(aux);
      break;
    case 15: // HW INT console
      System.err.println("Operator typed " + con.getLine());
      break;
    case 36: // SW INT read
      aux = readyList.popFront();
      diskList.pushBack(aux);
      dis.roda(0,0,0);
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
	
// This list implementation (and the 'next filed' in ProcessDescriptor) was
// programmed in a class to be faster than searching Java's standard lists,
// and it matches the names of the C++ STL. It is all we need now...

class ProcessList
{
  private String myName = "No name";
  private ProcessDescriptor first = null;
  private ProcessDescriptor last = null;
  public ProcessDescriptor getFront() { return first; }
  public ProcessDescriptor getBack() { return last; }
  
  public ProcessList(String name)
  { 
	  myName=name ;
	  SopaInterface.addList(myName);
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
      
      // Update interface
      SopaInterface.removeFromList(n.getPID(), myName);
      
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
    
    // Update interface
    SopaInterface.addToList(n.getPID(), myName);

    }
  }

class SopaInterface {
	/**
	 * @author Guilherme Peretti Pezzi
	 *
	 * 1st version of Sopa interface (05/10/05)
	 * Uses static fields and methods to minimize changes on the simulator code
	 * Send comments, suggestions and bugs to pezzi@inf.ufrgs.br
	 * 
	 * 
	 */
	
	// display stuff
	private static GridBagConstraints c;
	private static Container pane ;
    private static JFrame jFrame; 
	private static JPanel jContentPane ;
	private static JPanel pane2 ;
	private static JTextArea jTextArea;
	private static JButton playButton, stepButton, stopButton;
	private static JScrollPane scrollPane = null;
	// used to display lists
	private static HashMap myJLists = new HashMap();
	// used to keep lists data
	private static HashMap myLinkedLists = new HashMap();
	//	last grid x used for dynamic lists display
	private static int lastInsPos = 0; 
	private static GlobalSynch gs;


	//	PUBLIC METHODS	
	// initializes interface
    public static void initViewer(GlobalSynch gsynch) {
        gs =gsynch;
        
    	//fru fru java
        JFrame.setDefaultLookAndFeelDecorated(true);
        //Create and set up the window.
        jFrame = getJFrame();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Set up the content pane.
    	pane = jFrame.getContentPane();
    	pane.setLayout(new GridBagLayout());
    	c = new GridBagConstraints();
    	c.fill = GridBagConstraints.BOTH; //HORIZONTAL;
        addFixComponentsToPane();
 		//Display the window.
        jFrame.pack();
        jFrame.setVisible(true);
    }
    
	// dynamicaly creates process list on jframe 
	// when a new list is created    
    public static void addList(String name){
    	//Init lists    	
    	myJLists.put(name, new JList() );
        myLinkedLists.put(name, new LinkedList() );
        
        //GridBag parameters
        c.gridwidth = 1; 
    	c.ipadx = 10;
    	c.ipady = 0;
    	c.gridy = 0;
    	c.weighty=1;
    	c.gridx = lastInsPos;
		c.insets = new Insets(0,0,0,0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTH;
    	
    	//adding label
		JLabel titulo = new JLabel(name);
    	pane.add(titulo,c);
		
		//adding list
		c.gridy = 1;
    	pane2 = new JPanel();
        pane2.setLayout(new BoxLayout(pane2, BoxLayout.PAGE_AXIS));
        pane2.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pane2.setPreferredSize(new Dimension(20,160));
        pane2.add( (JList) myJLists.get(name) );
        pane.add(pane2, c);
        
        //increments x counter and repack frame
        lastInsPos++; 
        jFrame.pack();
    }
    
    //update text display
    public static void updateDisplay(int proc, int disk, int i)
    {
    	appendMsg("\n"+ "      " + Integer.toString(proc) + "\t      " +
    	 Integer.toString(disk) +"\t      " + 
    	 Integer.toString(i));
    }
	//adds element to list and redisplay list 
    public static void addToList(int PID, String name){
    	LinkedList changedList = ( (LinkedList) myLinkedLists.get(name));
    	//update linked list
    	changedList.add( Integer.toString(PID) );
    	//update screen structure 
        ( (JList) myJLists.get(name) ).setListData(changedList.toArray());
        jFrame.pack();
    }
    
	//	removes element from list and redisplay list
    public static void removeFromList(int PID, String name){
    	LinkedList changedList = ( (LinkedList) myLinkedLists.get(name));
    	int test = -1;
    	if( !changedList.isEmpty() ){
    		test = Integer.parseInt( (String) changedList.removeFirst() );
    		if ( test != PID ) 
    		{
				appendMsg("<INTERFACE> ERROR REMOVING " + PID + " FROM LIST: "+ name  + "\n" );
			}else;
		}else{
			appendMsg("<INTERFACE> ERROR REMOVING FROM EMPTY LIST: " +name  + "\n" );
        }
    	( (JList) myJLists.get(name) ).setListData(changedList.toArray());    	
		jFrame.pack();
    }
	
	// PRIVATE METHODS   
    /**
	 * This method initializes jFrame	
	 * 	
	 * @return javax.swing.JFrame	
	 */
	private static JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setSize(new java.awt.Dimension(400,400));
			jFrame.setContentPane(getJContentPane());
		}
		return jFrame;
	}

	/**
	 * This method initializes jContentPane	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private static JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
		}
		return jContentPane;
	}
	
	// adds time control buttons and text output
	private static void addFixComponentsToPane() {

		// LINE 3
		c.gridy = 2;
		 
		c.ipadx = 0;
		c.ipady = 5;
		c.gridwidth = 3;      
		c.insets = new Insets(5,10,5,10);

		playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				gs.play();                         
				}});
		c.gridx = 0;
		pane.add(playButton, c);
        
		stopButton = new JButton("Pause");
		stopButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				gs.pause();
				}});
		c.gridx = 3;
		pane.add(stopButton, c);

		stepButton = new JButton("Next Step");
		stepButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				gs.advance();                            
				}});
		c.gridx = 6;
		pane.add(stepButton, c);      
        
//		LINE 4
		c.gridy = 3;
        
		jTextArea = new JTextArea(" Process \t   Disk \t Interrupt");
		scrollPane = new JScrollPane(jTextArea);
		scrollPane.setPreferredSize(new Dimension(100,300));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		c.insets = new Insets(5,10,5,10);
		c.weighty = 1.0;   //request any extra vertical space
		c.anchor = GridBagConstraints.PAGE_END; //bottom of space
		c.ipady = 0;
		c.gridx = 0;       
		c.gridwidth = 9;   //9 columns wide
		pane.add(scrollPane, c);
                
	}

	private static void appendMsg(String msg){
    	jTextArea.setText(jTextArea.getText()+msg);
    	jTextArea.setCaretPosition(jTextArea.getText().length());
    }
}
