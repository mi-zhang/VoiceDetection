package edu.cornell.SocialDPU.ServiceControllers.AudioLib;

import java.io.FileOutputStream; 
import java.io.OutputStreamWriter;
import android.app.Service;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.media.AudioFormat;
import android.os.Binder;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import edu.cornell.audioProbe.AudioManager;
import edu.cornell.SocialDPU.SocialDPUStates;
import edu.cornell.SocialDPU.SocialDPUApplication;
import edu.cornell.SocialDPU.Storage.MySDCardStateListener;
import edu.dartmouthcs.mltoolkit.R;

/**
 * 
 * This class initiate a service with function from AudioManager
 * @author shuva
 *
 */
public class AudioService extends Service {
	
	
	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "edu.dartmouthcs.mltoolkit.ServiceControllers.AudioLib.FOREGROUND";
	public static final String ACTION_BACKGROUND = "edu.dartmouthcs.mltoolkit.ServiceControllers.AudioLib.BACKGROUND";

	private static NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;

	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	private FileOutputStream fOut;
	public static boolean Foreground_on;
	public static boolean Activity_on;
	private static Notification notification;
	private AudioManager ar;
	private edu.cornell.SocialDPU.SocialDPUApplication appState; 
	private SocialDPUStates dpuStates;

	private Thread t; 

	private RemoteViews contentView;

	public int no_of_records;

	/**
	 *  
	 * Called when the activity is first created. 
	 * This function initiates a class of AudioManager. And starts the audio sensing.
	 * 
	 * */
	@Override
	public void onCreate() {

		
		appState = (SocialDPUApplication) getApplicationContext(); //application class
		dpuStates = appState.dpuStates;
				
		dpuStates.audioService = this; //add a reference of this AudioService to the application
		dpuStates.audio_no_of_records = 0;
		CONTEXT = this;

		// start as a foreground service
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}

		// initialize
		//register phone state listener (for phone call monitoring), and SDcard mounting
		dpuStates.phoneCallInfo = new MyPhoneStateListener(this,appState);
		
		// ???
		dpuStates.mySDCardStateInfo = new MySDCardStateListener(this, appState);
		//getting the IMEI for uniquely naming the files
		TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		dpuStates.IMEI = mTelephonyMgr.getDeviceId();
		
		// ???
		dpuStates.initializeDataBase();
		
		// ???
		dpuStates.db_adapter.open();
		Log.w("DBNAME", "DB NAME" + this.dpuStates.db_adapter.getPathOfDatabase());								

		Toast.makeText(this,
				"Audio Service Started. ",
				Toast.LENGTH_SHORT).show();

		// NOTICE!!! 
		// Initiate an AudioManger (parameter setting)
		ar = new AudioManager(appState, this, true, android.media.MediaRecorder.AudioSource.MIC, 8000, android.media.AudioFormat.CHANNEL_CONFIGURATION_MONO,
				android.media.AudioFormat.ENCODING_PCM_16BIT);

		// Start audio sensing as a separate thread
		try {
			t = new Thread() {
				public void run() {
					ar.setOutputFile("/sdcard/audio3.txt");	
					// initialize the parameters 				
					ar.prepare();
					// start recording the raw audio data
					ar.start();
					Log.i("MiCheck", "ar.start(); ...");
				}
			};
			t.start();
			Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();			
		}
		catch(Exception ex) {			
			ar = null;
			Toast.makeText(this, "Cannot create audio file \n" + ex.toString() , Toast.LENGTH_SHORT).show();
		}		
		Activity_on = true;
		Foreground_on = true;
		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);		
	}


	public static Context getContext() {
		return CONTEXT;
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);
	}

	@Override
	//This function start the foreground process
	public int onStartCommand(Intent intent, int flags, int startId) {

		if(intent == null) {
			stopSelf(); 
			return START_NOT_STICKY;
		}
		handleCommand(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	
	void handleCommand(Intent intent) {
		
		if (ACTION_FOREGROUND.equals(intent.getAction())) {			
			notification = new Notification(R.drawable.logo, "Voice Detection", System.currentTimeMillis());
			contentView.setTextViewText(R.id.audio_text, "Audio on:" + " (" + dpuStates.audio_no_of_records + ")");
			contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			notification.contentView = contentView;
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(), 0);
			notification.contentIntent = contentIntent;
			startForegroundCompat(R.string.CUSTOM_VIEW,notification);
		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started_aud);
		}
	}
	
	
	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForegroundCompat(int id, Notification notification) {
		
		// If we have the new startForeground API, then use it.
		Foreground_on = true;
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			}
			return;
		}
		// Fall back on the old API.
		setForeground(true);
		mNM.notify(id, notification);
	}

	
	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	public void stopForegroundCompat(int id) {
		
		Foreground_on = false;
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			}
			return;
		}
		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		setForeground(false);
	}

	@Override
	/**
	 * Stop the notification, and foreground process
	 * Stop audio sensing
	 */
	public void onDestroy() {
		
		// Make sure our notification is gone.
		stopForegroundCompat(R.string.CUSTOM_VIEW);
		try {
			if(ar == null) {
				Toast.makeText(this, "No audio file selected", Toast.LENGTH_SHORT).show();
				return;
			}
			//ar.stop();
			ar.release();
			// Log.i("MiCheck", "AudioService onDestroy() ...");
			Toast.makeText(this, "REcording stopped", Toast.LENGTH_SHORT).show();
		}
		catch(Exception ex) {
			Toast.makeText(this, "Failed to stop recording \n" + ex.toString() , Toast.LENGTH_SHORT).show();
		}
		ar = null;
		dpuStates.audioService =null;

	}


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	/**
	 * Updates the notification area at the top with updates
	 */
	public void updateNotificationArea() {

		contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + dpuStates.audio_no_of_records + ")");					
		contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
		//contentView.setTextViewText(R.id.accel_text, "Accel Off");
		//contentView.setTextColor(R.id.accel_text, Color.argb(128, 115, 0, 0));		
		//contentView.setTextViewText(R.id.location_text, "Location off");					
		//contentView.setTextColor(R.id.location_text, Color.argb(128, 115, 0, 0));

		notification.contentView = contentView;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);
		notification.contentIntent = contentIntent;
		mNM.notify(R.string.CUSTOM_VIEW,notification);

	}


}
