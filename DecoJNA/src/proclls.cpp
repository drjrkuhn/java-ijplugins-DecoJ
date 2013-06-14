/***************************************************************************
                          proclls.cpp  -  description
                             -------------------
    begin                : Fri Oct 5 2001
    copyright            : (C) 2001 by Jeffrey Kuhn
    email                : jeffrey.kuhn@yale.edu
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#include <stdio.h>
#include <math.h>

#include "deco.h"
#include "stackdata.h"
#include "process.h"

BOOL processLLS (CStackData* pImage, CStackData* pPsf, double dThresh)
{
	int iResult;
//        fprintf(stderr, "pImage=%p  pPsf=%p\n", pImage, pPsf);
//        fprintf(stderr, "pImage size = %d x %d x %d\n", pImage->iLogicalWidth, pImage->iLogicalHeight, pImage->iLogicalDepth);
//        fprintf(stderr, "pPsf size = %d x %d x %d\n", pPsf->iLogicalWidth, pPsf->iLogicalHeight, pPsf->iLogicalDepth);
//
//        double dSum = 0.0;
//        for (int i=0; i<pImage->iRealStackSize; i++) {
//            pImage->pfRealData[i] = i % 126;
//            dSum += pImage->pfRealData[i];
//        }
//        dSum /= pImage->iRealStackSize;
//        fprintf(stderr, "pImage avg = %f\n", dSum);
//        dSum = 0.0;
//        for (int i=0; i < pPsf->iRealStackSize; i++)
//            dSum += pPsf->pfRealData[i]*1000;
//        dSum /= pPsf->iRealStackSize;
//        fprintf(stderr, "pPsf avg = %f\n", dSum);
//
//        pImage->createFFT();
//        pImage->forwardFFT();
//        pImage->inverseFFT();
//        //pImage->pfRealData = (FLOAT*)pImage->pfcComplexData;
//        pImage->destroyFFT();
//        return true;
	
	if (pImage->m_iLogicalWidth != pPsf->m_iLogicalWidth) {
		SHOW_ERROR ("image and Psf do not have the same width.");
		return FALSE;
	}
		
	if (pImage->m_iLogicalHeight != pPsf->m_iLogicalHeight) {
		SHOW_ERROR ("image and Psf do not have the same height.");
		return FALSE;
	}
		
	if (pImage->m_iLogicalDepth != pPsf->m_iLogicalDepth) {
		SHOW_ERROR ("image and Psf do not have the same depth.");
		return FALSE;
	}
		
	/* create the OTF */
	if (g_iVerbose) SHOW_MESSAGE ("calculating OTF from Psf");
	iResult = pPsf->forwardFFT ();
	if (!iResult) { SHOW_ERROR ("could not create the OTF from the Psf"); return FALSE; }
	CStackData* pOTF = pPsf;	/* a reference to make the code more readable */
	pPsf = 0;
	
	/* calculate the NORM of the OTF. This is the average value for the entire image, or the
	   magnitude in the fourier plane for frequency=0. We don't actually divide the OTF by
	   NORM, because this can be combined with another step */

	double dNorm = sqrt( pOTF->m_pfcComplexData[0].re * pOTF->m_pfcComplexData[0].re
			+ pOTF->m_pfcComplexData[0].im * pOTF->m_pfcComplexData[0].im);
        char pcBuffer[128];
        sprintf(pcBuffer, "%f", dNorm);
        if (g_iVerbose) SHOW_MESSAGE1 ("OTF Norm = %s", pcBuffer);

	
	/* forward transform of the image */
	if (g_iVerbose) SHOW_MESSAGE ("calculating forward FFT of the image");
	iResult = pImage->forwardFFT ();
	if (!iResult) { SHOW_ERROR ("could not create forward FFT of the image"); return FALSE; }
	CStackData* pFTImage = pImage;
	pImage = 0;
	
  	if (g_iVerbose) SHOW_MESSAGE ("calculating LLS version of IMAGE/OTF");
  	/* 	divide the IMAGE by the OTF. If the OTF magnitude is less than the threshold (fFactor),
  		then we assume that we are outside of the bandwidth of the OTF and set the resulting
  	    value to zero. */
		
  	double dDenom, dRe, dIm;
  	FCOMPLEX* pfcI = pFTImage->m_pfcComplexData;
  	FCOMPLEX* pfcO = pOTF->m_pfcComplexData;
  	int iCount = pFTImage->m_iComplexStackSize;
  	
  	while (iCount--) {
  		dDenom = pfcO->re * pfcO->re + pfcO->im * pfcO->im;
  		dDenom /= dNorm;
  		if (dDenom > dThresh) {
  			dRe = (pfcI->re * pfcO->re + pfcI->im * pfcO->im) / dDenom;
  			dIm = (pfcI->im * pfcO->re - pfcI->im * pfcO->im) / dDenom;
  		} else {
  			dRe = dIm = 0;
  		}
  		pfcO++;
  		pfcI->re = (FLOAT)dRe;
  		pfcI->im = (FLOAT)dIm;
  		pfcI++;
  	}
	
	/* inverse transform of the image */
	if (g_iVerbose) SHOW_MESSAGE ("calculating inverse FFT of the deconvolved image");
	iResult = pFTImage->inverseFFT ();
	if (!iResult) { SHOW_ERROR ("could not create inverse FFT of the deconvolved image"); }
	pImage = pFTImage;
	pFTImage = 0;
	
	/* normalize the final image and get rid of negative values */
	FLOAT* pf = pImage->m_pfRealData;
	if (g_iVerbose) SHOW_MESSAGE ("calculating FFT scale factor");
	dDenom = pImage->calcFFTScaleFactor ();
	iCount = pImage->m_iRealStackSize;
	if (g_iVerbose) SHOW_MESSAGE ("scaling image");
	while (iCount--) {
		if ((*pf) < 0)
			*pf = 0;
		else
			*pf /= dDenom;
		pf++;
	}

	if (g_iVerbose) SHOW_MESSAGE ("Done");
	return TRUE;
}
