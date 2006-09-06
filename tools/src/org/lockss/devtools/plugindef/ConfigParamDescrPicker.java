/*
 * $Id: ConfigParamDescrPicker.java,v 1.12 2006-09-06 16:38:41 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.lockss.daemon.*;
import org.lockss.util.Logger;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class ConfigParamDescrPicker
    extends JDialog
    implements EDPEditor {
  JPanel mainPanel = new JPanel();
  JPanel AssignedPanel = new JPanel();
  JList PluginParamList = new JList();
  JButton RemoveButton = new JButton();
  JPanel AvailablePanel = new JPanel();
  JPanel ButtonPanel = new JPanel();
  JButton OkButton = new JButton();
  JButton CreateButton = new JButton();
  JButton CancelButton = new JButton();
  FlowLayout flowLayout1 = new FlowLayout();
  JPanel btnPanel1 = new JPanel();
  JButton editButton = new JButton();
  JButton viewButton = new JButton();
  JPanel btnPanel2 = new JPanel();
  JButton addButton = new JButton();
  JList AvailableParamList = new JList();

  // non ui params
  private EDPCellData m_data;
  protected EditableDefinablePlugin plugin;
  protected Vector listeners;
  Map defaultDescrs;
  TitledBorder assignedBorder;
  TitledBorder availableBorder;
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  BorderLayout borderLayout1 = new BorderLayout();
  BorderLayout borderLayout2 = new BorderLayout();

  protected static Logger logger = Logger.getLogger("ConfigParamDescrPicker");

  public ConfigParamDescrPicker(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the configuration parameter picker";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Configuration Parameter Picker",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public ConfigParamDescrPicker(Frame frame) {
    this(frame, "Configuration Parameter Picker", false);
  }

  private void jbInit() throws Exception {
    assignedBorder = new TitledBorder("");
    availableBorder = new TitledBorder("");
    mainPanel.setLayout(gridBagLayout1);
    AssignedPanel.setLayout(borderLayout1);
    RemoveButton.setToolTipText("Remove Parameter from assigned parameter list.");
    RemoveButton.setText("Remove");
    RemoveButton.addActionListener(new
        ConfigParamDescrPicker_RemoveButton_actionAdapter(this));
    AvailablePanel.setLayout(borderLayout2);
    OkButton.setText("OK");
    OkButton.addActionListener(new
                               ConfigParamDescrPicker_OkButton_actionAdapter(this));
    CreateButton.setToolTipText("Create a custom Configuration Parameter.");
    CreateButton.setText("Custom...");
    CreateButton.addActionListener(new
        ConfigParamDescrPicker_CreateButton_actionAdapter(this));
    CancelButton.setText("Cancel");
    CancelButton.addActionListener(new
        ConfigParamDescrPicker_CancelButton_actionAdapter(this));
    ButtonPanel.setForeground(Color.black);
    ButtonPanel.setBorder(BorderFactory.createEtchedBorder());
    ButtonPanel.setLayout(flowLayout1);
    this.setTitle("Plugin Configuration Parameter List");
    editButton.setText("Edit");
    editButton.addActionListener(new
        ConfigParamDescrPicker_editButton_actionAdapter(this));
    viewButton.setToolTipText("View Configuration Parmameter.");
    viewButton.setText("View");
    viewButton.addActionListener(new
        ConfigParamDescrPicker_viewButton_actionAdapter(this));
    addButton.setToolTipText("Add parameter to assigned parameter list.");
    addButton.setText("Add");
    addButton.addActionListener(new
                                ConfigParamDescrPicker_addButton_actionAdapter(this));
    AssignedPanel.setBorder(assignedBorder);
    AssignedPanel.setMinimumSize(new Dimension(155, 150));
    AssignedPanel.setPreferredSize(new Dimension(180, 200));
    AvailablePanel.setBorder(availableBorder);
    AvailablePanel.setMinimumSize(new Dimension(155, 150));
    AvailablePanel.setPreferredSize(new Dimension(180, 200));
    assignedBorder.setTitle("Plugin Parameters");
    assignedBorder.setBorder(BorderFactory.createEtchedBorder());
    assignedBorder.setTitleFont(new java.awt.Font("Dialog", 0, 12));
    availableBorder.setTitle("Available Parameters");
    availableBorder.setBorder(BorderFactory.createEtchedBorder());
    availableBorder.setTitleFont(new java.awt.Font("Dialog", 0, 12));
    mainPanel.setMinimumSize(new Dimension(400, 220));
    mainPanel.setPreferredSize(new Dimension(480, 250));
    AvailableParamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    PluginParamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    ButtonPanel.add(CreateButton, null);
    ButtonPanel.add(OkButton, null);
    ButtonPanel.add(CancelButton, null);
    mainPanel.add(AssignedPanel,        new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 0, 0), 0, 0));
    getContentPane().add(mainPanel, BorderLayout.CENTER);
    mainPanel.add(AvailablePanel,       new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
    btnPanel2.add(addButton, null);
    btnPanel2.add(viewButton, null);
    AvailablePanel.add(AvailableParamList, BorderLayout.CENTER);
    AvailablePanel.add(btnPanel2, BorderLayout.SOUTH);
    btnPanel1.add(RemoveButton, null);
    btnPanel1.add(editButton, null);
    AssignedPanel.add(PluginParamList, BorderLayout.CENTER);
    mainPanel.add(ButtonPanel,             new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    AssignedPanel.add(btnPanel1, BorderLayout.SOUTH);
  }

  void initLists() {
    Collection knownList = plugin.getKnownConfigParamDescrs();
    Collection pluginList = plugin.getConfigParamDescrs();
    defaultDescrs = EditableDefinablePlugin.getDefaultConfigParamDescrs();
    PickerCellRenderer renderer = new PickerCellRenderer();
    Iterator it;
    // add current plugin parameters
    DefaultListModel dlm = new DefaultListModel();
    dlm.removeAllElements();
    for (it = pluginList.iterator(); it.hasNext(); ) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      dlm.addElement(cpd);
    }
    PluginParamList.setModel(dlm);
    PluginParamList.setCellRenderer(renderer);

    // add the available parameters
    dlm = new DefaultListModel();
    dlm.removeAllElements();
    for (it = knownList.iterator(); it.hasNext(); ) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      if (!pluginList.contains(cpd)) {
        dlm.addElement(cpd);
      }
    }
    AvailableParamList.setModel(dlm);
    AvailableParamList.setCellRenderer(renderer);
  }

  boolean addConfigParamDescr(ConfigParamDescr cpd) {
    if(addPluginParam(cpd)) {
      plugin.addConfigParamDescr(cpd);
      return true;
    }
    return false;
  }

  boolean addPluginParam(ConfigParamDescr cpd) {
    DefaultListModel dlm = (DefaultListModel) PluginParamList.getModel();
    String key = cpd.getKey();
    for (int index = 0; index < dlm.size(); index++) {
      ConfigParamDescr descr = (ConfigParamDescr) dlm.getElementAt(index);
      if (descr.getKey().equals(key)) {
        int ret = JOptionPane.showConfirmDialog(
            this, "Replace existing entry for " + key +
            " and display name " + descr.getDisplayName() + " with new entry?",
            "Replace Existing Configuration Param", JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.NO_OPTION) {
          return false;
        }
        else {
          dlm.removeElementAt(index);
          break;
        }
      }
    }
    dlm.addElement(cpd);
    return true;
  }

  void removeButton_actionPerformed(ActionEvent e) {
    int index = PluginParamList.getSelectedIndex();
    if (index >= 0) {
      DefaultListModel dlm = (DefaultListModel) PluginParamList.getModel();
      ConfigParamDescr cpd  = (ConfigParamDescr) dlm.getElementAt(index);
      if(plugin.canRemoveParam(cpd)) {
        dlm.removeElementAt(index);
        dlm = (DefaultListModel) AvailableParamList.getModel();
        dlm.addElement(cpd);
      }
      else {
        JOptionPane.showMessageDialog(this,
            "You cannot delete a parameter that is being used.",
            "Deletion Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  void addButton_actionPerformed(ActionEvent e) {
    int index = AvailableParamList.getSelectedIndex();
    if (index >= 0) {
      DefaultListModel dlm = (DefaultListModel) AvailableParamList.getModel();
      ConfigParamDescr cpd  = (ConfigParamDescr) dlm.getElementAt(index);
      if(addPluginParam(cpd)) {
        dlm.removeElementAt(index);
      }
    }
  }

  void editButton_actionPerformed(ActionEvent e) {
    int index = PluginParamList.getSelectedIndex();
    if (index >= 0) {
      DefaultListModel dlm = (DefaultListModel) PluginParamList.getModel();
      ConfigParamDescr cpd  = (ConfigParamDescr) dlm.getElementAt(index);
      showConfigParamEditor(cpd, !defaultDescrs.containsKey(cpd.getKey()));
    }
  }

  void viewButton_actionPerformed(ActionEvent e) {
    int index = AvailableParamList.getSelectedIndex();
    if (index >= 0) {
      DefaultListModel dlm = (DefaultListModel) AvailableParamList.getModel();
      ConfigParamDescr cpd  = (ConfigParamDescr) dlm.getElementAt(index);
      showConfigParamEditor(cpd, false);
    }
  }

  void OkButton_actionPerformed(ActionEvent e) {
    // we need to copy the current state of the displayed list to our plugin

    DefaultListModel dlm = (DefaultListModel) PluginParamList.getModel();
    plugin.removePluginConfigDescrs();
    for(int index = 0; index < dlm.size(); index++) {
      ConfigParamDescr descr = (ConfigParamDescr) dlm.getElementAt(index);
      plugin.addConfigParamDescr(descr);
    }
    plugin.notifyParamsChanged();
    setVisible(false);
  }

  void CancelButton_actionPerformed(ActionEvent e) {
    // we need to just leave the items unchanged
    setVisible(false);
  }

  void CreateButton_actionPerformed(ActionEvent e) {
    // open up the ConfigParamDescrEditor
    ConfigParamDescr cpd = new ConfigParamDescr();
    showConfigParamEditor(cpd, true);
  }

  void showConfigParamEditor(ConfigParamDescr cpd, boolean editable) {
    ConfigParamDescrEditor dlg = new ConfigParamDescrEditor(this, cpd, editable);
    Dimension dlgSize = dlg.getPreferredSize();
    Point loc = getLocation();
    dlg.setLocation(dlgSize.width / 2 + loc.x, dlgSize.height / 2 + loc.y);
    dlg.setModal(true);
    dlg.pack();
    dlg.setVisible(true);
  }

  /**
   * setCellData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
    plugin = data.getPlugin();
    initLists();
  }

}

class ConfigParamDescrPicker_RemoveButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_RemoveButton_actionAdapter(ConfigParamDescrPicker
      adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.removeButton_actionPerformed(e);
  }
}

class ConfigParamDescrPicker_OkButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_OkButton_actionAdapter(ConfigParamDescrPicker adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.OkButton_actionPerformed(e);
  }
}

class ConfigParamDescrPicker_CancelButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_CancelButton_actionAdapter(ConfigParamDescrPicker
      adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.CancelButton_actionPerformed(e);
  }
}

class ConfigParamDescrPicker_CreateButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_CreateButton_actionAdapter(ConfigParamDescrPicker
      adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.CreateButton_actionPerformed(e);
  }
}

class ConfigParamDescrPicker_viewButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_viewButton_actionAdapter(ConfigParamDescrPicker
                                                  adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.viewButton_actionPerformed(e);
  }
}

class ConfigParamDescrPicker_addButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_addButton_actionAdapter(ConfigParamDescrPicker
                                                 adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.addButton_actionPerformed(e);
  }
}

class ConfigParamDescrPicker_editButton_actionAdapter
    implements java.awt.event.ActionListener {
  ConfigParamDescrPicker adaptee;

  ConfigParamDescrPicker_editButton_actionAdapter(ConfigParamDescrPicker
                                                  adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.editButton_actionPerformed(e);
  }
}

class PickerCellRenderer extends JLabel implements ListCellRenderer {
    public PickerCellRenderer() {
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {

      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      }
      else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      if(value instanceof ConfigParamDescr) {
        ConfigParamDescr descr = (ConfigParamDescr) value;
        setText(descr.getKey() + " ( " + descr.getDisplayName() + " )");
      }
      return this;
    }
}

