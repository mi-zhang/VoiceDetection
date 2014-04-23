//list of voicing parameters

/* NOTE: This model is especially optimized for speech (both indoor and outdoor) */
double mean_unvoiced[3] = {0.2209, 13.6444, 0.1716};
double mean_voiced[3] = {0.5157, 6.6938,  0.3138};

double inv_cov_unvoiced[3][3] = {{55.9649, 0.0178, -18.739}, {0.0178, 0.0061, -0.0217}, {-18.739, -0.0217, 58.6004}};
double inv_cov_voiced[3][3] = {{45.996, 0.3466, -33.7135}, {0.3466, 0.0414, -0.5743}, {-33.7135, -0.5743, 64.4322}};

//changed to log for viterbi
double transitionMatrix[2][2] = {{-0.0258, -3.6689}, {-2.8408, -0.0602} }; //loge(transitionProb)
double prior[2] = {0.5, 0.5};

//converted to log for easy use in viterbi
double denom_gauss_unvoiced = 1.3149; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
double denom_gauss_voiced = 0.6604; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))


/*
double mean_unvoiced[3] = {0.2140, 4.2015, 0.1420};
double mean_voiced[3] = {0.5104, 6.6899,  0.2762};

double inv_cov_unvoiced[3][3] = {{46.7499, -0.1275, -15.8780}, {-0.1275, 0.0222, 0.0681}, {-15.8780, 0.0681, 166.3416}};
double inv_cov_voiced[3][3] = {{50.9276, 0.1172, -47.8968}, {0.1172, 0.0667, -0.4812}, {-47.8968, -0.4812, 93.3766}};

//changed to log for viterbi
double transitionMatrix[2][2] = {  {  -0.0132,   -4.3352}, { -2.9269,   -0.0550} };//loge(transitionProb)
double prior[2] = {0.5,0.5};

//converted to log for easy use in viterbi
//double denom_gauss_unvoiced = 0.2; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
//double denom_gauss_voiced = 0.23; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))
//double denom_gauss_unvoiced = 0.5767; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
//double denom_gauss_voiced = 0.5865; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))
*/

/*
//double mean_unvoiced[3] = { 0.409640767967304	11.70000325870890	0.487373951952576};
//double mean_voiced[3] = {0.958387666717486	4.74894514767933	0.198286016529145};
double mean_unvoiced[3] = {0.21,	15.70,	0.20};
double mean_voiced[3] = {0.66,	4.75,  0.49};


double inv_cov_unvoiced[3][3] = {{46.3543,  -0.0561,  -14.4209},{-0.0561,  0.0164,  -0.4166},{-14.4209,  -0.4166,  119.4729}};
double inv_cov_voiced[3][3] = {{38.2671,   -0.0561,   -9.9948},{-0.0561,    0.0717,    0.0722},{-9.9948,    0.0722,   30.6466}};


//changed to log for viterbi
double transitionMatrix[2][2] = {  {  -0.0132,   -4.3352}, { -2.9269,   -0.0550} };//log(transitionProb)
double prior[2] = {0.5,0.5};

//converted to log for easy use in viterbi
double denom_gauss_unvoiced = 0.5767; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
double denom_gauss_voiced = 0.5865; // loge(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))
*/

/*
double mean_unvoiced[3] = { 0.2768, 11.1210, 0.1827 };
double mean_voiced[3] = { 0.4739, 8.5085, 0.3860 };

double inv_cov_unvoiced[3][3] = {{143.0434,0.7241,-9.9084},{0.7241,0.0161,-1.0792},{-9.9084,-1.0792,262.4656}};
double inv_cov_voiced[3][3] = {{38.3079, 0.6209, -16.4404},{0.6209, 0.0394, -0.5794},{-16.4404, -0.5794, 60.4045}};

//changed to log for viterbi
double transitionMatrix[2][2] = {  {  -0.0132,   -4.3352}, { -2.9269,   -0.0550} };
double prior[2] = {0.5,0.5};

//converted to log for easy use in viterbi
//double denom_gauss_unvoiced = 1/0.5043; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
//double denom_gauss_voiced = 1/0.3381; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))

double denom_gauss_unvoiced = 1.1266; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
double denom_gauss_voiced = 0.4757; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))
*/

/*
double mean_unvoiced[3] = { 0.3226, 14.1607, 0.2024 };
double mean_voiced[3] = { 0.5003, 10.9901, 0.4280 };

double inv_cov_unvoiced[3][3] = {{96.1774,0.7692,1.6810},{0.7692,0.0146,-0.4368},{1.6810,-0.4368,101.6829}};
double inv_cov_voiced[3][3] = {{36.6337,0.5937,-22.2269},{0.5937,0.0315,0.0504},{-22.2269,0.0504,56.5892}};

//changed to log for viterbi
double transitionMatrix[2][2] = {  {  -0.0258  , -3.6689}, {-2.8408  , -0.0602} };
double prior[2] = {0.5,0.5};

//converted to log for easy use in viterbi
//double denom_gauss_unvoiced = 1/0.5043; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
//double denom_gauss_voiced = 1/0.3381; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))

double denom_gauss_unvoiced = 0.6846; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
double denom_gauss_voiced = 1.0844; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))
*/


/*
//list of voicing parameters

double mean_unvoiced[3] = { 0.2826,10.4119,0.2277 };
double mean_voiced[3] = { 0.5149,7.7064,0.4575 };

double inv_cov_unvoiced[3][3] = {{133.7130,0.7789,-27.3619},{0.7789,0.0157,-0.7833},{-27.3619,-0.7833,218.5793}};
double inv_cov_voiced[3][3] = {{55.4782,0.8701,-39.1898},{0.8701,0.0492,-0.8083},{-39.1898,-0.8083,66.8279}};

//changed to log for viterbi
double transitionMatrix[2][2] = {  {  -0.0132  , -4.3304}, {-2.9067  , -0.0562} };
double prior[2] = {0.5,0.5};

//converted to log for easy use in viterbi
//double denom_gauss_unvoiced = 1/0.5043; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
//double denom_gauss_voiced = 1/0.3381; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))

double denom_gauss_unvoiced = 0.6846; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,1))^0.5))
double denom_gauss_voiced = 1.0844; // 1/(((2*pi)^(3/2))*(det(speech_cov(:,:,2))^0.5))
*/
