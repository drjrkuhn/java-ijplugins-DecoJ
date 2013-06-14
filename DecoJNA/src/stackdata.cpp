/***************************************************************************
                          stackdata.cpp  -  description
                             -------------------
    begin                : Sun Sep 30 2001
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
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#include "deco.h"
#include "stackdata.h"

extern int g_iVerbose;

CStackData::
CStackData() {
    m_iLogicalWidth = m_iLogicalHeight = m_iLogicalDepth = 0;
    m_iRealWidth = m_iRealHeight = m_iRealDepth = 0;
    m_iRealLineSize = m_iRealPlaneSize = m_iRealStackSize = 0;
    m_iComplexWidth = m_iComplexHeight = m_iComplexDepth = 0;
    m_pfRealData = NULL;
    m_pfcComplexData = NULL;
    m_planForwardFFT = NULL;
    m_planBackwardFFT = NULL;
}

CStackData::
CStackData(int iLogicalWidth, int iLogicalHeight, int iLogicalDepth, BOOL bCreateFFTPlan, BOOL bQuickFFTPlan) {
    create(iLogicalWidth, iLogicalHeight, iLogicalDepth, bCreateFFTPlan, bQuickFFTPlan);
}

CStackData::
~CStackData() {
    destroy();
}

void CStackData::
calcSize(int iW, int iH, int iD) {
    m_iLogicalWidth = iW;
    m_iLogicalHeight = iH;
    m_iLogicalDepth = iD;

    m_iRealWidth = actualWidth(m_iLogicalWidth);
    m_iRealHeight = actualHeight(m_iLogicalHeight);
    m_iRealDepth = actualDepth(m_iLogicalDepth);

    m_iRealLineSize = m_iRealWidth;
    m_iRealPlaneSize = m_iRealLineSize * m_iRealHeight;
    m_iRealStackSize = m_iRealPlaneSize * m_iRealDepth;

    m_iComplexWidth = m_iRealWidth / 2;
    m_iComplexHeight = m_iRealHeight;
    m_iComplexDepth = m_iRealDepth;

    m_iComplexLineSize = m_iComplexWidth;
    m_iComplexPlaneSize = m_iComplexLineSize * m_iComplexHeight;
    m_iComplexStackSize = m_iComplexPlaneSize * m_iComplexDepth;
}

void CStackData::
create(int iWidth, int iHeight, int iDepth, BOOL bCreateFFTPlan, BOOL bQuickFFTPlan) {
    calcSize(iWidth, iHeight, iDepth);
    m_pfRealData = (FLOAT*) fftwf_malloc(m_iRealStackSize * sizeof (FLOAT));
    m_pfcComplexData = NULL;
    m_planForwardFFT = NULL;
    m_planBackwardFFT = NULL;
    m_bQuickPlan = bQuickFFTPlan;

    if (m_pfRealData == 0) {
        SHOW_ERROR1("not enough memory to create stack. At least %d free KB are required.", storageBytes(iWidth, iHeight, iDepth) / 1024);
    } else {
        /* Create the FFT plans */
        if (bCreateFFTPlan) {
            int iFlags = bQuickFFTPlan ? FFTW_ESTIMATE : FFTW_MEASURE;
            createInPlaceFFTPlans(m_pfRealData, iFlags);
        }

        /* zero out the memory */
        memset(m_pfRealData, 0, m_iRealStackSize * sizeof (FLOAT));
    }
}

