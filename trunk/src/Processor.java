import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class Processor extends Thread {
	// Access to hardware components
	private IntController hint;
	private GlobalSynch synch;
	private Memory mem;
	private ConsoleListener con;
	private Timer tim;
	private Disk disk1, disk2;
	
	// CPU internal components
	private int PC;	// Program Counter
	private int[] IR;	// Instruction Register
	private int[] reg;	// general purpose registers
	private int[] flag;   // flags Z E L
	private final int Z = 0;
	private final int E = 1;
	private final int L = 2;
	private int id;
	
	// Access methods
	public synchronized int getPC() { 
		return PC;
	}
	
	public synchronized void setPC(int i) { 
		PC = i;
	}
	
	public synchronized int[] getReg() { 
		return reg; 
	}
	
	public synchronized void setReg(int[] r) {
		reg = r;
	}
	
	public synchronized int[] getFlag() { 
		return flag;
	}
	
	public synchronized void setFlag(int[] f) {
		flag = f;
	}
	
	// Kernel is like a software in ROM
	private Kernel kernel;
	
	public Processor(int id, IntController i, GlobalSynch gs, Memory m, ConsoleListener c, 
		          Timer t, Disk d1, Disk d2, Kernel k) {
		this.id = id;
		hint = i;
		synch = gs;
		mem = m;
		con = c;
		tim = t;
		kernel = k;
		disk1 = d1;
		disk2 = d2;
		PC = 0;
		IR = new int[4];
		reg = new int[16];
		flag = new int[3];
	}
	
	public void run() {
		while (true) {
			// sleep a tenth of a second
			synch.mysleep(2);
			// read from memory in the address indicated by PC
			int RD = mem.read(PC++);
			// break the 32bit word into 4 separate bytes
			IR[0] = RD>>>24;
			IR[1] = (RD>>>16) & 255;
			IR[2] = (RD>>>8) & 255;
			IR[3] = RD & 255;
			// print CPU status to check if it is ok
			System.err.print("processor: "+id+ " PC= "+PC);
			System.err.print(" IR="+IR[0]+" "+IR[1]+" "+IR[2]+" "+IR[3]+" ");

			// Execute basic instructions of the architecture

			execute_basic_instructions();

			// Check for Hardware Interrupt and if so call the kernel
			int thisInt = hint.getReset();
			if(thisInt != 0) {
				// Call the kernel passing the interrupt number
				kernel.run(thisInt, id);
			}
		}
	}
	
	public void execute_basic_instructions() {
		//L M r m		r = memory[m]	Load register from Memory
		if ((IR[0]=='L') && (IR[1]=='M')) {
			System.err.println(" [L M r m] ");
			reg[IR[2]] = mem.read(IR[3]);
		}
		//L C r c		r = c 			Load register from Constant
		else if ((IR[0]=='L') && (IR[1]=='C')) {
			System.err.println(" [L C r c] ");
			reg[IR[2]] = IR[3];
		}
		//W M r m		memory[m] = r	Write register in Memory
		else if ((IR[0]=='W') && (IR[1]=='M')) {
			System.err.println(" [W M r m] ");
			mem.write(IR[3],reg[IR[2]]);
		}
		//S U r1 r2		r1 = r1 - r2	Subtract registers
		else if ((IR[0]=='S') && (IR[1]=='U')) {
			System.err.println(" [S U r1 r2] ");
			reg[IR[2]] = reg[IR[2]] - reg[IR[3]];
		}
		//A D r1 r2		r1 = r1 + r2	Add registers
		else if ((IR[0]=='A') && (IR[1]=='D')) {
			System.err.println(" [A D r1 r2] ");
			reg[IR[2]] = reg[IR[2]] + reg[IR[3]];
		}
		//D E C r1		r1 = r1 - 1		Decrement register
		else if ((IR[0]=='D') && (IR[1]=='E') && (IR[2]=='C')) {
			System.err.println(" [D E C r1] ");
			reg[IR[3]] = reg[IR[3]] - 1;
		}
		//I N C r1		r1 = r1 + 1		Increment register
		else if ((IR[0]=='I') && (IR[1]=='N') && (IR[2]=='C')) {
			System.err.println(" [I N C r1] ");
			reg[IR[3]] = reg[IR[3]] + 1;
		}
		//C P r1 r2						Compare registers
		else if ((IR[0]=='C') && (IR[1]=='P')) {
			System.err.println(" [C P r1 r2] ");
						//if r1==0 then Z = 1 else Z = 0
			flag[Z] = (reg[IR[2]] == 0) ?  1 : 0;
						//if r1==r2 then E = 1 else E = 0
			flag[E] = (reg[IR[2]] == reg[IR[3]]) ?  1 : 0;
						//if r1<r2 then L = 1 else L = 0
			flag[L] = (reg[IR[2]] < reg[IR[3]]) ? 1 : 0;
		}
		//J P A m		PC = m			Absolute jump
		else if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='A')) {
			System.err.println(" [J P A m] ");
			PC = IR[3];
		}
		//J P Z m						Jump on Zero
		else if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='Z')) {
			System.err.println(" [J P Z m] ");
						//if Z=1 then PC = m
			PC = (flag[Z] == 1) ? IR[3] : PC;
		}
		//J P E m						Jump on Equal
		else if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='E')) {
			System.err.println(" [J P E m] ");
						//if E=1 then PC = m
			PC = (flag[E] == 1) ? IR[3] : PC;
		}
		//J P L m						Jump on Less
		else if ((IR[0]=='J') && (IR[1]=='P') && (IR[2]=='L')) {
			System.err.println(" [J P L m] ");
						//if L=1 then PC = m
			PC = (flag[L] == 1) ? IR[3] : PC;
		}
		//I N T n						Software Int n
		else if (IR[0]=='I'&&IR[1]=='N'&&IR[2]=='T') {
			System.err.println(" [I N T n] ");
			kernel.run(IR[3], id);
		}
		else
			System.err.println(" [? ? ? ?] ");
	}
}
