/* Copyright 2003 University of Twente
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/* MFCC feature extractor
 * Part of module Extractor
 *
 * Author: Tim Kemna
 * Date: 4-3-2003
 * Version: 1.0
 */
 
#include <stdlib.h>
#include <math.h>
//#include "filehandler/filehandler.h"
#include "mfcc.h"
#include "fft.h"

#define SAMPLE_RATE		11025

void mfcc(short *frame, int vector[MFCC_NUM_COEFFS]);
double mel(double f);

double mel_step, a[MFCC_FRAME_SIZE], logs[MFCC_FRAME_SIZE/2], bins[MFCC_NUM_BINS];

static int *ip;				/* bit reversal table */
static double w[MFCC_FRAME_SIZE / 2];	/* cos/sin table */

int mfcc_init(){
   ip = (int *) malloc((3 + (int) sqrt(MFCC_FRAME_SIZE / 2)) * sizeof(int));
   if(ip != NULL) ip[0] = 0;
   return ip != NULL;
}

int extract_mfcc(short *samples, int num_samples, int vectors[MFCC_NUM_VECTORS][MFCC_NUM_COEFFS]){
   int i;

   for(i = 0 ; i < MFCC_NUM_VECTORS && i * (MFCC_FRAME_SIZE - MFCC_OVERLAP) + MFCC_FRAME_SIZE <= num_samples ; i++)
      mfcc(samples + i * (MFCC_FRAME_SIZE - MFCC_OVERLAP), vectors[i]);
   return i == MFCC_NUM_VECTORS;
}

void mfcc(short *frame, int vector[MFCC_NUM_COEFFS]){
   int i, j, bin; 

 
   /* Hamming window */
   for(i = 0 ; i < MFCC_FRAME_SIZE ; i++)
      a[i] = frame[i] * (0.54 - 0.46 * cos( 2 * M_PI * i / (MFCC_FRAME_SIZE - 1) ));


   /* Calculate DFT */
   
   //rdft(MFCC_FRAME_SIZE, 1, a, ip, w);



   /* Take the logaritm */
   if(a[0] == 0.0) logs[0] = 0.0;
   else logs[0] = log10(sqrt(a[0]*a[0]));
   for(i = 1 ; i < MFCC_FRAME_SIZE/2 ; i++)
      if(a[2*i] == 0.0 && a[2*i+1] == 0.0) logs[i] = 0.0;
      else logs[i] = log10(sqrt(a[2*i] * a[2*i] + a[2*i+1] * a[2*i+1]));

   
   /* Convert to Mel-spectrum */
   
   mel_step = mel(SAMPLE_RATE/2) / MFCC_NUM_BINS;
   for(bin = i = 0 ; bin < MFCC_NUM_BINS ; bin++ , i += j){
      bins[bin] = 0;
      for(j = 0 ; (i+j)<MFCC_FRAME_SIZE/2 && mel((i+j)*(SAMPLE_RATE/2)/(MFCC_FRAME_SIZE/2)) <= mel_step * (bin+1); j++){
	 bins[bin] += logs[i+j];
      }
      if(j > 1) bins[bin] /= j;	/* average log spectrum */
   }

   /* Calculate DCT */

   //ddct(MFCC_NUM_BINS, -1, bins, ip, w);

   /* Return feature vector */

   for(i = MFCC_FIRST_COEFF ; i < MFCC_NUM_COEFFS + MFCC_FIRST_COEFF ; i++)
      vector[i] = bins[i];
}

double mel(double f){
   return 2595 * log10(1 + f / 700);
}

void mfcc_free(){
   if(ip != NULL) free(ip);
}
