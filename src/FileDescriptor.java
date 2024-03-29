import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;

class FileDescriptor {
    private Memory mem;
    private ProcessDescriptor proc;
    
    private int address;
    private int disk;
    private int mode;
    private int size;
    private int readerPos;
    private int id;
    
    public final int FILEMODE_W = 0;
    public final int FILEMODE_R = 1;
    
    public FileDescriptor(int i, ProcessDescriptor p, Memory m) {
        id = i;
        mem = m;
        proc = p;
    }
    
    public void open(int m, int d, int add) {
        mode = m;
        address = add;
        disk = d;
        readerPos = 0;
        
        //TODO
        if(mode==FILEMODE_W) {
                size = 0;
        }
        
        else {
                
        }
    }

    public int[] get() {
        //TODO
        int[] args = new int[2];
        
        return args;
    }
    
    public void put(int data) {
        //TODO
    }
    
    public void close() {
        //TODO
    }

}
