package nachos.threads;

import java.util.PriorityQueue;
import java.util.Comparator;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		
		boolean intStatus = Machine.interrupt().disable();
		while(!pq.isEmpty() && pq.peek().wakeTime <= Machine.timer().getTime()){
				//cancel the timer
				cancel(pq.peek());
				//place the thread back on ready queue
				pq.poll().ready();
					
		}	
		Machine.interrupt().restore(intStatus);
		//System.out.println("Current Thread line is: " + KThread.currentThread().getName());
		KThread.currentThread().yield();	
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		boolean intStatus = Machine.interrupt().disable();

		if(x > 0){
			long wakeTime = Machine.timer().getTime() + x;
			KThread.currentThread().wakeTime = wakeTime; 
			pq.add(KThread.currentThread());
			KThread.sleep();
		}
		Machine.interrupt().restore(intStatus);
		
		/*
		while (wakeTime > Machine.timer().getTime())
			KThread.yield();
		*/
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
  public boolean cancel(KThread thread) {
		if(thread.wakeTime == 0){
			return true;
		}
		thread.wakeTime = 0;
		
		return false;
	}
	
	public PriorityQueue<KThread> pq = new PriorityQueue<KThread>(10, new Comparator<KThread>() {
    public int compare(KThread k1, KThread k2) {
				return (int) (k1.wakeTime -  k2.wakeTime);
    }
});

 public static void alarmTest1() {
	int durations[] = {1000, 10*1000, 100*1000};
	long t0, t1;

	for (int d : durations) {
	    t0 = Machine.timer().getTime();
	    ThreadedKernel.alarm.waitUntil (d);
	    t1 = Machine.timer().getTime();
	    System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
	}
    }

    // Implement more test methods here ...

    // Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
    public static void selfTest() {
			System.out.println ("alarmTest1 TESTING");
			alarmTest1();

	// Invoke your other test methods here ...
    }
}
