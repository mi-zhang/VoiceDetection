package edu.cornell.SocialDPU;

import android.app.Application;
import edu.cornell.SocialDPU.SocialDPUStates;

/**
 * This class is the application class. This class is accessible from all contexts possible under the same process
 * We store a lot of configuration variables from this application class
 * System wide different statuses are also available here.
 * Quite a lot of initialization happens here
 * @author shuva
 *
 */


public class SocialDPUApplication extends Application {
	
	
	public String database_path = "";
	public SocialDPUStates dpuStates = null;
	
	/**
	 * Contains status of voiced(vowels)/unvoiced:
	 * 0 means unvoiced. 
	 * 1 means voiced.
	 */
	public int voice_infernce_status = 0;	
	
	/**
	 * Contains status of conversation:
	 * 0 means NOT inside a conversation.
	 * 1 means inside a conversation.
	 */
	public int conversation_infernce_status = 0;
	
	
	public SocialDPUApplication(String database_path) {
				
		this.database_path = database_path;
		
		//dpu states
		dpuStates = new SocialDPUStates(this, database_path);
		
		//initialize
		dpuStates.initialize();
						
	}

}






