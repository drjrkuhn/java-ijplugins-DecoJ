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
import ij.io.*;
import java.util.*;
import java.io.*;

/** Extension of the Properties class to make it more usefull. */
public class PropertiesFile extends Properties {
    
    /** retrieve a string from a properties file */
    public String getPropString(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }
    
    /** place a string in a properties file */
    public void setPropString(String key, String val) {
        setProperty(key, val);
    }
    
    /** retrieve a boolean value from a properties file */
    public boolean getPropBoolean(String key, boolean defaultValue) {
        String s = getProperty(key);
        if (s==null)
            return defaultValue;
        s = s.toUpperCase();
        return s.equals("TRUE") || s.equals("T") || s.equals("YES") || s.equals("Y") || s.equals("1");
    }
    
    /** place a boolean value in a properties file */
    public void setPropBoolean(String key, boolean val) {
        setProperty(key, val ? "true" : "false");
    }
    
    /** retrieve an integer value from a properties file */
    public int getPropInt(String key, int defaultValue) {
        String s = getProperty(key);
        if (s!=null && !s.equals("")) {
            try {
                return Integer.decode(s).intValue();
            } catch (NumberFormatException e) {IJ.write(""+e);}
        }
        return defaultValue;
    }
    
    /** store an integer value in a properties file */
    public void setPropInt(String key, int val) {
        setProperty(key, Integer.toString(val));
    }
    
    /** retrieve a double-precision floating point value from a properties file */
    public double getPropDouble(String key, double defaultValue) {
        String s = getProperty(key);
        Double d = null;
        if (s!=null && !s.equals("")) {
            try {d = new Double(s);} catch (NumberFormatException e){ IJ.write(""+e); d = null;}
            if (d!=null) return(d.doubleValue());
        }
        return defaultValue;
    }
    
    /** store a double-precision floating point value in a properties file */
    public void setPropDouble(String key, double val) {
        setProperty(key, Double.toString(val));
    }
    
    /** get the full path of the default properties file */
    public static String getDefaultPropPath(String filename) {
        String sep = System.getProperty("file.separator");
        String homeDir = System.getProperty("user.dir");
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name");
        if (osName.indexOf("Windows",0)>-1) {
            //ImageJ folder on Windows
            return homeDir + sep + filename;
        } else {
            // Mac Preferences folder or Unix home dir
            return userHome + sep + filename;
        }
    }
    
    /** load default values from a property file. */
    void loadFromFile(String strPropFile) {
        FileInputStream f;
        try {
            f = new FileInputStream(strPropFile);
        } catch (FileNotFoundException e) {
            f = null;
        }
        if (f != null) {
            try {
                load(f);
                f.close();
            } catch (IOException e) {
                IJ.write("Error reading "+strPropFile);
            }
        }
    }
    
    /** save default values to a property file */
    void saveToFile(String strPropFile, String description) {
        // write the file
        try {
            FileOutputStream out = new FileOutputStream(strPropFile);
            store(out, description);
            out.close();
        } catch (IOException e) {
            IJ.write("Error writing "+strPropFile);
            return;
        }
    }
}

