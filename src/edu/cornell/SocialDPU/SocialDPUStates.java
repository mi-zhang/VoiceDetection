package edu.cornell.SocialDPU;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.impl.StackObjectPool;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import edu.cornell.SocialDPU.ServiceControllers.ServiceController;
import edu.cornell.SocialDPU.ServiceControllers.AudioLib.AudioService;
import edu.cornell.SocialDPU.ServiceControllers.AudioLib.MyPhoneStateListener;
import edu.cornell.SocialDPU.Storage.CircularBuffer;
import edu.cornell.SocialDPU.Storage.ML_toolkit_object;
import edu.cornell.SocialDPU.Storage.MyDBAdapter;
import edu.cornell.SocialDPU.Storage.MySDCardStateListener;




/**
 * This class is the state class. This class is accessible from all contexts possible under the same process
 * We store a lot of configuration variables from this application class
 * System wide different statuses are also available here.
 * Quite a lot of initialization happens here
 * @author shuva
 *
 */
public class SocialDPUStates {
	
	
	public static Context context; //holds application context
	private ServiceController serviceController;

	public NotificationManager mNM;

	// ???
	public final int ACCEL_SAMPLERATE = 32;
	public final int ACCEL_SAMPLES_REQUIRED = 128;
	
	// audio sample and inference configurations
	public final int AUDIO_SAMPLES_REQUIRED = 512; 
	public final int AUDIO_SAMPLERATE = 8000; // 8KHz

	// circular buffer to store data
	public CircularBuffer<ML_toolkit_object> ML_toolkit_buffer;

	// databse adapter
	public MyDBAdapter db_adapter;
	// database primary key
	public int database_primary_key_id;

	// write after how many entries, used in CircularBuffer
	public final int writeAfterThisManyValues = 200;
	public final int maximumDbSize = 6*1000000;

	//write to a file	
	public String currentFileName;
	public List<String> fileList;

	//upload
	public boolean uploadRunning;

	//flags to see whether sensors in turned on/off	
	public boolean audioSensorOn;
	public boolean accelSensorOn;
	public boolean locationSensorOn;

	//
	public boolean phone_call_on;

	//phone state object
	public MyPhoneStateListener phoneCallInfo;

	//sdcard state listener
	public MySDCardStateListener mySDCardStateInfo; 

	//application started once ... to ensure that services started only once
	//needed for Pending notifications in the main screen 
	public boolean applicatin_starated;

	//IMEI
	public String IMEI;

	//audio release true or false;
	public boolean audio_release = false;

	//raw audio flag
	public boolean rawAudioOn;

	//audio force locked
	public boolean audioForceLocked;


	//saved sensors in turned on/off	
	public boolean savedAudioSensorOn;
	public boolean savedAccelSensorOn;
	public boolean savedLocationSensorOn;
	public boolean savedRawSensorOn;

	//time zone offset
	public long timeOffset; 

	//current number of records
	public long accel_no_of_records;
	public long audio_no_of_records;
	public String location_text;

	//service pointers
	public AudioService audioService;
	public OutputStream currentOpenSDCardFile;

	//SD card mounted
	public boolean SDCardMounted;
	public boolean fileListInitialzied = false;
	
	//counter for next restart/stop of services
	public static int DayCounter = 0;

	public MlToolkitObjPool mMlToolkitObjectPool;

	//start and end of last conversation
	public long lastConversationStartTime=0;
	public long lastConversationEndTime=0;
	
	//inferred status of audio
	public int inferred_audio_Status = 0;
	
	private String  database_path;

	
	
