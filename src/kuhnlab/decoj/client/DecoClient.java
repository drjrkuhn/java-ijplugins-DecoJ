package kuhnlab.decoj.client;

import com.sun.jna.Callback;
import ij.*;
import ij.process.*;
import ij.io.*;
import java.io.*;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runtime;

class DecoClient extends Thread {

    static final boolean VERBOSE = true;
    boolean success = false;
    SourceFileInfo sfiIn;
    SourceFileInfo sfiPsf;
    SourceFileInfo sfiOut;
    DecoOptions options;
    String strName;

    public static interface ProgressCallback extends Callback {
        void callback (int iIteration, int nTotalIterations, double dError, int nSecRemaining);
    }

    static class IJProgressCallback implements ProgressCallback {
        int nLastSecRemaining = Integer.MAX_VALUE;
        void reset() {
            nLastSecRemaining = Integer.MAX_VALUE;
            IJ.showStatus("");
            IJ.showProgress(0.0);
        }
        public void callback(int iIteration, int nTotalIterations, double dError, int nSecRemaining) {
            if (nSecRemaining <= 0) {
                reset();
                return;
            }
            if (iIteration <= 10) {
                // Allow a few iterations for the time estimate to stabilize
                // update the remaining time regardless
                nLastSecRemaining = nSecRemaining;
            } else if (nLastSecRemaining > nSecRemaining) {
                // Only count down on the remaining time. If the remaining
                // time reported by the algorithm increases, just show
                // the previous estimate
                nLastSecRemaining = nSecRemaining;
            }
            int hr=nLastSecRemaining/3600;
            int min=(nLastSecRemaining - 3600*hr)/60;
            int sec=nLastSecRemaining - hr*3600 - min*60;
            
            String status = String.format("%d/%d   %02d:%02d:%02d   E=%f",
                    iIteration, nTotalIterations, hr, min, sec, dError);
            IJ.showStatus(status);
            IJ.showProgress(iIteration, nTotalIterations);
        }
    };
    static IJProgressCallback progress = new IJProgressCallback();
    
    static String getLibName() {
        if (System.getProperty("jna.library.path") == null) {
            // assume the shared library is in the plugins directory
            System.setProperty("jna.library.path", "plugins");
        }
        return "DecoJNA";
    }
    
    public interface DecoJNA extends Library {
       
 
        DecoJNA INSTANCE = (DecoJNA) Native.loadLibrary(getLibName(), DecoJNA.class);

        boolean setNumThreads(int nThreads);
                
        void setVerbose(int iVerbose);
        
        Pointer createEmptyStack(int iWidth, int iHeight, int iDepth, boolean bCreateFFTPlan, boolean bQuickFFTPlan);

        boolean setFloatPlane(Pointer pDestStack, int zDestPlane, float[] pfSrc, int iSrcLen);

        boolean setBytePlane(Pointer pDestStack, int zDestPlane, byte[] pbSrc, int iSrcLen);
        
        boolean setShortPlane(Pointer pDestStack, int zDestPlane, short[] psSrc, int iSrcLen);

        boolean destroyStack(Pointer pStack);

        boolean getPlane(float[] pfDest, int iDestLen, Pointer pSrcStack, int zSrcPlane);

        int getStackWidth(Pointer pStack);

        int getStackHeight(Pointer pStack);

        int getStackDepth(Pointer pStack);

        boolean processMain(Pointer pImage, Pointer pPsf);

        boolean processLLS(Pointer pImage, Pointer pPsf, double dThresh);

        boolean processMAP(Pointer pImage, Pointer pPsf, double dThresh);

        boolean processEM(Pointer pImage, Pointer pPsf, int iTotalIterations, ProgressCallback progress);
    }

    DecoClient(SourceFileInfo sfiIn, SourceFileInfo sfiPsf, SourceFileInfo sfiOut) {
        if (sfiIn.strFilename != null) {
            this.strName = sfiIn.strFilename;
        } else {
            this.strName = "";
        }
        this.sfiIn = sfiIn;
        this.sfiPsf = sfiPsf;
        this.sfiOut = sfiOut;
        this.options = sfiIn.options;
    }

