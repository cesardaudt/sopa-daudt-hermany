import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class Kernel {
	private final int KERNEL_PARTITION = 0;
	private final int USED_PARTITION = 1;
	private final int AVAILABLE_PARTITION = 2;
	private final int NO_FREE_PARTITION = -1;
	
	// Access to hardware components, including the processor
	private IntController hint;
	private Memory mem;
	private ConsoleListener con;
	private Timer tim;
	private Disk[] dis;
	private Processor[] pro;

	// Data used by the kernel
	private int n_Pid;
	private ProcessList[] procLists;
	private ProcessList[] diskLists;
	private ProcessList readyList;
	private int[] partitionsList;
	private int partitions;
	private int cpus;
	
	// In the constructor goes initialization code
	public Kernel(IntController i, Memory m, ConsoleListener c, Timer t, Disk d1, Disk d2, int cpus, int partitions) {
		hint = i;
		mem = m;
		con = c;
		tim = t;
		n_Pid = 1;
		this.partitions = partitions;
		this.cpus = cpus;

		dis = new Disk[2];
		dis[0] = d1;
		dis[1] = d2;
		
		readyList = new ProcessList ("Ready");
		
		diskLists = new ProcessList[2];
		diskLists[0] = new ProcessList("Disk 0");
		diskLists[1] = new ProcessList("Disk 1");
		
		procLists = new ProcessList[cpus];
		//TODO: CÉSAR: maybe we should use a for-loop (in case we have more cpus)
		procLists[0] = new ProcessList("Proc 0");
		procLists[1] = new ProcessList("Proc 1");
		
		partitionsList = new int[partitions];
		partitionsList[0] = KERNEL_PARTITION;
		partitionsList[1] = KERNEL_PARTITION;
		for(int j = 2; j < partitions; j++) {
			partitionsList[j] = AVAILABLE_PARTITION;
		}
	}
	// Each time the kernel runs it have access to all hardware components
	
	public void initKernel(Processor[] procs) {
		pro = procs;
		
		for(int i = 0; i<pro.length; i++) {
			runProcess(makeDummyProcess(), i);
		}
		//initProcesses(5);
	}
	
//	private void initProcesses(int n) {
//		ProcessDescriptor aux;
//		
//		for(int i = 1; i <= n; i++) {
//			//TODO:
//			aux = new ProcessDescriptor(i, freePartition(), false);	
//			mem.superWrite(aux.getPartition() * mem.getPartitionSize()+i, dis[0].getData(i));	
//			readyList.pushBack(aux);
//		}
//	}

	public ProcessDescriptor makeDummyProcess() {
		return new ProcessDescriptor(34, freePartition(), false);
	}
	
	private int freePartition() {
		for(int i=2; i<partitions; i++) {
			if(partitionsList[i]==AVAILABLE_PARTITION)
				return i;
		}
		//no free partition found
		return NO_FREE_PARTITION;
	}
	
	public ProcessDescriptor createProcess() {
		int p = freePartition();
		if(p != NO_FREE_PARTITION) {
			ProcessDescriptor np = new ProcessDescriptor(n_Pid++, p, true);
			partitionsList[p] = USED_PARTITION;
			np.setPC(p*mem.getPartitionSize());
			System.err.println("\t\tProcess "+np.getPID()+" in partition "+p+"with pc="+np.getPC());
			return np;
		}
		else {
			System.err.println("Error creating new process: no partitions available.");
            return null;
		}
	}
	
	public void runProcess(ProcessDescriptor proc, int cpuid) {
		if(proc==null) {
			proc = makeDummyProcess();
		}
		
		pro[cpuid].setPC(proc.getPC());
		pro[cpuid].setReg(proc.getReg());
		mem.setBaseRegister(proc.getPartition() * mem.getPartitionSize());
		//TODO:
		mem.setLimitRegister(proc.getPartition() * mem.getPartitionSize() + mem.getPartitionSize() - 1);
		proc.setTime(8);
		procLists[cpuid].pushBack(proc);
	}
	
	private void killRunningProcess(int cpuid) {
		killProcess(procLists[cpuid].popFront());
		runProcess(readyList.popFront(), cpuid);
	}
	
	private void killProcess(ProcessDescriptor proc) {
		partitionsList[proc.getPartition()] = AVAILABLE_PARTITION;
	}
	
	private synchronized void svContext(int cpuid) {
		int PC;
		int[] REGS;

		ProcessDescriptor aux = null;
		
		aux = procLists[cpuid].getFront();
		PC = pro[cpuid].getPC();
		aux.setPC(PC);
		
		aux = procLists[cpuid].getFront();
		REGS = pro[cpuid].getReg();
		aux.setReg(REGS);
	}
	
	private synchronized void getContext(int cpuid) {
		int PC;
		int[] REGS;
		
		ProcessDescriptor aux = null;
		
		aux = procLists[cpuid].getFront();
		PC = aux.getPC();
		pro[cpuid].setPC(PC);
		
		aux = procLists[cpuid].getFront();
		REGS = aux.getReg();
	}
	
	private void handTerminalInt() {
		int[] val = new int[2];
        boolean success = true;
        ProcessDescriptor aux = null;
        
        // parse the user's entry
        StreamTokenizer tokenizer = new StreamTokenizer( new StringReader(con.getLine()) );
        try {
        	//get 2 numbers from input
            for(int i=0; i<2; i++)
                if(tokenizer.nextToken() != StreamTokenizer.TT_EOF && tokenizer.ttype == StreamTokenizer.TT_NUMBER)
                	val[i] = (int) tokenizer.nval;
        } catch (IOException e) {
                //success=false;
                //System.err.println("Could not parse user's entry.");
        }
        if(success) {
        	//must be disk 0 or 1
        	if(val[0]==0 || val[0]==1) {
            // create the process without inserting it on readyList
	            aux = createProcess();
                if(aux!=null) {
					diskLists[val[0]].pushBack(aux);                 
                    dis[val[0]].roda(dis[val[0]].OPERATION_LOAD, val[1], 0);
                    for(int i=0; i<dis[val[0]].getSize(); i++) {
						mem.superWrite(aux.getPartition()*mem.getPartitionSize()+i, dis[val[0]].getData(i));
                    }
                }
            }
            else
        		System.err.println("Invalid disk: choose Disk 0 or 1.");
    	}
	}
	
	private synchronized void handTimerInt() {
		ProcessDescriptor aux;
		
		for(int i=0; i<cpus; i++) {
			if(procLists[i].getFront().timeInTicks()==0 ) {
		        procLists[i].getFront().setPC(pro[i].getPC());
		        procLists[i].getFront().setReg(pro[i].getReg());
		        aux = procLists[i].popFront();
		        if(aux.getPID()!=0)
                	readyList.pushBack(aux);
		        
		        aux = readyList.popFront();
		        
		        runProcess(aux, i);
		        
		        System.err.println("Time slice over! CPU " + i + " now runs: " + procLists[i].getFront().getPID());
            }	
		}
	}
	
	
	private synchronized void upIface(int cpuid, int intnum) {
		// Calls the interface
		// You need to inform the PIDs from the ready and disk Lists
		SopaInterface.updateDisplay(cpuid, procLists[cpuid].getFront().getPID(), intnum);

	}
	
	public void run(int interruptNumber, int cpuid) {
	
	ProcessDescriptor curr_process = null;
	FileDescriptor curr_file = null;
	int[] curr_REGS = null;
	
	// This is the entry point: must check what happened
	upIface(cpuid, interruptNumber);
	
	System.err.println("Kernel called for int "+interruptNumber+" by CPU "+cpuid);
	
	// save context
	svContext(cpuid);
	
	switch(interruptNumber)	{
		case 2: // TIMER INT
			handTimerInt();
		break;
		case 3: //ILLEGAL MEM ACCES
			killRunningProcess(cpuid);
		break;
		case 5: // DISK 0 INT 
		case 6: // DISK 1 INT
			curr_process = diskLists[interruptNumber-5].popFront();
			if(dis[interruptNumber-5].getError() == dis[interruptNumber-5].ERRORCODE_SUCCESS) {
				if(curr_process.isLoading()) {
					curr_process.setLoaded();
					for(int i=0; i<dis[interruptNumber-5].getSize(); i++) {
						mem.superWrite(curr_process.getPartition() * mem.getPartitionSize() + i, dis[interruptNumber-5].getData(i));
					}
				}
			}
			else {
		        switch(dis[interruptNumber-5].getError()) {
		            case 1: 
		            	System.err.println("Error trying to read from disc "+0+" : ERRORCODE_SOMETHING_WRONG");
	                break;
		            case 2:
		                System.err.println("Error trying to read from disc "+0+": ERRORCODE_ADDRESS_OUT_OF_RANGE");
	                break;
		            case 3:
		                System.err.println("Error trying to read from disc "+0+": missing EOF.");
	                break;
		        }
            }
            readyList.pushBack(curr_process);
		break;
		case 15: // CONSOLE INT
			handTerminalInt();
		break;
		
		case 32:
            // EXIT PROCESS INT
            //
            killRunningProcess(cpuid);
        break;
            
		case 34:
	        // OPEN FILE INT
	        //
	        curr_REGS = pro[cpuid].getReg();
	        
	        if( (curr_REGS[0]==0 || curr_REGS[0]==1) && (curr_REGS[1]==0 || curr_REGS[1]==1)) {
                curr_process = procLists[cpuid].getFront();
                curr_file = curr_process.addFile(mem);
                curr_file.open(curr_REGS[0]==0 ? curr_file.FILEMODE_R : curr_file.FILEMODE_W, curr_REGS[1], curr_REGS[2]);
	        }
	        else
                System.err.println("Error opening file: invalid parameters.");
        break;
            
    	case 35: // CLOSE
            curr_REGS = pro[cpuid].getReg();
            curr_process = procLists[cpuid].getFront();
            
            curr_file = curr_process.getFile(curr_REGS[0]);
            curr_file.close();
            curr_process.removeFile(curr_REGS[0]);
        break;
            
    case 36: // GET
        curr_REGS = pro[cpuid].getReg();
        curr_process = readyList.getFront();
        
        curr_file = curr_process.getFile(curr_REGS[0]);
        curr_REGS = curr_file.get();
        pro[cpuid].setReg(curr_REGS);
    break;

    case 37: // PUT
        //TODO
    break;
            
    case 46: // PRINT
        curr_REGS = pro[cpuid].getReg();
        System.out.println((curr_REGS[0]>>>24) + " " + ((curr_REGS[0]>>>16)&255) + " " + ((curr_REGS[0]>>>8)&255) + " " + (curr_REGS[0]&255));
    break;
            
    default:
        System.err.println("Unknown interrupt: " + interruptNumber);		
	}
	
	// restore context
	getContext(cpuid);
	}
}