BOOL CStackData::
setPlane(int zDestPlane, FLOAT* pfSrc, int iSrcLen) {
    //fprintf(stderr, "writing plane %d size %d\n", z, iSrcLen);
    if (!isValid()) {
        SHOW_ERROR("Not enough memory to store stack.");
        return FALSE;
    }

    if (iSrcLen < m_iLogicalWidth * m_iLogicalHeight) {
        SHOW_ERROR("source array is too short");
        return FALSE;
    }

    if (zDestPlane < 0 || zDestPlane >= m_iLogicalDepth) {
        SHOW_ERROR("invalid z plane");
        return FALSE;
    }

    int x, y;
    FLOAT* pfLine;
    FLOAT* pfPoint;
    int iWExtra = m_iRealWidth - m_iLogicalWidth;
    int iHExtra = m_iRealHeight - m_iLogicalHeight;
    size_t cbLineBytes = m_iRealLineSize * sizeof (FLOAT);
    size_t cbSrcLineBytes = m_iLogicalWidth * sizeof (FLOAT);
    FLOAT* pfSrcLine = pfSrc;

    pfLine = m_pfRealData + zDestPlane*m_iRealPlaneSize;
    for (y = 0; y < m_iLogicalHeight; y++) {
        pfPoint = pfLine;
        /* copy the line */
        memcpy(pfPoint, pfSrcLine, cbSrcLineBytes);
        pfPoint += m_iLogicalWidth;
        /* set any extra points at the end of the line to zero */
        x = iWExtra;
        while (x--) {
            *(pfPoint++) = 0;
        }
        pfSrcLine += m_iLogicalWidth;
        pfLine += m_iRealLineSize;
    } /* for y */
    /* set any extra lines at the end of the plane to zero */
    y = iHExtra;
    while (y--) {
        memset(pfLine, 0, cbLineBytes);
        pfLine += m_iRealLineSize;
    }

    return TRUE;
}

BOOL CStackData::
setPlane(int zDest, BYTE* pbSrc, int iSrcLen)
{
	FLOAT* pfTempPlane = new FLOAT[iSrcLen];
    if (pfTempPlane == NULL) {
        SHOW_ERROR("Not enough memory convert stack to float");
        return FALSE;
    }
	
	FLOAT* pf = pfTempPlane;
	BYTE* pb = pbSrc;
	int count = iSrcLen;
	while (count--) {
		*(pf++) = *(pb++);
	}
	BOOL bRet = setPlane (zDest, pfTempPlane, iSrcLen);
	delete pfTempPlane;
	return bRet;
}

BOOL CStackData::
setPlane(int zDest, WORD* pwSrc, int iSrcLen)
{
	FLOAT* pfTempPlane = new FLOAT[iSrcLen];
	if (pfTempPlane == NULL) {
		SHOW_ERROR("Not enough memory convert stack to float");
		return FALSE;
	}
	
	FLOAT* pf = pfTempPlane;
	WORD* pw = pwSrc;
	int count = iSrcLen;
	while (count--) {
		*(pf++) = *(pw++);
	}
	BOOL bRet = setPlane (zDest, pfTempPlane, iSrcLen);
	delete pfTempPlane;
	return bRet;
	
}


BOOL CStackData::
getPlane(FLOAT* pfDest, int iDestLen, int zSrcPlane) {
    //fprintf(stderr, "writing plane %d size %d\n", z, iDestLen);
    if (!isValid()) {
        SHOW_ERROR("Stack is empty.");
        return FALSE;
    }

    if (iDestLen < m_iLogicalWidth * m_iLogicalHeight) {
        SHOW_ERROR("destination array is too short");
        return FALSE;
    }

    if (zSrcPlane < 0 || zSrcPlane >= m_iLogicalDepth) {
        SHOW_ERROR("invalid z plane");
        return FALSE;
    }

    int y;
    FLOAT* pfLine;
    FLOAT* pfDestLine = pfDest;
    size_t cbDestLineBytes = m_iLogicalWidth * sizeof (FLOAT);

    pfLine = m_pfRealData + zSrcPlane*m_iRealPlaneSize;
    for (y = 0; y < m_iLogicalHeight; y++) {
        memcpy(pfDestLine, pfLine, cbDestLineBytes);
        pfDestLine += m_iLogicalWidth;
        pfLine += this->m_iRealLineSize;
    }
    return TRUE;
}

