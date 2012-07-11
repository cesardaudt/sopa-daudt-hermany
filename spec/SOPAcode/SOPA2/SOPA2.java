///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 18/04/2002
//
// SOPA2 - Implementa objeto semaforo a testa seu uso com o disco.
// A thread CPU libera a thread disco cada vez que encontra L como
// o ultimo byte de uma instruçao. O disco entao da sinal de seu
// funcionamento imprimindo uma volta que da cada 50 milisegundos.
//
///////////////////////////////////////////////////////////////////

public class SOPA2
  {
  public static void main(String args[])
    {
    Interrupt i	= new Interrupt();
    Memory m	= new Memory(1024);
    Console c	= new Console(i);
    Console b	= new Console(i);
    Timer t	= new Timer(i);
    Disco d	= new Disco(i,m);
    Processor p	= new Processor(i,m,c,t,d);
    // start all threads
    p.start();
    c.start();
    b.start();
    t.start();
    d.start();
    }
  }

class Semaphore 
  {
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

class Interrupt
  {
  }

class Memory
  {
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
  public int read(int address)
    {
    return word[address];
    }
  public void write(int address, int data)
    {
    word[address] = data;
    }
  }

class Console extends Thread
  {
  private Interrupt hint;
  public Console(Interrupt i)
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
  private Interrupt hint;
  public Timer(Interrupt i)
    {
    hint = i;
    }
  public void run()
    {
    while (true)
      {
      try {sleep(500);} // half second
      catch (InterruptedException e){}
      System.err.println("timer");
      }
    }
  }

class Disco extends Thread
  {
  private Interrupt hint;
  private Memory mem;
  private Semaphore sem;
  public Disco(Interrupt i,Memory m)
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
  private Interrupt hint;
  private Memory mem;
  private Console con;
  private Timer tim;
  private Disco dis;
  // CPU internal components
  private int PC;			// Program Counter
  private int[] IR;			// Instruction Register
  public Processor(Interrupt i, Memory m, Console c, Timer t, Disco d)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    PC = 0;
    IR = new int[4];
    }
  public void run()
    {
    while (true)
      {
      // sleep a tenth of a second
      try {sleep(100);} catch (InterruptedException e){}
      // read from memory in the address indicated by PC
      int RD = mem.read(PC);
      // breake the 32bit word into 4 separate bytes
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
      }
    }
  }
