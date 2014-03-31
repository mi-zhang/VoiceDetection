#define MFCC_NUM_COEFFS			13
#define MFCC_FIRST_COEFF		0
#define MFCC_NUM_VECTORS		250	
#define MFCC_FRAME_SIZE			256
#define MFCC_OVERLAP			100
#define MFCC_NUM_BINS			32

int extract_mfcc(short *samples, int num_samples, int vectors[MFCC_NUM_VECTORS][MFCC_NUM_COEFFS]);
int mfcc_init();
void mfcc_free();
