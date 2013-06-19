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
import ij.io.SaveDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.gui.Roi;
import ij.measure.*;
import java.awt.Rectangle;

public class Auto_Shift_And_Crop_PSF implements PlugInFilter {
    
    final int	ZERO_MEASURE_PERCENT = 10;		// percent of W,H,D to use
    // for zero measurement
    ImagePlus	imp;
    ImageStack	stack;
    static int		finalWidth=-1, finalHeight=-1, finalDepth=-1;
    static boolean	bAutoCenter = true;
    static boolean	bPeakAtCorners = true;
    static boolean	bAutoZero = true;
    static boolean	bNormalize = true;
    
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;

        if (finalWidth < 0) {
            finalWidth = (imp.getWidth()/2) * 2;
        }
        if (finalHeight < 0) {
            finalHeight = (imp.getHeight()/2) * 2;
        }
        if (finalDepth < 0) {
            finalDepth = (imp.getStackSize()/2) * 2;
        }
        
        GenericDialog gd = new GenericDialog("Auto Shift PSF");
        gd.addNumericField("Final Width:", finalWidth, 0);
        gd.addNumericField("Final Height:", finalHeight, 0);
        gd.addNumericField("Final Depth:", finalDepth, 0);
        gd.addCheckbox("Automatic Centering", bAutoCenter);
        gd.addCheckbox("Automatic Zero Subtraction", bAutoZero);
        gd.addCheckbox("Normalize PSF", bNormalize);
        gd.addCheckbox("Shift PSF to corners", bPeakAtCorners);
        
        gd.showDialog();
        if (gd.wasCanceled())
            return DONE;
        
        finalWidth = (int) gd.getNextNumber();
        finalHeight = (int) gd.getNextNumber();
        finalDepth = (int) gd.getNextNumber();
        bAutoCenter = gd.getNextBoolean();
        bAutoZero = gd.getNextBoolean();
        bNormalize = gd.getNextBoolean();
        bPeakAtCorners = gd.getNextBoolean();
        
