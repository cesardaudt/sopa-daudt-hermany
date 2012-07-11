import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class IntController {
	// The interrupt controler component has a private semaphore to maintain 
	// interrupt requests coming from all other components. 
	// Interruptions from memory are exceptions that need to be handled right
	// now, and have priority over other ints. So, memory interrupt has its
	// own indicator, and the others compete among them using the Semaphore.

	private int number;
	private Queue<Integer> numbers;
	private final int memoryInterruptNumber = 3;

	public IntController() {
		numbers = new LinkedList<Integer>();
		number = 0;
	}
	
	synchronized public void set(int n) { 
		if (n == memoryInterruptNumber)
			number = n;
		else {
			number.offer(n);
		}
	}
	
	synchronized public int getReset() { 
		int ret;
		
		if(number > 0) {
			ret = number;
			number = 0;
		}
		
		else {
			if(numbers.size() == 0)
				ret = 0;
			else
				ret = numbers.remove();
		}
		
		return ret;
	}
}