void CStackData::
destroy() {
    destroyFFTPlans();
    if (((void*) m_pfRealData) == ((void*) m_pfcComplexData)) {
        if (m_pfRealData != NULL) {
            fftwf_free(m_pfRealData);
        }
    } else {
        if (m_pfRealData != NULL) {
            fftwf_free(m_pfRealData);
        }
        if (m_pfcComplexData != NULL) {
            fftwf_free (m_pfcComplexData);
        }
    }
    m_iLogicalWidth = m_iLogicalHeight = m_iLogicalDepth = 0;
    m_iRealWidth = m_iRealHeight = m_iRealDepth = 0;
    m_iRealLineSize = m_iRealPlaneSize = m_iRealStackSize = 0;
    m_iComplexWidth = m_iComplexHeight = m_iComplexDepth = 0;
    m_pfRealData = 0;
    m_pfcComplexData = 0;
}

BOOL CStackData::
createInPlaceFFTPlans(FLOAT* pfData, int iFlags) {
    ASSERT(m_iLogicalWidth >= 0);
    ASSERT(m_iLogicalHeight >= 0);
    ASSERT(m_iLogicalDepth >= 0);

    if (iFlags == 0) {
        iFlags = FFTW_ESTIMATE;
    }

    /*
            FFTW assumes that arrays were created using C++'s multidimensional
        array declaration:
	
                    float pfArray[WIDTH][HEIGHT][DEPTH];
	   	
            Multidimensional arrays created this way are accessed using code
            such as:
	   		
                    float value = pfArray[x][y][z];

            Because of the "C" "right-to-left" precedence for the array operator
            "[]", The statement "float pfArray[WIDTH][HEIGHT][DEPTH];" is
            equivalent to "float pfArray[WIDTH]([HEIGHT]([DEPTH]))". Written
            in english (if the following can actually be called english), this
            translates to "allocate an array of WIDTH arrays of HEIGHT arrays
            of DEPTH floating point values and call it 'pfArray'."
	   	
            This creates a linear array in memory with the following order
		   	
                    x0y0z0, x0y0z1, x0y0z2, ..., x0y0zDEPTH, x0y1z0, x0y1z1, x0y1z2, ...
	   	
            That is, the Z values vary the quickest (are in sucessive memory
            locations) while X values vary the slowest. This scheme is known
            as "row-major" order, because in 2D arrays of this type, rows
            (y values) vary the quickest.
	   	
            Our CStackData stack arrays are allocated using another method:
	   	
                    float pfArray = (float*) malloc (WIDTH*HEIGHT*DEPTH*sizeof(float));
	   	
            and accessed using code such as:
	   		
                    float value = pfArray[x + WIDTH*y + (WIDTH*HEIGHT)*z];
	   	
            This creates a linear array in memory with the following order:
	   		
                    x0y0z0, x1y0z0, x2y0z0, ..., xWIDTHy0z0, x0y1z0, x1y1z0, x2y1z0, ...
	   	
            In these arrays, neighboring X values are next to each other in memory
            (vary the quickest), while neigboring Z values are separated by
            WIDTH*HEIGHT memory locations (vary the slowest). This scheme is known
            as "column-major" order, because in 2D arrays of this latter type,
            columns (x values) vary the quickest.
	   		
            This second method (column-major) is the one most widely used by
            images and imaging applications because arrays may be dynamically
            allocated in "C", while the first method (row-major) can only be
            used to allocate static arrays in "C".
            (I have *never* seen or even heard of an image file where y values
            were right next to each other in the file. Consecutive x values
            are almost *always* consecutive in the file.)
	   	
            So FFTW expects the values to be in a different order than most of
            the rest of the world. Heck, even multidimensional arrays in FORTRAN
            use the "column-major" ordering scheme.
	   	
            Luckily, this "feature" is not a problem at all. We can just reverse
            the order of WIDTH, HEIGHT, DEPTH dimensions we pass to FFTW, making
            it DEPTH, HEIGHT, WIDTH. That's all it takes!
     */

    /* always do "in-place" transforms */

    /* Usually only one pointer to the data is defined at any time, but in reality they
       both point to the same place. */
    ASSERT(pfData != NULL);

    /* NOTE: If the fftw3 planning flag is set to anything other than
     * FFTW_ESTIMATE, the planning phase overwrites any data while measuring
     * the best method to use. As a result, this method is called before
     * any data is written
     */

    /**#### Force flags to FFTW_ESTIMATE for testing purposes ####*/

    if (g_iVerbose) SHOW_MESSAGE("creating forward FFT plan");
    m_planForwardFFT = fftwf_plan_dft_r2c_3d(m_iLogicalDepth, m_iLogicalHeight, m_iLogicalWidth,
            pfData, (fftwf_complex*)pfData, FFTW_ESTIMATE);
    if (m_planForwardFFT == NULL) {
        return FALSE;
    }

    if (g_iVerbose) SHOW_MESSAGE("creating backward FFT plan");
    m_planBackwardFFT = fftwf_plan_dft_c2r_3d(m_iLogicalDepth, m_iLogicalHeight, m_iLogicalWidth,
            (fftwf_complex*)pfData, pfData, FFTW_ESTIMATE);

    if (m_planBackwardFFT == NULL) {
        fftwf_destroy_plan(m_planForwardFFT);
        return FALSE;
    }
    return TRUE;
}

