package edu.cornell.SocialDPU.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import edu.cornell.SocialDPU.SocialDPUApplication;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


/**
 * This is a worker class service. Every time a file is created this new service will be created
 * But this will wait if an earlier version is waiting to be executed,
 * Every file operation is done in a new thread.
 * @author shuva
 *
 */
public class SDCardStorageService extends IntentService {


	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];

	private static final String TAG = "XY_QUEUE_SD";
	private SocialDPUApplication appState;
	private boolean inputFileDeleteFlag = true; 

	public String UploadFileLocation;
	public SDCardStorageService() {
		super("SD card storage Service");
		// TODO Auto-generated constructor stub
	}


	@Override
	public void onCreate() { 
		super.onCreate(); 
		appState = (SocialDPUApplication)getApplicationContext();
		Log.i(TAG, "SD card copier started" );
	} 

	/**
	 * Copies a database file from the internal memory to external memory
	 * @param db_path, database path in the internal memory
	 */
	private void postData(String db_path) {  


		Log.i(TAG, "db_path :" +  db_path);

		InputStream in = null;
		appState.dpuStates.currentOpenSDCardFile = null; 
		int sep =  db_path.lastIndexOf("/");;
		try {
			in = new FileInputStream(db_path);
			//sep 
			appState.dpuStates.currentOpenSDCardFile = new FileOutputStream(appState.database_path+db_path.substring(sep + 1));
			while (true) {
				//synchronized (buffer) {
				int amountRead = in.read(buffer);
				if (amountRead == -1) {	
					//means file have been copied
					appState.dpuStates.fileList.add(appState.database_path+db_path.substring(sep + 1));
					appState.dpuStates.currentOpenSDCardFile.close();
					appState.dpuStates.currentOpenSDCardFile = null;
					Log.d(TAG, "file copied" );

					break;
				}
				appState.dpuStates.currentOpenSDCardFile.write(buffer, 0, amountRead); 
			} 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error" + e.toString() );
			this.inputFileDeleteFlag = false; //means sd card is not available
			
		}catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error" + e.toString() );

			//in the middle of file writing file to SD card, SD card was mounted
			//do not delete the input file
			this.inputFileDeleteFlag = false;
		} 
		finally {
			if (in != null) {
				try {
					in.close();
					
					//delete the file from main memory to save space
					if(this.inputFileDeleteFlag){ 
						//means there was a SD card failure so don't delete the files
						File f = new File(db_path);
						f.delete();
						Log.e(TAG, "Error deleted:" + db_path);
					}					
					else //means there was a mal-funciton in copying the files. So, let us not keep file (we are not coming back to it)... We just lost a minute
					{
						File f = new File(db_path);
						f.delete();
						Log.e(TAG, "Error deleted:" + db_path);
					}
				} catch (IOException e) {

				}
			}
			if (appState.dpuStates.currentOpenSDCardFile  != null) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "Starting Copy" + intent.getStringExtra("dbpath"));
		//function handles the database path sent
		postData(intent.getStringExtra("dbpath"));
	}  

}