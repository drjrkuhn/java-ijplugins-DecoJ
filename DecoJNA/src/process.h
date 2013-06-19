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
