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

public class RotAvg_PSF implements PlugInFilter {
    
    protected ImagePlus impSrc;
    protected Roi		roiSrc;
    
    public int setup(String arg, ImagePlus imp) {
        this.impSrc = imp;
        if (imp.getStackSize() <= 1) {
            IJ.showMessage("RotAvg PSF", "This filter must be used on a stack");
            return DONE;
        }
        
        return DOES_16 + STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) {
        
        int w = impSrc.getWidth();
        int h = impSrc.getHeight();
        int d = impSrc.getStackSize();
        int ss = w*h;
        
        // get average background intensity for each slice
        ImageStack stack = impSrc.getStack();
        final int SUBSAMPLE = 4;
        
        int x, y, xo, yo, z, r;
        int wH = w/2;
        int hH = h/2;
        double[] adLine = new double[w*SUBSAMPLE];
        int[] aiN = new int[w*SUBSAMPLE];
        
        if (impSrc.getType() == ImagePlus.GRAY16) {
            //
            // convert the image stack to an array of slices
            //
            short[] asSrc;
            short pixel;
            
            // do the shading correction for each slice
            double dMult;
            for (z=0; z<d; z++) {
                if ((z%10)==0) IJ.showProgress((z+1.0)/d);
                asSrc = (short[])impSrc.getStack().getPixels(z+1);
                
                // get the average
                for (r=0; r<adLine.length; r++) {
                    adLine[r] = 0;
                    aiN[r] = 0;
                }
                for (y=0; y<h; y++) {
                    yo = (y<hH) ? y : h-1-y;
                    for (x=0; x<w; x++) {
                        xo = (x<wH) ? x : w-1-x;
                        r = (int)(Math.sqrt(xo*xo + yo*yo)*SUBSAMPLE);
                        //if (r > wH)
                        //	IJ.write("r = "+r);
                        pixel = asSrc[y*w + x];
                        adLine[r] += pixel;
                        aiN[r] += 1;
                    }
                }
                for (r=0; r<adLine.length; r++) {
                    if (aiN[r] > 0)
                        adLine[r] /= aiN[r];
                }
                
                // store the average
                for (y=0; y<h; y++) {
                    yo = (y<hH) ? y : h-1-y;
                    for (x=0; x<w; x++) {
                        xo = (x<wH) ? x : w-1-x;
                        r = (int)(Math.sqrt(xo*xo + yo*yo)*SUBSAMPLE);
                        asSrc[y*w + x] = (short)adLine[r];
                    }
                }
            }
        }
        impSrc.updateAndDraw();
        IJ.showStatus("");
        IJ.showProgress(1.0);
    }
}

