/*
 * $Id: ConfigParamDescrEditor.java,v 1.6 2006-09-06 16:38:41 thib_gc Exp $
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

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import org.lockss.daemon.*;
import org.lockss.devtools.plugindef.TextInputVerifer.*;
import org.lockss.util.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class ConfigParamDescrEditor extends JDialog
    implements EDPEditor {
  JPanel ButtonPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  JPanel jPanel1 = new JPanel();
  JCheckBox definitionCheckBox = new JCheckBox();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel descriptorPanel = new JPanel();
  JLabel keyLabel = new JLabel();
  JTextField keyTextField = new JTextField();
  JLabel displayLabel = new JLabel();
  JTextField displayTextField = new JTextField();
  JLabel typeLabel = new JLabel();
  JComboBox typeComboBox = new JComboBox();
  JLabel sizeLabel = new JLabel();
  JLabel descriptionLabel = new JLabel();
  JTextField sizeTextField = new JTextField();
  JTextArea descriptionTextArea = new JTextArea();
  GridBagLayout gridBagLayout1 = new GridBagLayout();

  // data
  EDPCellData m_data;
  ConfigParamDescr m_paramDescr;
  private boolean m_isEditable;
  private ConfigParamDescrPicker m_picker;

  protected static Logger logger = Logger.getLogger("ConfigParamDescrEditor");

  public ConfigParamDescrEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the configuration parameter editor";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Configuration Parameter Editor",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public ConfigParamDescrEditor() {
    this(null, "", false);
  }

  public ConfigParamDescrEditor(ConfigParamDescrPicker picker,
                                ConfigParamDescr cpd, boolean editable) {
    this(null, "", false);
    initData(picker, cpd, editable);
  }

  protected void initData(ConfigParamDescrPicker picker,
                          ConfigParamDescr cpd, boolean editable) {
    m_paramDescr = cpd;
    m_isEditable = editable;
    m_picker = picker;
    if(cpd != null) {
      keyTextField.setText(cpd.getKey());
      keyTextField.setEditable(editable);
      displayTextField.setText(cpd.getDisplayName());
      displayTextField.setEditable(editable);
      sizeTextField.setText(String.valueOf(cpd.getSize()));
      sizeTextField.setEditable(editable);
      typeComboBox.setSelectedIndex(cpd.getType() - 1);
      typeComboBox.setEnabled(editable);
      String descr = cpd.getDescription();
      descriptionTextArea.setText(descr == null ? "" : descr);
      descriptionTextArea.setEditable(editable);
      definitionCheckBox.setSelected(cpd.isDefinitional());
      definitionCheckBox.setEnabled(true);
    }
  }

  protected boolean updateData() {
    if (m_paramDescr != null && isValidData()) {
      m_paramDescr.setKey(keyTextField.getText());
      m_paramDescr.setDisplayName(displayTextField.getText());
      m_paramDescr.setSize(Integer.parseInt(sizeTextField.getText()));
      m_paramDescr.setType(typeComboBox.getSelectedIndex() + 1);
      m_paramDescr.setDescription(descriptionTextArea.getText());
      m_paramDescr.setDefinitional(definitionCheckBox.isSelected());
      return m_picker.addConfigParamDescr(m_paramDescr);
    }
    return false;
  }

  private boolean isValidData() {

    if(StringUtil.isNullString(keyTextField.getText())) {
      return false;
    }

    if(StringUtil.isNullString(displayTextField.getText())) {
      return false;
    }

    if(StringUtil.isNullString(sizeTextField.getText())) {
      return false;
    }
    try {
      int val = Integer.parseInt(sizeTextField.getText());
      if(val > 0) {
        return true;
      }
    }
    catch(Exception ex) {
    }
    return false;
  }

  private void jbInit() throws Exception {
    jPanel1.setLayout(borderLayout2);
    definitionCheckBox.setToolTipText("Check box if changing the value of" +
                                      "this parameter would change the A.U.");
    definitionCheckBox.setText("Parameter is integral to the identity of an AU.");
    definitionCheckBox.setSelected(true);
    descriptorPanel.setLayout(gridBagLayout1);
    //keyLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    keyLabel.setText("Property Key:");
    keyTextField.setToolTipText("property key must be a single word with no spaces.");
    keyTextField.setText("");
    displayLabel.setText("Display Name:");
    //displayLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    displayTextField.setText("");
    displayTextField.setToolTipText("The name to display when configuring property.");
    displayTextField.setText("");
    //typeLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    typeLabel.setText("Data Type:");
    sizeLabel.setText("Field Size:");
    sizeLabel.setEnabled(true);
    //sizeLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    descriptionLabel.setText("Description:");
    //descriptionLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    sizeTextField.setInputVerifier(new UnsignedIntegerVerifier());
    sizeTextField.setText("10");
    sizeTextField.setColumns(10);
    sizeTextField.setSize(sizeTextField.getPreferredSize());
    sizeTextField.setToolTipText("The size of the parameters input field.");
    descriptionTextArea.setToolTipText("explantory or help text.");
    descriptionTextArea.setText("");
    typeComboBox.setToolTipText("The parameters data type..");
    // add our types
    for(int i=0; i < ConfigParamDescr.TYPE_STRINGS.length; i++) {
      typeComboBox.addItem(ConfigParamDescr.TYPE_STRINGS[i]);
    }
    descriptorPanel.setPreferredSize(new Dimension(405, 221));

    ButtonPanel.add(okButton, null);
    ButtonPanel.add(cancelButton, null);
    this.getContentPane().add(jPanel1,  BorderLayout.CENTER);
    jPanel1.add(definitionCheckBox,  BorderLayout.SOUTH);
    jPanel1.add(descriptorPanel,  BorderLayout.CENTER);
    this.getContentPane().add(ButtonPanel, BorderLayout.SOUTH);
    descriptorPanel.add(displayLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 12, 0, 0), 0, 0));
    descriptorPanel.add(keyLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 12, 6, 0), 0, 0));
    descriptorPanel.add(keyTextField,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 7, 0, 36), 251, 2));
    descriptorPanel.add(displayTextField,  new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(7, 7, 0, 36), 251, 2));
    descriptorPanel.add(typeComboBox,  new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(11, 7, 0, 36), 213, 6));
    descriptorPanel.add(descriptionLabel,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 12, 57, 0), 7, 0));
    descriptorPanel.add(typeLabel,     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 12, 5, 0), 21, 0));
    descriptorPanel.add(descriptionTextArea,  new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 7, 17, 36), 259, 40));
    descriptorPanel.add(sizeTextField,  new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 7, 0, 36), 251, 2));
    descriptorPanel.add(sizeLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 12, 9, 0), 14, 0));
    okButton.setText("OK");
    cancelButton.setText("Cancel");

    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });

    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
  }


  void okButton_actionPerformed(ActionEvent e) {
    if (m_isEditable) {
      if (!updateData()) {
        return;
      }
    }
    setVisible(false);
    dispose();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
    dispose();
  }

  /**
   * setCellData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
  }

}
