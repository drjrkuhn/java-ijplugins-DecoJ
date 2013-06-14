#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc.exe
CCC=g++.exe
CXX=g++.exe
FC=gfortran
AS=as.exe

# Macros
CND_PLATFORM=MinGW-Windows
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/1360937237/process.o \
	${OBJECTDIR}/_ext/1360937237/util.o \
	${OBJECTDIR}/_ext/1360937237/procem.o \
	${OBJECTDIR}/_ext/1360937237/proclls.o \
	${OBJECTDIR}/_ext/1360937237/stackdata.o \
	${OBJECTDIR}/_ext/1360937237/procmap.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-L../../fftwdist/win/lib -Llib/pthreads-win32/lib -lfftw3f -lfftw3f_threads -lpthreadGC2

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/../../../dist/DecoJNA.dll

${CND_DISTDIR}/../../../dist/DecoJNA.dll: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/../../../dist
	${LINK.cc} -shared -o ${CND_DISTDIR}/../../../dist/DecoJNA.dll ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/_ext/1360937237/process.o: ../src/process.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1360937237
	${RM} $@.d
	$(COMPILE.cc) -g -I../../fftwdist/win/include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1360937237/process.o ../src/process.cpp

${OBJECTDIR}/_ext/1360937237/util.o: ../src/util.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1360937237
	${RM} $@.d
	$(COMPILE.cc) -g -I../../fftwdist/win/include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1360937237/util.o ../src/util.cpp

${OBJECTDIR}/_ext/1360937237/procem.o: ../src/procem.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1360937237
	${RM} $@.d
	$(COMPILE.cc) -g -I../../fftwdist/win/include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1360937237/procem.o ../src/procem.cpp

${OBJECTDIR}/_ext/1360937237/proclls.o: ../src/proclls.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1360937237
	${RM} $@.d
	$(COMPILE.cc) -g -I../../fftwdist/win/include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1360937237/proclls.o ../src/proclls.cpp

${OBJECTDIR}/_ext/1360937237/stackdata.o: ../src/stackdata.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1360937237
	${RM} $@.d
	$(COMPILE.cc) -g -I../../fftwdist/win/include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1360937237/stackdata.o ../src/stackdata.cpp

${OBJECTDIR}/_ext/1360937237/procmap.o: ../src/procmap.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1360937237
	${RM} $@.d
	$(COMPILE.cc) -g -I../../fftwdist/win/include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/1360937237/procmap.o ../src/procmap.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/../../../dist/DecoJNA.dll

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
