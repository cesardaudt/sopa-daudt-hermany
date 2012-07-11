import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class Memory {
	// This is the memory system component. 
	private IntController hint;
	private int[] memoryWord;
	private int partition_size;	//size of each partition
	private int partitions;		//# of partitions
	
	// MMU: base and limit registers
	private int limitRegister;            // specified in logical addresses
	private int baseRegister;             // add base to get physical address
	// constructor
	public Memory(IntController i, int p_size, int n_part) {
		// remember size and create memory
		hint = i;
		partition_size = p_size;
		partitions = n_part;
		memoryWord = new int[p_size*n_part];
		
		// Initialize with the dummy program
		init(0, 'J', 'P', 'A', 0);
	}
	
	public int getPartitionSize() {
		return partition_size;
	}
	
	public int getPartitions() {
		return partitions;
	}
	
	// Access methods for the MMU: these are accessed by the kernel.
	// They do not check memory limits. It is interpreted as kernel's
	// fault to set these registers with values out of range. The result
	// is that the JVM will terminate with an 'out of range' exception
	// if a process uses a memory space that the kernel set wrongly.
	// This is correct: the memory interruption must be set in our
	// simulated machine only if the kernel was right and the process itself
	// tries to access an address which is out of its logical space.
	public void setLimitRegister(int val) {
		limitRegister = val; 
	};
	
	public void setBaseRegister(int val) { 
		baseRegister = val;
	};
	
	// Here goes some specifi methods for the kernel to access memory
	// bypassing the MMU (do not add base register or test limits)
	synchronized public int superRead(int address) {
		return memoryWord[address]; 
	}
	
	synchronized public void superWrite(int address, int data) {
		memoryWord[address] = data; 
	}
	
	// Access methods for the Memory itself
	public synchronized void init(int add, int a, int b, int c, int d) {
		memoryWord[add] = (a << 24)+(b<<16)+(c<<8)+d;
	}
	
	public synchronized int read(int address) {
		if (address >= limitRegister) {
			hint.set(3);
			return 0;
		}
		else
			return memoryWord[baseRegister + address];
	}
	
	public synchronized void write(int address, int data) {
	if (address >= limitRegister)
		hint.set(3);
	else
		memoryWord[baseRegister + address] = data;
	}
}
