///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 18/04/2002
//
// SOPA1 - Implementa PC RI e memoria simples absoluta sem protecao
// Observem como as threads estao intercaladas, pois outras threads
// imprimem suas mensagens no meio da mensagem da thread Processor
// (exemplo na posicao de memoria 89).
//
///////////////////////////////////////////////////////////////////

public class SOPA1
  {
  public static void main(String args[])
    {
    Interrupt i	= new Interrupt();
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
  public Disco(Interrupt i,Memory m)
    {
    hint = i;
    mem = m;
    }
  public void run()
    {
    while (true)
      {
      try {sleep(5000);} // 5 seconds
      catch (InterruptedException e){}
      System.err.println("disco");
      }
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
        System.err.println(" Last byte is char L ");
      else
        System.err.println(" ??? ");
      // advance PC to next instruction
      ++PC;
      }
    }
  }
