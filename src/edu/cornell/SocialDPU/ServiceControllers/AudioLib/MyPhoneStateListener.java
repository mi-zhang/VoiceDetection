package edu.cornell.SocialDPU.ServiceControllers.AudioLib;

import java.util.Timer;
import java.util.TimerTask;

import edu.cornell.SocialDPU.SocialDPUApplication;
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
 * This class makes sure to stop sensing when there is a phone call
 * IMPROVEMENT: For all kinds of requests..
 * @author shuva
 *
 */
public class MyPhoneStateListener {
	
	private Context Activity_Context;
	private SocialDPUApplication appState;


	private static final String TAG = "Phone_state_listener: ";

	/**
	 * Register for listeners for incoming and outgoing calls
	 * @param Activity_Context
	 * @param mlobj
	 */
	public MyPhoneStateListener(Context Activity_Context, SocialDPUApplication mlobj) {
		
		
		this.Activity_Context = Activity_Context;
		this.appState = mlobj;


		IntentFilter intentFilter = new IntentFilter("android.intent.action.PHONE_STATE");
		this.appState.registerReceiver(phoneStateReceiver, intentFilter);


		this.appState.registerReceiver( outgoingCallReceiver, new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL) );
		this.appState.dpuStates.phone_call_on = false;
		Log.i("telephony-example", "initiated ");
		
	}

	/**
	 * Handles jointly incoming/(partially)outgoing call case. Stops audio sensing in that case.
	 * Here the strategy is to stop audio sensing when the phone is ringing.
	 * Start sensing back when the phone is idle. This case works for both outgoing call ending.
	 */	
	private final BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive( Context context, Intent intent )			
		{
			String extra = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);

			if(extra.equals(android.telephony.TelephonyManager.EXTRA_STATE_OFFHOOK))//means call running
			{
				Log.i("telephony-example", "call underway ... ");
			}

			if(extra.equals(android.telephony.TelephonyManager.EXTRA_STATE_RINGING))//means call running
			{
				//strategy if the phone is ringing then stop the audio service
				Log.i("telephony-example", "call ringing ... ");
				if(appState.dpuStates.audioSensorOn == true)//if sensor is on then turn it off 
				{
					Log.i("telephony-example", "call ringing ... stop Audio sensor");
					appState.dpuStates.getServiceController().stopAudioSensor();
				}

			}


			if(extra.equals(android.telephony.TelephonyManager.EXTRA_STATE_IDLE))
			{
				//strategy if the phone call end then start the audio service
				Log.i("telephony-example", "call ended ... ");
				if(appState.dpuStates.audioSensorOn == false && appState.dpuStates.audioForceLocked == false) 
					// if sensor is on then off it, else nothing to do
					//or for the initial state, where there is no phone call but these method will be called
					appState.dpuStates.getServiceController().startAudioSensor();
			}


		}
	};

	/**
	 * Handles the outgoing call case. Stops audio sensing in that case.
	 */
	private final BroadcastReceiver outgoingCallReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive( Context context, Intent intent )			
		{
			Log.i("telephony-example", "outgoing call about to be underway ... ");

			try {
				Log.i("telephony-example", "In try " + appState.dpuStates.audioSensorOn + " " + appState.dpuStates.audio_release);
				if(appState.dpuStates.audioSensorOn == true)//if sensor is on then turn it off
				{
					Log.i("telephony-example", "Stopping Sesnor ");
					appState.dpuStates.getServiceController().stopAudioSensor();
					Thread.sleep(3000);
					Log.i("telephony-example", "Sucessful wait " + appState.dpuStates.audio_release);
				}
			} catch (InterruptedException e) {
				Log.e("telephony-example", "No wait " + e.toString());
			}

		}
	};



}