        if (IJ.versionLessThan("1.17y"))
            return DONE;
        else
            return DOES_8C + DOES_8G + DOES_16 + DOES_32 + STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) {
        if (imp == null) {
            IJ.showMessage("Shift_PSF", "No images are open.");
            return;
        }
        
        int width = imp.getWidth();
        int height = imp.getHeight();
        int depth = imp.getStackSize();
        double xC, yC, zC;
        
        Roi sel = imp.getRoi();
        Rectangle r = null;
        
        if (sel != null) {
            r = sel.getBoundingRect();
        }
        
        if (bAutoCenter) {
            double[] sdPeak = findPeak(imp, r);
            xC = sdPeak[0];
            yC = sdPeak[1];
            zC = sdPeak[2];
        } else {
            if (r == null) {
                IJ.showMessage("Shift_PSF",
                        "Please draw a ROI around the center\n"+
                        "of the Point Spread Function and move\n"+
                        "to the brightest Z-plane.");
                return;
            }
            
            xC = r.x + r.width/2;
            yC = r.y + r.height/2;
            zC = imp.getCurrentSlice() - 1;
        }
        
        ImagePlus psf = shiftStack(imp, xC, yC, zC, finalWidth, finalHeight, finalDepth, bPeakAtCorners);
        
        if (bAutoZero) {
            boolean bZeroAtStackCenter = bPeakAtCorners;
            subtractZero(psf, bZeroAtStackCenter);
        }
        
        if (bNormalize) {
            normalize(psf);
        }
        
        psf.show();
    }
    
    public ImagePlus shiftStack(ImagePlus imp, double xC, double yC, double zC,
            int iFW, int iFH, int iFD, boolean bToCorner) {
        ImageStack stack1 = imp.getStack();
        int iSW = imp.getWidth();
        int iSH = imp.getHeight();
        int iSD = imp.getStackSize();
        
        ImageStack stack2 = new ImageStack(iFW, iFH);
        
        int iFWHalf=iFW/2, iFHHalf=iFH/2, iFDHalf=iFD/2;
        
        int x, y, z;
        int xF, yF, zF;
        double xS, yS, zS;
        
        FloatProcessor fp;
        for (z=0; z<iFD; z++) {
            fp = new FloatProcessor(iFW, iFH);
            String strZLabel = "Z= ";
            if (bToCorner) {
                zF = (z<iFDHalf) ? z : z - (iFD);
                zS = (zF + zC) % iSD;
                strZLabel += ((int)zF);

            } else {
                zF = z - iFDHalf;
                zS = zF + zC;
                strZLabel += ((int)zS);
            }
            for (y=0; y<iFH; y++) {
                if (bToCorner) {
                    yF = (y<iFHHalf) ? y : y - (iFH);
                } else {
                    yF = y - iFHHalf;
                }
                yS = yF + yC;
                for (x=0; x<iFW; x++) {
                    if (bToCorner) {
                        xF = (x<iFWHalf) ? x : x - (iFW);
                    } else {
                        xF = x - iFWHalf;
                    }
                    xS = xF + xC;
                    fp.putPixelValue(x, y, getInterpolatedPixel(stack1, xS, yS, zS));
                }
            }
            stack2.addSlice(strZLabel, fp);
        }
        return new ImagePlus(imp.getTitle()+"_PSF", stack2);
    }
    
    
    double getInterpolatedPixel(ImageStack stack, double fx, double fy, double fz) {
        
        // linearly interpolate between 8 neighboring pixels to get the
        // fractional value
        int w = stack.getWidth();
        int h = stack.getHeight();
        int d = stack.getSize();
        
        int x0 = (int)fx;
        int y0 = (int)fy;
        int z0 = (int)fz;
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;
        double xFrac = fx - x0;
        double yFrac = fy - y0;
        double zFrac = fz - z0;
        
        if (x0 <  0) x0 = -1;
        if (x0 >= w) x0 = -1;
        if (y0 <  0) y0 = -1;
        if (y0 >= h) y0 = -1;
        if (z0 <  0) z0 = -1;
        if (z0 >= d) z0 = -1;

        if (x1 <  0) x1 = -1;
        if (x1 >= w) x1 = -1;
        if (y1 <  0) y1 = -1;
        if (y1 >= h) y1 = -1;
        if (z1 <  0) z1 = -1;
        if (z1 >= d) z1 = -1;
        
        ImageProcessor ip0, ip1;
        double LL0, LR0, UR0, UL0;
        double LL1, LR1, UR1, UL1;
        
        if (z0 < 0) {
            LL0 = LR0 = UR0 = UL0 = 0;
        } else {
            ip0 = stack.getProcessor(z0+1);
            LL0 = (x0<0||y0<0) ? 0 : ip0.getPixelValue(x0, y0);
            LR0 = (x1<0||y0<0) ? 0 : ip0.getPixelValue(x1, y0);
            UR0 = (x1<0||y1<0) ? 0 : ip0.getPixelValue(x1, y1);
            UL0 = (x0<0||y1<0) ? 0 : ip0.getPixelValue(x0, y1);
        }
        
        if (z1 < 0) {
            LL1 = LR1 = UR1 = UL1 = 0;
        } else {
            ip1 = stack.getProcessor(z1+1);
            LL1 = (x0<0||y0<0) ? 0 : ip1.getPixelValue(x0, y0);
            LR1 = (x1<0||y0<0) ? 0 : ip1.getPixelValue(x1, y0);
            UR1 = (x1<0||y1<0) ? 0 : ip1.getPixelValue(x1, y1);
            UL1 = (x0<0||y1<0) ? 0 : ip1.getPixelValue(x0, y1);
        }
        
        double UAvg0 = UL0 + xFrac * (UR0 - UL0);
        double LAvg0 = LL0 + xFrac * (LR0 - LL0);
        double Avg0 = LAvg0 + yFrac * (UAvg0 - LAvg0);
        double UAvg1 = UL1 + xFrac * (UR1 - UL1);
        double LAvg1 = LL1 + xFrac * (LR1 - LL1);
        double Avg1 = LAvg1 + yFrac * (UAvg1 - LAvg1);
        return Avg0 + zFrac * (Avg1 - Avg0);
    }
    
    
    float[] impToFloat(ImagePlus impSrc, int zPlane) {
        int w = impSrc.getWidth();
        int h = impSrc.getHeight();
        int ss = w*h;
        int xy;
        
        ImageStack stack = impSrc.getStack();
        ImageProcessor ip = stack.getProcessor(zPlane);
        
        
        float[] sfSrc = new float[ss];
        
        // copy from source pixels to floating point
        switch (impSrc.getType()) {
            case ImagePlus.COLOR_256:
            case ImagePlus.GRAY8:
            {
                byte[] sOld = (byte[])ip.getPixels();
                for (xy=0; xy<ss; xy++) {
                    sfSrc[xy] = sOld[xy] & 0xff;
                }
            }
            break;
            case ImagePlus.GRAY16:
            {
                short[] sOld = (short[])ip.getPixels();
                for (xy=0; xy<ss; xy++) {
                    sfSrc[xy] = sOld[xy] & 0xffff;
                }
            }
            break;
            case ImagePlus.GRAY32:
            {
                float[] sOld = (float[])ip.getPixels();
                for (xy=0; xy<ss; xy++) {
                    sfSrc[xy] = sOld[xy];
                }
            }
            break;
        }
        
        return sfSrc;
    }
    
    double[] findPeak(ImagePlus imp, Rectangle rSearch) {
        float fVal, fMaxVal = Float.MIN_VALUE;
        int xMax=-1, yMax=-1, zMax=-1;
        
        int w = imp.getWidth();
        int h = imp.getHeight();
        int d = imp.getStackSize();
        
        int xL=0, xR=w, yT=0, yB=h;
        
        if (rSearch != null) {
            xL = rSearch.x;
            xR = rSearch.x + rSearch.width;
            yT = rSearch.y;
            yB = rSearch.y + rSearch.height;
        }
        
        int ywT = yT * w;
        
        float[] sfSlice;
        int x, y, yw, z;
        // find the position of the maximum value
        for (z=0; z<d; z++) {
            sfSlice = impToFloat(imp, z+1);
            for (y=yT, yw=ywT; y<yB; y++, yw+=w) {
                for (x=xL; x<xR; x++) {
                    fVal = sfSlice[yw+x];
                    if (fVal > fMaxVal) {
                        fMaxVal = fVal;
                        xMax = x;
                        yMax = y;
                        zMax = z;
                    }
                }
            }
        }
        
        // perform parabolic interpolation to refine
        // x, y, and z of brightest point;
        
        double dBestX=xMax, dBestY=yMax, dBestZ=zMax;
        double dM, dF0, dF1, dF2, dDF, dDFF;
        
        sfSlice = impToFloat(imp, zMax+1);
        
        // interpolate in x
        if (xMax>0 && xMax<w-1) {
            dF0 = sfSlice[yMax*w + xMax-1];
            dF1 = sfSlice[yMax*w + xMax];
            dF2 = sfSlice[yMax*w + xMax+1];
            dDF = (dF2 - dF0)/2;
            dDFF = -dF0 + 2*dF1 - dF2;
            dM = xMax;
            if (Math.abs(dDFF) > 1.0e-5)
                dM += dDF/dDFF;
            dBestX = dM;
        }
        
        // interpolate in y
        if (yMax>0 && yMax<h-1) {
            dF0 = sfSlice[(yMax-1)*w + xMax];
            dF1 = sfSlice[(yMax)*w + xMax];
            dF2 = sfSlice[(yMax+1)*w + xMax+1];
            dDF = (dF2 - dF0)/2;
            dDFF = -dF0 + 2*dF1 - dF2;
            dM = yMax;
            if (Math.abs(dDFF) > 1.0e-5)
                dM += dDF/dDFF;
            dBestY = dM;
        }
        
        // interpolate in z
        if (zMax>0 && zMax<d-1) {
            sfSlice = impToFloat(imp, zMax+1-1);
            dF0 = sfSlice[yMax*w + xMax];
            sfSlice = impToFloat(imp, zMax+1);
            dF1 = sfSlice[yMax*w + xMax];
            sfSlice = impToFloat(imp, zMax+1+1);
            dF2 = sfSlice[yMax*w + xMax];
            dDF = (dF2 - dF0)/2;
            dDFF = -dF0 + 2*dF1 - dF2;
            dM = zMax;
            if (Math.abs(dDFF) > 1.0e-5)
                dM += dDF/dDFF;
            dBestZ = dM;
        }
        
        
        double [] sdRet = new double[3];
        sdRet[0] = dBestX;
        sdRet[1] = dBestY;
        sdRet[2] = dBestZ;
        
        return sdRet;
    }
    
    void subtractZero(ImagePlus imp, boolean bZeroAtCenter) {
        ImageStack stack = imp.getStack();
        int w = stack.getWidth();
        int h = stack.getHeight();
        int d = stack.getSize();
        
        // create a ROI at the center of the stack to measure
        // the black level.
        
        int measureW = (int)(((double)w * ZERO_MEASURE_PERCENT + 0.5)/100.0);
        int measureH = (int)(((double)h * ZERO_MEASURE_PERCENT + 0.5)/100.0);
        int measureD = (int)(((double)d * ZERO_MEASURE_PERCENT + 0.5)/100.0);
        
        // measure at least one plane
        if (measureD < 1) measureD = 1;
        
        double dAvg = 0;
        double dStdDev = 0;
        
        ImageProcessor ip;
        ImageStatistics stats;
        int z;
        int numMeasurements = 0;
        if (bZeroAtCenter) {
            int minX = (w - measureW)/2;
            int minY = (h - measureH)/2;
            int minZ = (d - measureD)/2;
            for (z=0; z<measureD; z++) {
                ip = stack.getProcessor(z + minZ +1);
                ip.setRoi(minX, minY, measureW, measureH);
                stats = ImageStatistics.getStatistics(ip, Measurements.MEAN + Measurements.STD_DEV, null);
                dAvg += stats.mean;
                dStdDev += stats.stdDev;
                numMeasurements++;
            }
        } else {
            int halfMW = measureW/2;
            int halfMH = measureH/2;
            int halfMD = measureD/2;
            // Zero is at the eight corners
            for (z=0; z<measureD; z++) {
                int zMeas = ((z-halfMD) + d) % d;
                ip = stack.getProcessor(zMeas+1);
                // TOP LEFT
                ip.setRoi(0, 0, halfMW, halfMH);
                stats = ImageStatistics.getStatistics(ip, Measurements.MEAN + Measurements.STD_DEV, null);
                dAvg += stats.mean;
                dStdDev += stats.stdDev;
                numMeasurements++;
                // TOP RIGHT
                ip.setRoi(w-halfMW, 0, halfMW, halfMH);
                stats = ImageStatistics.getStatistics(ip, Measurements.MEAN + Measurements.STD_DEV, null);
                dAvg += stats.mean;
                dStdDev += stats.stdDev;
                numMeasurements++;
                // BOTTOM LEFT
                ip.setRoi(0, h-halfMH, halfMW, halfMH);
                stats = ImageStatistics.getStatistics(ip, Measurements.MEAN + Measurements.STD_DEV, null);
                dAvg += stats.mean;
                dStdDev += stats.stdDev;
                numMeasurements++;
                // BOTTOM RIGHT
                ip.setRoi(w-halfMW, h-halfMH, halfMW, halfMH);
                stats = ImageStatistics.getStatistics(ip, Measurements.MEAN + Measurements.STD_DEV, null);
                dAvg += stats.mean;
                dStdDev += stats.stdDev;
                numMeasurements++;
            }
            
        }
        
        
        // The Zero level will be the upper 95% confidence level of the
        // distribution (mean plus 2 standard deviations).
        dAvg /= numMeasurements;
        dStdDev /= numMeasurements;
        
        double dZeroLevel = dAvg + 2*dStdDev;
        
        //IJ.write("mean="+dAvg+" stddev="+dStdDev+" zero="+dZeroLevel);
        
        for (z=0; z<d; z++) {
            ip = stack.getProcessor(z +1);
            ip.add(-dZeroLevel);
            ip.min(0.0);
        }
    }
    
    void normalize(ImagePlus imp) {
        ImageStack stack = imp.getStack();
        int w = stack.getWidth();
        int h = stack.getHeight();
        int d = stack.getSize();
        
        double dMin = Double.MAX_VALUE;
        double dMax = Double.MIN_VALUE;
        
        ImageProcessor ip;
        ImageStatistics stats;
        int z;
        
        for (z=0; z<d; z++) {
            ip = stack.getProcessor(z +1);
            ip.resetRoi();
            stats = ImageStatistics.getStatistics(ip, Measurements.MIN_MAX, null);
            if (stats.min < dMin)
                dMin = stats.min;
            if (stats.max > dMax)
                dMax = stats.max;
        }
        
        for (z=0; z<d; z++) {
            ip = stack.getProcessor(z +1);
            ip.add(-dMin);
            ip.multiply(1.0/(dMax - dMin));
            ip.add(dMin);
        }
    }
}

