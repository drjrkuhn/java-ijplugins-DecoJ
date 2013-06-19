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
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;

public class DecoOptionsDlg extends Dialog implements ActionListener, ItemListener, WindowListener  {
    
    public DecoOptions options;
    boolean bCanceled = false;
    boolean bModal;
    Panel panMain = null;
    Panel panDeco = null;
    
    /** CONSTRUCTOR */
    public DecoOptionsDlg(Frame owner, DecoOptions options, boolean bModal) {
        super(owner, "Deconvolution Options");
        this.bModal = bModal;
        setModal(bModal);
        
        addWindowListener(this);
        setResizable(true);
        this.options = options;
        panDeco = createDecoPanel();
        panMain = createMainPanel();
        add(panDeco, BorderLayout.CENTER);
        add(panMain, BorderLayout.SOUTH);
        pack();
        GUI.center(this);
        updateDecoPanel();
        updateMainPanel();
        doSetNumWL(options.iNumWL, true);
    }
    
    public boolean wasCanceled() {
        return bCanceled;
    }
    
    //=======================================================================
    // Deconvolution Options panel
    //=======================================================================
    
    // Controls
    TextField tfNumIterations	= new TextField("----");
    
    /** Create the deconvolution options control panel */
    Panel createDecoPanel() {
        Panel pan = new Panel();
        
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        pan.setLayout(gb);
        pan.setBackground(SystemColor.control);
        c.insets = new Insets(2,2,2,2);
        
        c.gridx = 0;	c.gridy = 0;
        c.gridwidth=4;	c.gridheight=1;
        c.fill = c.HORIZONTAL;
        c.weightx = 1;
        Label lb = new Label("Deconvolution Options", Label.CENTER);
        lb.setBackground(SystemColor.controlShadow);
        lb.setForeground(SystemColor.controlHighlight);
        pan.add(constrain(lb, gb, c));
        
        c.gridx = 0;	c.gridy++;
        c.gridwidth=1;	c.gridheight=1;
        c.fill = c.NONE;
        c.weightx = 0;
        pan.add(constrain(new Label("Num Iterations:", Label.RIGHT), gb, c));
        c.gridx++;
        pan.add(constrain(tfNumIterations, gb, c));
        
        return pan;
    }
    
    /** Retrieve user-input values from the panel and store in variables */
    void retrieveDecoPanel() {
        options.iNumIterations = Integer.parseInt(tfNumIterations.getText());
    }
    
    /** Load the controls from variables */
    void updateDecoPanel() {
        tfNumIterations.setText(""+options.iNumIterations);
    }
    
    
    
    //=======================================================================
    // Main panel
    //=======================================================================
    
    // Controls
    Choice chNumWL				= new Choice();
    TextField tfNumPlanes		= new TextField("----");
    Label[] albPsfFile			= new Label[options.MAX_WL];
    TextField[] atfPsfFile		= new TextField[options.MAX_WL];
    Button[] abtSetPsfFile		= new Button[options.MAX_WL];
    Button btOK					= new Button("    OK    ");
    Button btCancel				= new Button("Cancel");
    Button btLoadConfig			= new Button("Load...");
    Button btSaveConfig			= new Button("Save...");
    Button btHelp				= new Button("Help");
    
    /** Create the main control panel */
    Panel createMainPanel() {
        int i;
        for (i=0; i<options.MAX_WL; i++) {
            chNumWL.add(""+(i+1));
            albPsfFile[i] = new Label("PSF "+(i+1)+" File:", Label.RIGHT);
            atfPsfFile[i] = new TextField("--------");
            abtSetPsfFile[i] = new Button("Set...");
        }
        
        Panel pan = new Panel();
        
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        pan.setLayout(gb);
        pan.setBackground(SystemColor.control);
        c.insets = new Insets(2,2,2,2);
        
        c.gridx = 0;	c.gridy = 0;
        c.gridwidth=10;	c.gridheight=1;
        c.fill = c.HORIZONTAL;
        Label lb = new Label("Stack File Layout", Label.CENTER);
        lb.setBackground(SystemColor.controlShadow);
        lb.setForeground(SystemColor.controlHighlight);
        pan.add(constrain(lb, gb, c));
        
        c.gridx = 0;	c.gridy++;
        c.gridwidth=1;	c.gridheight=1;
        c.fill = c.NONE;
        pan.add(constrain(new Label("Num Planes:", Label.RIGHT), gb, c));
        c.gridx++;
        pan.add(constrain(tfNumPlanes, gb, c));
        
        c.gridx++;
        c.gridwidth=1;
        c.fill = c.HORIZONTAL;
        c.weightx = 1;
        pan.add(constrain(new Label(""), gb, c));
        
        c.gridx+=1;		//c.gridy++;
        c.gridwidth=1;
        c.fill = c.NONE;
        c.weightx = 0;
        pan.add(constrain(new Label("Num Wavelengths:", Label.RIGHT), gb, c));
        c.gridx+=1;
        c.gridwidth=1;
        pan.add(constrain(chNumWL, gb, c));
        
        for (i=0; i<options.MAX_WL; i++) {
            c.gridx=0;		c.gridy++;
            pan.add(constrain(albPsfFile[i], gb, c));
            c.gridx++;	c.gridwidth = 8;
            c.fill = c.HORIZONTAL;
            c.weightx = 1;
            pan.add(constrain(atfPsfFile[i], gb, c));
            c.gridx+=8;	c.gridwidth = 1;
            c.fill = c.NONE;
            c.weightx = 0;
            pan.add(constrain(abtSetPsfFile[i], gb, c));
        }
        
        Panel bpan = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        if (bModal) {
            bpan.add(btOK);
            bpan.add(btCancel);
        }
        bpan.add(btLoadConfig);
        bpan.add(btSaveConfig);
        bpan.add(btHelp);
        
        
        c.gridx=0;	c.gridy++;
        c.gridwidth = 10;
        c.fill = c.HORIZONTAL;
        pan.add(constrain(bpan, gb, c));
        
        chNumWL.addItemListener(this);
        if (bModal) {
            btOK.addActionListener(this);
            btCancel.addActionListener(this);
        }
        btLoadConfig.addActionListener(this);
        btSaveConfig.addActionListener(this);
        btHelp.addActionListener(this);
        for (i=0; i<options.MAX_WL; i++) {
            abtSetPsfFile[i].addActionListener(this);
        }
        btHelp.setEnabled(false);
        
        return pan;
    }
    
