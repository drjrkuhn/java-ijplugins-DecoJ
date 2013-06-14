package kuhnlab.decoj.client;


import ij.gui.*;
import ij.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.lang.*;

public class BatchListCheckDlg extends Dialog implements ActionListener, WindowListener  {
    
    boolean bCanceled = false;
    
    /** CONSTRUCTOR */
    public BatchListCheckDlg(Frame owner, List v) {
        super(owner, "Batch Processing");
        setModal(true);
        vBatchList = v;
        addWindowListener(this);
        setResizable(true);
        createMainPanel();
        setSize(500,300);
        doLayout();
        GUI.center(this);
        updateMainPanel();
    }
    
    public boolean wasCanceled() {
        return bCanceled;
    }
    
    //=======================================================================
    // Main panel
    //=======================================================================
    
    // Controls
    TextPanel tpList			= new TextPanel();
    Button btOK					= new Button("    OK    ");
    Button btCancel				= new Button("Cancel");
    
    // Values
    public List vBatchList = null;
    
    /** Create the main control panel */
    void createMainPanel() {
        
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gb);
        setBackground(SystemColor.control);
        c.insets = new Insets(2,2,2,2);
        
        
        c.gridx = 0;	c.gridy = 0;
        c.gridwidth=2;	c.gridheight=1;
        c.fill = c.HORIZONTAL;
        c.weightx = 3;
        Label lb = new Label("Process the following files?", Label.CENTER);
        lb.setBackground(SystemColor.controlShadow);
        lb.setForeground(SystemColor.controlHighlight);
        add(constrain(lb, gb, c));
        
        c.gridy ++;
        c.gridwidth=2;	c.gridheight=2;
        c.fill = c.BOTH;
        c.weightx = 3;
        c.weighty = 3;
        add(constrain(tpList, gb, c));
        
        Panel bpan = new Panel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        bpan.add(btOK);
        bpan.add(btCancel);
        
        c.gridy += 3;
        c.gridwidth = 2;	c.gridheight = 1;
        c.fill = c.NONE;
        c.weightx = 0;		c.weighty = 0;
        c.fill = c.HORIZONTAL;
        add(constrain(bpan, gb, c));
        
        btOK.addActionListener(this);
        btCancel.addActionListener(this);
    }
    
    /** Retrieve user-input values from the panel and store in variables */
    void retrieveMainPanel() {
    }
    
    /** Load the controls from variables */
    void updateMainPanel() {
        String strLastDir = "";
        String strDir;
        SourceFileInfo sfi;
        tpList.setColumnHeadings("Directory\tFile\tZSize\t#WL\t#Iter");
        int i, len=vBatchList.size();
        if (len > 0) {
            for (i=0; i<len; i++) {
                sfi = (SourceFileInfo)vBatchList.get(i);
                strDir = sfi.strPath;
                if (strLastDir.equals(strDir))
                    strDir = "    ........";
                else
                    strLastDir = strDir;
                tpList.appendLine(strDir
                        +"\t"+ sfi.strFilename
                        +"\t"+ sfi.options.iNumPlanes
                        +"\t"+ sfi.options.iNumWL
                        +"\t"+ sfi.options.iNumIterations);
            }
        }
    }
    
    //-------------
    // Actions
    //-------------
    
    /** called when OK was pressed */
    void doOK() {
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
        if (b == btOK) {
            doOK();
        } else if (b == btCancel) {
            doCancel();
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
