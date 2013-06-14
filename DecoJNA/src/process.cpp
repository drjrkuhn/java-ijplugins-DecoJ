/***************************************************************************
                          procmain.cpp  -  description
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
#include <stdlib.h>

#include "deco.h"
#include "stackdata.h"
#include "process.h"

int g_iVerbose = 1;

BOOL setNumThreads(int nThreads)
{
#ifndef NOTHREADS
    BOOL bRet = fftwf_init_threads();
    if (!bRet) {
        return FALSE;
    }
    fftwf_plan_with_nthreads(nThreads);
#endif
	return TRUE;
}

void setVerbose(int iVerbose)
{
    g_iVerbose = iVerbose;
}

int getStackWidth(CStackData* pStack)
{
    return pStack->m_iLogicalWidth;
}

int getStackHeight(CStackData* pStack)
{
    return pStack->m_iLogicalHeight;
}

int getStackDepth(CStackData* pStack)
{
    return pStack->m_iLogicalDepth;
}

CStackData* createEmptyStack(int iWidth, int iHeight, int iDepth, BOOL bCreateFFTPlan, BOOL bQuickFFTPlan)
{
    CStackData* stack = new CStackData(iWidth, iHeight, iDepth, bCreateFFTPlan, bQuickFFTPlan);
    if (stack->isValid()) {
        return stack;
    }
    delete stack;
    return NULL;
}

BOOL setFloatPlane(CStackData* pDestStack, int zDestPlane, FLOAT* pfSrc, int iSrcLen)
{
    if (pDestStack->isValid()) {
        if (pDestStack->setPlane(zDestPlane, pfSrc, iSrcLen)) {
            return TRUE;
        }
    }
    return FALSE;
}

BOOL setBytePlane(CStackData* pDestStack, int zDestPlane, BYTE* pbSrc, int iSrcLen)
{
    if (pDestStack->isValid()) {
        if (pDestStack->setPlane(zDestPlane, pbSrc, iSrcLen)) {
            return TRUE;
        }
    }
    return FALSE;
}

BOOL setShortPlane(CStackData* pDestStack, int zDestPlane, WORD* pwSrc, int iSrcLen)
{
    if (pDestStack->isValid()) {
        if (pDestStack->setPlane(zDestPlane, pwSrc, iSrcLen)) {
            return TRUE;
        }
    }
    return FALSE;
}

BOOL destroyStack(CStackData* pStack)
{
    delete pStack;
	return TRUE;
}

BOOL getPlane(FLOAT* pfDest, int iDestLen, CStackData* pSrcStack, int zSrcPlane)
{
    return pSrcStack->getPlane(pfDest, iDestLen, zSrcPlane);
}
