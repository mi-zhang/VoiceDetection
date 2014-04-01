package edu.cornell.audioProbe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import org.json.JSONException;
import org.json.JSONObject;
import edu.cornell.SocialDPU.SocialDPUApplication;
import edu.cornell.SocialDPU.SocialDPUStates;
import edu.cornell.SocialDPU.ServiceControllers.AudioLib.AudioService;
import edu.cornell.SocialDPU.Storage.ML_toolkit_object;
import edu.cornell.SocialDPU.UtilLibs.MyDataTypeConverter;
import edu.dartmouthcs.mltoolkit.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;



public class AudioManager {
	
	
	/**
	 * INITIALIZING : recorder is initializing;
	 * READY : recorder has been initialized, recorder not yet started
	 * RECORDING : recording
	 * ERROR : reconstruction needed
	 * STOPPED: reset needed
	 */
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED};

	// Log tag
	private final String TAG = "AudioManager"; 
		
	// hook to Application class for global state sharing
	private SocialDPUApplication appState;

	public static final boolean RECORDING_UNCOMPRESSED = true;
	public static final boolean RECORDING_COMPRESSED = false;

	// The interval in which the recorded samples are output to the file
	// Used only in uncompressed mode
	private static final int TIMER_INTERVAL = 120;

	// Toggles uncompressed recording on/off; RECORDING_UNCOMPRESSED / RECORDING_COMPRESSED
	private boolean 		 rUncompressed;

	// Recorder used for uncompressed recording
	private AudioRecord 	 aRecorder = null;
	// Recorder used for compressed recording
	private MediaRecorder	 mRecorder = null;

	// Stores current amplitude (only in uncompressed mode)
	private int				 cAmplitude= 0;
	// Output file path
	private String			 fPath = null;

	// Recorder state; see State
	private State			 state;

	static {
		System.loadLibrary("computeFeatures");
	}

	// Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
	private short 			 nChannels;
	private int				 sRate;
	private short			 bSamples;
	private int				 bufferSize;
	private int				 aSource;
	private int				 aFormat;

	// Number of frames written to file on each output(only in uncompressed mode)
	private int				 framePeriod;

	// Buffer for output(only in uncompressed mode)
	private short[] 		 buffer;
	private short[]		     tempBuffer = {-68,8,22,40,94,77,119,126,80,82,61,60,80,64,79,51,4,9,-7,14,20,-9,-16,19,-28,-50,-38,-82,-135,-120,-112,-95,-105,-74,10,53,15,52,88,21,32,15,-31,13,22,32,8,12,89,88,42,22,7,-49,-115,-148,-117,22,33,65,138,133,78,60,89,92,83,67,53,8,-17,-35,-31,-35,-21,4,-2,27,-18,-97,-79,-63,-54,-26,-3,-38,-58,-34,-48,-19,29,17,-15,-3,-46,-91,-65,10,106,112,110,72,83,46,-14,13,54,117,116,77,23,-4,48,76,31,-5,8,1,-21,-47,-104,-129,-141,-110,-47,-13,4,57,-7,-40,-87,-62,-12,20,48,40,41,34,34,-7,-29,-57,-115,-100,-75,-69,-38,36,43,2,3,0,-19,-60,-92,-32,-37,-25,-7,-14,-22,-12,9,11,2,-19,25,24,-1,31,69,47,-34,-67,-101,-129,-130,-115,-51,1,29,53,42,26,9,22,33,65,138,133,78,60,89,92,83,67,53,8,-17,-35,-31,-35,-21,4,-2,27,-18,-97,-79,-63,-54,-26,-3,-38,-58,-34,-48,-19,29,17,-15,-3,-46,-91,-65,10,106,112,110,72,83,46,-14,13,54,117,116,77,23,-4,48,76,31,-5,8,1,-21,-47,-104,-129,-141,-110,-47,-13,4,57};


	// Number of bytes written to file after header(only in uncompressed mode)
	// after stop() is called, this size is written to the header/data chunk in the wave file
	private int				payloadSize;
	private int 			updateFlag;
	private AudioService 	ASobj;

	//audio feature options
	private final int FRAME_SIZE = 256;
	private int FRAME_STEP = FRAME_SIZE/2; 
	private short[] audioFrame;	
	
	//private native double[] features(short[] audio, float[] observationProbability, byte[] inferenceResults, float[] autoCorrelationPeaks, short[] autoCorrelationPeakLags);
	
	//audio buffer
	private short audioBuffer[][];
	private int audioBufferSize = 500;
	private int audioBufferNextPos = 0;

	//feature and audio syncing
	private int sync_id_counter = 0;
	private FileOutputStream fOut;
	private OutputStreamWriter osw;
	private int writeCounter = 0;
	public boolean recordingStopped;

	// audio data object for passing between producing and extraction queue
	public class AudioData {

		public short data[];
		public long timestamp;
		public int sync_id;

		public AudioData() {

		}

		public AudioData(short[] data, long timestamp, int sync_id) {
			//this.data = data;
			System.arraycopy(data, 0, audioBuffer[audioBufferNextPos], 0, FRAME_STEP);			
			this.data = audioBuffer[audioBufferNextPos];
			audioBufferNextPos = (audioBufferNextPos + 1) % audioBufferSize;
			this.timestamp = timestamp;
			this.sync_id = sync_id;
		}
	}
	
	private AudioData audioFromQueueData = new AudioData();
	
	// circular buffer
	private CircularBufferFeatExtractionInference<AudioData> cirBuffer; 	
	private long tempTimestamp = 0;

	///////////////////////////////////////////////////////////////
	////////////   Conversation detection codes:start /////////////
	//////////////////////////////////////////////////////////////

	//all conversation decision making variables are here
	//variable names are self explanatory
	//if any part of the conversation code needs to be changed or 
	//differently parameterized then this is a place to do it
	private double[] extractedFeatures;
	private double currentInference;
	private double leavingInference;
	private double minuteToLookBackForPopup = 1; //means only history of last minute will be kept
	private double[] circularQueueOfInference;
	private final int LengthCircularQueueOfInference = (int) (minuteToLookBackForPopup*3750); //number of inferences possible in minuteToLookBackForPopup minutes. 60*8000/128 = 3750
	private double sumOfPreviousInferences = 0;
	private int indexToCircularQueueOfInference=0;
	private boolean inCoversation;
	private boolean conversationIntentSent;
	//currently at 3 percent is the threshold
	private double thresholdForConversation = ((double)LengthCircularQueueOfInference)*3.0/100.0; 
	private long conversationStartTime; 
	private long conversationEndTime;

	//conversation pop-up timer code
	private Handler mHandler = new Handler();
	private final int rateNotification = 1000*10; //every 10 seconds

	//////////////////////////////////////////////////////////////
	////////////   Conversation detection codes:end  /////////////
	//////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////
	////////////   Native Function Declaration  /////////////
	//////////////////////////////////////////////////////////////
	private native int energy(short[] array);
	//private native double[] features(short[] array,float[] features);
	//private native double[] features(short[] audio, float[] voicingFeatures, float[] observationProbability, byte[] inferenceResults, int[] numbeOfPeaks, float[] autoCorrelationPeaks, short[] autoCorrelationPeakLags);
	private native void features(short[] audio, float[] voicingFeatures, float[] observationProbability, byte[] inferenceResults, int[] numbeOfPeaks, float[] autoCorrelationPeaks, short[] autoCorrelationPeakLags);
	private native void audioFeatureExtractionInit();
	private native void audioFeatureExtractionDestroy();
	
	// Important !!!
	// NOTE: Pay attention to the Primitive Data Types
	private float[] voicingFeatures = new float[6]; // 6 audio features 
	private byte[] inferanceResults =  new byte[20]; // ??? why 20 bytes?
	private float[] observationProbability =  new float[2]; // 2 probabilities for voiced and unvoiced respectively 
	private float[] autoCorrelationPeaks = new float[128]; // autocorrelation peak values
	private short[] autoCorrelationPeakLags = new short[128]; // autocorrelation peak lags
	private int[] numberOfPeaks = new int[1]; // ??? why redundant?
	
	//send notification
	Notification notification;
	NotificationManager mNotificationManager;

	private String dataString;
	private int audioEnergy;
	//debug
	private int noOfPoints = 0;
	public ML_toolkit_object AudioObject;

	//states
	private SocialDPUStates dpuStates;

	/**
	 * 
	 * Returns the state of the recorder in a RehearsalAudioRecord.State typed object.
	 * Useful, as no exceptions are thrown.
	 * 
	 * @return recorder state
	 */
	public State getState() {
		
		return state;
		
	}


	/**
	 * 
	 * This thread fetches the audio data from the circular buffer
	 * And process the data by calling native C functions
	 * 
	 */
	// NOTE: MyQueuePopper is the consumer in the producer-consumer model.	
	public class MyQueuePopper extends Thread {

		CircularBufferFeatExtractionInference<AudioData> obj;
		double[] audioFrameFeature;
		double [] audioWindowFeature;
		private FileOutputStream fOut;
		private OutputStreamWriter osw;
		private int writeCounter = 0;
		private String dataString;
		private volatile Thread blinker;

		//for debug code
		private FileInputStream fIn;
		private BufferedReader br;
		private String inputStr;

		/**
		 * Intializes audio data variables and necessary variables for processing
		 * @param circular buffer where audio data is getting stored
		 */
		public MyQueuePopper(CircularBufferFeatExtractionInference<AudioData> obj) {
			
			//initialization
			this.obj = obj;
			audioFrame = new short[FRAME_SIZE];

			//initialize the first half with zeros
			//this is needed since we will be doing a 
			//moving window during feature calculation
			for(int i=0; i < FRAME_STEP; i++)
				audioFrame[i] = 0;		

		}

		//stop the thread
		public void stopper() {
			blinker = null;
		}

		@Override
		// start the thread
		public void start() {
			blinker = new Thread(this);
			blinker.start();
		}

		@Override
		//code that run inside the thread
		//fetches data from the buffer and process using native C libraries
		public void run() {
			
			// TODO Auto-generated method stub
			double[] tempFeatures;
			Thread thisThread = Thread.currentThread();
			while (blinker == thisThread) {

				// get AudioData data structure from the Circular Buffer
				audioFromQueueData = obj.deleteAndHandleData();
				System.arraycopy(audioFromQueueData.data, 0, audioFrame, FRAME_STEP, FRAME_STEP);

				///////////////////////////////////////////////////////////////
				////////////   audio features is computed here   /////////////
				//////////////////////////////////////////////////////////////

				//this function calls the c function for audio processing
				//extractedFeatures = features(audioFrame,features,); // problem is here we want to assign the array to a fixed place, but variable length is causing problems?
				//private native double[] features(short[] audio, float[] observationProbability, byte[] inferenceResults, float[] autoCorrelationPeaks, short[] autoCorrelationPeakLags);
				//extractedFeatures = 
			    features(audioFrame, voicingFeatures, observationProbability, inferanceResults, numberOfPeaks, autoCorrelationPeaks, autoCorrelationPeakLags); 
				
				///////////////////////////////////////////////////////////////
				////////////   Conversation detection codes:start /////////////
				//////////////////////////////////////////////////////////////

				//add the new inference results. 0 = non-human-voice, 1=human-voice. 
				leavingInference = circularQueueOfInference[indexToCircularQueueOfInference];
				sumOfPreviousInferences = sumOfPreviousInferences - leavingInference;
				//currentInference = extractedFeatures[8]; // 0 = non-human-voice, 1 = human-voice
				currentInference = inferanceResults[0]; // 0 = non-human-voice, 1 = human-voice
				
				Log.e(TAG,"============ Voice/unvoiced, " + currentInference + " ===============");
				Log.e(TAG,"Features, " + Arrays.toString(voicingFeatures));
				Log.e(TAG,"observationProbability, " + Arrays.toString(observationProbability));
				Log.e(TAG,"inferanceResults, " + Arrays.toString(inferanceResults));
				Log.e(TAG,"numberOfPeaks, " + Arrays.toString(numberOfPeaks));
				Log.e(TAG,"autoCorrelationPeaks, " + Arrays.toString(autoCorrelationPeaks));
				Log.e(TAG,"autoCorrelationPeakLags, " + Arrays.toString(autoCorrelationPeakLags));
				
				//Log.i("MiCheck", "MyQueuePopper run" + currentInference);

				//set inferred_audio_Status
				dpuStates.inferred_audio_Status = (int)currentInference;//set current inference in the appState object
				appState.voice_infernce_status = dpuStates.inferred_audio_Status;

				sumOfPreviousInferences = sumOfPreviousInferences + currentInference;
				circularQueueOfInference[indexToCircularQueueOfInference] = currentInference;
				indexToCircularQueueOfInference = (indexToCircularQueueOfInference + 1) % LengthCircularQueueOfInference;

				///////////////////////////////////////////////////////////////
				////////////   Conversation detection codes:end  /////////////
				//////////////////////////////////////////////////////////////
			
				//AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 24, 
				//		MyDataTypeConverter.toByta(extractedFeatures),audioFromQueueData.sync_id);
				
				
				
				//separate entry for separate values
				/*
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 25, 
						MyDataTypeConverter.toByta(voicingFeatures),audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 26, 
						MyDataTypeConverter.toByta(inferanceResults),audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 27, 
						MyDataTypeConverter.toByta(observationProbability),audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 28, 
						MyDataTypeConverter.toByta(numberOfPeaks),audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 29, 
						MyDataTypeConverter.toByta(autoCorrelationPeaks),audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 30, 
						MyDataTypeConverter.toByta(autoCorrelationPeakLags),audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				*/				
				
				// make a buffer with byte array with all the values
				AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 31, 
						MyDataTypeConverter.toByta(voicingFeatures, inferanceResults, observationProbability,
								numberOfPeaks, autoCorrelationPeaks, autoCorrelationPeakLags), audioFromQueueData.sync_id);
				dpuStates.ML_toolkit_buffer.insert(AudioObject);
				
				// move the processed frame_step to the first half of audioFrame
				System.arraycopy(audioFromQueueData.data, 0, audioFrame, 0, FRAME_STEP);

			}

		}

	}
	
	private MyQueuePopper myQueuePopper;
	public boolean freeCMemoryActivated;


	/**
	 * 
	 * Method used for recording and storing data to the buffer for queue insertion
	 * 
	 */
	private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
		
		public void onPeriodicNotification(AudioRecord recorder) {

			//////////////////////////////////////////////////////
			///////// buffer contains the audio data /////////////
			//////////////////////////////////////////////////////

			// producer producing data!
			aRecorder.read(buffer, 0, buffer.length); // Fill buffer with available audio

			if(!recordingStopped){
				//put data in circular buffer for processing
				//you can do other stuffs with the data
				tempTimestamp = System.currentTimeMillis();
				sync_id_counter = (sync_id_counter + 1) % 16384;				

				//input to circular buffer
				cirBuffer.insert(new AudioData(buffer, tempTimestamp, sync_id_counter));//<-----------check why 16384
				//Log.i("MiCheck", "onPeriodicNotification");
				boolean rawAudioOn = true;
				if(rawAudioOn == true) {
					AudioObject =  dpuStates.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 0, MyDataTypeConverter.toByta(buffer),sync_id_counter);
					dpuStates.ML_toolkit_buffer.insert(AudioObject);//inserting into the buffer
				}
				
				
				payloadSize += buffer.length;
				updateFlag++;
				dpuStates.audio_no_of_records = payloadSize;
				if (updateFlag%3750 == 0){//update the notification area, every one minute, 62.5 = 1000/16 frames per second; 62.5*60 = 3750 
					ASobj.no_of_records = payloadSize;
					ASobj.updateNotificationArea();
				}
			}
			else{
				//no new data will be inserted at this stage
				//so we will activate the freeCMemory thread
				//this will wait if there is element in the queue
				//when the queue is free it will free the memory. 
				//Since no data will
				//be inserted at this stage, thus free queue means 
				//(earlier insert calls are completed at this stage)
				//no more data
				if(freeCMemoryActivated==false){
					Log.e("starting","starting stopping");
					freeCMemoryActivated = true;
					new Thread(new Runnable() {
						public void run() {					      
							cirBuffer.freeCMemory();

							Log.e("stopping","stopping");
							audioFeatureExtractionDestroy();
						}
					}).start();

					//log that we input is stopping
					Log.e(AudioManager.class.getName(),"audio input is stopping");
				}
			}

		}

		public void onMarkerReached(AudioRecord recorder)
		{
			// NOT USED
		}
	};



	///////////////////////////////////////////////////////////////
	////////////   Conversation detection codes:start /////////////
	//////////////////////////////////////////////////////////////

	/** 
	 * start and end time of conversation get/set here
	 */
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {

			//decide for conversation
			//a timer has been added which will check every 30 seconds to see whether 
			//there is x% percent voice being sensed in the last minute

			
			//Log.i("MiCheck", "in Runnable()");

			Log.e("CurrentStat",""+sumOfPreviousInferences+"," +600);//here 600 is the threshold

			//this code runs every 10 seconds look how much conversation is present
			//in the last minute (or minuteToLookBackForPopup)

			if(sumOfPreviousInferences > 600) // a threashold is set for number of inferences within the 10 seconds
			{
				if(inCoversation == false){ //then we are going from non-conversation to conversation
					conversationStartTime = System.currentTimeMillis()-10*1000; //go back 10 seconds to set an optimistic bound about conversation
					inCoversation = true;

					//start the conversation
					appState.conversation_infernce_status = 1;


					conversationIntentSent = false;//means send it next time
					Log.e("CurrentStat","Starting a conversation");
				}
			}
			else if(sumOfPreviousInferences < 400) //conversation finished
			{
				inCoversation = false; 
				if(conversationIntentSent == false)//I want to send only when previous call was in conversation. This happens when a conversation ends.
				{			
					conversationEndTime = System.currentTimeMillis();
					Log.e("CurrentStat","Finished a conversation");

					//write the conversation start and end time in JSOn
					JSONObject jsonObject = new JSONObject();
					try {
						jsonObject.put("CONVERSATION_START", conversationStartTime);
						jsonObject.put("CONVERSATION_END", conversationEndTime);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					

					//finish the conversation
					appState.conversation_infernce_status = 0;

				}

			}


			//make sure we call this handler again afterwards to continue checking
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateNotification);
		}
	};
	///////////////////////////////////////////////////////////////
	////////////   Conversation detection codes:end  /////////////
	//////////////////////////////////////////////////////////////


	/** 
	 * 
	 * 
	 * Default constructor
	 * 
	 * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
	 * In case of errors, no exception is thrown, but the state is set to ERROR
	 * 
	 */ 
	public AudioManager(SocialDPUApplication apppState,AudioService obj, boolean uncompressed, int audioSource, int sampleRate, int channelConfig,
			int audioFormat) {
		
		
		Log.i("MiCheck", "on constructor of audiomanager.");
		this.ASobj = obj;
		this.appState = apppState;
		this.dpuStates = this.appState.dpuStates;
				
		//initialize features, inference, others
		inferanceResults = new byte[20];
		voicingFeatures = new float[6];
		observationProbability = new float[2];
		autoCorrelationPeaks = new float[128];
		autoCorrelationPeakLags = new short[128];
		
		try {
			
			rUncompressed = uncompressed;
			if (rUncompressed)
			{ // RECORDING_UNCOMPRESSED

				if (audioFormat == AudioFormat.ENCODING_PCM_16BIT)
				{
					bSamples = 16;
				}
				else
				{
					bSamples = 8;
				}

				if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO)
				{
					nChannels = 1;
				}
				else
				{
					nChannels = 2;
				}

				aSource = audioSource;
				sRate   = sampleRate;
				aFormat = audioFormat;

				framePeriod = this.FRAME_STEP;
				bufferSize = framePeriod * 100 * bSamples * nChannels / 8;
				if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat))
				{ // Check to make sure buffer size is not smaller than the smallest allowed one 
					bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
					// Set frame period and timer interval accordingly
					framePeriod = bufferSize / ( 2 * bSamples * nChannels / 8 );
					Log.w(AudioManager.class.getName(), "Increasing buffer size to " + Integer.toString(bufferSize));
				}

				// aRecorder is the producer in the producer-consumer model.
				aRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
				if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
					throw new Exception("AudioRecord initialization failed");
				// register a listener and set up the frame size to call the listener.
				aRecorder.setRecordPositionUpdateListener(updateListener);
				aRecorder.setPositionNotificationPeriod(framePeriod);
				this.updateFlag = 0;

				//add a new buffer for putting audio-stuff
				cirBuffer = new CircularBufferFeatExtractionInference<AudioManager.AudioData>(null, 200);

				//array puller
				audioBuffer = new short[audioBufferSize][FRAME_STEP];

				// myQueuePopper is the consumer in the producer-consumer model.
				//start a new thread for reading audio stuff
				myQueuePopper = new MyQueuePopper(cirBuffer);
				myQueuePopper.start();

				//write file init <--------------- Rifat commented these two lines

				//initialize percentage computation queue				
				//total number of voiced frames in the last minuteToLookBackForPopup minutes
				//60*8000/128=3750
				circularQueueOfInference = new double[(int) (minuteToLookBackForPopup*3750)];


				inCoversation = false;				

				//if conversationIntentSent==false and if non-conversation is found then we will send intent. 
				//If conversationIntentSent==true then if non-conversation is found then we will not send intent. 
				conversationIntentSent = true;  

				// timer for conversation decision
				mHandler.removeCallbacks(mUpdateTimeTask);
				mHandler.postDelayed(mUpdateTimeTask, rateNotification);

				// initiate notification
				String ns = Context.NOTIFICATION_SERVICE;
				mNotificationManager = (NotificationManager) this.ASobj.getSystemService(ns);
				int icon = R.drawable.icon;
				CharSequence tickerText = "Conversation Survey";
				long when = System.currentTimeMillis();
				notification = new Notification(icon, tickerText, when);



			} else
			{ // RECORDING_COMPRESSED
				//not used	
				mRecorder = new MediaRecorder();
				mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			}
			cAmplitude = 0;
			fPath = null;
			state = State.INITIALIZING;
		} catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(AudioManager.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(AudioManager.class.getName(), "Unknown error occured while initializing recording");
			}
			state = State.ERROR;
		}
	}

	/**
	 * Sets output file path, call directly after construction/reset.
	 *  
	 * @param output file path
	 * 
	 */
	public void setOutputFile(String argPath)
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				fPath = argPath;
				if (!rUncompressed)
				{
					mRecorder.setOutputFile(fPath);
				}
			}
		}
		catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(AudioManager.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(AudioManager.class.getName(), "Unknown error occured while setting output path");
			}
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * Returns the largest amplitude sampled since the last call to this method.
	 * 
	 * @return returns the largest amplitude since the last call, or 0 when not in recording state. 
	 * 
	 */
	public int getMaxAmplitude()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				int result = cAmplitude;
				cAmplitude = 0;
				return result;
			}
			else
			{
				try
				{
					return mRecorder.getMaxAmplitude();
				}
				catch (IllegalStateException e)
				{
					return 0;
				}
			}
		}
		else
		{
			return 0;
		}
	}


	/**
	 * 
	 * Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
	 * the recorder is set to the ERROR state, which makes a reconstruction necessary.
	 * In case uncompressed recording is toggled, the header of the wave file is written.
	 * In case of an exception, the state is changed to ERROR
	 * 	 
	 */
	public void prepare()
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				if (rUncompressed)
				{
					if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (fPath != null))
					{
						buffer = new short[framePeriod * bSamples / 16 * nChannels];
						state = State.READY;
						Log.i("MiCheck", "on prepare audio manager state is set to ready ...");
					}
					else
					{
						Log.e(AudioManager.class.getName(), "prepare() method called on uninitialized recorder");
						state = State.ERROR;
					}
				}
				else
				{
					mRecorder.prepare();
					state = State.READY;
				}
			}
			else
			{
				Log.e(AudioManager.class.getName(), "prepare() method called on illegal state");
				release();
				state = State.ERROR;
			}
		}
		catch(Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(AudioManager.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(AudioManager.class.getName(), "Unknown error occured in prepare()");
			}
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
	 *  
	 */
	public void release()
	{
		if (state == State.RECORDING)
		{

			stop();

		}
		else
		{
			if ((state == State.READY) & (rUncompressed))
			{
				(new File(fPath)).delete();
			}
		}

		if (rUncompressed)
		{
			if (aRecorder != null)
			{
				aRecorder.release();

				recordingStopped = true;
				//aRecorder = null;

				//audioFeatureExtractionDestroy();
				//stop the timer.
				mHandler.removeCallbacks(mUpdateTimeTask);
			}
		}
		else
		{
			if (mRecorder != null)
			{
				mRecorder.release();

			}
		}

	}

	/**
	 * 
	 * 
	 * Resets the recorder to the INITIALIZING state, as if it was just created.
	 * In case the class was in RECORDING state, the recording is stopped.
	 * In case of exceptions the class is set to the ERROR state.
	 * 
	 */
	public void reset()
	{
		try
		{
			if (state != State.ERROR)
			{
				release();
				fPath = null; // Reset file path
				cAmplitude = 0; // Reset amplitude
				if (rUncompressed)
				{
					aRecorder = new AudioRecord(aSource, sRate, nChannels+1, aFormat, bufferSize);
				}
				else
				{
					mRecorder = new MediaRecorder();
					mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				}
				state = State.INITIALIZING;
			}
		}
		catch (Exception e)
		{
			Log.e(AudioManager.class.getName(), e.getMessage());
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 * Starts the recording, and sets the state to RECORDING.
	 * Call after prepare().
	 * 
	 */
	public void start()
	{
		Log.i("MiCheck", "on start audio manager ...");
		if (state == State.READY)
		{
			if (rUncompressed)
			{
				payloadSize = 0;
				audioFeatureExtractionInit();
				aRecorder.startRecording();
				aRecorder.read(buffer, 0, buffer.length);
				recordingStopped = false;
				freeCMemoryActivated = false;
				Log.i("MiCheck", "on start audio manager rUncompressed ...");
			}
			else
			{
				mRecorder.start();
				Log.i("MiCheck", "on start audio manager compressed ...");
			}
			state = State.RECORDING;
		}
		else
		{
			Log.i("MiCheck", "on start audio manager not ready ...");
			Log.e(AudioManager.class.getName(), "start() called on illegal state");
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 *  Stops the recording, and sets the state to STOPPED.
	 * In case of further usage, a reset is needed.
	 * Also finalizes the wave file in case of uncompressed recording.
	 * 
	 */
	public void stop()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				aRecorder.stop();
			}
			else
			{
				mRecorder.stop();
			}
			state = State.STOPPED;
		}
		else
		{
			Log.e(AudioManager.class.getName(), "stop() called on illegal state");
			state = State.ERROR;
		}
	}

	/** 
	 * 
	 * Converts a byte[2] to a short, in LITTLE_ENDIAN format
	 * 
	 */
	private short getShort(byte argB1, byte argB2)
	{
		return (short)(argB1 | (argB2 << 8));
	}




	/**
	 * 
	 * A vibrate notification is sent when a conversation is detected
	 * 
	 */
	private void vibrateNotification()
	{
		NotificationManager nManager = (NotificationManager) this.ASobj.getSystemService(this.ASobj.NOTIFICATION_SERVICE); 
		Notification n = new Notification();

		// Now we set the vibrate member variable of our Notification
		// After a 100ms delay, vibrate for 200ms then pause for another
		//100ms and then vibrate for 500ms
		n.vibrate = new long[]{ 0, 300, 200, 300, 400, 300 , 600, 300};

		n.ledOnMS  = 200;    //Set led blink (Off in ms)
		n.ledOffMS = 200;    //Set led blink (Off in ms)
		n.ledARGB = 0x9400d4;   //Set led color
		n.flags = Notification.FLAG_SHOW_LIGHTS;

		n.defaults = Notification.DEFAULT_SOUND;

		nManager.notify(0, n);
	}


	private static final int HELLO_ID = 1;
	public void updateNotificationArea(){

		String text = "Time: ";
		Intent notificationIntent = new Intent();
		notificationIntent.setAction("edu.cornellis.mltoolkit.ConversationPopupIntent");
		notificationIntent.putExtra("ConversationPrimaryKey", System.currentTimeMillis());
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();

		PendingIntent contentIntent = PendingIntent.getActivity(this.ASobj, 0,
				notificationIntent,PendingIntent.FLAG_CANCEL_CURRENT);


		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this.ASobj, "Please complete the survey", text 
				+ " " +  dateFormat.format(cal.getTime()), contentIntent);

		mNotificationManager.notify(HELLO_ID, notification);
	}
}

