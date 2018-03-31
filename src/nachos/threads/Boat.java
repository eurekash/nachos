package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat
{
    static BoatGrader bg;
    
    private static class Island {
    	int numChildren;
    	int numAdults;
    	boolean boat;
    	Lock lock;
    	Condition2 adult, waitOnLand, waitOnBoat;
    	int numWaitForBoat = 0;
    	public Island(int numChildren, int numAdults, boolean boat) {
    		this.numChildren = numChildren;
    		this.numAdults = numAdults;
    		this.boat = boat;
    		this.lock = new Lock();
    		adult = new Condition2(this.lock);
    		waitOnLand = new Condition2(this.lock);
    		waitOnBoat = new Condition2(this.lock);
    	}
    	
    	public void acquire() {
    		lock.acquire();
    	}
    	
    	public void release() {
    		lock.release();
    	}
    	
    	public void childWaitOnLand() {
    		waitOnLand.sleep();
    	}
    	
    	public void notifyChildOnLand() {
    		waitOnLand.wake();
    	}
    	
    	public void childRide() {
    		Lib.assertTrue(numWaitForBoat == 0);
    		numWaitForBoat ++;
    		notifyChildOnLand();
    		waitOnBoat.sleep();
    	}
    	
    	public void childRow() {
    		Lib.assertTrue(numWaitForBoat < 2);
    		numWaitForBoat ++;
    		waitOnBoat.wake();
    	}
    	
    	public void adultWait() {
    		adult.sleep();
    	}
    	
    	public void notifyAdult() {
    		adult.wake();
    	}
    	
    	public int getNumOfChildren() {
    		return numChildren;
    	}
    	
    	public int getNumOfAdults() {
    		return numAdults;
    	}
    	
    	public void childrenLeave() {
    		Lib.assertTrue(numChildren >= numWaitForBoat && boat);
    		numChildren -= numWaitForBoat;
    		numWaitForBoat = 0;
    		boat = false;
    	}
    	
    	public void childrenArrive(int add) {
    		Lib.assertTrue(boat == false);
    		numChildren += add;
    		boat = true;
    	}
    	
    	public void adultLeave() {
    		Lib.assertTrue(numAdults > 0 && boat);
    		numAdults --;
    		boat = false;
    	}
    	
    	public void adultArrive() {
    		Lib.assertTrue(!boat);
    		numAdults ++;
    		boat = true;
    	}
    	
    	public boolean isBoatHere() {
    		return boat;
    	}
    	
    	public void reverse() {
    		boat = !boat;
    	}
    	
    	public int getNumOfChildrenWaitForBoat() {
    		return this.numWaitForBoat;
    	}
    }
    
    private static Island oahu = null, molokai = null;
    private static boolean OAHU = false;
    private static boolean MOLOKAI = true;
    private static Semaphore finish = new Semaphore(0);
   // private static int numWaitForBoatOahu = 0;   //number of children waiting on 
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

//	Runnable r = new Runnable() {
//	    public void run() {
//                SampleItinerary();
//            }
//        };
//        KThread t = new KThread(r);
//        t.setName("Sample Boat Thread");
//        t.fork();
	
		oahu = new Island(adults, children, true);
		molokai = new Island(0, 0, false);

		for (int i = 0; i < adults; i ++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			new KThread(r).setName("Adult(" + i + ")").fork();
		}
		
		for (int i = 0; i < children; i ++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			new KThread(r).setName("Child(" + i + ")").fork();
		}
		
		finish.P();
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
		oahu.acquire();
		while (oahu.getNumOfChildren() > 1 || oahu.isBoatHere() == false)
			oahu.adultWait();
		oahu.adultLeave();
		oahu.release();
		bg.AdultRowToMolokai();
		//let a child to pilot the boat back
		molokai.acquire();
		molokai.adultArrive();
		molokai.notifyChildOnLand();
		molokai.release();
		//wake a child on Molokai
    }

    static void ChildItinerary()
    {
    	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
    	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
    	while (oahu.getNumOfAdults() + oahu.getNumOfChildren() > 1) {
	    	oahu.acquire();
	    	if (oahu.getNumOfChildren() == 1) {
	    		oahu.notifyAdult();
	    	} 
	    	while (oahu.getNumOfChildrenWaitForBoat() >= 2 || oahu.isBoatHere() == false) {
	    		oahu.childWaitOnLand();
	    	}
	    	if (oahu.getNumOfChildrenWaitForBoat() == 0) { //become rider
	    		bg.ChildRideToMolokai();
	    		oahu.childRide();
	    		oahu.release();
	    		molokai.acquire();
	    		molokai.childWaitOnLand();
	    	} else { 	//rower
	    		bg.ChildRowToMolokai();
	    		oahu.childRow();
	    		oahu.childrenLeave();
	    		oahu.release();
	    		molokai.acquire();
	    		molokai.childrenArrive(2);
	    	}
	    	molokai.childRow();
    		molokai.childrenLeave();
    		bg.ChildRowToOahu();
    		molokai.release();
    		oahu.acquire();
    		oahu.childrenArrive(1);
    		oahu.release();
    	}
    	oahu.acquire();
    	bg.ChildRowToMolokai();
    	oahu.childRide();
    	oahu.childrenLeave();
    	oahu.release();
    	molokai.acquire();
    	molokai.childrenArrive(1);
    	molokai.release();
    	System.out.println("Island Oahu: #Children = " + oahu.getNumOfChildren() + ", #Adults = " + oahu.getNumOfAdults());
    	System.out.println("Island Molokai: #Children = " + molokai.getNumOfChildren() + ", #Adults = " + molokai.getNumOfAdults());
    	finish.V();
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
