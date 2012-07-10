///////////////////////////////////////////////////////////////////
//
// Sistema Operacional Para Avaliacao - Marcelo Johann - 18/04/2002
//
// SOPA0 - Demostra as threads que serao usadas rodando em paralelo
// Com comentários explicando o que fará cada componente ou thread
//
///////////////////////////////////////////////////////////////////

public class SOPA0
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

// Controlador de interrupcoes, sincroniza e enfileira os
// pedidos de interrupcao de hardware usando um semaforo
// Eh um objeto, recurso de hardware acessivel pelos outros
// componentes.

class Interrupt
  {
  }

// Memoria tambem eh um recurso acessivel ao Processador e
// ao disco, que pode fazer DMA. Memoria implementara 
// protecao e traducao de enderecos.

class Memory
  {
  }

// O console aqui previsto eh uma mistura de hardware
// de entrada (controlador de teclado, por exemplo), e de
// shell, pois deve ler todo um comando do usuario antes
// de gerar uma interrupcao. O shell completo sera uma
// rotina do S.O. que le esta informacao e a trata para
// carga e execucao de um novo programa.

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

// O timer sera realmente parecido com esse: dorme um
// tempinho e depois acorda, quando devera gerar uma
// interrupcao de hardware (timer). O tempo seta programavel,
// definido pelo S.O. atraves de um metodo proprio de timer.

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

// O disco tambem eh uma thread porque deve funcionar totalmente
// em paralelo com os demais componentes de hardware, conforme
// sera visto no codigo SOPA2.

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

// Finalmente vem a thread do Processador. Esta thread inclui
// tudo que eh executado pela CPU, ou seja, tudo que eh software.
// Aqui incluem-se os programas de usuario, executados em codigo
// objeto, e o kernel do S.O., executado com uma chamada de
// sub-rotina, pois ele tambem so eh executado pela CPU, nao 
// havendo parelelismo entre esses dois. Essa subrotina, no
// entanto, deve ser melhor implementada como metodo de entrada
// de objeto de outra classe Kernel, a qual encapsula os dados
// proprios do kernel, separando-os dos dados do processador,
// embora o kernel tenha acesso a estes.

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