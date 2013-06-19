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

package kuhnlab.decoj.client;


import ij.*;
import ij.io.*;
import ij.io.FileInfo;
import ij.io.FileOpener;
import java.io.*;
import java.lang.*;
import java.util.List;
import java.util.ArrayList;

/* NOTE: the command-line executable decop.exe usually requires a LOT of
 * memory. Unfortunately, this is a Cygwin program (see www.cygwin.com)
 * and Cygwin programs can only use a certain amount of memory (usually
 * only about 128 megabytes. There is a way around it. Use the registry
 * editor and create a new DWORD key called
 * "HKEY_CURRENT_USER/Software/CygnusSolutions/Cygwin/heap_chunk_in_mb"
 * and set the value to the amount of physical RAM installed on the
 * computer. This should fix the memory problem. To test your registry
 * editing skills, close all cygwin shells, set this value to 4 and try
 * to open a new cygwin shell. If you get an error, then you have the
 * right registry entry. Set it back to the big value and deconvolve
 * to your heart's content.
 */

public class Deconvolver /*implements Runnable*/ {
    static final String SEP = File.separator;
    static final String OUTDIR = "out";
    static final String OUTSUFFIX = "_out";
    static final String OUTEXTENSION = ".tif";
    
    // index of return values from splitPath
    static final int PATH=0;
    static final int NAME=1;
    static final int EXTENSION=2;
    
    // arrays of SourceFileInfo's for Input files and PSF files
    public List vPsfFileInfos = null;
    public List vSplitFileInfos = null;
    
    //==========================================================================
    // Main interface
    //==========================================================================
    
    /** Perform deconvolution on a single file or stack. If the file
     *	is not an open window, it is opened, processed, and closed. */
    public boolean deconvolve(SourceFileInfo sfi, boolean bNewPsf) {
        if (bNewPsf) {
            if (vPsfFileInfos != null) {
                deleteFiles(vPsfFileInfos);
            }
            vPsfFileInfos = new ArrayList();
            //
            // Save the PSF file in RAW format
            //
            int i, nPsf=sfi.options.iNumWL;
            for (i=0; i<nPsf; i++) {
                // Open the PSF file
                String strPsfFile = sfi.options.astrPsfFile[i];
                ImagePlus impPsf = (new Opener()).openImage(strPsfFile);
                if (impPsf == null) {
                    IJ.write("Unable to open PSF file "+strPsfFile);
                    return false;
                }
                DecoOptions optPsf = new DecoOptions();
                optPsf.iNumWL = 1;
                optPsf.iNumPlanes = impPsf.getStackSize();
                
                // Save the RAW psf file
                List vNewPsfFileInfos = saveRawStacks(impPsf, "psf"+(i+1), optPsf);
                if (vNewPsfFileInfos.size() != 1) {
                    deleteFiles(vNewPsfFileInfos);
                    IJ.write("Error writing "+strPsfFile+" to temp directory");
                    return false;
                }
                
                SourceFileInfo sfiPsf = (SourceFileInfo)vNewPsfFileInfos.get(0);
                sfiPsf.iWavelength = i;
                vPsfFileInfos.add(sfiPsf);
            }
        }
        
        //
        // Open the image file if not already a window
        //
        boolean bCloseWhenDone = false;
        boolean bSaveResult = false;
        if (sfi.getImagePlus() == null) {
            // open the file
            sfi.setImagePlus((new Opener()).openImage(sfi.strPath, sfi.strFilename));
            if (sfi.getImagePlus() == null) {
                IJ.write("Unable to open image file "+sfi.strPath+SEP+sfi.strFilename);
                return false;
            }
            bCloseWhenDone = true;
            bSaveResult = true;
        }
        
        // keep a copy of the FileInfo for later storage
        FileInfo fi = sfi.getImagePlus().getFileInfo();
        
        //
        // Save the multidimensional stack to a series of RAW files
        //
        vSplitFileInfos = saveRawStacks(sfi.getImagePlus(), sfi.strFilename, sfi.options);
        if (bCloseWhenDone) {
            sfi.imp = null;
            System.gc();
        }
        
        //
        // Deconvolve the list of files (with the list of PSF files)
        //
        List vOutFileInfos = deconvolveFileList(vSplitFileInfos, vPsfFileInfos);
        
        //
        // combine the raw output files into one stack and save if necessary
        //
        if (vOutFileInfos != null) {
            ImagePlus impOutStack = combineFiles(vOutFileInfos, fi);
            if (bSaveResult && impOutStack != null) {
                String[] astrSplit = splitPath(sfi.strPath + SEP + sfi.strFilename);
                String strOutDir = astrSplit[PATH] + OUTDIR + SEP;
                // create the directory
                (new File(strOutDir)).mkdirs();
                String strOutFile = strOutDir + astrSplit[NAME] + OUTSUFFIX + OUTEXTENSION;
                if (!(new FileSaver(impOutStack)).saveAsTiffStack(strOutFile)) {
                    IJ.write("Unable to save output file to "+strOutFile);
                    return false;
                }
                impOutStack = null;
            } else if (impOutStack != null) {
                impOutStack.show();
            }
            deleteFiles(vOutFileInfos);
            vOutFileInfos = null;
        }
        
        deleteFiles(vSplitFileInfos);
        vSplitFileInfos = null;
        System.gc();
        return true;
    }
    
    
    /** Called to delete temporary files when done. */
    public void deleteTempFiles() {
        if (vPsfFileInfos != null) {
            deleteFiles(vPsfFileInfos);
        }
    }
    
    
    //==========================================================================
    // File utilities
    //==========================================================================
    
