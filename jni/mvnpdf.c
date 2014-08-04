#include "mvnpdf.h"
#include <android/log.h>
#include <stdio.h>
#include <math.h>


#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "JNI_DEBUGGING", __VA_ARGS__)

int i,j;
double xMinusMu[3], nom, expNom;
char s[32];

// compute the log of Gaussian pdf
// NOTE: computing log avoids underflow issue.
double computeMvnPdf(double *x, double *mean, double invCov[][3], double denom) {

	nom = 0;

	// x minus mean
	for(i=0;i<3;i++)
		xMinusMu[i] = x[i] - mean[i];

	// compute the nominator (Mahalanobis distance)
	for(i=0;i<3;i++)
		for(j=0;j<3;j++)
			nom = nom - invCov[i][j] * xMinusMu[i] * xMinusMu[j];

	//expNom = exp(0.5*nom);
	//sprintf(s,"%f %f", 0.5*nom - denom,nom);
	//LOGE(s);

	// return the log of Gaussian pdf
	return 0.5*nom - denom;

}
