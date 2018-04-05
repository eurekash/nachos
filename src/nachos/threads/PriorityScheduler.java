package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);
	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
    	PriorityQueue(boolean transferPriority) {
    		this.transferPriority = transferPriority;
    	}

    	public void waitForAccess(KThread thread) {
    	//	System.out.println(this.toString() + " waitForAccess " + thread.getName());
    	//	print();
    		Lib.assertTrue(Machine.interrupt().disabled());
    		ThreadState state = getThreadState(thread);
    		//if (holder == state)    //special case for ready queue
    		//	holder = null;
    		state.waitForAccess(this);
    	//	System.out.println("acquire()");
		//	print();
    	//	print();
    	//	System.out.println("waitFor Access end.");
    	}

    	public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (holder != null) {
				holder.release(this);
				holder = null;
			}
			getThreadState(thread).acquire(this);
		//	System.out.println("acquire()");
		//	print();
    	}

		public KThread nextThread() {
		//	System.out.println("nextThread");
		//	print();
		    Lib.assertTrue(Machine.interrupt().disabled());
		    // implement me
		    
		    if (this.holder != null) {
		    	this.holder.release(this);
		    }
		    ThreadState state = pickNextThread();
		    if (state == null)  return null;
		    waitQueue.remove(state);
		    state.acquire(this);
		   // System.out.println("nextThread()");
		   // print();
		   	return state.thread;
		}
		
		protected void donate() {
			Lib.assertTrue(this.holder != null);
			//this.holder.resetEffectivePriority();
			this.effectivePriority = -1;
			for (ThreadState current: waitQueue ) {
				int temp = current.getEffectivePriority();
				if (this.effectivePriority < temp)
					this.effectivePriority = temp;
				//this.holder.updateEffectivePriority(current.getEffectivePriority());
			}
		}
		

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		

		protected ThreadState pickNextThread() {
		    // implement me
			ThreadState result = null;
			for (ThreadState current: waitQueue) {
				if (result == null || result.getEffectivePriority() < current.getEffectivePriority()) {
					result = current;
				}
			}
			
			return result;
		}
		
		public void print() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    
		    // implement me (if you want)
		    if (holder != null) {
		    	System.out.println("queue holder = " + holder.thread.toString() + "(" + holder.priority + "," + holder.effectivePriority + ")");
		    }
		    System.out.println("------------------begin------------------");
		    for (ThreadState ts: waitQueue) {
		    	System.out.println(ts.thread.toString() + "(" + ts.priority + "," + ts.effectivePriority + ")");
		    }
		    System.out.println("------------------end------------------");
		}
		
		public void setHolder(KThread th) {
			holder = getThreadState(th);
		}
	
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		protected ThreadState holder = null;
		protected int effectivePriority = -1;
		private LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
		    this.thread = thread;
		    this.waitFor = null;
		    setPriority(priorityDefault);
		}
	
		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
		    return priority;
		}
	
		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
		    // implement me
		    return effectivePriority;
		}
	
		protected void donate() {
			ThreadState current = this;
			
			while (current.waitFor != null && current.waitFor.transferPriority) {
				current.waitFor.donate();
				ThreadState next = current.waitFor.holder;
				next.update();
				current = next;
			}
			//System.out.println(current.waitFor.holder.thread.getName());
			//int eff = current.effectivePriority;
			//while (current.waitFor != null && current.waitFor.transferPriority) {
			//	//System.out.println(current.thread.getName());
			//	ThreadState next = ((PriorityQueue) current.waitFor).holder;
			//	if (next == null)  break;
			//	next.updateEffectivePriority(eff);
			//	current = next;
			//}
			
		}
		
		protected void update() {
			this.effectivePriority = this.priority;
			for (PriorityQueue res: this.holdResource) {
				if (this.effectivePriority < res.effectivePriority)
					this.effectivePriority = res.effectivePriority;
			}
		}
		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
		    if (this.priority == priority)
			return;
		    
		    this.priority = priority;
		    this.effectivePriority = priority;
		    // implement me
		    this.donate();
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
		    // implement me
			this.waitFor = waitQueue;
			if (!waitQueue.waitQueue.contains(this))
    			waitQueue.waitQueue.add(this);
			this.donate();
		}
	
		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			//System.out.println("Thread " + this.thread.getName() + " acquire (" + waitQueue.toString() + ")");
		    // implement me
			this.waitFor = null;
			waitQueue.holder = this;
			this.holdResource.add(waitQueue);
			//System.out.println("Change holder " + this.thread.getName());
			this.donate();
		}	
		
		public void release(PriorityQueue waitQueue) {
			this.holdResource.remove(waitQueue);
			this.donate();
		}
	
		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;	
		protected PriorityQueue waitFor = null;
		protected LinkedList<PriorityQueue> holdResource = new LinkedList<PriorityQueue>();
    }
    
    
    private static class T1 implements Runnable {
    	Lock lock;
    	int id;
    	T1(Lock l, int id) {
    		lock = l;
    		this.id = id;
    	}
    	public void run() {
    		//KThread.yield();
    		lock.acquire();
    		boolean intStatus = Machine.interrupt().disable();
    		ThreadedKernel.scheduler.setPriority(1);
    		Machine.interrupt().restore(intStatus);
    		for (int i = 0; i < 1000; i++)
    			KThread.yield();
    		lock.waitList("lock1");
    		lock.release();
    	}
    }
    
    private static class T2 implements Runnable {
    	Lock lock1, lock2;
    	int id;
    	int reset;
    	T2(Lock l1,Lock l2, int id, int reset) {
    		lock1 = l1;
    		lock2 = l2;
    		this.id = id;
    		this.reset = reset;
    	}
    	public void run() {
    		lock2.waitList("lock2");
    		boolean intStatus = Machine.interrupt().disable();
    		ThreadedKernel.scheduler.setPriority(reset);
    		Machine.interrupt().restore(intStatus);
    		lock2.acquire();
    		
    		KThread.yield();
    		lock1.acquire();
    		KThread.yield();
    		lock1.waitList("lock1");
    		lock1.release();
    		lock2.waitList("lock2");
    		lock2.release();
    	}
    }
  
    
    private static class Thread2 implements Runnable {
    	Lock lock;
    	int id;
    	Thread2(Lock l, int id) {
    		lock = l;
    		this.id = id;
    	}
    	public void run() {
    		System.out.println("type2 " + id + " acquiring lock1");
    		lock.acquire();
    		lock.waitList("lock1");
    		KThread.yield();
    		System.out.println("type2 " + id + " acquire lock.");
    		System.out.println("type2 " + id + " releasing lock1");
    		lock.release();
    	}
    }
    
    public static void selfTest() {
    	Lock lock1 = new Lock();
    	Lock lock2 = new Lock();

    	
  
    	KThread t1, t2, t3, t4;
    	boolean intStatus;
    	intStatus = Machine.interrupt().disable();
    	
    	t1 = new KThread(new T1(lock1,1)).setName("t1");
    	ThreadedKernel.scheduler.setPriority(t1,7);
    	
    	t2 = new KThread(new T1(lock1,2)).setName("t2");
    	ThreadedKernel.scheduler.setPriority(t2,4);
    
    	t3 = new KThread(new T2(lock1,lock2,3,1)).setName("t3");
    	ThreadedKernel.scheduler.setPriority(t3,6);
  
    	t4 = new KThread(new T2(lock1,lock2,4,5)).setName("t4");
    	ThreadedKernel.scheduler.setPriority(t4,4);
    	
    	Machine.interrupt().restore(intStatus);
    	t1.fork();
    	t2.fork();
    	t3.fork();
    	t4.fork();

    	t1.join();
    	t2.join();
    	t3.join();
    	t4.join();
    }
}
