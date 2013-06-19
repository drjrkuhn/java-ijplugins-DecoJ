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


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "deco.h"
#include "stackdata.h"
#include "process.h"

#define EM_LOWER_CUTOFF		1.0E-4

#define SHOW_STATS  0

void copyToLowerZ(CStackData* pDest, CStackData* pSrc) {
    ASSERT(pSrc->m_iRealPlaneSize == pDest->m_iRealPlaneSize);
    ASSERT(pSrc->m_iRealDepth <= pDest->m_iRealDepth);

    int iPlaneSize = pSrc->m_iRealPlaneSize;
    size_t cbPlaneBytes = iPlaneSize * sizeof (FLOAT);

    FLOAT* pfSrc = pSrc->m_pfRealData;
    FLOAT* pfDest = pDest->m_pfRealData;
    int iCount = pSrc->m_iLogicalDepth;
    while (iCount--) {
        memcpy(pfDest, pfSrc, cbPlaneBytes);
        pfSrc += iPlaneSize;
        pfDest += iPlaneSize;
    }
}

void mirrorLowerToUpperZ(CStackData* pStack) {
    int iPlaneSize = pStack->m_iRealPlaneSize;
    size_t cbPlaneBytes = iPlaneSize * sizeof (FLOAT);

    FLOAT* pfFwd = pStack->m_pfRealData;
    FLOAT* pfRev = pStack->m_pfRealData + pStack->m_iRealStackSize - iPlaneSize;
    int iHalfD = pStack->m_iLogicalDepth / 2;
    int iCount = iHalfD;
    while (iCount--) {
        memcpy(pfRev, pfFwd, cbPlaneBytes);
        pfFwd += iPlaneSize;
        pfRev -= iPlaneSize;
    }
}

void calcStats(CStackData* pStack, double* pdAvg, double* pdSD, double dScale = 1.0, int iLowerOnly = FALSE) {
    FLOAT* pf = pStack->m_pfRealData;
    double dSum = 0.0;
    double dSumSq = 0.0;
    int iN = pStack->m_iRealStackSize;
    double dVal;

    if (iLowerOnly) iN /= 2;

    int iCount = iN;
    while (iCount--) {
        dVal = *(pf++) * dScale;
        dSum += dVal;
        dSumSq += dVal * dVal;
    }

    *pdAvg = dSum / iN;
    *pdSD = sqrt(dSumSq) / iN;
}

void calcComplexStats(CStackData* pStack, double* pdAvg, double* pdSD, int iLowerOnly = FALSE) {
    FCOMPLEX* pfc = pStack->m_pfcComplexData;
    double dSum = 0.0;
    double dSumSq = 0.0;
    int iN = pStack->m_iComplexStackSize;
    double dVal;

    if (iLowerOnly) iN /= 2;

    int iCount = iN;
    while (iCount--) {
        dVal = sqrt((pfc->re * pfc->re) + (pfc->im * pfc->im));
        pfc++;
        dSum += dVal;
        dSumSq += dVal * dVal;
    }

    *pdAvg = dSum / iN;
    *pdSD = sqrt(dSumSq) / iN;
}

void copyLowerToUpperZ(CStackData* pStack) {
    int iPlaneSize = pStack->m_iRealPlaneSize;
    size_t cbPlaneBytes = iPlaneSize * sizeof (FLOAT);

    int iHalfD = pStack->m_iLogicalDepth / 2;
    FLOAT* pfSrc = pStack->m_pfRealData;
    FLOAT* pfDest = pStack->m_pfRealData + iHalfD*iPlaneSize;
    for (int z = 0; z < iHalfD; z++) {
        memcpy(pfDest, pfSrc, cbPlaneBytes);
        pfSrc += iPlaneSize;
        pfDest += iPlaneSize;
    }
}

