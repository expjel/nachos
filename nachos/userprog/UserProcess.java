package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.HashMap;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();

		UserKernel.processLock.acquire();
		pID = UserKernel.pidCounter;	
		UserKernel.pidCounter++;
		UserKernel.numProcesses++;
		UserKernel.processLock.release();

		fdTable[0] = UserKernel.console.openForReading();
		fdTable[1] = UserKernel.console.openForWriting();

		status = -1;
		parent = null;
		childrenExitStatus = new HashMap<>();
		children = new HashMap<>();
		// for (int i = 0; i < numPhysPages; i++)
		// 	pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return 	readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
		//System.out.println("RVM params: " + vaddr + " " + offset + " " + length + " " + data.length);
		//System.out.println("RVM param test 1");
		if(data == null || data.length == 0){
			return 0;
		}
		
		//System.out.println("RVM param test 2");
		//System.out.println("before fail params: " + " " + offset + " " + length + " " + data.length);
	
		if(!(offset >= 0 && length >= 0 && offset + length <= data.length))
			return 0;
		
			

		//System.out.println("allocating memory" + Machine.processor().getMemory());
		byte[] memory = Machine.processor().getMemory();

		//System.out.println("RVM param test 3");
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;
		//System.out.println("RVM param tests passed");

		int vpn = Processor.pageFromAddress(vaddr);
		int vpnOffset = Processor.offsetFromAddress(vaddr);
		TranslationEntry entry = pageTable[vpn];
		entry.used = true;
		int actualAddr = entry.ppn * pageSize + vpnOffset;

		//test actual Physical Addr
		if (actualAddr < 0 || actualAddr >= memory.length || entry.valid == false)
			return 0;

		int totalBytesWritten = 0;
		int bufferOffset = offset;
		int pageOffset = vpnOffset;
		int leftToWrite = length;
		int currAddr = actualAddr;
		int currPPN = entry.ppn;
		int currVPN = vpn;

		//System.out.println("About to Read VM, total length to write in is: " + length);
		//write bytes into array, each iteration should be of at most pageSize
		while(totalBytesWritten < length){ //while there are still bytes left to write
			//write in pageSize chunks (length - totalbyteswritten = amount left )
			if(leftToWrite + pageOffset > pageSize){ 
				int amount = Math.min(length, pageSize - pageOffset); //calc amount then use in copy
				System.arraycopy(memory, currAddr, data, bufferOffset, amount);
				//calc offsets
				bufferOffset += amount;
				totalBytesWritten += amount;
				leftToWrite -= amount;
				currVPN++; //increment to next page, check if it's within bounds

				if(currVPN < pageTable.length){ 
					//set used bit of prev (current) page to false, 
					//used bit is set to true whenever a file is being read/write by user, set to false after writing
					pageTable[currVPN -1].used = false; 
					//entry = pageTable[currVPN - 1]; //advance entry to next, break out if invalid
					entry = pageTable[currVPN];

					if(!entry.valid) 
						break;
					//updating variables to current entry, page offset 
					entry.used = true;
					currPPN = entry.ppn;
					currAddr = entry.ppn*pageSize;
					pageOffset = 0; //
					
				}else{ 
					break; //if currvpn is out of bounds, break
				}

			}else{ //less than pageSize left, final iteration
				
				
				leftToWrite = Math.min(leftToWrite, pageSize - pageOffset); 
				//System.out.println("Last iteration in RVM, curr amount is: " + leftToWrite);
				System.arraycopy(memory, currAddr, data, bufferOffset, leftToWrite);
				bufferOffset += leftToWrite;
				totalBytesWritten += leftToWrite;
				pageTable[currVPN].used = false;
				//at this point totalbytes written should be equal to length
			}

		}
		//System.out.println("Total bytes written: " + totalBytesWritten);
		return totalBytesWritten;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		//System.out.println("WVM params: " + vaddr + " " + offset + " " + length + " " + data.length);
		// Lib.assertTrue(offset >= 0 && length >= 0
		// 		&& offset + length <= data.length);
		//System.out.println("WVM param test 1");
		if(data == null || data.length == 0)
			return 0;
		//	System.out.println("WVM param test 2");
		if(!(offset >= 0 && length >= 0 && offset + length <= data.length))
			return 0;
	
		byte[] memory = Machine.processor().getMemory();
	//	System.out.println("WVM param test 3");
		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		//System.out.println("WVM param passed");
		int vpn = Processor.pageFromAddress(vaddr);
		int vpnOffset = Processor.offsetFromAddress(vaddr);
		TranslationEntry entry = pageTable[vpn];
		entry.used = true;
		int actualAddr = entry.ppn * pageSize + vpnOffset;

		//test actual Physical Addr
	//	System.out.println("WVM param phys addr test");
		if (actualAddr < 0 || actualAddr >= memory.length || entry.valid == false)
			return 0;
		
		int totalBytesRead = 0;
		int bufferOffset = offset;
		int pageOffset = vpnOffset;
		int leftToRead = length;
		int currAddr = actualAddr;
		int currPPN = entry.ppn;
		int currVPN = vpn;

		//System.out.println("About to write VM, total length to read in is: " + length);
		while(totalBytesRead < length){
		
			//length - totalBytesRead + pageOffset = current bytes left to read
			//initially TBR = 0 so it's length + pageOffset > pagesize
			if(leftToRead + pageOffset > pageSize){
				int amount = Math.min(length, pageSize - pageOffset); 
				System.arraycopy(data, bufferOffset, memory, currAddr, amount);

				bufferOffset += amount;
				totalBytesRead += amount;
				leftToRead -= amount;
				currVPN++; //increment to next page, check if it's within bounds

				if(currVPN < pageTable.length){
					pageTable[currVPN -1].used = false; 
					entry = pageTable[currVPN];

					if(!entry.valid) //check entry
						break;

					//update VPN and addr for next entry
					entry.used = true;
					currPPN = entry.ppn;
					currAddr = entry.ppn*pageSize;
					pageOffset = 0; //
				}else{
					break;
				}
			}else{
				leftToRead = Math.min(leftToRead, pageSize - pageOffset); 
				//System.out.println("Last iteration in WVM, curr amount is: " + leftToRead);
				System.arraycopy(data, bufferOffset, memory, currAddr, leftToRead);
				bufferOffset += leftToRead;
				totalBytesRead += leftToRead;
				pageTable[currVPN].used = false;
				//at this point totalbytes written should be equal to length	
			}
			
		}
		
		//System.out.println("totalBytesRead is: " + totalBytesRead);
		return totalBytesRead;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
		
		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		//init page table with numPages
		pageTable = new TranslationEntry[numPages];

		//fill the table
		for (int i = 0; i < numPages; i++)
		 	pageTable[i] = new TranslationEntry(i, i, true, false, false, false);

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) { //for each coff section (which is a page long), get it's VPN 
				int vpn = section.getFirstVPN() + i;

				//acquire lock to access UK ppnTable
				UserKernel.processLock.acquire();
				
				//find first available ppnUsed space, store as ppn
				//int ppn = UserKernel.getFirstUnused();
				if(UserKernel.ppnUsed.size() == 0){
					unloadSections();
					UserKernel.processLock.release();
					return false;
				}
				int ppn = UserKernel.ppnUsed.removeFirst();
				//release lock
				UserKernel.processLock.release();

				//update pageTable entry with ppn, valid bit, and readonly stat
				pageTable[vpn].ppn = ppn;
				//pageTable[vpn].valid = true;
				pageTable[vpn].readOnly = section.isReadOnly();

				//section load page
				section.loadPage(i, ppn);
			}
		}

		//load in stack + argument pages
		final int stackAndArg = 9;
		for(int i = numPages - stackAndArg; i < numPages; i++){
			//acquire lock to access UK ppnTable
				UserKernel.processLock.acquire();

				//find first available ppnUsed space, store as ppn
				//if none, unload all pages and return error && release lock
				if(UserKernel.ppnUsed.size() == 0){
					unloadSections();
					UserKernel.processLock.release();
					return false;
				}
				int ppn = UserKernel.ppnUsed.removeFirst();
				
				//System.out.println("Stack args, current index is: " + i + " and current ppn is: " + ppn );
				UserKernel.processLock.release();

				pageTable[i].ppn = ppn;
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		//for each section in the pagetable
		for(int i = 0; i < pageTable.length; i++){
			//if it is valid (in use), free the pages, set it to not valid 
			if(pageTable[i].valid == true){
				pageTable[i].valid = false; //set to not valid
				//engage lock
				UserKernel.processLock.acquire();
				UserKernel.ppnUsed.add(pageTable[i].ppn);
				UserKernel.processLock.release();
			}
		}
	
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		if(pID == 0){
			Machine.halt();
		}
		
		//Lib.assertNotReached("Machine.halt() did not halt machine!, not PID");
		return -1;
	}

	/**
	 * Handle the exit() system call.
	 */

	 /*
	private int handleExit(int status) {
	        // Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.

		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
		// for now, unconditionally terminate with just one process
		Kernel.kernel.terminate();

		return 0;
	}
*/
	private int handleCreat(int vaName){
		
		int fileDesc = 0;
		while(fdTable[fileDesc] != null && fileDesc < 16){
			fileDesc++;
		}

		if (fileDesc < 16) {
			String name = readVirtualMemoryString(vaName, 256);
			if (name != null) {
				OpenFile tempFile = ThreadedKernel.fileSystem.open(name, true);
				//OpenFile tempFile = Machine.stubFileSystem().open(name, true);
				if (tempFile != null) {
					fdTable[fileDesc] = tempFile;
					//System.out.println("Handle Creat success, file Desc is: " + fileDesc);
					return fileDesc;
				} 
			}
		}
		//System.out.println("Handle Creat failed, file Desc is: " + fileDesc);
		return -1;
	}

	private int handleOpen(int vaName){
		int fileDesc = 0;
		while(fdTable[fileDesc] != null && fileDesc < 16){
			fileDesc++;
		}

		if (fileDesc < 16) {
			String name = readVirtualMemoryString(vaName, 256);
			if (name != null) {
				OpenFile tempFile = ThreadedKernel.fileSystem.open(name, false);
				// OpenFile tempFile = Machine.stubFileSystem().open(name, false);
				if (tempFile != null) {
					fdTable[fileDesc] = tempFile;
					//System.out.println("Handle Open success, file Desc is: " + fileDesc);
					return fileDesc;
				} 
			}
		}
		//System.out.println("Handle Open failed, file Desc is: " + fileDesc);
		return -1;
	}

	private int handleClose(int fileDescriptor){
		if(fileDescriptor < 16 && fileDescriptor >= 0){
			if(fdTable[fileDescriptor] != null){
				fdTable[fileDescriptor].close();
				fdTable[fileDescriptor] = null;
				return 0;
			}
		}
		return -1;
	}

	private int handleUnlink(int vaName){
		String name = readVirtualMemoryString(vaName, 256);
			if (name != null) {
				//if(ThreadedKernel.fileSystem.remove(name) == true){
				if(Machine.stubFileSystem().remove(name) == true){
					return 0;
				}
			}
		return -1;
	}

	private int handleRead(int fileDescriptor, int userBuffer, int count){
		if(fileDescriptor < 0 || fileDescriptor > 15 || userBuffer <= 0 || count < 0){
			 System.out.println("read params failed, user buffer is: " + userBuffer + " count is: " + count + " fd is: " + fileDescriptor);
			return -1;
		}

		byte[] localBuffer = new byte[pageSize];
		//total bytes read counter
		int totalBytes = 0;
		if(fdTable[fileDescriptor] == null){
			 System.out.println("null file");
			return -1;
		}

			while(count >= pageSize){
				int read = fdTable[fileDescriptor].read(localBuffer, 0, pageSize);
				if(read != -1 
					&& writeVirtualMemory(userBuffer, localBuffer, 0, read) == read){

					userBuffer += read; //Advance userBuffer
					totalBytes += read; //count total bytes
					count -= pageSize;
				}else{
					System.out.println("breakpoint 1");
					return -1;
				}
			}

			int read = fdTable[fileDescriptor].read(localBuffer, 0, count);
			if(read != -1 && writeVirtualMemory(userBuffer, localBuffer, 0, read) == read){

						totalBytes += read;
						System.out.println("read successful, totalBytes is: " + totalBytes);
						return totalBytes;
						//our read is reaching EoF, but instead of returning 0 bytes read it is conintuously returning 1 byte read. How do we ensure that it stops reading when reaching EoF??
			}
	//	}
		System.out.println("read failed at end");
		return -1;
		//else, return -1
	}

	int handleWrite(int fileDescriptor, int userBuffer, int count){
		if(fileDescriptor < 0 || fileDescriptor > 15 || userBuffer <= 0 || count < 0){
			return -1;
		}

		byte[] localBuffer = new byte[pageSize];

		int totalBytes = 0;
		if(fdTable[fileDescriptor] == null){
			return -1;
		}

			//read into open file into localbuffer, write local buffer to userbuffer
			while(count >= pageSize){
			
				if(readVirtualMemory(userBuffer, localBuffer, 0, pageSize) == pageSize 
					&& fdTable[fileDescriptor].write(localBuffer, 0, pageSize) != -1){
				
					userBuffer += pageSize; //Advance userBuffer
					totalBytes += pageSize; //count total bytes
					count -= pageSize;
				}else{
					return -1;
				}
			}

	
			if(readVirtualMemory(userBuffer, localBuffer, 0, count) == count) { 
					if(fdTable[fileDescriptor].write(localBuffer, 0, count) != -1){
						totalBytes += count;
						return totalBytes;
					}
			}
		System.out.println("write failed at end");
		return -1;
	}

	public int handleExec(int file, int argc, int argv){
		//Sanity check for arguments, none should be <0
		if(file < 0 || argc < 0 || argv < 0){
			System.out.println("Args failed");
			return -1;
		}
		//use ReadVirtualMemoryString to extract
		//file should end in .coff, should be non-null
		String fileName = readVirtualMemoryString(file, 256);
		if(fileName == null){
			System.out.println("FileName failed");
			return -1;
		}	

		if(fileName.length() < 6 || !fileName.substring(fileName.length() - 5).equals(".coff")){
			return -1;
		}
		//load in argv, create an array to hold arguments, pass each argv into argument
		//argv size should be equal to argc, each element should be non-null

		
		String[] args = new String[argc]; 
		byte[] buffer = new byte[argc*4];
		if(readVirtualMemory(argv, buffer) != 4*argc){
			return -1;
		}

		for(int i = 0; i < argc; i++){
			int argAddress = Lib.bytesToInt(buffer, i*4);

			if(argAddress < 0 || readVirtualMemoryString(argAddress, 256) == null){
				return -1;
			}
			args[i] = readVirtualMemoryString(argAddress, 256);
		}

		//create new process
		UserProcess newChild = newUserProcess();
		//keep track of parent + child relationships
		this.children.put(newChild.pID, newChild);
		newChild.parent = this;
		childrenExitStatus.put(newChild.pID, -1);
		//exec new process with filename and arguments
		if(newChild.execute(fileName, args)){
		
			//return child PID if pass
			return newChild.pID;
		}
		//return -1 if failed
		return -1;
	}

	public int handleJoin(int processID, int statusAddr){
		if(statusAddr < 0 || statusAddr > Machine.processor().getMemory().length-4){
			return -1;
		}

		if(!children.containsKey(processID)){
			return -1; 
		}
		children.get(processID).thread.parent = this.thread;
		children.get(processID).thread.join();
		int exitStatus = childrenExitStatus.get(processID);
		
		
		if(exitStatus == 1){
			writeVirtualMemory(statusAddr, Lib.bytesFromInt(exitStatus));
			return 1;
		}else{
			return 0;
		}

	}

	//
	public void handleExit(int status){
		Machine.autoGrader().finishingCurrentProcess(status);
		System.out.println("The status is:" + status);
		if(parent != null){
			this.parent.childrenExitStatus.put(this.pID, status);
		}
	

		for(int i = 0; i < fdTable.length; i++){
			handleClose(i);
		}

		unloadSections();

		coff.close();

		UserKernel.processLock.acquire();
		UserKernel.numProcesses--;
		if(UserKernel.numProcesses == 0){
			Kernel.kernel.terminate();
		}
		UserKernel.processLock.release();

		KThread.finish();
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {

		 System.out.println("Current Syscall is: " + syscall);

		switch (syscall) {
	  case syscallHalt:
				return handleHalt();
		case syscallExit:
				handleExit(a0);
		case syscallOpen:
				return handleOpen(a0);
		case syscallClose:
				return handleClose(a0);
		case syscallUnlink:
				return handleUnlink(a0);
		case syscallRead:
				return handleRead(a0,a1,a2);
		case syscallWrite:
				return handleWrite(a0,a1,a2);
		case syscallCreate:
				return handleCreat(a0);
		case syscallExec:
				return handleExec(a0, a1, a2);
		case syscallJoin:
				return handleJoin(a0, a1);
		
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
        protected UThread thread;
    
	private int initialPC, initialSP;

	private int argc, argv;

	public OpenFile fdTable[] = new OpenFile[16];	

	public int pID;

	public UserProcess parent;

	public int status;	

	public HashMap<Integer, Integer> childrenExitStatus;

	public HashMap<Integer, UserProcess> children;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';
}
