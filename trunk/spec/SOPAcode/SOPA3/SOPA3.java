///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 28/05/2002
//
// SOPA3 - Implementa objeto IntController sincronizado e com semaforo.
// Este objeto era antes chamado somente de Interrupt. Mas na verdade
// precisamos modelar o componente de hardware que eh o controlador 
// de interrupcoes. Tambem foi criado o objeto Kernel e feita a sua
// chamada de dentro do processador quando ocorre interrupcao de HW.
// Foram testadas as geracoes de interrupcoes no timer e no disco,
// e verificado que a CPU passou a executar o kernel, mas esta parte
// (gera interrupcao) foi removida para que os alunos mesmo a coloquem.
//
///////////////////////////////////////////////////////////////////

public class SOPA3
  {
  public static void main(String args[])
    {
    // The program models a complete computer with most HW components
    // The kernel, which is the software component, might have been
    // created here also, but Processor has a refernce to it and it
    // has a reference to the processor, so I decided that all software
    // is under the processor environment: kernel inside processor.
    IntController i	= new IntController();
    Memory m	= new Memory(1024);
    Console c	= new Console(i);
    Timer t	= new Timer(i);
    Disco d	= new Disco(i,m);
    Processor p	= new Processor(i,m,c,t,d);
    // start all threads
    p.start();
    c.start();
    t.start();
    d.start();
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
  // interrupt requests coming from all other components. But I could not
  // make the operations on this semaphore automatic, since it cannot do a
  // V operation while locked in a sinchronized method that called P.
  // Other components have then to call P() before calling set() and call
  // V() before calling reset(). We could also make IntController extends
  // Semaphore instead of having one. Try this out.
  private Semaphore semhi;
  private int number;
  public IntController()		{ semhi = new Semaphore(1);    }
  public void P()			{ semhi.P(); }
  public void V()			{ semhi.V(); }
  public synchronized void set(int n)	{ number = n;    }
  public synchronized int get()		{ return number;   }
  public synchronized void reset()	{ number = 0;    }
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
    // Initialize with something on it for testing
    for (int i=0; i < s; ++i)
      word[i] = 2*i;
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

class Console extends Thread
  {
  // Console is an intelligent terminal that reads an entire command
  // line and then generates an interrupt. It should provide a method
  // for the kernel to read the command line, and make sure no other
  // line is being typed in this while.
  private IntController hint;
  public Console(IntController i)
    {
    hint = i;
    }
  public void run()
    {
    while (true)
      {
      try {sleep(2000);} // 2 seconds
      catch (InterruptedException e){}
      System.err.println("console");
      }
    }
  }

class Timer extends Thread
  {
  // Our programable timer. Need to implement its programability.
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
	// Here goes the code (2 lines) that generates an interrupt
	hint.P();
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
  public Disco(IntController i,Memory m)
    {
    hint = i;
    mem = m;
    sem = new Semaphore(0);
    }
  public void run()
    {
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
      // Here goes the code (2 lines) that generates an interrupt
      hint.P();
      hint.set(5);
      }
    }
  public void roda()
    {
    sem.V();
    }
  }

class Processor extends Thread
  {
  // Access to hardware components
  private IntController hint;
  private Memory mem;
  private Console con;
  private Timer tim;
  private Disco dis;
  // CPU internal components
  private int PC;			// Program Counter
  private int[] IR;			// Instruction Register
  // Kernel is like a software in ROM
  private Kernel kernel;
  public Processor(IntController i, Memory m, Console c, Timer t, Disco d)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    PC = 0;
    IR = new int[4];
    kernel = new Kernel(i,m,c,t,d,this);
    // I am not sure it is a good idea to pass 'this' in the constructor.
    // If you have any problem calling processor methods from the kernel 
    // I strongly suggest you to pass to the kernel a reference to the
    // processor after the interrupt number.
    }
  public void run()
    {
    while (true)
      {
      // sleep a tenth of a second
      try {sleep(100);} catch (InterruptedException e){}
      // read from memory in the address indicated by PC
      int RD = mem.read(PC);
      // break the 32bit word into 4 separate bytes
      IR[0] = RD>>>24;
      IR[1] = (RD>>>16) & 255;
      IR[2] = (RD>>>8) & 255;
      IR[3] = RD & 255;
      // print CPU status to check if it´s ok
      System.err.print("processor: PC="+PC);
      System.err.print(" IR="+IR[0]+" "+IR[1]+" "+IR[2]+" "+IR[3]);
      if (IR[3]=='L')
        {
        System.err.println(" Last byte is L : Request for disk");
	    dis.roda();
	    }
      else
        System.err.println(" ??? ");
        // advance PC to next instruction
        ++PC;
        // Check for Hardware Interrupt and if so call the kernel
        if (hint.get() != 0)
	    {
	    // Call the kernel passing the interrupt number
	    kernel.run(hint.get());
	    // This goes here because only HW interrupts are reset in the controller
	    // But reseting the interrupt controller might be a task for the Kernel 
	    // in a real system.
	    hint.reset();
	    hint.V();
	    }
      }
    }
  }

class Kernel
  {
  // Access to hardware components, including the processor
  private IntController hint;
  private Memory mem;
  private Console con;
  private Timer tim;
  private Disco dis;
  private Processor pro;
  // In the constructor goes initialization code
  public Kernel(IntController i, Memory m, Console c, Timer t, Disco d, Processor p)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    pro = p;
    }
  // Each time the kernel runs it have access to all hardware components
  public void run(int hardwareInterrupNumber)
    {
    // This is the entry point: must check what happened
    System.err.println("Kernel called for int "+hardwareInterrupNumber);
    }
  }
