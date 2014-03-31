package edu.cornell.audioProbe;



import android.content.Context;
import android.util.Log;

/**
 * This is a generic circular buffer class for storing and retrieving audio data for processing
 */
public class CircularBufferFeatExtractionInference<T> {
	
	private int qMaxSize;// max queue size
	private int fp = 0;  // front pointer
	private int rp = 0;  // rear pointer
	private int qs = 0;  // size of queue
	private T[] q;    // actual queue
	//private static Ml_Toolkit_Application appState;
	private T[] tempQ;

	//thread to write in the database
	private Thread t; 

	private static final String TAG = "XYY_QUEUE";	


	@SuppressWarnings("unchecked")
	/**
	 * Initialize circular buffer 
	 * @param context, application context, not used in the current version..
	 * @param size, what is the size of the buffer
	 */
	public CircularBufferFeatExtractionInference(Context context, int size) {
		qMaxSize = size;
		fp = 0;
		rp = 0;
		qs = 0;
		q = (T[]) new Object[qMaxSize];
		tempQ = (T[]) new Object[1] ;
	}

	
	
	/**
	 * pop an element for the buffer
	 * @return
	 */
	public T delete() {
		if (!emptyq()) {
			//will not decrease size to avoid race condition
			qs--;
			fp = (fp + 1)%qMaxSize;
			return q[fp];
		}
		else {
			return null;
		}
	}

	
	/**
	 * push an element from the buffer
	 * @return
	 */
	public synchronized void insert(T c) { 
		//insert case; if the queue is full then we will just skip.
		//this is not a typical producer consumer where if queue is full we 
		//we just stop producing. Data will always be produced by sensors. 
		//We just cannot stop it. So, if anytime the queue is full then
		// we just drop the samples. 
		//
		//If we forcefully put the new element forcefully in the queue the sequence will be broken.
		//
		//There is no reason to put the thread who is calling to sleep (or "wait") because we don't have space
		//And the calling thread is the sensing thread. Stopping it will be disasterous.
		//
		//tempQ[0] = c;
		if (!fullq()) {
			qs++;
			rp = (rp + 1)%qMaxSize;
			q[rp] = c;
			
			//Calling feature extaction or inference thread 
			notify(); 

			
		}
		
		
		else
			Log.d(TAG, "Frame dropped for a full buffer" );

	}
	

	/**
	 * delete and provide a lock (so that we are not reading when there is no data) for the delete data
	 * @return returns the popped object
	 */
	public synchronized T deleteAndHandleData() {
		
		if(emptyq())
		{
			try {
				notifyAll(); // this is needed to activate freeCMemory thread 
				wait();
			} catch(InterruptedException e) {				
			}
		}
		
		return delete();
	}
	
	
	
	/**
	 * Can't understand why I wrote this code ???
	 */
	public synchronized void freeCMemory()
	{
		Log.e("Going for stop", "free C memory" );
		if(!emptyq())
		{
			try {
				Log.e("Going for stop", "Not empty yet free C memory" );
				wait(); // wait because there is more data to process. We will wait until it becomes empty
			} catch(InterruptedException e) {
			}
		}
			
	}

	/**
	 * Returns whether the queue is empty
	 * @return
	 */
	public boolean emptyq() {
		return qs == 0;
	}

	/**
	 * Returns whether the queue is full
	 * @return
	 */
	public boolean fullq() {
		return qs == qMaxSize;
	}

	/**
	 * Returns the size of the queue
	 * @return
	 */	
	public int getQSize() {
		return qs;
	}



	

}