package edu.cornell.SocialDPU.ServiceControllers;

import edu.cornell.SocialDPU.SocialDPUApplication;
import edu.cornell.SocialDPU.ServiceControllers.AudioLib.AudioService;
import edu.cornell.SocialDPU.Storage.SDCardStorageService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

/**
 * This class 
 * @author shuva
 *
 */

public class ServiceController {
	
	
	private static final String TAG = "Sensor_turn_on_or_off";
	
	// application context (used as a holder for global variables accessible to all activities and services)
	private final SocialDPUApplication applicationCONTEXT;
		
	public ServiceController() {
		
		applicationCONTEXT = null;
	}
	
	public ServiceController(Context con) {
		
		applicationCONTEXT = (SocialDPUApplication)con;
	}

	
	/**
	 * Start audio service
	 */
	public void startAudioSensor() {
		
		Intent intent = new Intent(AudioService.ACTION_FOREGROUND);
		intent.setClass(this.applicationCONTEXT, AudioService.class);
		this.applicationCONTEXT.startService(intent);
		this.applicationCONTEXT.dpuStates.audioSensorOn = true;
		//Log.i("MiCheck", "startAudioSensor() ...");
	}
	
	
	/**
	 * Stop audio service
	 */
	public void stopAudioSensor() {
		
        Intent intent = new Intent(AudioService.ACTION_FOREGROUND);
        intent.setClass(this.applicationCONTEXT, AudioService.class);
        this.applicationCONTEXT.stopService(intent);
        this.applicationCONTEXT.dpuStates.audioSensorOn = false;
	}	
	
	
	/**
	 * Start the SD card service. This service copies internal memory sql-lite database into external memory
	 * @param db_path, database path in the internal memory
	 */
	public void startSDCardStorageService(String db_path) {
		
		Intent intent = new Intent(this.applicationCONTEXT,SDCardStorageService.class);
		intent.putExtra("dbpath", db_path);
		this.applicationCONTEXT.startService(intent);
	}
	

	
}