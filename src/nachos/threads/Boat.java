package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat
{
    static BoatGrader bg;
    
    private static int nChildrenOnOahu;
    private static int nChildrenOnMolokai;
    private static int nAdultsOnOahu;
    private static int nAdultsOnMolokai;
    private static int numWaitForBoat = 0;
    
    private static Lock lockOahu = new Lock();
    private static Lock lockMolokai = new Lock();
    private static Condition2 oahuAdults = new Condition2(lockOahu);
    private static Condition2 oahuWaitOnLand = new Condition2(lockOahu);
    private static Condition2 oahuWaitOnBoat = new Condition2(lockOahu);
    private static Condition2 molokaiAdults = new Condition2(lockMolokai);
    private static Condition2 molokaiWaitOnLand = new Condition2(lockMolokai);
    private static Condition2 molokaiWaitOnBoat = new Condition2(lockMolokai);
    
    private static final boolean OAHU = true;
    private static final boolean MOLOKAI = false;
    private static boolean boat = OAHU;
    
   
    private static Semaphore finish = new Semaphore(0);
   // private static int numWaitForBoatOahu = 0;   //number of children waiting on 
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	//sSystem.out.println("\n ***Testing Boats with only 2 children***");
	//begin(0, 2, b);

	//System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	begin(1, 2, b);

  	//System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(0, 5, b);
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
		
		nAdultsOnOahu = adults;
		nChildrenOnOahu = children;
		nAdultsOnMolokai = 0;
		nChildrenOnMolokai = 0;
		
		KThread[] threadAdults = new KThread[adults];
		KThread[] threadChildren = new KThread[children];
		for (int i = 0; i < adults; i ++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			(threadAdults[i] = new KThread(r).setName("Adult(" + i + ")")).fork();
		}
		
		for (int i = 0; i < children; i ++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			(threadChildren[i] = new KThread(r).setName("Child(" + i + ")")).fork();
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
		lockOahu.acquire();
		while (nChildrenOnOahu > 1 || boat == MOLOKAI)
			oahuAdults.sleep();
			//oahu.adultWait();
		bg.AdultRowToMolokai();
		nAdultsOnOahu --;
		boat = MOLOKAI;
		lockOahu.release();
		//let a child to pilot the boat back
		lockMolokai.acquire();
		
		nAdultsOnMolokai ++;
		molokaiWaitOnLand.wake();
		lockMolokai.release();
		//wake a child on Molokai
    }

    static void ChildItinerary()
    {
    	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
    	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
    	while (nAdultsOnOahu + nChildrenOnOahu > 1) {
	    	lockOahu.acquire();
	    	//System.out.println(oahu.getNumOfChildren());
	    	if (nChildrenOnOahu == 1) {
	    		oahuAdults.wake();
	    		oahuWaitOnLand.sleep();
	    	}
	    	while (numWaitForBoat >= 2 || boat == MOLOKAI)
	    		oahuWaitOnLand.sleep();
	    	//while (oahu.getNumOfChildrenWaitForBoat() >= 2 || oahu.isBoatHere() == false) {
	    	//	oahu.childWaitOnLand();
	    	//}
	    	
	    	if (numWaitForBoat == 0) {
	    		numWaitForBoat ++;
	    		bg.ChildRowToMolokai();
	    		oahuWaitOnLand.wake();
	    		oahuWaitOnBoat.sleep();
	    		lockOahu.release();
	    		lockMolokai.acquire();
	    		//bg.ChildRideToMolokai();
	    		//nChildrenOnMolokai ++;
	    		molokaiWaitOnLand.sleep();
	    	} else {
	    		numWaitForBoat ++;
	    		oahuWaitOnBoat.wake();
	    		bg.ChildRideToMolokai();
	    		nChildrenOnOahu -= numWaitForBoat;
	    		numWaitForBoat = 0;
	    		boat = MOLOKAI;
	    		lockOahu.release();
	    		lockMolokai.acquire();
	    		nChildrenOnMolokai += 2;
	    		
	    	}
	    	nChildrenOnMolokai --;
	    	
	    	lockMolokai.release();
	    	lockOahu.acquire();
	    	bg.ChildRowToOahu();
	    	boat = OAHU;
	    	nChildrenOnOahu ++;
	    	lockOahu.release();
	    	
    	}
    	if (nChildrenOnOahu > 0) {
    		lockOahu.acquire();
    		bg.ChildRowToMolokai();
    		nChildrenOnOahu --;
    		lockOahu.release();
    		lockMolokai.acquire();
    		nChildrenOnMolokai++;
    		boat = MOLOKAI;
    		lockMolokai.release();
 
    	}
    	System.out.println("Island Oahu: #Children = " + nChildrenOnOahu + ", #Adults = " + nAdultsOnOahu);
    	System.out.println("Island Molokai: #Children = " + nChildrenOnMolokai + ", #Adults = " + nAdultsOnMolokai);
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
