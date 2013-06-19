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


#ifndef DECOP_H
#define DECOP_H

#include <time.h>

/* standard data types */
typedef int		BOOL;
typedef unsigned char 	BYTE;
typedef unsigned short 	WORD;
typedef unsigned int	DWORD;
typedef float 		FLOAT;
typedef struct {float re, im; }	FCOMPLEX;

#define FALSE	0
#define TRUE	(!FALSE)

///* deconvolution methods */
//#define METHOD_UNDEFINED	0
//#define METHOD_LLS		1
//#define METHOD_MAP		2
//#define METHOD_EM		3
//#define METHOD_IDIV		4
//#define METHOD_WLS		5
//#define METHOD_WCAR		6
//#define METHOD_BLIND		7
//
///* size of file data */
//#define DATA_SIZE_MASK			0x000f
//#define DATA_SIZE_UNDEFINED		0x0000
//#define DATA_SIZE_BYTE			0x0001
//#define DATA_SIZE_WORD			0x0002
//#define DATA_SIZE_FLOAT			0x0003
//
///* byte order of file data */
//#define DATA_ENDIAN_MASK		0x00f0
//#define DATA_ENDIAN_UNDEFINED	0x0000
//#define DATA_ENDIAN_BIG			0x0010
//#define DATA_ENDIAN_LITTLE		0x0020
//#define DATA_ENDIAN_NETWORK		(DATA_ENDIAN_LITTLE)
//#define DATA_ENDIAN_NATIVE		0x0030
//
//class CSocketStream;		// forward definition of class
//
//extern	int 	g_iMethod;
//extern	int		g_iNumberIterations;
//extern	int		g_iContinue;
//extern	char*	g_pcContinueFileName;
//extern	int		g_iBackupEvery;
//extern	char*	g_pcBackupFileName;
//extern	int		g_iNativeDataEndian;
//extern	char*	g_pcInputFileName;
//extern	int		g_iInputDataEndian;
//extern	int 	g_iInputFileSkip;
//extern	int		g_iInputDataSize;
//extern	int		g_iInputDataType;
//extern	int		g_iInputDataWidth;
//extern	int		g_iInputDataHeight;
//extern	int		g_iInputDataDepth;
//extern	char*	g_pcPsfFileName;
//extern	int		g_iPsfDataEndian;
//extern	int		g_iPsfFileSkip;
//extern	int		g_iPsfDataSize;
//extern	int		g_iPsfDataType;
//extern	int		g_iPsfDataWidth;
//extern	int		g_iPsfDataHeight;
//extern	int		g_iPsfDataDepth;
//extern	char*	g_pcPsfOutputFileName;
//extern	char*	g_pcOutputFileName;
//extern	int		g_iOutputDataText;
//extern	int		g_iOutputDataEndian;
//extern	int		g_iOutputDataType;
//extern	int		g_iOutputDataWidth;
//extern	int		g_iOutputDataHeight;
//extern	int		g_iOutputDataDepth;
//extern	char*	g_pcLogFileName;
//extern  char*	g_pcWisdomFileName;
//extern  char*	g_pcProgressFileName;
//extern  FILE*	g_pLogFile;
//extern	int		g_iVerbose;
//extern	int		g_iNice;
//extern	int		g_iCalcMemory;
//extern	float	g_fLlsThreshold;
//extern	float	g_fMapAlpha;
//extern	int		g_iChildID;
//extern CSocketStream*	g_pssIOSocket;
//


void swabWordArray(unsigned short* psSrc, int iLen);
void swabFloatArray(float* pfSrc, int iLen);
int nativeEndian();

char* addStrings (const char* pcA, const char* pcB);
const char* dataSizeToString (int iSize);
size_t dataSizeToBytes (int iSize);
const char* dataEndianToString (int iEndian);
const char* methodToString (int iMethod);
const char* boolToString (int b);

void resetGlobals ();
void printAllOptions ();

typedef void (*ProgressFunc)(int iIteration, int nTotalIterations, double dError, int nSecRemaining);

