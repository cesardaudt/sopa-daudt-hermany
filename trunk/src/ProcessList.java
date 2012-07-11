import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class ProcessList {
	private String myName = "No name";
	private ProcessDescriptor first = null;
	private ProcessDescriptor last = null;
	
	public ProcessDescriptor getFront() { 
		return first;
	}
	
	public ProcessDescriptor getBack() { 
		return last;
	}

	public ProcessList(String name) { 
		myName = name;
		SopaInterface.addList(myName);
	}

	synchronized public ProcessDescriptor popFront() { 
		ProcessDescriptor n;
		if(first!=null) {
	  		n = first;
	  		first=first.getNext();
	  		if (last == n)
				last = null;
	  		n.setNext(null);
	  
		  // Update interface
		  SopaInterface.removeFromList(n.getPID(), myName);
		  
		  return n;
	  	}
		return null;
	}
	
	synchronized public void pushBack(ProcessDescriptor n) { 
		n.setNext(null);
		if (last!=null)
			last.setNext(n);
		else
			first = n; 
		last = n;

		// Update interface
		SopaInterface.addToList(n.getPID(), myName);

	}
}
