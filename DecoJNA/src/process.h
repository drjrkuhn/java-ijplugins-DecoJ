/***************************************************************************
                          process.h  -  description
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

#ifndef PROCESS_H
#define PROCESS_H

extern "C" {

extern int g_iVerbose;

BOOL setNumThreads(int nThreads);
void setVerbose(int iVerbose);
CStackData* createEmptyStack(int iWidth, int iHeight, int iDepth, BOOL bCreateFFTPlan, BOOL bQuickFFTPlan);
BOOL setFloatPlane(CStackData* pDestStack, int zDestPlane, FLOAT* pfSrc, int iSrcLen);
BOOL setBytePlane(CStackData* pDestStack, int zDestPlane, BYTE* pbSrc, int iSrcLen);
BOOL setShortPlane(CStackData* pDestStack, int zDestPlane, WORD* pwSrc, int iSrcLen);

BOOL destroyStack(CStackData* pStack);
BOOL getPlane(FLOAT* pfDest, int iDestLen, CStackData* pSrcStack, int zSrcPlane);
int getStackWidth(CStackData* pStack);
int getStackHeight(CStackData* pStack);
int getStackDepth(CStackData* pStack);

BOOL processLLS (CStackData* pImage, CStackData* pPsf, double dThresh);
BOOL processMAP (CStackData* pImage, CStackData* pPsf, double dThresh);
BOOL processEM (CStackData* pImage, CStackData* pPsf, int iTotalIterations, ProgressFunc progress);

}

#endif /* PROCESS_H */
