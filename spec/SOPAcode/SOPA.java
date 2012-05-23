///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 18/04/2002
//
// SOPA - Demostra as threads que serao usadas rodando em paralelo
// Sem comentários
//
///////////////////////////////////////////////////////////////////

public class SOPA
  {
  public static void main(String args[])
    {
    Interrupt i	= new Interrupt();
    Memory m	= new Memory();
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
  private Interrupt hint;
  private Memory mem;
  private Console con;
  private Timer tim;
  private Disco dis;
  public Processor(Interrupt i, Memory m, Console c, Timer t, Disco d)
    {
    hint = i;
    mem = m;
    con = c;
    tim = t;
    dis = d;
    }
  public void run()
    {
    while (true)
      {
      try {sleep(100);} // tenth of a second
      catch (InterruptedException e){}
      System.err.println("processor");
      }
    }
  }