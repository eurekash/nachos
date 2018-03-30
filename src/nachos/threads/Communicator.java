package nachos.threads;

import java.util.Random;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	
    	while (this.word != null) {
    		waitSpeakers.sleep();
    	}
    	
    	Lib.assertTrue(this.word == null);
    
    	this.word = new Integer(word);
    	
    	if (readyListener) {
    		ready.wake();
    	} else {
    		ready.sleep();
    	}
    	
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	lock.acquire();
    	
    	while (readyListener) {
    		waitListeners.sleep();
    	}
    	
    	readyListener = true;
    	
    	if (this.word != null) {
    		ready.wake();
    	} else {
    		ready.sleep();
    	}
    	
    	int result = this.word.intValue();
    	this.word = null;
    	
    	readyListener = false;
    	waitSpeakers.wake();
    	waitListeners.wake();
    	lock.release();
    	return result;
    }
    
    private static class Speaker implements Runnable {
    	private Communicator comm;
    	private Alarm alarm;
    	private int id;
    	private Random random = new Random();
    	Speaker(Communicator comm, Alarm alarm, int id) {
    		this.comm = comm;
    		this.alarm = alarm;
    		this.id = id;
    	}
    	public void run() {
    		alarm.waitUntil(100);
			System.out.println("Speak " + id);
			//KThread.yield();
			comm.speak(id);
    	}
    }
    
    private static class Listener implements Runnable {
    	private Communicator comm;
    	private Alarm alarm;
    	private Random random = new Random();
    	Listener(Communicator comm, Alarm alarm) {
    		this.comm = comm;
    		this.alarm = alarm;
    	}
    	public void run() {
    		//KThread.yield();
    		alarm.waitUntil(random.nextInt(100));
    		System.out.println("Listen " + comm.listen());
    	}
    }
    
    public static void selfTest() {
    	Communicator comm = new Communicator();
    	Alarm alarm = new Alarm();
    	System.out.println("Communicator selftest");
  
    	int n = 10;
    	KThread[] speaker = new KThread[n];
    	KThread[] listener = new KThread[n];
    	for (int i = 0; i < n; i ++) {
    		speaker[i] = new KThread(new Speaker(comm, alarm, i)).setName("speaker " + i);
    		speaker[i].fork();
    		listener[i] = new KThread(new Listener(comm, alarm)).setName("listener " + i);
    		listener[i].fork();
    	}
    	
    	
    	for (int i = 0; i < n; i++) {
    		speaker[i].join();
    		listener[i].join();
    	}
    }
    
    private Lock lock = new Lock();
    private Condition2 waitListeners = new Condition2(lock);
    private Condition2 waitSpeakers  = new Condition2(lock);
    private Condition2 ready = new Condition2(lock);
    
    private Integer word = null;
    private boolean readyListener = false;
}
