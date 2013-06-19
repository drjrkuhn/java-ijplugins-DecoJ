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


import ij.plugin.filter.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import java.awt.*;
import java.util.*;

public class Correct_Exposure_Avg implements PlugInFilter {
    
    protected ImagePlus impSrc;
    protected Roi		roiSrc;
    
    public int setup(String arg, ImagePlus imp) {
        this.impSrc = imp;
        if (imp.getStackSize() <= 1) {
            IJ.showMessage("Correct Exposure", "This filter must be used on a stack");
            return DONE;
        }
        
        this.roiSrc = imp.getRoi();
        if (roiSrc == null || roiSrc.getType() >= Roi.LINE) {
            IJ.showMessage("Correct Exposire", "Please select a ROI to measure");
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
        double[] adBackground = new double[d];
        //int[] mask = impSrc.getMask();
        ByteProcessor mask = (ByteProcessor)impSrc.getMask();
        Rectangle r = impSrc.getRoi().getBoundingRect();
        Calibration cal = impSrc.getCalibration();
        int measurements = Measurements.MEAN;
        
        IJ.showStatus("Calculating background...");
        int z, xy;
        double dMax = Double.MIN_VALUE;
        for (z=0; z<d; z++) {
            ip = stack.getProcessor(z+1);
            ip.setRoi(r);
            ip.setMask(mask);
            ImageStatistics stats = ImageStatistics.getStatistics(ip, measurements, cal);
            adBackground[z] = stats.mean;
            if (stats.mean > dMax)
                dMax = stats.mean;
        }
        
        
        IJ.showStatus("Correcting Exposure...");
        
        if (impSrc.getType() == ImagePlus.GRAY16) {
            //
            // convert the image stack to an array of slices
            //
            short[] asSrc;
            
            // do the shading correction for each slice
            double dMult;
            for (z=0; z<d; z++) {
                if ((z%10)==0) IJ.showProgress((z+1.0)/d);
                dMult = dMax / adBackground[z];
                asSrc = (short[])impSrc.getStack().getPixels(z+1);
                for (xy=0; xy<ss; xy++) {
                    asSrc[xy] = (short)(((int)(dMult*((double)(asSrc[xy] & 0xffff))))&0xffff);
                }
            }
        } else if (impSrc.getType() == ImagePlus.GRAY32) {
            //
            // convert the image stack to an array of slices
            //
            float[] afSrc;
            
            // do the shading correction for each slice
            double dMult;
            for (z=0; z<d; z++) {
                if ((z%10)==0) IJ.showProgress((z+1.0)/d);
                dMult = dMax / adBackground[z];
                afSrc = (float[])impSrc.getStack().getPixels(z+1);
                for (xy=0; xy<ss; xy++) {
                    afSrc[xy] = (float)(dMult*((double)afSrc[xy]));
                }
            }
        }
        impSrc.updateAndDraw();
        IJ.showStatus("");
        IJ.showProgress(1.0);
    }
}