void CStackData::
destroyFFTPlans() {
    if (m_planForwardFFT) {
        fftwf_destroy_plan(m_planForwardFFT);
    }
    if (m_planBackwardFFT) {
        fftwf_destroy_plan(m_planBackwardFFT);
    }
    m_planForwardFFT = NULL;
    m_planBackwardFFT = NULL;
}

BOOL CStackData::
forwardFFT() {
    if (m_pfRealData == NULL || m_planForwardFFT == NULL) {
        return FALSE;
    }

    fftwf_execute(m_planForwardFFT);
    /* transforms are always in-place, so swap pointers */
    m_pfcComplexData = (FCOMPLEX*) m_pfRealData;
    m_pfRealData = 0;
    return TRUE;
}

BOOL CStackData::
inverseFFT() {
    if (m_pfcComplexData == NULL || m_planBackwardFFT == NULL) {
        return false;
    }

    fftwf_execute(m_planBackwardFFT);
    /* transforms are always in-place, so swap pointers */
    m_pfRealData = (FLOAT*) m_pfcComplexData;
    m_pfcComplexData = 0;
    return TRUE;
}

void CStackData::
normalizeInverseFFT() {
    ASSERT(m_pfRealData != 0);

    int iLen = m_iRealStackSize;
    double dScale = 1.0 / calcFFTScaleFactor();
    ASSERT(dScale != 0.0);
    FLOAT* pf = m_pfRealData;
    while (iLen--) {
        *(pf++) *= dScale;
    }
}

