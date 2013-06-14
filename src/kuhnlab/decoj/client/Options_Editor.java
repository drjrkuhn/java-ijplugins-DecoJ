package kuhnlab.decoj.client;


import ij.plugin.*;
import ij.*;
import java.lang.*;
import java.util.*;

public class Options_Editor implements PlugIn {
    public void run(String arg) {
        DecoOptions options = new DecoOptions();
        DecoOptionsDlg dlg = new DecoOptionsDlg(IJ.getInstance(), options, false);
        dlg.show();
    }
}
