import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

public class SOPA {
	private final static int CPUS = 2;
	private final static int PARTITIONS = 6;
	private final static int PARTITION_WORDS = 8;

	public static void main(String args[]) {
		// The program models a complete computer with most HW components
		// The kernel, which is the software component, might have been
		// created here also, but Processor has a refernce to it and it
		// has a reference to the processor, so I decided that all software
		// is under the processor environment: kernel inside processor.

		GlobalSynch global_sych = new GlobalSynch(50);  // quantum of 50ms
		IntController int_controller = new IntController();

		// Create interface
		SopaInterface.initViewer(global_sych);

		// Create window console
		MyWin mw = new MyWin();
		mw.addWindowListener(new WindowAdapter() {
			public void windowClosing (WindowEvent e) {
				System.exit(0);
			}
		});
		
		ConsoleListener console_listener = mw.getListener();
		console_listener.setInterruptController(int_controller);
		console_listener.setGlobalSynch(global_sych);
		
		Memory memory = new Memory(int_controller, PARTITION_WORDS, PARTITIONS);
		Timer timer	= new Timer(int_controller, global_sych);
		Disk disk1	= new Disk(0, int_controller, global_sych, memory, 1024, "disk.txt");
		Disk disk2	= new Disk(1, int_controller, global_sych, memory, 1024, "disk.txt");

		Kernel kernel = new Kernel(int_controller, memory, console_listener, timer, disk1, disk2, CPUS, PARTITIONS);
				
		Processor[] processors = new Processor[CPUS];
		
		//TODO: maybe use a for-loop (if we have more procs)
		processors[0] = new Processor(0, int_controller, global_sych, memory, console_listener, timer, disk1, disk2, kernel);
		processors[1] = new Processor(1, int_controller, global_sych, memory, console_listener, timer, disk1, disk2, kernel);
		
		// start all threads
		//TODO: maybe use a for-loop (if we have more procs)
		processors[0].start();
		processors[1].start();
		timer.start();
		disk1.start();
		disk2.start();
		global_sych.start();
	}
}
