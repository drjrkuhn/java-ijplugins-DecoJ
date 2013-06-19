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
#include <time.h>

#include <unistd.h>
#include <sys/types.h>

#include "deco.h"

#define MESSAGE_OUT     stderr
#define ERROR_OUT       stderr

#define MSG_BUFFER_LEN		2048
static char pcMsgBuffer[MSG_BUFFER_LEN];

char*
addStrings(const char* pcA, const char* pcB) {
    char* pcRes = (char*) malloc(strlen(pcA) + strlen(pcB));
    strcpy(pcRes, pcA);
    strcat(pcRes, pcB);
    return pcRes;
}

static void
showMsgPrefix(int type, const char* file, int line) {
    pcMsgBuffer[0] = '\0';
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, "error (%s:%d): ", file, line);
            break;
        case SHOWMSG_WARNING:
            fprintf(ERROR_OUT, "warning (%s:%d): ", file, line);
            break;
        case SHOWMSG_MESSAGE:
            break;
    }
}

static void
showMsgPostfix(int type, const char* file, int line) {
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, "\n");
            break;
        case SHOWMSG_WARNING:
        case SHOWMSG_MESSAGE:
            fprintf(MESSAGE_OUT, "\n");
            break;
    }
    if (type == SHOWMSG_FATAL) {
        throw EXIT_FAILURE;
    }
}

void
showMsg(int type, const char* file, int line, const char* fmt) {
    showMsgPrefix(type, file, line);
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, fmt);
            break;
        case SHOWMSG_WARNING:
        case SHOWMSG_MESSAGE:
            fprintf(MESSAGE_OUT, fmt);
            break;
    }
    showMsgPostfix(type, file, line);
}

void
showMsg1(int type, const char* file, int line, const char* fmt, const void* s1) {
    showMsgPrefix(type, file, line);
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, fmt, s1);
            break;
        case SHOWMSG_WARNING:
        case SHOWMSG_MESSAGE:
            fprintf(MESSAGE_OUT, fmt, s1);
            break;
    }
    showMsgPostfix(type, file, line);
}

void
showMsg2(int type, const char* file, int line, const char* fmt, const void* s1, const void* s2) {
    showMsgPrefix(type, file, line);
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, fmt, s1, s2);
            break;
        case SHOWMSG_WARNING:
        case SHOWMSG_MESSAGE:
            fprintf(MESSAGE_OUT, fmt, s1, s2);
            break;
    }
    showMsgPostfix(type, file, line);
}

void
showMsg3(int type, const char* file, int line, const char* fmt, const void* s1, const void* s2, const void* s3) {
    showMsgPrefix(type, file, line);
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, fmt, s1, s2, s3);
            break;
        case SHOWMSG_WARNING:
        case SHOWMSG_MESSAGE:
            fprintf(MESSAGE_OUT, fmt, s1, s2, s3);
            break;
    }
    showMsgPostfix(type, file, line);
}

void
showMsg4(int type, const char* file, int line, const char* fmt, const void* s1, const void* s2, const void* s3, const void* s4) {
    showMsgPrefix(type, file, line);
    switch (type) {
        case SHOWMSG_ERROR:
        case SHOWMSG_FATAL:
            fprintf(ERROR_OUT, fmt, s1, s2, s3, s4);
            break;
        case SHOWMSG_WARNING:
        case SHOWMSG_MESSAGE:
            fprintf(MESSAGE_OUT, fmt, s1, s2, s3, s4);
            break;
    }
    showMsgPostfix(type, file, line);
}

char*
printProgress(char* pcBuffer, int i, int iTotal, time_t* ptStart, double dError, ProgressFunc progress) {
    if (i < 1) {
        ASSERT(ptStart != 0);
        time(ptStart);
        ASSERT(pcBuffer != 0);
        pcBuffer[0] = '\0';
        return pcBuffer;
    }

    time_t tNow;
    time(&tNow);
    double dSecondsElapsed = difftime(tNow, *ptStart);
    double dEstimateTotal = iTotal * dSecondsElapsed / i;
    //double dSecondsRemaining = dEstimateTotal - dSecondsElapsed;

#if 0
    int iEHr = (int) (dSecondsElapsed / 3600);
    int iEMin = (int) (dSecondsElapsed / 60 - iEHr * 60);
    int iESec = (int) (dSecondsElapsed - iEHr * 3600 - iEMin * 60);

    int iRHr = (int) (dSecondsRemaining / 3600);
    int iRMin = (int) (dSecondsRemaining / 60 - iRHr * 60);
    int iRSec = (int) (dSecondsRemaining - iRHr * 3600 - iRMin * 60);

    sprintf(pcBuffer, "%4d/%d %3d:%02d:%02d Elapsed %3d:%02d:%02d Remain", i, iTotal, iEHr, iEMin, iESec, iRHr, iRMin, iRSec);
#else
    sprintf(pcBuffer, "ITERATION %d OF %d ELAPSED %.1f SEC TOTAL %.1f SEC ERROR %g", 
            i, iTotal, dSecondsElapsed, dEstimateTotal, dError);
#endif
    if (progress) {
        (*progress)(i, iTotal, dError, (int)(dEstimateTotal - dSecondsElapsed));
    }
    
    return pcBuffer;
}

