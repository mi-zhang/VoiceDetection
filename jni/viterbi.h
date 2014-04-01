#include <jni.h>
#define LOOK_BACK_LENGTH 20

//int getViterbiInference(double *x,double *featureAndInference);
int getViterbiInference(double *x,double *observationLikelihood, jbyte* viterbitPath);
