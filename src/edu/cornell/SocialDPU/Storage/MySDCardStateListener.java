package edu.cornell.SocialDPU.Storage;

import java.util.Timer;
import java.util.TimerTask;

import edu.cornell.SocialDPU.SocialDPUApplication;
import edu.cornell.SocialDPU.ServiceControllers.ServiceController;
import edu.cornell.SocialDPU.Storage.ML_toolkit_object;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * This class initially created to handle cases of sd card mounting
 * Newer versions of android don't let users mount sd card, so this not a problem anymore
 * @author shuva
 *
 */
public class MySDCardStateListener {

	//public int battery_level;
	//public int charging_state;
	//public String wifi_status;	
	private Context Activity_Context;
	private SocialDPUApplication appState;
	//private ServiceController serviceController; 


	private static final String TAG = "Phone_state_listener: ";

	/**
	 * Initialize a broadcast listener
	 * @param Activity_Context, the context from which this is called. Typically this will be the main activity 
	 * @param mlobj, application class to update the variables
	 */
	public MySDCardStateListener(Context Activity_Context, SocialDPUApplication mlobj)
	{
		this.Activity_Context = Activity_Context;
		this.appState = mlobj;

		IntentFilter intentFilter = new IntentFilter(); 
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED); 
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED); 
		intentFilter.addDataScheme("file"); 
		intentFilter.addDataAuthority("*", null); 
		this.appState.registerReceiver(SDCardStateReceiver, intentFilter);

		Log.i("SD card-examplee", "initiated ");
	}

	/**
	 * Sdcard state lister (a broadcast receiver)
	 */
	private final BroadcastReceiver SDCardStateReceiver = new BroadcastReceiver() {
		@Override 
		public void onReceive(Context context, Intent intent) { 
			try { 
				//When there is no sd card 
				if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)){ 
					Log.i("SD cardddddddddddd", "Unmounted");

					appState.dpuStates.SDCardMounted = true;
					if(appState.dpuStates.currentOpenSDCardFile != null) //stop writing to SDcard
						appState.dpuStates.currentOpenSDCardFile.close();

					Thread.sleep(2000);

					//stop the audio sensor since there is nothing to write on (sdcard gone)
					appState.dpuStates.getServiceController().stopAudioSensor();


				}else 
					//sd card is back
					if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){ 
						Log.i("SD cardddddddddddddd", "Mounted");

						appState.dpuStates.SDCardMounted = false;

						//start the audio sensor, since we can write to something now
						appState.dpuStates.getServiceController().startAudioSensor();

					} 
					else{

					}
			} catch (Exception e) { 
				Log.i("SD cardddddddddddd", e.toString()); 
			} 
		}
	};





}