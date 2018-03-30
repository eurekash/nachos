package nachos.threads;

import java.util.PriorityQueue;
//import java.util.TreeSet;
import java.util.Random;

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
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	
    boolean intStatus = Machine.interrupt().disable();
    while (!waitQueue.isEmpty() && waitQueue.peek().time() <= Machine.timer().getTime()) 
    	waitQueue.poll().thread().ready();
	KThread.yield();
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	//long wakeTime = Machine.timer().getTime() + x;
	//while (wakeTime > Machine.timer().getTime())
	//    KThread.yield();
    	
    boolean intStatus = Machine.interrupt().disable();
    
    long wakeTime = Machine.timer().getTime() + x;
    waitQueue.add(new Pair(wakeTime, KThread.currentThread()));
    KThread.sleep();
    Machine.interrupt().restore(intStatus);
    
    }
    
    private class Pair implements Comparable {
    	public Pair(long time, KThread thread) {
    		this.time = time;
    		this.thread = thread;
    	}
    	
    	public long time() {
    		return time;
    	}
    	
    	public KThread thread() {
    		return thread;
    	}
    	
    	public int compareTo(Object o) {
    		Pair p = (Pair) o;
    		
    		long diff = this.time - p.time;
    		if (diff < 0)
    			return -1;
    		else if (diff > 0)
    			return 1;
    		else
    			return this.thread.compareTo(p.thread);
    	}
    	
    	private long time;
    	private KThread thread;
    }
    
    private static class PingTest implements Runnable {
    	PingTest(Alarm alarm, int id, long delay) {
    		this.alarm = alarm;
    		this.id = id;
    		this.delay = delay;
    	}
    	
    	public void run() {
    		for (int i = 0; i < 100; i ++) {
    		long current = Machine.timer().getTime();
    		alarm.waitUntil(delay);
    		System.out.println("Thread " + id + " now: " + Machine.timer().getTime() + " ideal: " + (current + delay));
    		}
    	}
    	
    	Alarm alarm;
    	int id;
    	long delay;
    }
    
    public static void selfTest() {
    	Random random = new Random();
    	Alarm alarm = new Alarm();
    	for (int i = 1; i <= 100; i ++) {

    		KThread th = 
    		new KThread(new PingTest(alarm, i, random.nextInt(5000000))).setName(new Integer(i).toString());
    		th.fork();
    	}
    	new PingTest(alarm, 0, random.nextInt(5000000)).run();
    }
    
    private PriorityQueue<Pair> waitQueue = new PriorityQueue<Pair> ();
}
