
Version: small_feature_size_wav_read

This version reads the audio files in .csv format (transformed from .wav using the Matlab script wavToCSV_all.m) 
into the mobile phone and calculates the audio features inside the phone. The output is the _features.txt files.

HowTo:
1. Manually import the .csv files in DDMS
2. Inside DDMS, import the .csv files into the "voice_data" folder.
3. Run the VoiceDetection App
4. Pressing the "Start Sensing" button to let the app read the .csv file until the end of the file.

NOTE: Read the .csv file one at a time. 