	public SocialDPUStates(SocialDPUApplication appState, String database_path) {
		
		
		context = appState;
		
		//initiate a service controller class
		serviceController = new ServiceController(context);

		//set database path 
		this.database_path = database_path;

		//phone call running
		phone_call_on = false;

		//set application started to false. This will turned on when services get started
		applicatin_starated = false;

		//files currently available
		fileList = new ArrayList<String>();

		//IMEI
		IMEI = "dummy";

		//raw audio on flag
		rawAudioOn = false;

		// if necessary then service controller will start them and fix the values and 
		// that will be determined by the current state
		audioSensorOn = false;
		accelSensorOn = false;
		locationSensorOn = false;

		//audio force locked (can be initiated if a call is ongoing)
		audioForceLocked = false;

		//number of records
		this.accel_no_of_records = 0;
		this.audio_no_of_records = 0;
		this.location_text = "";

		//pointers to service
		this.audioService = null;

		//SD card file
		currentOpenSDCardFile = null;

		//SD card mounted
		this.SDCardMounted = false;

		//initiate the subject pool for 
		mMlToolkitObjectPool = new MlToolkitObjPool();
		
		//initialize toolkit directory
		intiializFiles();
				
	}
	
	
	/**
	 * Get the current list of files in the external sdcard
	 */
	public void getFileList() {
		// TODO Auto-generated method stub
		File folder = new File(this.database_path);
		File[] listOfFiles = folder.listFiles();

		if(listOfFiles!=null){//if sd card is not avaiable
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					//System.out.println("File " + listOfFiles[i].getName());
					try {
						fileList.add(listOfFiles[i].getCanonicalPath());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
				} 
			}
		}
	}

	
	/** 
	 * Create the place where data will be stored
	 */
	public void intiializFiles() {
		// TODO Auto-generated method stub
		File f = new File(this.database_path);
		if(f.exists() == false) {
			f.mkdirs();
			Log.i("FAILED", "f.exists() == false ");
		}				
	}
		
	
	/**
	 * Start a new database when the program starts.
	 */
	public void initializeDataBase() {
		//delete broken data base files
		deleteBrokenDbrFileInInternalMemory();

		// TODO Auto-generated method stub
		this.database_primary_key_id = 0;
		long my_time = java.lang.System.currentTimeMillis();
		my_time  = (long)(my_time /= 1000);
		//
		db_adapter = new MyDBAdapter(context,  my_time + "_" + IMEI + ".dbr");	
	}


	/**
	 * Delete corrupted database files in the internal memory
	 */
	private void deleteBrokenDbrFileInInternalMemory() {
		// TODO Auto-generated method stub
		String d = "/data/data/edu.dartmouthcs.mltoolkit/databases/";
		String e = ".dbr";
		ExtensionFilter filter = new ExtensionFilter(e);
		File dir = new File(d);

		String[] list = dir.list(filter);
		File file;
		if (list == null || list.length == 0) 
			return;

		for (int i = 0; i < list.length; i++) {
			file = new File(d + list[i]);
			file.delete();
		}

		Log.w("DELETEDD", "FILE Deleted " + list.length);

	}

	class ExtensionFilter implements FilenameFilter {

		private String extension;

		public ExtensionFilter( String extension ) {
			this.extension = extension;             
		}
		public boolean accept(File dir, String name) {
			return (name.endsWith(extension));
		}
	}

	
	/**
	 * A helper function to get service controller class
	 * @return
	 */
	public ServiceController getServiceController() {
		return this.serviceController;
		//serviceController = new ServiceController(context);
	}


	/**
	 * Reads the configuration flags for turning on or off sensors. If configuration file is not available then, this function will create one  
	 */
	public void initialize() {
		
		Properties prop = new Properties();
		String fileName = "mltoolkit_states.config";

		//create the circular buffer for storing data
		ML_toolkit_buffer = new CircularBuffer<ML_toolkit_object>(context, 6000); //circular buffer size
				
		InputStream is;
		
		try {
			is = context.openFileInput(fileName);
			prop.load(is);
			this.savedAudioSensorOn = Boolean.parseBoolean(prop.getProperty("Audio"));
			this.audioForceLocked = !this.savedAudioSensorOn;//because this like the setting in the sensor screen

			this.savedAccelSensorOn = Boolean.parseBoolean(prop.getProperty("Accel"));
			this.savedLocationSensorOn = Boolean.parseBoolean(prop.getProperty("Location"));
			rawAudioOn = this.savedRawSensorOn = Boolean.parseBoolean(prop.getProperty("Raw_Audio"));	


		} catch (Exception e) {
			//means file doesn't exist
			//start writing the files
			
			Log.i("INFO", "i am here ...");
			
			this.savedAudioSensorOn = true;
			this.savedAccelSensorOn = false;
			this.savedLocationSensorOn = false;
			this.savedRawSensorOn = true;
			writeToPropertiesFile(this.savedAudioSensorOn, this.savedAccelSensorOn, this.savedLocationSensorOn, this.savedRawSensorOn);
		}
	}

	/**
	 * Writes new settings for configuration files.
	 */
	public void writeToPropertiesFile(boolean savedAudioSensorOn2,
			boolean savedAccelSensorOn2, boolean savedLocationSensorOn2,
			boolean savedRawSensorOn2) {
		
		Properties prop = new Properties();
		String fileName = "mltoolkit_states.config";
		OutputStream outs;
		try {
			outs = context.openFileOutput(fileName,context.MODE_PRIVATE);
			prop.setProperty("Audio", ""+ savedAudioSensorOn2);
			prop.setProperty("Accel", ""+ savedAccelSensorOn2);
			prop.setProperty("Location", ""+ savedLocationSensorOn2);
			prop.setProperty("Raw_Audio", ""+ savedRawSensorOn2);
			prop.store(outs, "ML Toolkit COnfig file");
		} catch (Exception e) {
			e.printStackTrace();			
			Log.w("FAILED", "FILE WRITE FAILED " + e.toString());
		}
	}
	

	/**
	 * The following class is used for efficient reuse of space since we are creating new datatypes, and we don't want to use proportional space as data
	 * Efficient use ensures that garbage collector don't gets called often.
	 * @author shuva
	 *
	 */
	class MlToolkitObjectFactory extends BasePoolableObjectFactory {
		public Object makeObject() {
			return new ML_toolkit_object();
		}
	}

	
	/**
	 * The following class is used for efficient reuse of space since we are creating new datatypes, and we don't want to use proportional space as data
	 * Efficient use ensures that garbage collector don't gets called often.
	 * @author shuva
	 *
	 */
	public class MlToolkitObjPool extends StackObjectPool {
		public MlToolkitObjPool() {
			super(new MlToolkitObjectFactory());
			try {
				// Ensure that there are at least 200 ML_toolkit objects ready
				// to use
				PoolUtils.prefill(this, 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public ML_toolkit_object borrowObject() {
			try {
				return (ML_toolkit_object) super.borrowObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void returnObject(Object obj) {
			try {
				super.returnObject(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


}