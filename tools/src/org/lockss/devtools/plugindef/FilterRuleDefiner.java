/*
 * $Id: FilterRuleDefiner.java,v 1.5 2006-09-06 16:38:41 thib_gc Exp $
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
import java.awt.event.*;
import javax.swing.*;

import org.lockss.util.Logger;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class FilterRuleDefiner extends JDialog
    implements ItemListener, EDPEditor {
  ImageIcon upIcon = new ImageIcon(PluginDefiner.class.getResource("images/Up24.gif"));
  ImageIcon dnIcon = new ImageIcon(PluginDefiner.class.getResource("images/Down24.gif"));
  static final String STRING_FILTER = "String Filter";
  static final String TAG_FILTER = "Tag Filter";
  static final String WHITE_SPACE_FILTER = "White Space Filter";
  String[] filterNames = {TAG_FILTER, STRING_FILTER, WHITE_SPACE_FILTER};
  JPanel mainPanel = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JList filterList = new JList();
  JPanel editPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JComboBox filterComboBox = new JComboBox();
  JPanel stringPanel = new JPanel();
  JLabel originalLabel = new JLabel();
  JLabel replacementLabel = new JLabel();
  JTextField originalTextField = new JTextField();
  JTextField replacementTextField = new JTextField();
  CardLayout editorCardLayout = new CardLayout();
  JPanel htmlTagPanel = new JPanel();
  JTable tagTable = new JTable();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel tagButtonPanel = new JPanel();
  JButton addTagButton = new JButton();
  JButton deleteTagButton = new JButton();
  JPanel filterPanel = new JPanel();
  BorderLayout borderLayout3 = new BorderLayout();
  JCheckBox caseCheckBox = new JCheckBox();
  JButton upButton = new JButton();
  JButton downButton = new JButton();
  JPanel whitespcPanel = new JPanel();
  JCheckBox whiteSpcCheckBox = new JCheckBox();
  BorderLayout borderLayout4 = new BorderLayout();
  JButton addFilterButton = new JButton();
  JButton deleteFilterButton = new JButton();
  JButton upFilterButton = new JButton();
  JButton dnFilterButton = new JButton();
  JPanel filterBtnPanel = new JPanel();
  JButton saveButton = new JButton();
  GridLayout gridLayout1 = new GridLayout();
  private EDPCellData m_data;

  protected static Logger logger = Logger.getLogger("FilterRuleDefiner");

  public FilterRuleDefiner(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the filter rule definer";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Filter Rule Definer",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public FilterRuleDefiner() {
    this(null, "", false);
  }

  private void jbInit() throws Exception {
    mainPanel.setLayout(borderLayout1);
    editPanel.setLayout(editorCardLayout);
    editPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    editPanel.setPreferredSize(new Dimension(270, 200));
    filterList.setBorder(BorderFactory.createEtchedBorder());
    filterList.setPreferredSize(new Dimension(210, 200));
    stringPanel.setPreferredSize(new Dimension(10, 10));
    stringPanel.setLayout(gridLayout1);
    originalLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    originalLabel.setHorizontalAlignment(SwingConstants.CENTER);
    originalLabel.setText("orignal String");
    replacementLabel.setText("replacement String");
    replacementLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    replacementLabel.setHorizontalAlignment(SwingConstants.CENTER);
    originalTextField.setText("");
    replacementTextField.setText("");
    htmlTagPanel.setLayout(borderLayout2);
    tagTable.setBorder(BorderFactory.createRaisedBevelBorder());
    addTagButton.setText("Add");
    addTagButton.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
    deleteTagButton.setText("Delete");
    mainPanel.setMinimumSize(new Dimension(202, 103));
    filterPanel.setLayout(borderLayout3);
    caseCheckBox.setFont(new java.awt.Font("DialogInput", 0, 12));
    caseCheckBox.setText("Ignore Case");
    upButton.setText("Up");
    downButton.setText("Dn");
    whitespcPanel.setPreferredSize(new Dimension(210, 50));
    whitespcPanel.setLayout(borderLayout4);
    whiteSpcCheckBox.setDebugGraphicsOptions(0);
    whiteSpcCheckBox.setText("Filter All White Space");
    addFilterButton.setText("Add");
    addFilterButton.addActionListener(new FilterRuleDefiner_addFilterButton_actionAdapter(this));
    deleteFilterButton.setText("Delete");
    deleteFilterButton.addActionListener(new FilterRuleDefiner_deleteFilterButton_actionAdapter(this));
    upFilterButton.setText("Up");
    upFilterButton.addActionListener(new FilterRuleDefiner_upFilterButton_actionAdapter(this));
    dnFilterButton.setText("Down");
    dnFilterButton.addActionListener(new FilterRuleDefiner_dnFilterButton_actionAdapter(this));
    saveButton.setText("Save");
    saveButton.addActionListener(new FilterRuleDefiner_saveButton_actionAdapter(this));
    filterBtnPanel.setBorder(BorderFactory.createEtchedBorder());
    filterBtnPanel.setMinimumSize(new Dimension(149, 35));
    filterBtnPanel.setPreferredSize(new Dimension(149, 35));
    buttonPanel.setBorder(BorderFactory.createEtchedBorder());
    gridLayout1.setColumns(1);
    gridLayout1.setHgap(0);
    gridLayout1.setRows(5);
    gridLayout1.setVgap(15);
    buttonPanel.add(upFilterButton, null);
    buttonPanel.add(dnFilterButton, null);
    buttonPanel.add(addFilterButton, null);
    buttonPanel.add(deleteFilterButton, null);
    getContentPane().add(mainPanel);
    mainPanel.add(filterList,  BorderLayout.WEST);
    mainPanel.add(filterPanel, BorderLayout.EAST);
    filterPanel.add(filterComboBox, BorderLayout.NORTH);
    filterPanel.add(editPanel, BorderLayout.CENTER);
    filterPanel.add(filterBtnPanel,  BorderLayout.SOUTH);
    editPanel.add(whitespcPanel, WHITE_SPACE_FILTER);
    editPanel.add(htmlTagPanel, TAG_FILTER);
    editPanel.add(stringPanel, STRING_FILTER);

    stringPanel.add(originalLabel, null);
    stringPanel.add(originalTextField, null);
    stringPanel.add(replacementLabel, null);
    stringPanel.add(replacementTextField, null);
    stringPanel.add(caseCheckBox, null);

    filterComboBox.addItemListener(this);
    whitespcPanel.add(whiteSpcCheckBox, BorderLayout.CENTER);
    htmlTagPanel.add(tagTable, BorderLayout.NORTH);
    htmlTagPanel.add(tagButtonPanel, BorderLayout.SOUTH);
    tagButtonPanel.add(deleteTagButton, null);
    tagButtonPanel.add(addTagButton, null);
    tagButtonPanel.add(upButton, null);
    tagButtonPanel.add(downButton, null);

    filterBtnPanel.add(saveButton, null);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
  }

  void saveButton_actionPerformed(ActionEvent e) {
    // add this filter to the list
  }


  void upFilterButton_actionPerformed(ActionEvent e) {
    // move the items up in the list
  }

  void dnFilterButton_actionPerformed(ActionEvent e) {
    // move the items down in the list
  }

  void addFilterButton_actionPerformed(ActionEvent e) {
    // add a new filter
  }

  void deleteFilterButton_actionPerformed(ActionEvent e) {
    // delete the current filter
  }

  /**
   * itemStateChanged
   *
   * @param evt ItemEvent
   */
  public void itemStateChanged(ItemEvent evt) {
    editorCardLayout.show(editPanel, (String)evt.getItem());
  }

  /**
   * setCellData
   *
   * @param data EDPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;

  }

}

class FilterRuleDefiner_saveButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRuleDefiner adaptee;

  FilterRuleDefiner_saveButton_actionAdapter(FilterRuleDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.saveButton_actionPerformed(e);
  }
}

class FilterRuleDefiner_upFilterButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRuleDefiner adaptee;

  FilterRuleDefiner_upFilterButton_actionAdapter(FilterRuleDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.upFilterButton_actionPerformed(e);
  }
}

class FilterRuleDefiner_dnFilterButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRuleDefiner adaptee;

  FilterRuleDefiner_dnFilterButton_actionAdapter(FilterRuleDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.dnFilterButton_actionPerformed(e);
  }
}

class FilterRuleDefiner_addFilterButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRuleDefiner adaptee;

  FilterRuleDefiner_addFilterButton_actionAdapter(FilterRuleDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.addFilterButton_actionPerformed(e);
  }
}

class FilterRuleDefiner_deleteFilterButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRuleDefiner adaptee;

  FilterRuleDefiner_deleteFilterButton_actionAdapter(FilterRuleDefiner adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.deleteFilterButton_actionPerformed(e);
  }
}