void CStackData::
pad(int iNewWidth, int iNewHeight, int iNewDepth, int iPadCenter) {
    ASSERT(m_pfRealData != 0);
    ASSERT(iNewWidth >= m_iLogicalWidth);
    ASSERT(iNewHeight >= m_iLogicalHeight);
    ASSERT(iNewDepth >= m_iLogicalDepth);

    CStackData* pDest = new CStackData(iNewWidth, iNewHeight, iNewDepth, hasFFTPlan(), m_bQuickPlan);
    if (!pDest->isValid()) {
        FATAL_ERROR("not enough memory to pad stack");
    }

    if (iPadCenter) {
        /* for clarity */
        int iSrcW = this->m_iLogicalWidth;
        int iSrcH = this->m_iLogicalHeight;
        int iSrcD = this->m_iLogicalDepth;

        int iDestW = pDest->m_iLogicalWidth;
        int iDestH = pDest->m_iLogicalHeight;
        int iDestD = pDest->m_iLogicalDepth;

        int iHalfW = iSrcW / 2;
        int iHalfH = iSrcH / 2;
        int iHalfD = iSrcD / 2;

        /* copy each of the eight corners of this stack to the dest stack */
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                0,					0,					0,
                0,					0,					0);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                iSrcW - iHalfW,		0,					0,
                iDestW - iHalfW,	0,					0);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                0,					iSrcH - iHalfH,		0,
                0,					iDestH - iHalfH,	0);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                iSrcW - iHalfW,		iSrcH - iHalfH,		0,
                iDestW - iHalfW,	iDestH - iHalfH,	0);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                0,					0,					iSrcD - iHalfD,
                0,					0,					iDestD - iHalfD);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                iSrcW - iHalfW,		0,					iSrcD - iHalfD,
                iDestW - iHalfW,	0,					iDestD - iHalfD);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                0,					iSrcH - iHalfH,		iSrcD - iHalfD,
                0,					iDestH - iHalfH,	iDestD - iHalfD);
        pDest->copySubStack(this, iHalfW, iHalfH, iHalfD,
                iSrcW - iHalfW,		iSrcH - iHalfH,		iSrcD - iHalfD,
                iDestW - iHalfW,	iDestH - iHalfH,	iDestD - iHalfD);
    } else {
        pDest->copySubStack(this, this->m_iLogicalWidth, this->m_iLogicalHeight, this->m_iLogicalDepth,
                0, 0, 0, 0, 0, 0);
    }

    /* steal the destination stack's data and put it in this stack */
    calcSize(iNewWidth, iNewHeight, iNewDepth);
    fftwf_free(this->m_pfRealData);
    this->m_pfRealData = pDest->m_pfRealData;
    this->m_pfcComplexData = 0;

    /* steal the destination stack's FFT plan and put it in this stack */
    destroyFFTPlans();
    this->m_planForwardFFT = pDest->m_planForwardFFT;
    this->m_planBackwardFFT = pDest->m_planBackwardFFT;

    /* set the destination's data pointer and plans to zero so it won't free the memory when destroyed */
    pDest->m_pfRealData = 0;
    pDest->m_planForwardFFT = 0;
    pDest->m_planBackwardFFT = 0;
    delete pDest;
}

void CStackData::
copySubStack(CStackData* pSrc, int iW, int iH, int iD,
        int iSrcX, int iSrcY, int iSrcZ,
        int iDestX, int iDestY, int iDestZ) {
    ASSERT(m_pfRealData != 0);
    ASSERT(pSrc->m_pfRealData != 0);
    CStackData* pDest = this;

    /* check dimensions */
    ASSERT((iSrcX >= 0) && ((iSrcX + iW) <= pSrc->m_iLogicalWidth));
    ASSERT((iSrcY >= 0) && ((iSrcY + iH) <= pSrc->m_iLogicalHeight));
    ASSERT((iSrcZ >= 0) && ((iSrcZ + iD) <= pSrc->m_iLogicalDepth));
    ASSERT((iDestX >= 0) && ((iDestX + iW) <= pDest->m_iLogicalWidth));
    ASSERT((iDestY >= 0) && ((iDestY + iH) <= pDest->m_iLogicalHeight));
    ASSERT((iDestZ >= 0) && ((iDestZ + iD) <= pDest->m_iLogicalDepth));

    FLOAT *pfSrcPlane, *pfSrcLine;
    FLOAT *pfDestPlane, *pfDestLine;
    size_t cbCopyBytes = iW * sizeof (FLOAT);

    /* point to start of copy */
    pfSrcPlane = pSrc->m_pfRealData + iSrcX + iSrcY * pSrc->m_iRealLineSize + iSrcZ * pSrc->m_iRealPlaneSize;
    pfDestPlane = pDest->m_pfRealData + iDestX + iDestY * pDest->m_iRealLineSize + iDestZ * pDest->m_iRealPlaneSize;

    /* copy one line at a time */
    int y, z;
    for (z = 0; z < iD; z++) {
        pfSrcLine = pfSrcPlane;
        pfDestLine = pfDestPlane;
        for (y = 0; y < iH; y++) {
            memcpy(pfDestLine, pfSrcLine, cbCopyBytes);
            pfSrcLine += pSrc->m_iRealLineSize;
            pfDestLine += pDest->m_iRealLineSize;
        }
        pfSrcPlane += pSrc->m_iRealPlaneSize;
        pfDestPlane += pDest->m_iRealPlaneSize;
    }
}


