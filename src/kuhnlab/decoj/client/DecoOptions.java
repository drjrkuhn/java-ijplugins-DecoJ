package kuhnlab.decoj.client;


import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class DecoOptions {
    static final int MAX_WL = 10;	// maximum number of wavelengths in stack
    static final String DEFAULT_PROP_FILE = "allstacks";
    static final String DEFAULT_PROP_EXT = ".dop";
    static final String DEFAULT_PROP_DESC = "Deconvolution Options";
    
    int iNumIterations=500;
    int iNumPlanes=64;
    int iNumWL=1;
    String[] astrPsfFile;
    
    // Profile keys
    static final String P_NUMITERATIONS	= "num_iterations";
    static final String P_NUMPLANES		= "num_planes";
    static final String P_NUMWL			= "num_wavelengths";
    static final String P_PSFFILE		= "psf";
    
    
    DecoOptions() {
        astrPsfFile = new String[MAX_WL];
        for (int i=0; i<MAX_WL; i++) {
            astrPsfFile[i] = new String();
        }
    }
    
    
    /** Store variables in properties file */
    void storeValues(PropertiesFile props) {
        props.setPropInt(P_NUMITERATIONS, iNumIterations);
        props.setPropInt(P_NUMPLANES, iNumPlanes);
        props.setPropInt(P_NUMWL, iNumWL);
        for (int i=0; i<iNumWL; i++) {
            props.setPropString(P_PSFFILE+(i+1), astrPsfFile[i]);
        }
    }
    
    /** Load variables from properties file */
    void loadValues(PropertiesFile props) {
        iNumIterations = props.getPropInt(P_NUMITERATIONS, 500);
        iNumPlanes = props.getPropInt(P_NUMPLANES, 64);
        iNumWL = props.getPropInt(P_NUMWL, 1);
        if (iNumWL > MAX_WL)
            iNumWL = MAX_WL;
        for (int i=0; i<iNumWL; i++) {
            astrPsfFile[i] = props.getPropString(P_PSFFILE+(i+1), "");
        }
    }
    
    
    /** load a configuration file */
    void loadFromFile(String filename) {
        if (filename == null) {
            OpenDialog od = new OpenDialog("Load "+DEFAULT_PROP_DESC, null);
            if (od.getFileName() == null)
                return;
            filename = od.getDirectory() + od.getFileName();
        }
        PropertiesFile props = new PropertiesFile();
        props.loadFromFile(filename);
        loadValues(props);
    }
    
    /** save a configuration file */
    void saveToFile(String filename) {
        if (filename == null) {
            filename = DEFAULT_PROP_FILE + DEFAULT_PROP_EXT;
            SaveDialog sd = new SaveDialog("Save " + DEFAULT_PROP_DESC, filename, DEFAULT_PROP_EXT);
            if (sd.getFileName() == null)
                return;
            filename = sd.getDirectory() + sd.getFileName();
        }
        PropertiesFile props = new PropertiesFile();
        storeValues(props);
        props.saveToFile(filename, DEFAULT_PROP_DESC);
    }
}