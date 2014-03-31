package edu.cornell.socialdpudemo2;

import edu.cornell.SocialDPU.SocialDPUApplication;

/**
 * This class is the application class. This class is accessible from all contexts possible under the same process
 * We store a lot of configuration variables from this application class
 * System wide different statuses are also available here.
 * Quite a lot of initialization happens here
 * @author shuva
 *
 */

public class SocialDPUApplication2 extends SocialDPUApplication {	
	
	// local folder for storing database data
	public static String database_path = "/sdcard/VoiceDetection/";
	//MASH: public static String database_path = "/sdcard/MoodRhythm/SocialDPU/";	
	
	public SocialDPUApplication2() {
		
		super(database_path);
	}

}