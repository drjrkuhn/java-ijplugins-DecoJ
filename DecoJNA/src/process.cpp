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
