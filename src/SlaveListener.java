import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class SlaveListener extends Thread {
	private IntController hint;
	private Semaphore sem;
	
	public SlaveListener(IntController i) {
		hint = i;
		sem = new Semaphore(0);
	}
	public void setInterrupt() {
		sem.V();
	}

	public void run() {
		while(true) {
			sem.P();
			hint.set(15);
	  	}
	}
}
