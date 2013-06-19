//   Copyright 2013 Jeffrey R. Kuhn
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.


#ifndef STACKDATA_H
#define STACKDATA_H

#include <fftw3.h>

/**Class to hold stack data
 *@author Jeffrey Kuhn
 */

class CStackData {
public:
    /** default constructor */
    CStackData();

    /** construct an empty stack of a given size */
    CStackData(int iWidth, int iHeight, int iDepth, BOOL bCreateFFTPlan, BOOL bQuickFFTPlan);

    /** destructor */
    ~CStackData();

    /** returns TRUE if the stack data is valid */
    int isValid() {
        return (m_pfRealData != 0) || (m_pfcComplexData != 0);
    }

    /** calculates the actual width (in floats) needed to store the fourier transform */
    static int actualWidth(int iWidth) {
        return 2 * (iWidth / 2 + 1);
    }

    /** calculates the actual height (in floats) needed to store the fourier transform */
    static int actualHeight(int iHeight) {
        return iHeight;
    }

    /** calculates the actual depth (in floats) needed to store the fourier transform */
    static int actualDepth(int iDepth) {
        return iDepth;
    }

    /** calculate the number of bytes needed to store the fourier transform */
    static size_t storageBytes(int iWidth, int iHeight, int iDepth) {
        return sizeof (float) *actualWidth(iWidth) * actualHeight(iHeight) * actualDepth(iDepth);
    }

    /** create the data storage space. Use isValid() to check the results. */
    void create(int iWidth, int iHeight, int iDepth, BOOL bCreateFFTPlan, BOOL bQuickFFTPlan);

    BOOL setPlane(int zDest, FLOAT* pfSrc, int iSrcLen);
	
	BOOL setPlane(int zDest, BYTE* pbSrc, int iSrcLen);
	
	BOOL setPlane(int zDest, WORD* pwSrc, int iSrcLen);
    
	BOOL getPlane(FLOAT* pfDest, int iDestLen, int zSrc);

    /** destroy any storage space previously created with "create" */
    void destroy();

    BOOL hasFFTPlan() {
        return (m_planForwardFFT != NULL && m_planBackwardFFT != NULL);
    }

    /** perform a forward, in-place FFT (real to complex) */
    BOOL forwardFFT();

    /** perform an inverse, in-place FFT (complex to real). NOTE: the results are
        unscaled! Remember to divide by the scale factor (width*height*depth) after
        an inverse FFT. */
    BOOL inverseFFT();

    /** calculates the scale factor to divide by after an inverse FFT. */
    double calcFFTScaleFactor() {
        return ((double) m_iLogicalWidth)*m_iLogicalHeight*m_iLogicalDepth;
    }

    /** divides every value in the stack by (W*H*D) to normalize the inverse FFT */
    void normalizeInverseFFT();

    /** changes the dimensions of the stack, padding either the "right" edge (iPadCenter=FALSE)
        or the center (iPadCenter=TRUE) with zeros. The new dimensions must be larger than the
        old dimensions. */
    void pad(int iNewWidth, int iNewHeight, int iNewDepth, int iPadCenter = FALSE);

    /** copy a portion of another stack to this stack */
    void copySubStack(CStackData* pSrc, int iW, int iH, int iD, int iSrcX, int iSrcY, int iSrcZ, int iDestX, int iDestY, int iDestZ);

public: // Public attributes

    /** logical width of the stack */
    int m_iLogicalWidth;
    /** logical height of the stack */
    int m_iLogicalHeight;
    /** logical depth of the stack */
    int m_iLogicalDepth;

    /** actual storage width of the stack */
    int m_iRealWidth;
    /** actual storage height of the stack */
    int m_iRealHeight;
    /** actual storage depth of the stack */
    int m_iRealDepth;
    /** actual size of a single line in the stack (in floats) */
    int m_iRealLineSize;
    /** actual size of a single plane in the stack (in floats) */
    int m_iRealPlaneSize;
    /** actual size of the entire stack (in floats) */
    int m_iRealStackSize;

    /** width of stack when it is in FCOMPLEX form */
    int m_iComplexWidth;
    /** height of stack when it is in FCOMPLEX form */
    int m_iComplexHeight;
    /** depth of stack when it is in FCOMPLEX form */
    int m_iComplexDepth;
    /** actual size of a single line in the stack (in FCOMPLEX's) */
    int m_iComplexLineSize;
    /** actual size of a single plane in the stack (in FCOMPLEX's) */
    int m_iComplexPlaneSize;
    /** actual size of the entire stack (in FCOMPLEX's) */
    int m_iComplexStackSize;

    /** pointer to the actual data when it is in real form */
    FLOAT* m_pfRealData;

    /** pointer to the actual data when it is in complex form. Normally, this
        should point to the same place as pfRealData, because the FFT is in-place */
    FCOMPLEX* m_pfcComplexData;

protected: // Protected methods

    /** internal method to calculate all of the stack size numbers */
    void calcSize(int iW, int iH, int iD);

    /** internal method that creates an fftw "plan" file for real Fourier Transforms */
    BOOL createInPlaceFFTPlans(FLOAT* pfData, int iFlags /* = FFTW_MEASURE */);

    /** internal method that destroys an fftw "plan" previously created with createInPlaceFft */
    void destroyFFTPlans();

    /** plan for the forward FFT transform */
    fftwf_plan m_planForwardFFT;

    /** plan for the backward FFT transform */
    fftwf_plan m_planBackwardFFT;

    BOOL m_bQuickPlan;

};

#endif /* STACKDATA_H */
