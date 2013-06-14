/***************************************************************************
                          procmap.cpp  -  description
                             -------------------
    begin                : Sat Oct 6 2001
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

BOOL processMAP (CStackData* pImage, CStackData* pPsf, double dAlpha)
{
	int iResult;
	
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
		
	//BOOL bSuccess;

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

	/* forward transform of the image */
	if (g_iVerbose) SHOW_MESSAGE ("calculating forward FFT of the image");
	iResult = pImage->forwardFFT ();
	if (!iResult) { SHOW_ERROR ("could not create forward FFT of the image"); return FALSE; }
	CStackData* pFTImage = pImage;
	pImage = 0;
	
  	if (g_iVerbose) SHOW_MESSAGE ("calculating MAP version of IMAGE/OTF");
  	/* 	divide the IMAGE by the OTF. If the OTF magnitude is less than the threshold (fFactor),
  		then we assume that we are outside of the bandwidth of the OTF and set the resulting
  	    value to zero. */
		
  	const double dPi = 2.0 * acos(0.0);
  	
  	double dDenom, dRe, dIm;
  	FCOMPLEX* pfcI = pFTImage->m_pfcComplexData;
  	FCOMPLEX* pfcO = pOTF->m_pfcComplexData;
  	int iCount = pFTImage->m_iComplexStackSize;
  	int x, y, z;
  	
  	int iLogWidth = pFTImage->m_iComplexWidth;
  	int iLogHeight = pFTImage->m_iComplexHeight;
  	int iLogDepth = pFTImage->m_iComplexDepth;
  	
  	int iCpxWidth = pFTImage->m_iComplexWidth;
  	int iCpxHeight = pFTImage->m_iComplexHeight;
  	int iCpxDepth = pFTImage->m_iComplexDepth;
  	
  	int iMaxY = iLogHeight / 2;
  	int iMaxZ = iLogDepth / 2;

	double dScaleX = 2.0 * dPi / iLogWidth;
	double dScaleY = 2.0 * dPi / iLogHeight;
	double dScaleZ = 2.0 * dPi / iLogDepth;
	
	double dOmegaX, dOmegaY, dOmegaZ;
	double dOmegaXSq, dOmegaYSq, dOmegaZSq, dSumOmegaYSqZSq;
	double dTwoAlpha = 2.0 * dAlpha;

	for (z=0; z<iCpxDepth; z++) {
		if (z <= iMaxZ)
			dOmegaZ = z * dScaleZ;
		else
			dOmegaZ = (z - iLogDepth) * dScaleZ;
		dOmegaZSq = dOmegaZ * dOmegaZ;
		for (y=0; y<iCpxHeight; y++) {
			if (y <= iMaxY)
				dOmegaY = y * dScaleY;
			else
				dOmegaY = (y - iLogHeight) * dScaleY;
			dOmegaYSq = dOmegaY * dOmegaY;
			dSumOmegaYSqZSq = dOmegaZSq + dOmegaYSq;
			for (x=0; x<iCpxWidth; x++) {
				/* note: since this was a real->complex DFT, only the left half of the X plane is stored */
				if ((pfcI->re != 0) || (pfcI->im != 0)) {
					dOmegaX = x * dScaleX;
					dOmegaXSq = dOmegaX * dOmegaX;
					
            		dDenom = (pfcO->re * pfcO->re + pfcO->im * pfcO->im) / dNorm
            					+ dTwoAlpha * (dOmegaXSq + dSumOmegaYSqZSq);
        			dRe = (pfcI->re * pfcO->re + pfcI->im * pfcO->im) / dDenom;
        			dIm = (pfcI->im * pfcO->re - pfcI->im * pfcO->im) / dDenom;
            		pfcI->re = (FLOAT)dRe;
            		pfcI->im = (FLOAT)dIm;
            	}
  				pfcO++;
  				pfcI++;
  			} /* for x */
  		} /* for y */
  	} /* for z */
	
	/* inverse transform of the image */
	if (g_iVerbose) SHOW_MESSAGE ("calculating inverse FFT of the deconvolved image");
	iResult = pFTImage->inverseFFT ();
	if (!iResult) { SHOW_ERROR ("could not create inverse FFT of the deconvolved image"); }
	pImage = pFTImage;
	pFTImage = 0;
	
	/* normalize the final image and get rid of negative values */
	FLOAT* pf = pImage->m_pfRealData;
	dDenom = pImage->calcFFTScaleFactor ();
	iCount = pImage->m_iRealStackSize;
	while (iCount--) {
		if ((*pf) < 0)
			*pf = 0;
		else
			*pf /= dDenom;
		pf++;
	}

	return TRUE;
}
