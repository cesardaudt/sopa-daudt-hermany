import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class GlobalSynch extends Thread {
	// This is a master clock for the simulation. Instead of running concurrent
	// threads with the normal sleep from Java, we use instead this GlobalSynch
	// sleep system that can be controlled and excecuted step by step.
	private int quantum;
	private boolean stepMode;
	private Semaphore lock;
	
	public GlobalSynch(int q) {
		quantum = q;
		stepMode = false; 
		lock = new Semaphore(1);
	}
	
	public synchronized void mysleep(int n) {
		for (int i=0; i<n; ++i)
			try { 
	  			wait(); 
		  	}
		  	catch(InterruptedException e) {}
	}
	
	public synchronized void mywakeup() {
		notifyAll();
	}
	
	public void run() {
		while (true) {
			lock.P();
			if (stepMode == false)
				lock.V();
			try {
				sleep(quantum);
			}
			catch (InterruptedException e) {}
			mywakeup();
		}
	}
	
	public synchronized void advance() {
		if (steimport java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;pMode == true)
	  		lock.V();
	}
	
	public synchronized void pause() {
		if (stepMode == false) {
			stepMode = true;
			lock.P();
		}
	}
	
	public synchronized void play() {
		if (stepMode == true) {
			stepMode = false;
			lock.V();
		}
	}
}
