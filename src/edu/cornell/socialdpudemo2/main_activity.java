package edu.cornell.socialdpudemo2;

//import com.simple.gui.R;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.cornell.SocialDPU.SocialDPUApplication;
import edu.cornell.SocialDPU.ServiceControllers.ServiceController;
import edu.dartmouthcs.mltoolkit.R;


/**
 * This activity is the activity when the app icon gets clicked
 * @author shuva
 *
 */
public class main_activity extends Activity {
	
	
	//service controller class for starting and stopping sensor (we only have audio sensor in the SocialDPU)
	private static ServiceController serviceController = null;
	public static Context context;
	private static Timer audioTimer; // this time periodically update the voice/non-voice inference
	private SocialDPUApplication appState; // reference to application class
	
	private boolean activity_paused;	
	private String prev_audio_status = "Not Available"; //status of audio
	
	//update gui stuff
	private ImageView audioImageView;
	private TextView audioTextView;
	//private ImageView conversationImageView;
	//private TextView conversationTextView;		
	
	private Button startnStopSensingButton;
	private Button uploadButton;
	private TextView IMEITextView;
	

	private static final String TAG = "Voice Detection";	
	
	@Override
	/**
	 * Starts sensors that enabled. In case we have the audio sensor
	 * 
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		appState = (SocialDPUApplication2)getApplicationContext();
		serviceController = appState.dpuStates.getServiceController();
		Log.i(TAG, "onCreate ...");
		setContentView(R.layout.main);

		// Get UI elements
		audioTextView = (TextView) findViewById(R.id.button_status_audio);
		audioImageView = (ImageView) findViewById(R.id.status_icon_audio);
		//conversationTextView = (TextView) findViewById(R.id.button_status_conversation);
		//conversationImageView = (ImageView) findViewById(R.id.status_icon_conversation);
		
		startnStopSensingButton = (Button) findViewById(R.id.button_startnstopsensing);
		uploadButton = (Button) findViewById(R.id.button_upload);
		IMEITextView = (TextView) findViewById(R.id.textview_imei);
		IMEITextView.setText(appState.dpuStates.IMEI);
				
		initializeClickListeners();
		
		/*
		// main activity will never start the sensing servcices twice
		if(appState.dpuStates.applicatin_starated == false) {

			//this variable makes sure the same service don't get started twice
			//Technically same service can't be started twice. We want make sure context reference 
			//don't screwed since we can't stop assigning.
			//For exampple, there will can be two MyPhoneStateListener trying to stop the audio sensor together and there
			// is a race condition.
			
			appState.dpuStates.applicatin_starated = true;			
			
			// Start the audio service
			if(appState.dpuStates.savedAudioSensorOn == true) {
				
				serviceController.startAudioSensor();
				//Toast.makeText(this, "Audio service is started.", Toast.LENGTH_SHORT).show(); 
			}

		}

		//activity on or off
		this.activity_paused = false;
		*/
		

	}

	@Override
	public void onStart() {
		
		super.onStart();
		Log.i(TAG, "onStart ...");
	}

	@Override
	/**
	 * We start a timer to change the status of the audio inference 
	 */
	public void onResume() {
		super.onResume();
		//start a timer to update the gui
		audioTimer = new Timer();
		audioTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				audioTimerMethod();
			}
		}, 0, 250);

		//bindings are online so start reading the values again
		this.activity_paused = false;
	}

	@Override
	/**
	 * Get called whenever the activity is not visible
	 */
	public void onPause() {
		super.onPause();	
		//so stop showing results, so stop the audio timer to update
		audioTimer.cancel();
		this.activity_paused = true;
		Log.i(TAG, "onPause ...");
	}

	@Override
	public void onStop() {		
		super.onStop();		
		Log.i(TAG, "onStop ...");
	}

	@Override
	public void onDestroy() {		
		super.onDestroy();
		Log.i(TAG, "onDestroy ...");
	}

	@Override
	public void onRestart() {		
		super.onDestroy();
		Log.i(TAG, "onRestart ...");
	}
	
	// =============================================================================================
	
	private void initializeClickListeners() {
		
		// Called when the user clicks the startnStopSensingButton.
		startnStopSensingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {								
				// main activity will never start the sensing servcices twice
				if(appState.dpuStates.applicatin_starated == false) {

					//this variable makes sure the same service don't get started twice
					//Technically same service can't be started twice. We want make sure context reference 
					//don't screwed since we can't stop assigning.
					//For exampple, there will can be two MyPhoneStateListener trying to stop the audio sensor together and there
					// is a race condition.
					
					appState.dpuStates.applicatin_starated = true;								
					// Start the audio service !!!
					if(appState.dpuStates.savedAudioSensorOn == true) {						
						serviceController.startAudioSensor();
						//Toast.makeText(this, "Audio service is started.", Toast.LENGTH_SHORT).show(); 
					}					
					//activity on or off
					activity_paused = false;					
					// Update UI
					startnStopSensingButton.setText("Stop Sensing");					
				} else {
					
					appState.dpuStates.applicatin_starated = false;			
					
					// Stop the audio service
					if(appState.dpuStates.savedAudioSensorOn == true) {						
						serviceController.stopAudioSensor();
						//Toast.makeText(this, "Audio service is started.", Toast.LENGTH_SHORT).show(); 
					}					
					//activity on or off
					activity_paused = true;					
					// Update UI
					startnStopSensingButton.setText("Start Sensing");					
				}
			}
		});
		
		
		// Called when the user clicks the upload button.
		uploadButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {	
				
				do{ // at least one file is there

					HttpClient httpclient = new DefaultHttpClient();
					httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);	
					HttpPost httppost = new HttpPost("http://" + "data.pac.cs.cornell.edu/SchizophreniaSense/upload" );//appState.WebServiceURL); 

					File file = new File(appState.dpuStates.fileList.get(0));//file at the first location
					if(!file.exists())
						break;

					MultipartEntity mpEntity = new MultipartEntity();
					ContentBody cbFile = new FileBody(file, "binary/octet-stream");
					mpEntity.addPart("myfile", cbFile);
					
					httppost.setEntity(mpEntity);
					String str="";
					try {  

						Log.e("Upload-Info","Response:  " + "Uploading starting");
						
						str = str + " " + "executing request " + httppost.getRequestLine();
						//System.out.println("executing request " + httppost.getRequestLine());
						HttpResponse response = httpclient.execute(httppost);
						HttpEntity resEntity = response.getEntity();
						
						//System.out.println(response.getStatusLine());
						if (resEntity != null) {
							//System.out.println(EntityUtils.toString(resEntity));
							String temp = EntityUtils.toString(resEntity);
							str = str + " --- " + temp;
							
							//<class 'web.webapi.OK'>
							if(temp.equals("<class 'web.webapi.OK'>")==false){
								Log.e("Upload-Info","Uploaded " + temp);
								continue;
							}
							else
								Log.e("Upload-Info","Response:  Request Succeeded " + temp);
						}
						if (resEntity != null) {
							resEntity.consumeContent();
						}
						
						Log.e("Upload-Info","Response:  " + str);
						
						
						httpclient.getConnectionManager().shutdown();
						//uploading finished, so delete the file
						//File f = new File(db_path);
						
						//IDIAP: donot delete the files
						
						Log.e("Upload-Info","Response: File Deleted " + file.delete());
						//move file to other directory
						//boolean success = file.renameTo(new File(dir, file.getName()));
						
						//appState.file_count++;
						
						appState.dpuStates.fileList.remove(0);//delete file from the list also
					}
					
					catch(IOException e)
					//catch(Exception e)
					{
						//SD card mounted
						//means if there is any mounting issue with the SD card it will 
						str = str + " "+e.toString();
						Log.e("Upload-Info","Response:  " + str);
						//appState.uploadRunning = false;
						//stopSelf(); //don't try now
						//break; //don't try again

					}
				} while(appState.dpuStates.fileList.size() != 0);
				
			}
		});
		
	}	
		
		
		
	//this function gets called whenever the timer completes a round
	protected void audioTimerMethod() {
		//this method will run on the timer thread
		this.runOnUiThread(audioTimer_Tick);
	}

	private Runnable audioTimer_Tick = new Runnable() {
		public void run() {			
			//if audio sensor is turned off
			if (appState.dpuStates.audioSensorOn == false || activity_paused == true) {
				setAudioImage("Not Available");
			}
			//audio sensor is running. We need to show the inference results
			else {
				if(appState.voice_infernce_status == 1) {
					setAudioImage("Voiced");
				}
				else if(appState.voice_infernce_status==0) {
					setAudioImage("Unvoiced");
				}
			}			
			//set the conversation icon
			/*
			if(appState.conversation_infernce_status == 0){
				conversationImageView.setImageResource(R.drawable.quiet_001);
				conversationTextView.setText("No conversation");
				//Log.i("MiCheck", "No conversation ...");
			}				
			else{
				conversationImageView.setImageResource(R.drawable.converation);
				conversationTextView.setText("In conversation");
				//Log.i("MiCheck", "In conversation ...");
			}
			*/			
		}				
	};
	
	protected void setAudioImage(String inferred_status) {		
		if(!this.prev_audio_status.equals(inferred_status)) {
			prev_audio_status = inferred_status;
			if(inferred_status.equals("Not Available")){
				this.audioImageView.setImageResource(R.drawable.cross);
				this.audioTextView.setText("Not Available");
			}
			else if(inferred_status.equals("Voiced")){
				this.audioImageView.setImageResource(R.drawable.talking_001);
				this.audioTextView.setText("Voiced");
			}
			else if(inferred_status.equals("Unvoiced")){
				this.audioImageView.setImageResource(R.drawable.noise_001);
				this.audioTextView.setText("Unvoiced");
			}
		}
	}





}