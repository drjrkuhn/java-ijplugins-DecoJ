package kuhnlab.decoj.client;


import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.util.ArrayList;
import java.util.List;

public class Deconvolve_Stack implements PlugInFilter {
    ImagePlus imp;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        if (IJ.versionLessThan("1.17y"))
            return DONE;
        else
            return DOES_8C + DOES_8G + DOES_16 + DOES_32 + STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) {
        if (imp == null) {
            IJ.showMessage("Deconvolve Stack", "No images are open.");
            return;
        }
        
        // prompt the user for deconvolution options
        DecoOptions options = new DecoOptions();
        DecoOptionsDlg dlg = new DecoOptionsDlg(IJ.getInstance(), options, true);
        dlg.show();
        
        if (dlg.wasCanceled())
            return;
        
        List vFileInfo = new ArrayList();
        vFileInfo.add(new SourceFileInfo(imp, options));
        
        BatchListCheckDlg blcdlg
                = new BatchListCheckDlg(IJ.getInstance(), vFileInfo);
        blcdlg.show();
        
    }
    
}