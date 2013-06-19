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
import java.lang.*;

/** Holds file information for deconvolution processing. */
public class SourceFileInfo implements Cloneable {
    public int				iType			= 0;
    public int				iWidth			= 0;
    public int				iHeight			= 0;
    public int				iDepth			= 0;
    public int				iTime			= 0;
    public int				iWavelength		= 0;
    public DecoOptions		options			=null;
    public String 			strPath			=null;
    public String 			strFilename		=null;
    public String 			strLayoutFile	=null;
    protected ImagePlus		imp				=null;
    
    public SourceFileInfo() {}
    
    public SourceFileInfo(ImagePlus imp, DecoOptions options) {
        this.imp = imp;
        this.iType = imp.getType();
        this.iWidth = imp.getWidth();
        this.iHeight = imp.getHeight();
        this.iDepth = imp.getStackSize();
        this.strPath = "WINDOW";
        this.options = options;
        this.strFilename = imp.getTitle();
    }
    
    public SourceFileInfo(String strPath, String strFilename,
            DecoOptions options) {
        this.strPath = strPath;
        this.strFilename = strFilename;
        this.options = options;
    }
    
    public void setImagePlus(ImagePlus imp) {
        this.imp = imp;
        this.iType = imp.getType();
        this.iWidth = imp.getWidth();
        this.iHeight = imp.getHeight();
        this.iDepth = imp.getStackSize();
    }
    
    public ImagePlus getImagePlus() {
        return imp;
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
    
    public String toString() {
        return strPath +" "+ strFilename +" "+ iWidth +"x"+ iHeight +"x"+ iDepth
                +" t="+ iTime +" wl="+ iWavelength;
    }
}