    /** Delete a list of files */
    public void deleteFiles(List vFileInfos) {
        int i, len=vFileInfos.size();
        if (len==0) return;
        for (i=0; i<len; i++) {
            String strName = ((SourceFileInfo)vFileInfos.get(i)).strPath;
            (new File(strName)).delete();
        }
    }
    
    /** Split a fully qualified file name into PATH, NAME, and
     *	EXTENSION. */
    static public String[] splitPath(String strPath) {
        String strDir;
        String strName;
        String strExt;
        //IJ.write("splitPath: \""+strPath+"\"");
        int iDirPos = strPath.lastIndexOf(SEP);
        if (iDirPos < 0)
            iDirPos = 0;
        else
            iDirPos += SEP.length();
        //IJ.write("iDirPos = "+iDirPos);
        int iExpPos = strPath.lastIndexOf('.');
        strDir = strPath.substring(0, iDirPos);
        //IJ.write("iExpPos = "+iDirPos);
        if (iExpPos < 0) {
            strName = strPath.substring(iDirPos);
            strExt = "";
        } else {
            strName = strPath.substring(iDirPos, iExpPos);
            strExt = strPath.substring(iExpPos);
        }
        String[] res = new String[3];
        res[PATH] = strDir;
        res[NAME] = strName;
        res[EXTENSION] = strExt;
        return res;
    }
    
    //==========================================================================
    // Stack file read/write
    //==========================================================================
    
    /** Save a multidimensional stack to a series of temp files. One for
     *	each timepoint & wavelength. Returns the list of temp filenames. */
    public List saveRawStacks(ImagePlus imp, String strName, DecoOptions opt) {
        int iStackSize = imp.getStackSize();
        int iNumPlanes = opt.iNumPlanes;
        int iNumWL = opt.iNumWL;
        int iNumT = iStackSize / iNumWL / iNumPlanes;
        int iNumStacks = iNumWL * iNumT;
        if (iStackSize != iNumPlanes * iNumWL * iNumT) {
            IJ.write("WARNING: Timelapse stack is truncated");
        }
        int i, t, w, p;
        String strTempName = "";
        String strIndex;
        String[] astrSplit = splitPath(strName);
        FileSaver saver;
        List vTempFileInfos = new ArrayList();
        
        int iStack = 0;
        for (t=0; t<iNumT; t++) {
            for (w=0; w<iNumWL; w++) {
                strIndex = "_t"+t+"w"+w;
                try {
                    strTempName = File.createTempFile(astrSplit[NAME], strIndex).getAbsolutePath();
                } catch (IOException e) {
                    IJ.write(""+ e +"Unable to create temp file "+strTempName);
                    deleteFiles(vTempFileInfos);
                    return null;
                }
                
                p = (t*iNumWL + w)*iNumPlanes;
                ImagePlus impSub = getSubStack(imp, p, p+iNumPlanes-1);
                if (impSub == null) {
                    IJ.write("Could not get sub-stack from "+p+" to "+(p+iNumPlanes-1));
                    return null;
                }
                
                // set the information for this particular stack
                SourceFileInfo sfiNew = new SourceFileInfo(impSub, opt);
                sfiNew.iTime = t;
                sfiNew.iWavelength = w;
                sfiNew.strPath = strTempName;
                sfiNew.strFilename = astrSplit[NAME] + "(" + (iStack+1) + "/" + iNumStacks + ")";
                
                // write the raw stack as intel byte order
                saver = new FileSaver(impSub);
                saver.saveAsRawStack(strTempName);
                vTempFileInfos.add(sfiNew);
                iStack++;
            }
        }
        
        return vTempFileInfos;
    }
    