    public Pointer createStack(PrintStream out, SourceFileInfo sfi, String strWhich) {

        // create a new FileInformation to read the entire image
        FileInfo fiNew = new FileInfo();
        fiNew.fileFormat = FileInfo.RAW;
        fiNew.fileName = null;
        fiNew.directory = null;
        fiNew.width = sfi.iWidth;
        fiNew.height = sfi.iHeight;
        fiNew.offset = 0;
        fiNew.nImages = 1;
        fiNew.gapBetweenImages = 0;
        fiNew.whiteIsZero = false;
        fiNew.intelByteOrder = false;
        fiNew.lutSize = 0;

        Pointer pStack = DecoJNA.INSTANCE.createEmptyStack(sfi.iWidth, sfi.iHeight, sfi.iDepth, true, false);

        switch (sfi.iType) {
            case ImagePlus.COLOR_256:
            case ImagePlus.GRAY8:
                fiNew.fileType = FileInfo.GRAY8;
                break;
            case ImagePlus.GRAY16:
                fiNew.fileType = FileInfo.GRAY16_UNSIGNED;
                break;

            case ImagePlus.GRAY32:
                fiNew.fileType = FileInfo.GRAY32_FLOAT;
                break;
            default:
                fiNew.fileType = -1;
                break;
        }

        // read entire stack from file and convert it to a native stack
        ImageReader reader = new ImageReader(fiNew);
        FileInputStream in;
        try {
            in = new FileInputStream(new File(sfi.strPath));
        } catch (FileNotFoundException e) {
            IJ.write("Cannot find file " + sfi.strPath + " to send to server.");
            return null;
        }

        int xy, z, ss = sfi.iWidth * sfi.iHeight;
        for (z = 0; z < sfi.iDepth; z++) {
            //System.out.println(strName + " Sending " + strWhich + " " + (z + 1) + "/" + sfi.iDepth);
            switch (sfi.iType) {
                case ImagePlus.COLOR_256:
                case ImagePlus.GRAY8:
                    byte[] ab = (byte[]) reader.readPixels(in);
                    DecoJNA.INSTANCE.setBytePlane(pStack, z, ab, ab.length);
                    break;

                case ImagePlus.GRAY16:
                    short[] as = (short[]) reader.readPixels(in);
                    DecoJNA.INSTANCE.setShortPlane(pStack, z, as, as.length);
                    break;

                case ImagePlus.GRAY32:
                    float[] af = (float[]) reader.readPixels(in);
                    DecoJNA.INSTANCE.setFloatPlane(pStack, z, af, af.length);
                    break;
            }
        }
        IJ.showProgress(1.0);

        try {
            in.close();
        } catch (IOException e) {
            IJ.write("Could not close stack file after sending to server");
            return null;
        }

        return pStack;
    }

