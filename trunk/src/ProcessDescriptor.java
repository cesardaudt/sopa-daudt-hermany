import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class ProcessDescriptor	{
	private int PID;
	private int PC;
	private int[] reg;
	private ProcessDescriptor next;
	private int partition;
	private boolean loading;
	private ArrayList<FileDescriptor> files;
	private int time;
	
	public FileDescriptor addFile(Memory m) {
		FileDescriptor f = new FileDescriptor(files.size(), this, m);
		files.add(files.size(), f);
		return f;
	}
	
	public void removeFile(int i) {
		files.remove(i);
	}
	
	public FileDescriptor getFile(int i) {
		return files.get(i);
	}
	
	public void setTime(int t) {
		time = t;
	}
	
	public int timeInTicks() {
		System.err.println("Proc "+PID+" tickd " +time);
		return --time;
	}
	
	public boolean isLoading() {
		return loading;
	}
	
	public void setLoaded() {
		loading = false;
	}
	
	synchronized public int getPID() { 
		return PID;
	}
	
	synchronized public int getPC() {
		return PC;
	}
	
	synchronized public void setPC(int i) {
		PC = i;
	}
	
	synchronized public int[] getReg() {
		return reg;
	}
	
	synchronized public void setReg(int[] r) {
		reg = r;
	}
	
	synchronized public int getPartition() {
		return partition;
	}
	
	synchronized public void setPartition(int p) {
		partition = p;
	}
	
	public ProcessDescriptor getNext() {
		return next;
	}
	
	public void setNext(ProcessDescriptor n) {
		next = n;
	}
	
	public ProcessDescriptor(int pid, int p, boolean load) {
		PID = pid;
		PC = 0;
		partition = p;
		loading = load;
		reg = new int[16];
		files = new ArrayList<FileDescriptor>();
		time = 0;
	}
}