    /** Combine a number of raw (32-bit float) input files into one
     *	multidimensional stack. */
    public ImagePlus combineFiles(List vOutFileInfos, FileInfo fiSrc) {
        ImagePlus impCombined=null, impNew;
        ImageStack stkCombined=null, stkNew;
        int i, len=vOutFileInfos.size();
        for (i=0; i<len; i++) {
            SourceFileInfo sfiOutFile = (SourceFileInfo)vOutFileInfos.get(i);
            String strOutFile = sfiOutFile.strPath;
            impNew = readRawFloatStack(strOutFile, fiSrc, sfiOutFile.iDepth);
            if (impNew == null) {
                IJ.write("Could not read output file "+strOutFile);
                return impCombined;
            }
            if (impCombined==null) {
                impCombined = impNew;
                stkCombined = impCombined.getStack();
            } else {
                int p, ss=impNew.getStackSize();
                stkNew = impNew.getStack();
                for (p=1; p<=ss; p++) {
                    stkCombined.addSlice("", stkNew.getProcessor(p));
                }
            }
        }
        
        return impCombined;
    }
    
    /** Read a single float stack as a raw image. */
    public ImagePlus readRawFloatStack(String strFile, FileInfo fiSrc, int iNumPlanes) {
        String[] astrSplit = splitPath(strFile);
        
        // create a new FileInformation based on the old FileInformation
        FileInfo fiNew = new FileInfo();
        fiNew.fileFormat = FileInfo.RAW;
        fiNew.fileType = FileInfo.GRAY32_FLOAT;
        fiNew.fileName = astrSplit[NAME] + astrSplit[EXTENSION];
        fiNew.directory = astrSplit[PATH];
        fiNew.width = fiSrc.width;
        fiNew.height = fiSrc.height;
        fiNew.offset = 0;
        fiNew.nImages = iNumPlanes;
        fiNew.gapBetweenImages = 0;
        fiNew.whiteIsZero = false;
        fiNew.intelByteOrder = false;
        fiNew.lutSize = 0;
        
        fiNew.pixelWidth = fiSrc.pixelWidth;
        fiNew.pixelHeight = fiSrc.pixelHeight;
        fiNew.pixelDepth = fiSrc.pixelDepth;
        fiNew.unit = fiSrc.unit;
        fiNew.calibrationFunction = fiSrc.calibrationFunction;
        fiNew.coefficients = fiSrc.coefficients;
        fiNew.valueUnit = fiSrc.valueUnit;
        fiNew.frameInterval = fiSrc.frameInterval;
        
        return (new FileOpener(fiNew)).open(false);
    }
    
    //==========================================================================
    // Stack utilities
    //==========================================================================
    
    /** Create a new stack containing only a portion of an existing stack. */
    static public ImagePlus getSubStack(ImagePlus imp, int iFirstSlice, int iLastSlice) {
        // NOTE: slices begin at index 0 (not 1)
        ImageStack oldStack = imp.getStack();
        ImageStack newStack = new ImageStack(oldStack.getWidth(), oldStack.getHeight());
        int i, iSize = oldStack.getSize();
        if (iFirstSlice < 0 || iFirstSlice >= iSize || iLastSlice <= iFirstSlice || iLastSlice >= iSize) {
            return null;
        }
        for (i=iFirstSlice; i<=iLastSlice; i++) {
            newStack.addSlice(""+i, oldStack.getProcessor(i+1));
        }
        return new ImagePlus(imp.getTitle(), newStack);
    }
    
    //==========================================================================
    // Deconvolution processing
    //==========================================================================
    
    /** Deconvolve a list of files from one multidimensional stack */
    public List deconvolveFileList(List vSplitFileInfos, List vPsfFileInfos) {
        List vOutFileInfos = new ArrayList();
        int i, len=vSplitFileInfos.size();
        for (i=0; i<len; i++) {
            SourceFileInfo sfiIn = (SourceFileInfo)vSplitFileInfos.get(i);
            String[] astrSplit = splitPath(sfiIn.strPath);
            String strOutFile = "";
            try {
                strOutFile = File.createTempFile(astrSplit[NAME], OUTSUFFIX).getAbsolutePath();
            } catch (IOException e) {
                IJ.write(""+ e +"Unable to create temp file " + strOutFile);
                deleteFiles(vOutFileInfos);
                return null;
            }
            
            SourceFileInfo sfiPsf = (SourceFileInfo)vPsfFileInfos.get(sfiIn.iWavelength);
            if (sfiPsf == null) {
                IJ.write("No PSF file for wavelength " + sfiIn.iWavelength);
                return null;
            }
            
            SourceFileInfo sfiOut = (SourceFileInfo) sfiIn.clone();
            sfiOut.strPath = strOutFile;
            
            if (!deconvolveFile(sfiIn, sfiPsf, sfiOut)) {
                deleteFiles(vOutFileInfos);
                return null;
            }
            
            vOutFileInfos.add(sfiOut);
        }
        return vOutFileInfos;
    }
    
    /** Start deconvolution on a single file */
    public boolean deconvolveFile(SourceFileInfo sfiIn, SourceFileInfo sfiPsf, SourceFileInfo sfiOut) {
        DecoClient client = new DecoClient(sfiIn, sfiPsf, sfiOut);
        client.success = false;
        client.start();
        try {
            client.join();
        } catch (InterruptedException e) {
            return false;
        }
        return client.success;
    }
    
}