    public boolean retrieveStack(Pointer pSrcStack, SourceFileInfo sfi, String strWhich) throws IOException {
        // get dimensions in the form WIDTHxHEIGHTxDEPTH
        int iWidth, iHeight, iDepth;

        iWidth = DecoJNA.INSTANCE.getStackWidth(pSrcStack);
        iHeight = DecoJNA.INSTANCE.getStackHeight(pSrcStack);
        iDepth = DecoJNA.INSTANCE.getStackDepth(pSrcStack);

        System.out.println("Retriving stack " +iWidth+ " x " +iHeight+ " x " +iDepth);

        // get the data type
        int iDataType = ImagePlus.GRAY32;

        // get the data
        ImageStack stack = new ImageStack(iWidth, iHeight);

        int xy, z, ss = iWidth * iHeight;
        for (z = 0; z < iDepth; z++) {
            //System.out.println(strName + " Receiving " + strWhich + " " + (z + 1) + "/" + iDepth);
            FloatProcessor fp = new FloatProcessor(iWidth, iHeight);
            float[] af = (float[]) fp.getPixels();
            DecoJNA.INSTANCE.getPlane(af, af.length, pSrcStack, z);

            double dSum = 0;
            for (float f : af)
                dSum += f;
            dSum /= af.length;
            //System.out.println(strWhich+" getPlane "+z+" size = "+af.length+" avg = "+dSum);

            stack.addSlice("", fp);
        }
        IJ.showProgress(1.0);

        // write the raw stack
        ImagePlus imp = new ImagePlus("", stack);
        FileSaver saver = new FileSaver(imp);
        saver.saveAsRawStack(sfi.strPath);
        //imp.show();

        return true;
    }

//    public boolean receiveProcessingStatus(BufferedReader in) throws IOException {
//        StreamTokenizer tokenizer = new StreamTokenizer(in);
//
//        tokenizer.resetSyntax();
//        tokenizer.wordChars('!', '~');
//        tokenizer.whitespaceChars(1, 32);
//        tokenizer.lowerCaseMode(false);
//
//        String strToken;
//        int iTokenType;
//
//        DecimalFormat dfFloat = new DecimalFormat("0.###E0");
//        DecimalFormat dfCent = new DecimalFormat("00");
//
//        // get the BEGIN_STACK token
//        while (true) {
//            if (tokenizer.nextToken() == tokenizer.TT_EOF) {
//                IJ.write(MSG_SERVER_CLOSED);
//                return false;
//            }
//            strToken = tokenizer.sval.toUpperCase();
//            //IJ.write("Received "+strToken);
//
//            if (strToken.equals(REPLY_DONE)) {
//                //IJ.write("Found DONE");
//                return true;
//            }
//
//            if (strToken.equals(REPLY_ITER)) {
//                //IJ.write("Found ITERATION");
//                // parse the status string
//                // server sends process status strings in the form:
//                //	ITERATION 23 OF 300 ELAPSED 34.2 SEC TOTAL 321.4 SEC ERROR 3.2123e-5
//                int iIteration, iTotal;
//                double dSecElapsed, dSecTotal, dError;
//
//                // get iteration
//                if (tokenizer.nextToken() == tokenizer.TT_EOF) {
//                    IJ.write(MSG_SERVER_CLOSED);
//                    return false;
//                }
//                //IJ.write(tokenizer.sval);
//                iIteration = Integer.parseInt(tokenizer.sval);
//
//                // skip OF
//                tokenizer.nextToken();
//                //IJ.write("OF "+tokenizer.sval);
//
//                // get total
//                if (tokenizer.nextToken() == tokenizer.TT_EOF) {
//                    IJ.write(MSG_SERVER_CLOSED);
//                    return false;
//                }
//                //IJ.write(tokenizer.sval);
//                iTotal = Integer.parseInt(tokenizer.sval);
//
//                // skip ELAPSED
//                tokenizer.nextToken();
//                //IJ.write("ELAPSED "+tokenizer.sval);
//
//                // get sec_elapsed
//                if (tokenizer.nextToken() == tokenizer.TT_EOF) {
//                    IJ.write(MSG_SERVER_CLOSED);
//                    return false;
//                }
//                //IJ.write(tokenizer.sval);
//                dSecElapsed = Double.parseDouble(tokenizer.sval);
//
//                // skip SEC TOTAL
//                tokenizer.nextToken();
//                //IJ.write("SEC "+tokenizer.sval);
//                tokenizer.nextToken();
//                //IJ.write("TOTAL "+tokenizer.sval);
//
//                // get sec_total
//                if (tokenizer.nextToken() == tokenizer.TT_EOF) {
//                    IJ.write(MSG_SERVER_CLOSED);
//                    return false;
//                }
//                //IJ.write(tokenizer.sval);
//                dSecTotal = Double.parseDouble(tokenizer.sval);
//
//                // skip SEC ERROR
//                tokenizer.nextToken();
//                //IJ.write("SEC "+tokenizer.sval);
//                tokenizer.nextToken();
//                //IJ.write("ERROR "+tokenizer.sval);
//
//                // get error
//                if (tokenizer.nextToken() == tokenizer.TT_EOF) {
//                    IJ.write(MSG_SERVER_CLOSED);
//                    return false;
//                }
//                //IJ.write(tokenizer.sval);
//                dError = Double.parseDouble(tokenizer.sval);
//
//
//                // show everything as a status
//                long lSecRemain = (long) (dSecTotal - dSecElapsed);
//
//                int eHrs = (int) (lSecRemain / 3600);
//                int eMin = (int) (lSecRemain / 60 - eHrs * 60);
//                int eSec = (int) (lSecRemain - eHrs * 3600 - eMin * 60);
//
//                String strStat = strName + " I=" + iIteration + "/" + iTotal;
//                strStat += " E=" + dfFloat.format(dError);
//                strStat += " " + dfCent.format(eHrs) + ":" + dfCent.format(eMin) + ":" + dfCent.format(eSec) + " left";
//                IJ.showStatus(strStat);
//                IJ.showProgress((iIteration + 1.0) / iTotal);
//            }
//        }
//    }

    public void run() {
        try {
            DecoJNA.INSTANCE.setNumThreads(Runtime.getRuntime().availableProcessors());
            DecoJNA.INSTANCE.setVerbose(0);
            PrintStream out = null;
            Pointer pImage = null;
            Pointer pPsf = null;
            pImage = createStack(out, sfiIn, "Image");
            if (pImage != null) {
                pPsf = createStack(out, sfiPsf, "PSF");
            }
            if (pImage == null || pPsf == null) {
                success = false;
                return;
            }
            System.out.println("Starting EM algorithm");

            progress.reset();
            DecoJNA.INSTANCE.processEM(pImage, pPsf, options.iNumIterations, progress);
            //DecoJNA.INSTANCE.processLLS(pImage, pPsf, 0.01);
            progress.reset();

            System.out.println("Finished EM algorithm");
            boolean bOK = retrieveStack(pImage, sfiOut, "Output");
            DecoJNA.INSTANCE.destroyStack(pImage);
            DecoJNA.INSTANCE.destroyStack(pPsf);

        } catch (IOException ex) {
            Logger.getLogger(DecoClient.class.getName()).log(Level.SEVERE, null, ex);
            success = false;
        }
        success = true;
    }
}