char* printProgress (char* pcBuffer, int i, int iTotal, time_t* ptStart, double dError, ProgressFunc progress);
void writeProgressFile (char* pcBuffer, const char* pcFileName);

#define SHOWMSG_MESSAGE		0
#define SHOWMSG_WARNING		1
#define SHOWMSG_ERROR		2
#define SHOWMSG_FATAL		3

void showMsg (int,const char*,int,const char*);
void showMsg1 (int,const char*,int,const char*,const void*);
void showMsg2 (int,const char*,int,const char*,const void*,const void*);
void showMsg3 (int,const char*,int,const char*,const void*,const void*,const void*);
void showMsg4 (int,const char*,int,const char*,const void*,const void*,const void*,const void*);

#define SHOW_MESSAGE(fmt)				showMsg(SHOWMSG_MESSAGE,__FILE__,__LINE__,fmt)
#define SHOW_MESSAGE1(fmt,s1)			showMsg1(SHOWMSG_MESSAGE,__FILE__,__LINE__,fmt,(const void*)(s1))
#define SHOW_MESSAGE2(fmt,s1,s2)		showMsg2(SHOWMSG_MESSAGE,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2))
#define SHOW_MESSAGE3(fmt,s1,s2,s3)		showMsg3(SHOWMSG_MESSAGE,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3))
#define SHOW_MESSAGE4(fmt,s1,s2,s3,s4)	showMsg4(SHOWMSG_MESSAGE,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3),(const void*)(s4))

#define SHOW_WARNING(fmt)				showMsg(SHOWMSG_WARNING,__FILE__,__LINE__,fmt)
#define SHOW_WARNING1(fmt,s1)			showMsg1(SHOWMSG_WARNING,__FILE__,__LINE__,fmt,(const void*)(s1))
#define SHOW_WARNING2(fmt,s1,s2)		showMsg2(SHOWMSG_WARNING,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2))
#define SHOW_WARNING3(fmt,s1,s2,s3)		showMsg3(SHOWMSG_WARNING,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3))
#define SHOW_WARNING4(fmt,s1,s2,s3,s4)	showMsg4(SHOWMSG_WARNING,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3),(const void*)(s4))

#define SHOW_ERROR(fmt)					showMsg(SHOWMSG_ERROR,__FILE__,__LINE__,fmt)
#define SHOW_ERROR1(fmt,s1)				showMsg1(SHOWMSG_ERROR,__FILE__,__LINE__,fmt,(const void*)(s1))
#define SHOW_ERROR2(fmt,s1,s2)			showMsg2(SHOWMSG_ERROR,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2))
#define SHOW_ERROR3(fmt,s1,s2,s3)		showMsg3(SHOWMSG_ERROR,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3))
#define SHOW_ERROR4(fmt,s1,s2,s3,s4)	showMsg4(SHOWMSG_ERROR,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3),(const void*)(s4))

//** NOTE: The FATAL_ERROR functions throw an exception of type integer, rather than exiting
//** the program completely. This allows the main routine to handle errors in whatever
//** manner it wants with a "try ... catch(int e)" clause

#define FATAL_ERROR(fmt)				showMsg(SHOWMSG_FATAL,__FILE__,__LINE__,fmt)
#define FATAL_ERROR1(fmt,s1)			showMsg1(SHOWMSG_FATAL,__FILE__,__LINE__,fmt,(const void*)(s1))
#define FATAL_ERROR2(fmt,s1,s2)			showMsg2(SHOWMSG_FATAL,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2))
#define FATAL_ERROR3(fmt,s1,s2,s3)		showMsg3(SHOWMSG_FATAL,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3))
#define FATAL_ERROR4(fmt,s1,s2,s3,s4)	showMsg4(SHOWMSG_FATAL,__FILE__,__LINE__,fmt,(const void*)(s1),(const void*)(s2),(const void*)(s3),(const void*)(s4))

#ifndef ASSERT
#define ASSERT(cond)  					{if(!(cond)){SHOW_MESSAGE2("ASSERT failed: %s line %d",__FILE__,__LINE__);exit (-1);}}
#endif

#endif /* DECOP_H */
