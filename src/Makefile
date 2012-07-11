JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		ConsoleListener.java\
		GlobalSynch.java\
		Kernel.java\
		MyWin.java\
		ProcessList.java\
		Semaphore.java\
		SopaInterface.java\
		Timer.java\
		Disk.java\
		IntController.java\
		Memory.java\
		ProcessDescriptor.java\
		Processor.java\
		SlaveListener.java\
		SOPA.java


default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
