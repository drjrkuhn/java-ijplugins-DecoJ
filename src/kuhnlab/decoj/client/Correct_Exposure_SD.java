package kuhnlab.decoj.client;


import ij.plugin.filter.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import java.awt.*;
import java.util.*;

public class Correct_Exposure_SD implements PlugInFilter {
    
    protected ImagePlus impSrc;
    protected Roi		roiSrc;
    
    public int setup(String arg, ImagePlus imp) {
        this.impSrc = imp;
        if (imp.getStackSize() <= 1) {
            IJ.showMessage("Correct Exposure Max", "This filter must be used on a stack");
            return DONE;
        }
        
        this.roiSrc = imp.getRoi();
        if (roiSrc == null || roiSrc.getType() >= Roi.LINE) {
            IJ.showMessage("Correct Exposure", "Please select a ROI to measure");
            return DONE;
        }
        
        return DOES_16 + DOES_32 + STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) {
        
        int w = impSrc.getWidth();
        int h = impSrc.getHeight();
        int d = impSrc.getStackSize();
        int ss = w*h;
        
        // get average background intensity for each slice
        ImageStack stack = impSrc.getStack();
        double[] adMin = new double[d];
        double[] adMax = new double[d];
        double dMin, dMax;
        //int[] mask = impSrc.getMask();
        ByteProcessor mask = (ByteProcessor)impSrc.getMask();
        Rectangle r = impSrc.getRoi().getBoundingRect();
        Calibration cal = impSrc.getCalibration();
        int measurements = Measurements.MEAN + Measurements.STD_DEV;
        
        IJ.showStatus("Calculating background...");
        int z, xy;
        double dMaxMin = Double.MIN_VALUE;
        double dMaxMax = Double.MIN_VALUE;
        for (z=0; z<d; z++) {
            ip = stack.getProcessor(z+1);
            ip.setRoi(r);
            ip.setMask(mask);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            dMin = stats.mean - stats.stdDev;
            dMax = stats.mean + stats.stdDev;
            
            adMin[z] = dMin;
            adMax[z] = dMax;
            if (dMin > dMaxMin)
                dMaxMin = dMin;
            if (dMax > dMaxMax)
                dMaxMax = dMax;
        }
        //IJ.write("Max Min = "+dMax);
        
        double dMaxRange = dMaxMax - dMaxMin;
        double dRange, dVal, dMult;
        
        IJ.showStatus("Correcting Exposure...");
        
        if (impSrc.getType() == ImagePlus.GRAY16) {
            //
            // convert the image stack to an array of slices
            //
            short[] asSrc;
            
            // do the shading correction for each slice
            for (z=0; z<d; z++) {
                if ((z%10)==0) IJ.showProgress((z+1.0)/d);
                //IJ.write(""+(z+1)+" mode="+IJ.d2s(adMean[z],2)+" max="+IJ.d2s(dMax,2));
                dRange = adMax[z] - adMin[z];
                dMin = adMin[z];
                dMult = dMaxRange / dRange;
                asSrc = (short[])impSrc.getStack().getPixels(z+1);
                for (xy=0; xy<ss; xy++) {
                    dVal = asSrc[xy] & 0xffff;
                    dVal = (dVal - dMin)*dMult + dMaxMin;
                    asSrc[xy] = (short)(((int)dVal)&0xffff);
                }
            }
        } else if (impSrc.getType() == ImagePlus.GRAY32) {
            //
            // convert the image stack to an array of slices
            //
            float[] afSrc;
            
            // do the shading correction for each slice
            for (z=0; z<d; z++) {
                if ((z%10)==0) IJ.showProgress((z+1.0)/d);
                dRange = adMax[z] - adMin[z];
                dMin = adMin[z];
                dMult = dMaxRange / dRange;
                afSrc = (float[])impSrc.getStack().getPixels(z+1);
                for (xy=0; xy<ss; xy++) {
                    dVal = afSrc[xy];
                    dVal = (dVal - dMin)*dMult + dMaxMin;
                    afSrc[xy] = (float)dVal;
                }
            }
        }
        impSrc.updateAndDraw();
        IJ.showStatus("");
        IJ.showProgress(1.0);
    }
}

