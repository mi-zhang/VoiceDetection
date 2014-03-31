package edu.cornell.SocialDPU.Storage;

import java.util.ArrayList;
import java.util.List;
import edu.cornell.SocialDPU.SocialDPUApplication;
import android.content.Context;


/**
 * This is a generic circular buffer class for storing and retriving for data storage and reading
 */
public class CircularBuffer<T> {
	private int qMaxSize;// max queue size
	private int fp = 0;  // front pointer
	private int rp = 0;  // rear pointer
	private int qs = 0;  // size of queue
	private T[] q;    // actual queue
	private static SocialDPUApplication appState;


	//thread to write in the database
	private Thread t; 
	private String db_path;
	private List<T> tempList;
	private MyFileWriter myWriterThread;
	

	@SuppressWarnings("unchecked")
	/**
	 * Initialize circular buffer 
	 * @param context, application context, not used in the current version..
	 * @param size, what is the size of the buffer
	 */
	public CircularBuffer(Context context, int size) {
		qMaxSize = size;
		fp = 0;
		rp = 0;
		qs = 0;
		//q = new char[qMaxSize];
		q = (T[]) new Object[qMaxSize];
		appState = (SocialDPUApplication) context;
		tempList = new ArrayList<T>();
		
		//start the writer thread
		myWriterThread = new MyFileWriter(this);
		myWriterThread.start();
	}

	
	/**
	 * pop an element for the buffer
	 * @return element inserted
	 */
	public T delete() {
		if (!emptyq()) {

			//will not decrease size to avoid race condition
			qs--;


			fp = (fp + 1)%qMaxSize;
			return q[fp];
		}
		else {
			//System.err.println("Underflow");
			return null;
		}
	}

	
	/**
	 * push an element from the buffer
	 * @return
	 */
	public synchronized void insert(T c) {
		if (!fullq()) {
			qs++;
			rp = (rp + 1)%qMaxSize;
			q[rp] = c;


			//avoid race condition
			//only will start writing when buffer has writeAfterThisManyValues elements
			if(qs == appState.dpuStates.writeAfterThisManyValues)
			{
				notifyAll(); // start the write thread
			}


		}
		//else
		//System.err.println("Overflow\n");
	}



	public boolean emptyq() {
		return qs == 0;
	}

	public boolean fullq() {
		return qs == qMaxSize;
	}

	public int getQSize() {
		return qs;
	}

	public void printq() {
		System.out.print("Size: " + qs +
				", rp: " + rp + ", fp: " + fp + ", q: ");
		for (int i = 0; i < qMaxSize; i++)
			System.out.print("q[" + i + "]=" 
					+ q[i] + "; ");
		System.out.println();
	}




	//wrtier thread
	public class MyFileWriter extends Thread {

		CircularBuffer<T> obj;

		public MyFileWriter(CircularBuffer<T>  obj)
		{
			this.obj=obj;
		}

		@Override
		public void run() {
			while(true) {
				obj.writeToFile();
			}

		}

	}

	/**
	 * Pops element for the buffer and starts writing the to the database
	 * Creates a new database if file size exceeds the limit
	 * SDCardManager now will take care of these
	 */
	public synchronized void writeToFile() {
		// TODO Auto-generated method stub

		if(appState.dpuStates.db_adapter == null) return;
		
		//means that buffer doesn't yet have appState.writeAfterThisManyValues elements so sleep
		if(qs <= appState.dpuStates.writeAfterThisManyValues)
		{
			try {//lock to ensure that we don't start writing while half of the buffer is full
				wait();
			} catch(InterruptedException e) {
			}
		}

		//start writing into a list		
		for(int i = 0; i < appState.dpuStates.writeAfterThisManyValues; ++i){
			if(appState.dpuStates.db_adapter.database_online != false)
				appState.dpuStates.db_adapter.insertMltObj((Object)delete());//inserting all the objects
		}
		

		//check if the file is already oversized, if it is then change the filename		
		if((appState.dpuStates.db_adapter.getDbSize()) > appState.dpuStates.maximumDbSize)
		{
			try{
				
				db_path = appState.dpuStates.db_adapter.getPathOfDatabase();
				appState.dpuStates.db_adapter.close();
				appState.dpuStates.database_primary_key_id = 0;
				long my_time = java.lang.System.currentTimeMillis();
				my_time  = (long)(my_time /= 1000);
				appState.dpuStates.db_adapter = new MyDBAdapter(appState, my_time + "_" + appState.dpuStates.IMEI + ".dbr");
				appState.dpuStates.db_adapter.open();
				
				//copy the file to SD card
				appState.dpuStates.getServiceController().startSDCardStorageService(db_path);
			}
			catch(Exception ex){
			}
		}
	}

}