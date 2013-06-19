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
import ij.gui.Roi;
import java.awt.Rectangle;


public class Auto_Shift_PSF implements PlugInFilter {
    
    ImagePlus	imp;
    ImageStack	stack;
    int			xC, yC, zC;
    
    static boolean bUseIntelByteOrder = true;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
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
        
        int[] siPeak = findPeak(imp);
        
        xC = siPeak[0];
        yC = siPeak[1];
        zC = siPeak[2];
        
        switch (imp.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                shiftByteStack(imp, xC, yC, zC);
                break;
                
            case ImagePlus.GRAY16:
                shiftShortStack(imp, xC, yC, zC);
                break;
                
            case ImagePlus.GRAY32:
                shiftFloatStack(imp, xC, yC, zC);
                break;
        }
        imp.setSlice(zC + 1);
        
    }
    
    void shiftByteStack(ImagePlus imp, int xC, int yC, int zC) {
        ImageStack stackOld = imp.getStack();
        int w, h, d;
        w = stackOld.getWidth();
        h = stackOld.getHeight();
        d = stackOld.getSize();
        
        ImageStack stackNew = new ImageStack(w, h, stackOld.getColorModel());
        
        int zNew;
        int zOld;
        int iOld, iNew, len, oldOff;
        len = w * h;
        ByteProcessor procOld;
        ByteProcessor procNew;
        byte[] aOld;
        byte[] aNew;
        zOld = zC;
        for (zNew=0; zNew<d; zNew++) {
            IJ.showStatus("Processing: " + (zNew+1) +"/"+ d);
            procOld = (ByteProcessor) stackOld.getProcessor(zOld+1);
            procNew = (ByteProcessor) procOld.createProcessor(w, h);
            aOld = (byte[])procOld.getPixels();
            aNew = (byte[])procNew.getPixels();
            oldOff = yC * w + xC;
            iOld = oldOff;
            for (iNew=0; iNew<len; iNew++) {
                aNew[iNew] = aOld[iOld];
                iOld++;
                if (iOld >= len)
                    iOld = 0;
            }
            stackNew.addSlice(stackOld.getSliceLabel(zOld+1), procNew);
            
            zOld++;
            if (zOld >= d)
                zOld = 0;
        }
        
        imp.setStack(imp.getTitle(), stackNew);
    }
    
    void shiftShortStack(ImagePlus imp, int xC, int yC, int zC) {
        ImageStack stackOld = imp.getStack();
        int w, h, d;
        w = stackOld.getWidth();
        h = stackOld.getHeight();
        d = stackOld.getSize();
        
        ImageStack stackNew = new ImageStack(w, h, stackOld.getColorModel());
        
        int zNew;
        int zOld;
        int iOld, iNew, len, oldOff;
        len = w * h;
        ShortProcessor procOld;
        ShortProcessor procNew;
        short[] aOld;
        short[] aNew;
        zOld = zC;
        for (zNew=0; zNew<d; zNew++) {
            IJ.showStatus("Processing: " + (zNew+1) +"/"+ d);
            procOld = (ShortProcessor) stackOld.getProcessor(zOld+1);
            procNew = (ShortProcessor) procOld.createProcessor(w, h);
            aOld = (short[])procOld.getPixels();
            aNew = (short[])procNew.getPixels();
            oldOff = yC * w + xC;
            iOld = oldOff;
            for (iNew=0; iNew<len; iNew++) {
                aNew[iNew] = aOld[iOld];
                iOld++;
                if (iOld >= len)
                    iOld = 0;
            }
            stackNew.addSlice(stackOld.getSliceLabel(zOld+1), procNew);
            
            zOld++;
            if (zOld >= d)
                zOld = 0;
        }
        
        imp.setStack(imp.getTitle(), stackNew);
    }
    
    void shiftFloatStack(ImagePlus imp, int xC, int yC, int zC) {
        ImageStack stackOld = imp.getStack();
        int w, h, d;
        w = stackOld.getWidth();
        h = stackOld.getHeight();
        d = stackOld.getSize();
        
        ImageStack stackNew = new ImageStack(w, h, stackOld.getColorModel());
        
        int zNew;
        int zOld;
        int iOld, iNew, len, oldOff;
        len = w * h;
        FloatProcessor procOld;
        FloatProcessor procNew;
        float[] aOld;
        float[] aNew;
        zOld = zC;
        for (zNew=0; zNew<d; zNew++) {
            IJ.showStatus("Processing: " + (zNew+1) +"/"+ d);
            procOld = (FloatProcessor) stackOld.getProcessor(zOld+1);
            procNew = (FloatProcessor) procOld.createProcessor(w, h);
            aOld = (float[])procOld.getPixels();
            aNew = (float[])procNew.getPixels();
            oldOff = yC * w + xC;
            iOld = oldOff;
            for (iNew=0; iNew<len; iNew++) {
                aNew[iNew] = aOld[iOld];
                iOld++;
                if (iOld >= len)
                    iOld = 0;
            }
            stackNew.addSlice(stackOld.getSliceLabel(zOld+1), procNew);
            
            zOld++;
            if (zOld >= d)
                zOld = 0;
        }
        
        imp.setStack(imp.getTitle(), stackNew);
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
    
    int[] findPeak(ImagePlus imp) {
        float fVal, fMaxVal = Float.MIN_VALUE;
        int xMax=-1, yMax=-1, zMax=-1;
        
        int w = imp.getWidth();
        int h = imp.getHeight();
        int d = imp.getStackSize();
        
        float[] sfSlice;
        int x, y, yw, z;
        // find the position of the maximum value
        for (z=0; z<d; z++) {
            sfSlice = impToFloat(imp, z+1);
            for (y=0, yw=0; y<h; y++, yw+=w) {
                for (x=0; x<w; x++) {
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
        int[] siRet = new int[3];
        siRet[0] = xMax;
        siRet[1] = yMax;
        siRet[2] = zMax;
        
        return siRet;
    }
}