    /** Retrieve user-input values from the panel and store in variables */
    void retrieveMainPanel() {
        options.iNumPlanes = Integer.parseInt(tfNumPlanes.getText());
        for (int i=0; i<options.MAX_WL; i++) {
            options.astrPsfFile[i] = atfPsfFile[i].getText();
        }
    }
    
    /** Load the controls from variables */
    void updateMainPanel() {
        tfNumPlanes.setText(""+options.iNumPlanes);
        for (int i=0; i<options.MAX_WL; i++) {
            atfPsfFile[i].setText(options.astrPsfFile[i]);
        }
    }
    
    //-------------
    // Actions
    //-------------
    
    /** Set the number of wavelengths to use */
    void doSetNumWL(int n, boolean bSetChoice) {
        options.iNumWL = n;
        for (int i=0; i<options.MAX_WL; i++) {
            albPsfFile[i].setVisible(i<n);
            atfPsfFile[i].setVisible(i<n);
            abtSetPsfFile[i].setVisible(i<n);
        }
        if (bSetChoice)
            chNumWL.select(n-1);
        Dimension dim = getSize();
        //hide();
        //pack();
        setSize(dim.width, getPreferredSize().height);
        doLayout();
        panMain.doLayout();
        panDeco.doLayout();
        //show();
    }
    
    /** load a configuration file */
    void doLoadConfig() {
        options.loadFromFile(null);
        updateDecoPanel();
        updateMainPanel();
        doSetNumWL(options.iNumWL, true);
    }
    
    /** save a configuration file */
    void doSaveConfig() {
        retrieveDecoPanel();
        retrieveMainPanel();
        options.saveToFile(null);
    }
    
    /** set a psf file name */
    void doSetPsfFile(int i) {
        retrieveDecoPanel();
        retrieveMainPanel();
        OpenDialog od = new OpenDialog("Set PSF File "+(i+1), null);
        String name = od.getFileName();
        if (name == null)
            return;
        options.astrPsfFile[i] = od.getDirectory() + name;
        updateDecoPanel();
        updateMainPanel();
    }
    
    /** called when OK was pressed */
    void doOK() {
        retrieveDecoPanel();
        retrieveMainPanel();
        hide();
        dispose();
    }
    
    void doCancel() {
        bCanceled = true;
        doOK();
    }
    
    
    //=======================================================================
    // Event Listeners
    //=======================================================================
    
    public void actionPerformed(ActionEvent e) {
        Object b = e.getSource();
        if (b == btLoadConfig) {
            doLoadConfig();
        } else if (b == btSaveConfig) {
            doSaveConfig();
        } else if (b == btOK) {
            doOK();
        } else if (b == btCancel) {
            doCancel();
        } else {
            for (int i=0; i<options.MAX_WL; i++) {
                if (b == abtSetPsfFile[i])
                    doSetPsfFile(i);
            }
        }
    }
    
    public void itemStateChanged(ItemEvent e) {
        Object b = e.getSource();
        if (b==chNumWL) {
            retrieveDecoPanel();
            retrieveMainPanel();
            doSetNumWL(chNumWL.getSelectedIndex()+1, false);
        }
    }
    
    //=======================================================================
    // Labeling stuff
    //=======================================================================
    
    /** create a new pane containing a text label and the given component */
    Panel labelComponent(String strLabel, Component comp) {
        Panel pan = new Panel(new GridLayout(1,2));
        //pan.setBackground(SystemColor.control);
        pan.add(new Label(strLabel, Label.RIGHT));
        pan.add(comp);
        return pan;
    }
    
    /** Apply a GridBagConstraint to a component. */
    Component constrain(Component comp, GridBagLayout gb, GridBagConstraints c) {
        gb.setConstraints(comp, c);
        return comp;
    }
    
    //=======================================================================
    // Window listener interface
    //=======================================================================
    
    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {
        doCancel();
    }
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    
}
