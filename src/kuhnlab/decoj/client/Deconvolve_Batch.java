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


import ij.plugin.*;
import ij.io.*;
import ij.*;
import java.lang.*;
import java.util.List;
import java.io.*;
import java.util.ArrayList;

public class Deconvolve_Batch implements PlugIn {
    
    List vFileInfo = new ArrayList();	// collection of SourceFileInfo's
    
    public void run(String arg) {
        
        // ask the user for the Directory to process
        OpenDialog od = new OpenDialog("Select Image Directory...", arg);
        String strRootDir = od.getDirectory();
        String strName = od.getFileName();
        if (strName==null)
            return;
        
        vFileInfo.clear();
        buildFileList(strRootDir, vFileInfo);
        
        if (vFileInfo.size() == 0) {
            IJ.showMessage(
                    "No Deconvolution options file (*.dop) was found.\n\n"+
                    "Please use the Options Editor to create a single\n"+
                    "options file for each subdirectory you wish to\n"+
                    "be processed.");
            return;
        }
        
        BatchListCheckDlg blcdlg;
        blcdlg = new BatchListCheckDlg(IJ.getInstance(), vFileInfo);
        blcdlg.show();
        
        if (blcdlg.wasCanceled())
            return;
        
        Deconvolver deco = new Deconvolver();
        int i, len=vFileInfo.size();
        for (i=0; i<len; i++) {
            SourceFileInfo sfi = (SourceFileInfo)vFileInfo.get(i);
            IJ.write("Deconvolving "+sfi.strPath+"   "+sfi.strFilename);
            deco.deconvolve(sfi, true);
        }
        deco.deleteTempFiles();
    }
    
    public void buildFileList(String strRootDir, List vFileInfo) {
        File[] aFileList = new File(strRootDir).listFiles();
        if (aFileList == null)
            return;
        
        List vToAdd = new ArrayList();
        int iNumSFL = 0;
        int i, len=aFileList.length;
        String strSFL = "";
        File f;
        for (i=0; i<len; i++) {
            f = aFileList[i];
            if (f.isDirectory()) {
                // recursively check
                buildFileList(f.getAbsolutePath(), vFileInfo);
                continue;
            }
            if (!f.isHidden()) {
                String strName = f.getName();
                String strExt = getExt(strName);
                if (strExt == null)
                    continue;
                strExt = strExt.toLowerCase();
                if (strExt.equals(DecoOptions.DEFAULT_PROP_EXT)) {
                    strSFL = f.getAbsolutePath();
                    iNumSFL++;
                } else {
                    // consider this to be an image stack and add it
                    vToAdd.add(aFileList[i]);
                }
            }
        }
        
        if (iNumSFL < 1) {
            // no stack file layout file was found in the directory. Do not batch it
            return;
        } else if (iNumSFL > 1) {
            IJ.showMessage("Only one Stack File Layout (*.sfl) file\nis permitted per directory");
            return;
        }
        
        DecoOptions options = new DecoOptions();
        options.loadFromFile(strSFL);
        
        len = vToAdd.size();
        SourceFileInfo sfi;
        for (i=0; i<len; i++) {
            f = (File)vToAdd.get(i);
            sfi = new SourceFileInfo(strRootDir, f.getName(), options);
            vFileInfo.add(sfi);
        }
        
        return;
    }
    
    public String getExt(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0) return null;
        return name.substring(i);
    }
}