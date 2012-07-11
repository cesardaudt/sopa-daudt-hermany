import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

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
