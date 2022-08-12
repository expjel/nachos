package nachos.threads;

import nachos.machine.*;

import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

/**
 * A <i>GameMatch</i> groups together player threads of the same
 * ability into fixed-sized groups to play matches with each other.
 * Implement the class <i>GameMatch</i> using <i>Lock</i> and
 * <i>Condition</i> to synchronize player threads into groups.
 */
public class GameMatch {
    
    /* Three levels of player ability. */
    public static final int abilityBeginner = 1,
	abilityIntermediate = 2,
	abilityExpert = 3;
    public int matchNumber;
    public int lobbySize;

    public int begSize;
    public int intSize;
    public int advSize;

    public Lock lock;

    public Condition cvBeg;
    public Condition cvInt;
    public Condition cvAdv;
    
    //<Thread , Match Number>
    public HashMap<KThread, Integer> MatchMap = new HashMap<KThread, Integer>();
    public Queue<KThread> begQueue = new LinkedList<>();
    public Queue<KThread> intQueue = new LinkedList<>();
    public Queue<KThread> advQueue = new LinkedList<>();

    /**
     * Allocate a new GameMatch specifying the number of player
     * threads of the same ability required to form a match.  Your
     * implementation may assume this number is always greater than zero.
     */
    public GameMatch (int numPlayersInMatch) {
        matchNumber = 0;
        lobbySize = numPlayersInMatch;
        begSize = 0;
        intSize = 0;
        advSize = 0;
        lock = new Lock();
        // final Condition empty = new Condition(lock);
        cvBeg = new Condition(lock);
        cvInt = new Condition(lock);
        cvAdv = new Condition(lock);

    
    }

    /**
     * Wait for the required number of player threads of the same
     * ability to form a game match, and only return when a game match
     * is formed.  Many matches may be formed over time, but any one
     * player thread can be  assigned to only one match.
     *
     * Returns the match number of the formed match.  The first match
     * returned has match number 1, and every subsequent match
     * increments the match number by one, independent of ability.  No
     * two matches should have the same match number, match numbers
     * should be strictly monotonically increasing, and there should
     * be no gaps between match numbers.
     * 
     * @param ability should be one of abilityBeginner, abilityIntermediate,
     * or abilityExpert; return -1 otherwise.
     */
    public int play (int ability) {
        int currMatchNum = 0;
        //ability beginner
        if(ability == abilityBeginner){
            lock.acquire();
            begSize++;
            if(begSize == lobbySize){ //lobby filled
                matchNumber++;
                currMatchNum = matchNumber;
                begSize = 0;
                while(!begQueue.isEmpty()){
                    MatchMap.put(begQueue.poll(), currMatchNum);
                }
                cvBeg.wakeAll();
            }else{ //lobby still below capacity
                begQueue.add(KThread.currentThread());
                cvBeg.sleep();
                currMatchNum = MatchMap.get(KThread.currentThread());
            }
            lock.release();

        //ability intermediate
        }else if(ability == abilityIntermediate){
            lock.acquire();
            intSize++;
            if(intSize == lobbySize){ //lobby filled
                matchNumber++;
                currMatchNum = matchNumber;
                intSize = 0;
                while(!intQueue.isEmpty()){
                    MatchMap.put(intQueue.poll(), currMatchNum);
                }
                cvInt.wakeAll();
            }else{ //lobby still below capacity
                intQueue.add(KThread.currentThread());
                cvInt.sleep();
                currMatchNum = MatchMap.get(KThread.currentThread());
            }
            lock.release();

        //ability expert
        }else if(ability == abilityExpert){
            lock.acquire();
            advSize++;
            if(advSize == lobbySize){ //lobby filled
                matchNumber++;
                currMatchNum = matchNumber;
                advSize = 0;
                while(!advQueue.isEmpty()){
                    MatchMap.put(advQueue.poll(), currMatchNum);
                }
                cvAdv.wakeAll();
            }else{ //lobby still below capacity
                advQueue.add(KThread.currentThread());
                cvAdv.sleep();
                currMatchNum = MatchMap.get(KThread.currentThread());
            }
            lock.release();
        }else{
            return -1;
        }

        return currMatchNum;       
    }

        // Place GameMatch test code inside of the GameMatch class.

    public static void matchTest4 () {
	final GameMatch match = new GameMatch(2);

	// Instantiate the threads
	KThread beg1 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityBeginner);
		     System.out.println ("beg1 matched, match number is " + r);
		    // beginners should match with a match number of 1
		    //Lib.assertTrue(r == 1, "expected match number of 1");
		}
	    });
	beg1.setName("B1");

	KThread beg2 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityBeginner);
		    System.out.println ("beg2 matched, match number is " + r);
		    // beginners should match with a match number of 1
		    //Lib.assertTrue(r == 1, "expected match number of 1");
		}
	    });
	beg2.setName("B2");

    KThread beg3 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityBeginner);
		    System.out.println ("beg3 matched, match number is " + r);
		    // beginners should match with a match number of 1
		    //Lib.assertTrue(r == 2, "expected match number of 2");
		}
	    });
	beg3.setName("B3");

    KThread beg4 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityBeginner);
		    System.out.println ("beg4 matched, match number is " + r);
		    // beginners should match with a match number of 1
		    //Lib.assertTrue(r == 2, "expected match number of 2");
		}
	    });
	beg4.setName("B4");

	KThread int1 = new KThread( new Runnable () {
		public void run() {
		   	int r = match.play(GameMatch.abilityIntermediate);
            System.out.println ("int1 matched, match number is " + r);
		    // beginners should match with a match number of 1
		}
	    });
	int1.setName("I1");

    KThread int2 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityIntermediate);
            System.out.println ("int2 matched, match number is " + r);
		    // beginners should match with a match number of 1
		}
	    });
	int2.setName("I2");

	KThread exp1 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityExpert);
		    System.out.println ("exp1 matched, match number is " + r);
		    // beginners should match with a match number of 1
		}
	    });
	exp1.setName("E1");

    KThread exp2 = new KThread( new Runnable () {
		public void run() {
		    int r = match.play(GameMatch.abilityExpert);
		    System.out.println ("exp2 matched, match number is " + r);
		    // beginners should match with a match number of 1
		}
	    });
	exp2.setName("E2");

	// Run the threads.  The beginner threads should successfully
	// form a match, the other threads should not.  The outcome
	// should be the same independent of the order in which threads
	// are forked.
    beg1.fork();
    beg2.fork();
    beg3.fork();
    beg4.fork();
	int1.fork();
    int2.fork();
	exp1.fork();
    exp2.fork();



	// Assume join is not implemented, use yield to allow other
	// threads to run
	for (int i = 0; i < 10; i++) {
	    KThread.currentThread().yield();
	}
    }
    
    public static void selfTest() {
	matchTest4();
    }
}
