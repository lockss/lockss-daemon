package org.lockss.devtools.plugindef;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class FilterRuleDefiner extends JDialog {
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
  CardLayout cardLayout1 = new CardLayout();
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

  public FilterRuleDefiner(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public FilterRuleDefiner() {
    this(null, "", false);
  }

  private void jbInit() throws Exception {
    mainPanel.setLayout(borderLayout1);
    editPanel.setLayout(cardLayout1);
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
    stringPanel.add(originalLabel, null);
    stringPanel.add(originalTextField, null);
    stringPanel.add(replacementLabel, null);
    stringPanel.add(replacementTextField, null);
    stringPanel.add(caseCheckBox, null);
    filterPanel.add(filterBtnPanel,  BorderLayout.SOUTH);
    editPanel.add(whitespcPanel, "whitespcPanel");
    whitespcPanel.add(whiteSpcCheckBox, BorderLayout.CENTER);
    editPanel.add(htmlTagPanel, "jPanel1");
    editPanel.add(stringPanel, "stringPanel");
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

  }


  void upFilterButton_actionPerformed(ActionEvent e) {

  }

  void dnFilterButton_actionPerformed(ActionEvent e) {

  }

  void addFilterButton_actionPerformed(ActionEvent e) {

  }

  void deleteFilterButton_actionPerformed(ActionEvent e) {

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
