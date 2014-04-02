#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <math.h>
#include "kiss_fftr.h"
#include "voice_features.h"
#include "viterbi.h"

//**********************************************************************************
//
// 	GLOBAL VARIABLES
//
//**********************************************************************************

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "JNI_DEBUGGING", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "JNI_DEBUGGING", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "JNI_DEBUGGING", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "JNI_DEBUGGING", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "JNI_DEBUGGING", __VA_ARGS__)


jint sum = 0;
char buffer [2500];
char temp_buffer [50];
int n;
double spec[FFT_LENGTH];
kiss_fft_cpx freq[FFT_LENGTH];
kiss_fft_cpx y[FRAME_LENGTH];
kiss_fft_cpx z[FRAME_LENGTH];
kiss_fft_cpx powerSpecCpx[FFT_LENGTH];
kiss_fft_scalar powerSpec[FFT_LENGTH];
kiss_fft_scalar magnitudeSpec[FFT_LENGTH];
double spectral_entropy;
float rel_spectral_entropy;
int divider;
double peak_vals[FRAME_LENGTH/2];
int peak_loc[FRAME_LENGTH/2];
int nacorr = (int)(FRAME_LENGTH/2);
double comp[FRAME_LENGTH/2];

//features
double energy;
double relSpecEntr;
double featuresValuesTemp[264 + LOOK_BACK_LENGTH]; //(6 + 128 + 128 +  = 262) + 2 + LOOK_BACK_LENGTH
double featureAndInference[2+LOOK_BACK_LENGTH];
double observationLikihood[2];
char viterbiPath[LOOK_BACK_LENGTH];

double x[3];
int inferenceResult;


//**********************************************************************************
//
// 	initialization function to allocate memory for reuse later.
//
//**********************************************************************************
void Java_edu_cornell_audioProbe_AudioManager_audioFeatureExtractionInit(JNIEnv* env, jobject javaThis) {

	//intialize feature arrays
	initVoicedFeaturesFunction();

	//initialize viterbi
	viterbiInitialize();

}

//**********************************************************************************
//
// 	destroy function for the c code. Currently not called
//
//**********************************************************************************
void Java_edu_cornell_audioProbe_AudioManager_audioFeatureExtractionDestroy(JNIEnv* env, jobject javaThis) {

	//destroy features arrays
	destroyVoicedFeaturesFunction();

	//kill viterbi
	viterbiDestroy();

}


//**********************************************************************************
//
// 	compute three features for voicing detection. Also a variable length autocorrelation values and
//	lags are stored in the returned double array
//
//**********************************************************************************
void Java_edu_cornell_audioProbe_AudioManager_features(JNIEnv* env, jobject javaThis, jshortArray audio, jfloatArray features,
		jfloatArray observationProbability, jbyteArray inferenceResults, jintArray numberOfPeaks, jfloatArray autoCorrelationPeaks, jshortArray autoCorrelationPeakLags) {


	jshort* buff =  (*env)->GetShortArrayElements(env, audio, 0);
	jfloat * fVector =  (*env)->GetFloatArrayElements(env, features, 0);
	jfloat * obsProbVector =  (*env)->GetFloatArrayElements(env, observationProbability, 0);
	jbyte * inferRes =  (*env)->GetByteArrayElements(env, inferenceResults, 0);
	jint * numOfPeaks =  (*env)->GetIntArrayElements(env, numberOfPeaks, 0);
	jfloat * autoCorPeakVal =  (*env)->GetFloatArrayElements(env, autoCorrelationPeaks, 0);
	jshort* autoCorPeakLg =  (*env)->GetShortArrayElements(env, autoCorrelationPeakLags, 0);

	// zero mean data
	normalize_data(buff, normalizedData);

	// apply hamming window
	computeHamming(normalizedData, dataHamming);

	// computeFwdFFT
	kiss_fftr(cfgFwd, dataHamming, fftx);

	// compute power spectrum
	computePowerSpec(fftx, powerSpec, FFT_LENGTH);

	// compute magnitude spectrum
	computeMagnitudeSpec(powerSpec, magnitudeSpec, FFT_LENGTH);

	// compute total energy
	energy = computeEnergy(powerSpec, FFT_LENGTH) / FFT_LENGTH;

	// compute Spectral Entropy
	relSpecEntr = computeSpectralEntropy2(magnitudeSpec, FFT_LENGTH);

	// compute auto-correlation peaks
	computeAutoCorrelationPeaks2(powerSpec, powerSpecCpx, NOISE_LEVEL, FFT_LENGTH, autoCorPeakVal, autoCorPeakLg);

	//write on the feature vector
	fVector[0] = numAcorrPeaks;
	fVector[1] = maxAcorrPeakVal;
	fVector[2] = maxAcorrPeakLag;
	fVector[3] = spectral_entropy;
	fVector[4] = relSpecEntr;
	fVector[5] = energy;

	//gaussian distribution
	//test the gaussian distribution with some dummy values first
	x[0] = maxAcorrPeakVal;
	x[1] = numAcorrPeaks;
	x[2] = relSpecEntr;

	inferenceResult = getViterbiInference(x, observationLikihood, inferRes);

	//observation likelihood
	obsProbVector[0] = (float)observationLikihood[0]; // unvoiced
	obsProbVector[1] = (float)observationLikihood[1]; // voiced

	//infer results are already assigned during getViterbiInference call

	// ??? why
	numOfPeaks[0] = numAcorrPeaks;

	(*env)->ReleaseShortArrayElements(env, audio, buff, JNI_ABORT);
	(*env)->ReleaseFloatArrayElements(env, features, fVector, 0);
	(*env)->ReleaseFloatArrayElements(env, observationProbability, obsProbVector, 0);
	(*env)->ReleaseByteArrayElements(env, inferenceResults, inferRes, 0);
	(*env)->ReleaseFloatArrayElements(env, autoCorrelationPeaks, autoCorPeakVal, 0);
	(*env)->ReleaseShortArrayElements(env, autoCorrelationPeakLags, autoCorPeakLg, 0);
	(*env)->ReleaseIntArrayElements(env, numberOfPeaks, numOfPeaks, 0);


}