BOOL processEM(CStackData* pImage, CStackData* pPsf, int iTotalIterations, ProgressFunc progress) {
    BOOL bSuccess;

    int iLogicalWidth = pImage->m_iLogicalWidth;
    int iLogicalHeight = pImage->m_iLogicalHeight;
    int iLogicalDepth = pImage->m_iLogicalDepth;

#if SHOW_STATS	
    char pcBuffer[128];
    double dAvg, dSD;
#endif
	
    double dNorm, dTemp;
    int iCount;
    FLOAT fRe, fIm;
    FCOMPLEX* pfcWork;
    FCOMPLEX* pfcOtf;

    FLOAT* pfImage;
    FLOAT* pfWork;
    FLOAT* pfGuess;
    double dOldGuess, dNewGuess, dError, dSumSq;
    int iIteration;
    //double dUnNormalizedCutoff;


    if (iLogicalWidth < pPsf->m_iLogicalWidth) {
        SHOW_ERROR("image width must be larger than psf width.");
        return FALSE;
    }
    if (iLogicalHeight < pPsf->m_iLogicalHeight) {
        SHOW_ERROR("image height must be larger than psf height.");
        return FALSE;
    }
    if (iLogicalDepth < pPsf->m_iLogicalDepth) {
        SHOW_ERROR("image depth must be larger than psf depth.");
        return FALSE;
    }

#if SHOW_STATS
    calcStats(pImage, &dAvg, &dSD);
    sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
    SHOW_MESSAGE1("\timage average = %s", pcBuffer);
#endif
	
    /*  create the OTF
	
        NOTE: because of the cyclical nature of the DFT-based convolution,
        the edges of the data create discontinuities and wrap-around errors. These
            problems can be overcome by doubling the size of each dimension and padding
            the new bits with zeros. Since it would take up too much space to double width,
            height, and	depth, only the depth is doubled, since improving axial resolution
            is the overall goal. A couple of notes about the padding: The psf will be
            padded in the center (because it already wraps around), and the image
            will be padded in the z direction. Instead of just padding the z-direction
            with zeros, it is padded with a reflected version of the image. This edge
            reflection smooths out the discontinuities at the edges. */

    pPsf->pad(iLogicalWidth, iLogicalHeight, 2 * iLogicalDepth, TRUE);
    /* PSF now has dimensions W x H x 2D */

	if (g_iVerbose) SHOW_MESSAGE ("calculating OTF from Psf");
	bSuccess = pPsf->forwardFFT ();
	if (!bSuccess) { SHOW_ERROR ("could not create the OTF from the Psf"); return FALSE; }
    CStackData* pOTF = pPsf; /* a reference to make the code more readable */
    pPsf = 0;

    /* normalize the OTF by dividing by the DC average (frequency = 0) */
    pfcOtf = pOTF->m_pfcComplexData;
    iCount = pOTF->m_iComplexStackSize;
    dNorm = sqrt((pfcOtf->re * pfcOtf->re) + (pfcOtf->im * pfcOtf->im));
    while (iCount--) {
        pfcOtf->re /= dNorm;
        pfcOtf->im /= dNorm;
        pfcOtf++;
    }

#if SHOW_STATS
    calcComplexStats(pOTF, &dAvg, &dSD);
    sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
    SHOW_MESSAGE1("\tOTF average = %s", pcBuffer);
#endif

    /* create the "guess" of the image with all values set to 1. This guess is updated every iteration. */
    CStackData* pGuess = new CStackData(pImage->m_iLogicalWidth, pImage->m_iLogicalHeight, pImage->m_iLogicalDepth, false, false);
    iCount = pGuess->m_iRealStackSize;
    FLOAT* pf = pGuess->m_pfRealData;
    while (iCount--) {
        *(pf++) = 1.0;
    }
    if (!pGuess->isValid()) {
        SHOW_ERROR("could not create guess stack.");
        delete pGuess;
        return FALSE;
    }

    CStackData* pWork = new CStackData(iLogicalWidth, iLogicalHeight, 2 * iLogicalDepth, true, false);
    if (!pWork->isValid()) {
        SHOW_ERROR("could not create working stack.");
        delete pGuess;
        delete pWork;
        return FALSE;
    }


#if SHOW_STATS
    calcStats(pImage, &dAvg, &dSD);
    sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
    SHOW_MESSAGE1("\timage average = %s", pcBuffer);
#endif

    /* start progress estimation */
    time_t tStart;
    static char pcProgress[128];
    static char pcProgressMessage[256];
    printProgress(pcProgress, 0, iTotalIterations, &tStart, 0, NULL);

    for (iIteration = 0; iIteration < iTotalIterations; iIteration++) {

#if SHOW_STATS
        calcStats(pGuess, &dAvg, &dSD);
        sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
        SHOW_MESSAGE1("\tcurrent guess average = %s", pcBuffer);
#endif

        if (g_iVerbose) SHOW_MESSAGE("projecting current guess");

        /* copy current guess to lower half of working stack and mirror it in upper half*/
        copyToLowerZ(pWork, pGuess);
        mirrorLowerToUpperZ(pWork);

        /* take fourier transform of guess (stored in work) */
        bSuccess = pWork->forwardFFT();
        if (!bSuccess) {
            SHOW_ERROR("could not take DFT of working stack");
            delete pGuess;
            delete pWork;
            return FALSE;
        }

        /* multiply guess (stored in work) by OTF
                Complex multiply:
                (A + iB)(C + iD) 	= AC + iAD + iBC + (i^2)BD 		= (AC - BD) + i(AD + BC)
         */
        pfcWork = pWork->m_pfcComplexData;
        pfcOtf = pOTF->m_pfcComplexData;
        ASSERT(pfcWork);
        ASSERT(pfcOtf);
        iCount = pWork->m_iComplexStackSize;
        ASSERT(iCount > 0);
        while (iCount--) {
            fRe = (pfcWork->re * pfcOtf->re) - (pfcWork->im * pfcOtf->im);
            fIm = (pfcWork->re * pfcOtf->im) + (pfcWork->im * pfcOtf->re);
            pfcOtf++;
            pfcWork->re = fRe;
            pfcWork->im = fIm;
            pfcWork++;
        }

        /* take inverse fourier transform of guess (stored in work) */
        bSuccess = pWork->inverseFFT();
        if (!bSuccess) {
            SHOW_ERROR("could not take inverse DFT of working stack");
            delete pGuess;
            delete pWork;
            return FALSE;
        }

#if SHOW_STATS
        calcStats(pWork, &dAvg, &dSD, 1.0 / pWork->calcFFTScaleFactor(), FALSE);
        sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
        SHOW_MESSAGE1("\tprojected guess average = %s", pcBuffer);
#endif

        if (g_iVerbose) SHOW_MESSAGE("calculating ratio of image / projected guess");

        /* calculate ratio:
           divide image by the convolved guess (stored in work) and store results in work.
           Normalize the convolved guess as we go in one combined step. */
        dNorm = pWork->calcFFTScaleFactor();
        pfImage = pImage->m_pfRealData;
        pfWork = pWork->m_pfRealData;
        iCount = pImage->m_iRealStackSize;
        while (iCount--) {
            dTemp = (*pfWork) / dNorm;
            if (dTemp < EM_LOWER_CUTOFF)
                dTemp = EM_LOWER_CUTOFF;
            *(pfWork++) = *(pfImage++) / dTemp;
        }

#if SHOW_STATS
        calcStats(pWork, &dAvg, &dSD, 1.0, TRUE);
        sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
        SHOW_MESSAGE1("\tratio average = %s", pcBuffer);
#endif

        if (g_iVerbose) SHOW_MESSAGE("projecting ratio");
        /* mirror lower half of ratio to upper half */
        mirrorLowerToUpperZ(pWork);

        /* take fourier transform of the ratio (stored in work) */
        bSuccess = pWork->forwardFFT();
        if (!bSuccess) {
            SHOW_ERROR("could not take DFT of ratio stack");
            delete pGuess;
            delete pWork;
            return FALSE;
        }

        /* multiply ratio (stored in work) by complex conjugate of OTF
                Complex multiply:
                (A + iB)(C + iD)* = (A + iB)(C - iD) = AC - iAD + iBC - (i^2)BD = (AC + BD) + i(BC - AD)
         */
        pfcWork = pWork->m_pfcComplexData;
        pfcOtf = pOTF->m_pfcComplexData;
        ASSERT(pfcWork);
        ASSERT(pfcOtf);
        iCount = pWork->m_iComplexStackSize;
        ASSERT(iCount > 0);
        while (iCount--) {
            fRe = (pfcWork->re * pfcOtf->re) + (pfcWork->im * pfcOtf->im);
            fIm = (pfcWork->im * pfcOtf->re) - (pfcWork->re * pfcOtf->im);
            pfcOtf++;
            pfcWork->re = fRe;
            pfcWork->im = fIm;
            pfcWork++;
        }

        /* take inverse fourier transform of convolved ratio (stored in work) */
        bSuccess = pWork->inverseFFT();
        if (!bSuccess) {
            SHOW_ERROR("could not take inverse DFT of ratio stack");
            delete pGuess;
            delete pWork;
            return FALSE;
        }

#if SHOW_STATS
        calcStats(pWork, &dAvg, &dSD, 1.0 / pWork->calcFFTScaleFactor(), TRUE);
        sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
        SHOW_MESSAGE1("\tprojected ratio average = %s", pcBuffer);
#endif

        /* 	There are a few things we need to do with the guess and projected ratio at this
                point:
                1) normalize the projected ratio because the inverse DFT used here did
                   not divide the results by WxHxD
                2) update the guess: new guess = old guess * projected ratio (stored in work)
                3) zero-out any pixels in guess whose absolute value is less than EM_LOWER_CUTOFF
                4) estimate mean squared error = 1/N sqrt(sum((image - current guess)^2))
    		
                Memory access and CPU cache misses really slow down most processors.
                Therefore, it is always better to read in a little data, do a lot of
                processing on it, and write the results back out, instead of go through
                the entire data set multiple times, performing a few floating point
                operations on each pass. So, we combine these four operations in one pass
                through the data. */

        if (g_iVerbose) SHOW_MESSAGE("calculating new guess from ratio");

        dNorm = pWork->calcFFTScaleFactor();
        iCount = pGuess->m_iRealStackSize;
        pfGuess = pGuess->m_pfRealData;
        pfWork = pWork->m_pfRealData;
        pfImage = pImage->m_pfRealData;
        dSumSq = 0.0;
        while (iCount--) {
            dOldGuess = *pfGuess;
            dNewGuess = dOldGuess * (*pfWork++) / dNorm;
#if 1
            dTemp = dNewGuess;
            if (dTemp < 0)
                dTemp = -dTemp;
            if (dTemp < EM_LOWER_CUTOFF)
                dNewGuess = 0;
#endif
            *(pfGuess++) = dNewGuess;
            dTemp = dNewGuess - dOldGuess;
            dTemp *= dTemp;
            dSumSq += dTemp;
        }
        dError = sqrt(dSumSq) / dNorm;

#if SHOW_STATS
        calcStats(pGuess, &dAvg, &dSD);
        sprintf(pcBuffer, "%g +/- %g SD", dAvg, dSD);
        SHOW_MESSAGE1("\tnew guess average = %s", pcBuffer);
#endif

        printProgress(pcProgress, iIteration + 1, iTotalIterations, &tStart, dError, progress);
        sprintf(pcProgressMessage, "%s", pcProgress);
        SHOW_MESSAGE(pcProgressMessage);
    }

    /* Copy the final guess to the image */
    memcpy(pImage->m_pfRealData, pGuess->m_pfRealData, pImage->m_iRealStackSize * sizeof (FLOAT));

    /* clean up */
    delete pGuess;
    delete pWork;

    return TRUE;
}
