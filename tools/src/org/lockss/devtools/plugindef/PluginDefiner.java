/*
 * $Id: PluginDefiner.java,v 1.1 2004-05-25 00:17:45 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.devtools.plugindef;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class PluginDefiner extends JFrame {
  JPanel contentPane;
  JMenuBar jMenuBar1 = new JMenuBar();
  JMenu jMenuFile = new JMenu();
  JMenuItem jMenuFileExit = new JMenuItem();
  JMenu jMenuHelp = new JMenu();
  JMenuItem jMenuHelpAbout = new JMenuItem();
  JMenuItem jMenuFileNew = new JMenuItem();
  JMenuItem jMenuFileOpen = new JMenuItem();
  JMenuItem jMenuFileSave = new JMenuItem();
  JMenuItem jMenuFileSaveAs = new JMenuItem();
  JMenu jMenuEdit = new JMenu();
  JMenuItem jMenuEditCut = new JMenuItem();
  JMenuItem jMenuEditCopy = new JMenuItem();
  JMenuItem jMenuEditPaste = new JMenuItem();
  JMenuItem jMenuEditDelete = new JMenuItem();
  JFileChooser jFileChooser1 = new JFileChooser();
  { jFileChooser1.setFileFilter(
        new SimpleFileFilter(EditableDefinablePlugin.MAP_SUFFIX));
  }
  JTable jTable1 = new CellEditorJTable();
  TitledBorder titledBorder1;
  EditableDefinablePlugin edp = null;
  String location = null;
  String name = null;
  EDPInspectorTableModel inspectorModel = new EDPInspectorTableModel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JMenu jMenuPlugin = new JMenu();
  JMenuItem rulesTestMenuItem = new JMenuItem();
  JMenuItem jMenuItem2 = new JMenuItem();
  JCheckBoxMenuItem expertModeMenuItem = new JCheckBoxMenuItem();
  JMenuItem filtersTestMenuItem = new JMenuItem();
  //Construct the frame
  public PluginDefiner() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  //Component initialization
  private void jbInit() throws Exception  {
    contentPane = (JPanel) this.getContentPane();
    titledBorder1 = new TitledBorder("");
    contentPane.setLayout(gridBagLayout1);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.setSize(new Dimension(380, 320));
    this.setTitle("Plugin Configuration");
    jMenuFile.setText("File");
    jMenuFileExit.setText("Exit");
    jMenuFileExit.addActionListener(new Configurator_jMenuFileExit_ActionAdapter(this));
    jMenuHelp.setText("Help");
    jMenuHelpAbout.setText("About");
    jMenuHelpAbout.addActionListener(new Configurator_jMenuHelpAbout_ActionAdapter(this));
    jMenuFileNew.setText("New");
    jMenuFileNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke('N', java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuFileNew.addActionListener(new Configurator_jMenuFileNew_actionAdapter(this));
    jMenuFileOpen.setText("Open...");
    jMenuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke('O', java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuFileOpen.addActionListener(new Configurator_jMenuFileOpen_actionAdapter(this));
    jMenuFileSave.setText("Save");
    jMenuFileSave.setActionCommand("Save");
    jMenuFileSave.addActionListener(new Configurator_jMenuFileSave_actionAdapter(this));
    jMenuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke('S', java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuFileSaveAs.setActionCommand("SaveAs");
    jMenuFileSaveAs.setText("Save as...");
    jMenuFileSaveAs.addActionListener(new Configurator_jMenuFileSave_actionAdapter(this));
    jMenuEdit.setText("Edit");
    jMenuEditCut.setText("Cut");
    jMenuEditCut.setAccelerator(javax.swing.KeyStroke.getKeyStroke('X', java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuEditCopy.setText("Copy");
    jMenuEditCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke('C', java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuEditPaste.setText("Paste");
    jMenuEditPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke('V', java.awt.event.KeyEvent.CTRL_MASK, false));
    jMenuEditDelete.setText("Delete");
    jTable1.setBorder(BorderFactory.createEtchedBorder());
    jTable1.setMinimumSize(new Dimension(50, 200));
    jTable1.setPreferredSize(new Dimension(360, 300));
    jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    jTable1.setCellSelectionEnabled(true);
    jTable1.setColumnSelectionAllowed(false);
    jTable1.setModel(inspectorModel);
    jTable1.setRowHeight(20);

    contentPane.setMinimumSize(new Dimension(80, 200));
    contentPane.setPreferredSize(new Dimension(380, 320));
    jMenuPlugin.setText("Plugin");
    rulesTestMenuItem.setText("Test CrawlRules...");
    rulesTestMenuItem.addActionListener(new Configurator_rulesTestMenuItem_actionAdapter(this));
    filtersTestMenuItem.setText("Test Filters...");
    expertModeMenuItem.setText("ExpertMode");
    expertModeMenuItem.addActionListener(new Configurator_expertModeMenuItem_actionAdapter(this));
    filtersTestMenuItem.setText("Test Filters");
    filtersTestMenuItem.addActionListener(new Configurator_filtersTestMenuItem_actionAdapter(this));
    jMenuFile.add(jMenuFileNew);
    jMenuFile.add(jMenuFileOpen);
    jMenuFile.add(jMenuFileSave);
    jMenuFile.add(jMenuFileSaveAs);
    jMenuFile.addSeparator();
    jMenuFile.add(jMenuFileExit);
    jMenuHelp.add(jMenuHelpAbout);
    jMenuBar1.add(jMenuFile);
    jMenuBar1.add(jMenuEdit);
    jMenuBar1.add(jMenuPlugin);
    jMenuBar1.add(jMenuHelp);
    jMenuEdit.add(jMenuEditCut);
    jMenuEdit.add(jMenuEditCopy);
    jMenuEdit.add(jMenuEditPaste);
    jMenuEdit.add(jMenuEditDelete);
    contentPane.add(jTable1,   new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 360, 0));
    jMenuPlugin.add(expertModeMenuItem);
    jMenuPlugin.addSeparator();
    jMenuPlugin.add(rulesTestMenuItem);
    jMenuPlugin.add(filtersTestMenuItem);
    jMenuPlugin.add(filtersTestMenuItem);
    this.setJMenuBar(jMenuBar1);
    edp = new EditableDefinablePlugin();
    CellEditorJTable cejTable = (CellEditorJTable)jTable1;
    inspectorModel.setCellEditor((CellEditorJTable.CellEditorModel)cejTable.getCellEditorModel());
    inspectorModel.setPluginData(edp);
  }

  //File | Exit action performed
  public void jMenuFileExit_actionPerformed(ActionEvent e) {
    System.exit(0);
  }

  //Help | About action performed
  public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
    Configurator_AboutBox dlg = new Configurator_AboutBox(this);
    Dimension dlgSize = dlg.getPreferredSize();
    Dimension frmSize = getSize();
    Point loc = getLocation();
    dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
    dlg.setModal(true);
    dlg.pack();
    dlg.show();
  }

  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      jMenuFileExit_actionPerformed(null);
    }
  }

  void jMenuFileOpen_actionPerformed(ActionEvent e) {
    int option = jFileChooser1.showOpenDialog(this);
    if(option == jFileChooser1.APPROVE_OPTION
       || jFileChooser1.getSelectedFile() != null) {
      name = jFileChooser1.getSelectedFile().getName();
      location = jFileChooser1.getSelectedFile().getParent();
      edp = new EditableDefinablePlugin();
      edp.loadMap(location, name);
      // update the table
      inspectorModel.setPluginData(edp);
    }
  }

  void jMenuFileSave_actionPerformed(ActionEvent e) {
    if(e.getActionCommand().equals("SaveAs") ||
       edp == null || edp.getMapName() == null) {
      int option = jFileChooser1.showSaveDialog(this);
      if(option != jFileChooser1.APPROVE_OPTION ||
         jFileChooser1.getSelectedFile() == null)
        return;
      name = jFileChooser1.getSelectedFile().getName();
      location = jFileChooser1.getSelectedFile().getParent();
    }

    // write the file
    edp.writeMap(location, name);
  }

  void jMenuFileNew_actionPerformed(ActionEvent e) {
    if(edp != null && edp.getMapName() != null) {
      edp.writeMap(location, name);
    }
    edp = new EditableDefinablePlugin();
    name = null;
    inspectorModel.setPluginData(edp);
  }

  void expertModeMenuItem_actionPerformed(ActionEvent e) {
    inspectorModel.setExpertMode(expertModeMenuItem.getState());
  }

  void rulesTestMenuItem_actionPerformed(ActionEvent e) {
    JDialog dialog = new CrawlRuleTestDialog(this, edp);
    dialog.show();
  }

  void filtersTestMenuItem_actionPerformed(ActionEvent e) {
    JDialog dialog = new FilterTestDialog(this, edp);
    dialog.show();
  }

}

class Configurator_jMenuFileExit_ActionAdapter implements ActionListener {
  PluginDefiner adaptee;

  Configurator_jMenuFileExit_ActionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jMenuFileExit_actionPerformed(e);
  }
}

class Configurator_jMenuHelpAbout_ActionAdapter implements ActionListener {
  PluginDefiner adaptee;

  Configurator_jMenuHelpAbout_ActionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jMenuHelpAbout_actionPerformed(e);
  }
}

class Configurator_jMenuFileOpen_actionAdapter implements ActionListener {
  PluginDefiner adaptee;

  Configurator_jMenuFileOpen_actionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jMenuFileOpen_actionPerformed(e);
  }
}

class Configurator_jMenuFileSave_actionAdapter implements ActionListener {
  PluginDefiner adaptee;

  Configurator_jMenuFileSave_actionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jMenuFileSave_actionPerformed(e);
  }
}

class Configurator_jMenuFileNew_actionAdapter implements java.awt.event.ActionListener {
  PluginDefiner adaptee;

  Configurator_jMenuFileNew_actionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jMenuFileNew_actionPerformed(e);
  }
}

class Configurator_expertModeMenuItem_actionAdapter implements java.awt.event.ActionListener {
  PluginDefiner adaptee;

  Configurator_expertModeMenuItem_actionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.expertModeMenuItem_actionPerformed(e);
  }
}

class Configurator_rulesTestMenuItem_actionAdapter implements java.awt.event.ActionListener {
  PluginDefiner adaptee;

  Configurator_rulesTestMenuItem_actionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.rulesTestMenuItem_actionPerformed(e);
  }
}

class Configurator_filtersTestMenuItem_actionAdapter implements java.awt.event.ActionListener {
  PluginDefiner adaptee;

  Configurator_filtersTestMenuItem_actionAdapter(PluginDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.filtersTestMenuItem_actionPerformed(e);
  }
}

