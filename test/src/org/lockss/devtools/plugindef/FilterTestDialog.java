package org.lockss.devtools.plugindef;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.lockss.devtools.*;
import org.lockss.plugin.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University Libraries - LOCKSS</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class FilterTestDialog extends JDialog {
  JFileChooser m_fileChooser = new JFileChooser();
  File m_srcFile;
  File m_destFile;
  EditableDefinablePlugin m_plugin;

  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel filterPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JButton filterButton = new JButton();
  JButton cancelButton = new JButton();
  JLabel filterLabel = new JLabel();
  JTextField filterTextField = new JTextField();
  JLabel sourceLabel = new JLabel();
  JTextField sourceTextField = new JTextField();
  JButton srcButton = new JButton();
  JLabel destLabel = new JLabel();
  JTextField destTextField = new JTextField();
  JButton destButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();


  public FilterTestDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public FilterTestDialog(Frame frame, EditableDefinablePlugin plugin) {
    this(frame, "Fiter Runner", false);
    m_plugin = plugin;
  }

  public FilterTestDialog() {
    this(null, "", false);
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    filterPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    filterPanel.setPreferredSize(new Dimension(300, 90));
    filterPanel.setLayout(gridBagLayout1);
    filterButton.setText("Filter");
    filterButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        filterButton_actionPerformed(e);
      }
    });
    buttonPanel.setPreferredSize(new Dimension(300, 40));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    filterLabel.setText("Filter:");
    filterTextField.setPreferredSize(new Dimension(280, 22));
    filterTextField.setToolTipText("Enter name of filter class to use.");
    filterTextField.setText("");
    sourceLabel.setText("Source:");
    sourceTextField.setText("");
    sourceTextField.setToolTipText("Enter name of source file or directory.");
    sourceTextField.setPreferredSize(new Dimension(280, 22));
    srcButton.setBorder(BorderFactory.createLineBorder(Color.black));
    srcButton.setToolTipText("");
    srcButton.setActionCommand("srcFile");
    srcButton.setText("...");
    srcButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        srcButton_actionPerformed(e);
      }
    });
    destLabel.setText("Dest:");
    destTextField.setPreferredSize(new Dimension(280, 22));
    destTextField.setToolTipText("Enter name of destination file or directory.");
    destTextField.setText("");
    destButton.setText("...");
    destButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        destButton_actionPerformed(e);
      }
    });
    destButton.setActionCommand("destFile");
    destButton.setToolTipText("");
    destButton.setBorder(BorderFactory.createLineBorder(Color.black));
    panel1.setPreferredSize(new Dimension(380, 160));
    getContentPane().add(panel1);
    panel1.add(filterPanel, BorderLayout.CENTER);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(filterButton, null);
    buttonPanel.add(cancelButton, null);
    filterPanel.add(filterLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 4, 0, 0), 10, 0));
    filterPanel.add(sourceLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(15, 4, 0, 0), 0, 0));
    filterPanel.add(filterTextField,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
    filterPanel.add(sourceTextField,  new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(12, 0, 0, 0), 0, 0));
    filterPanel.add(srcButton,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(14, 0, 0, 23), 0, 0));
    filterPanel.add(destLabel,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(15, 4, 42, 0), 13, 0));
    filterPanel.add(destTextField,  new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(12, 0, 42, 0), 0, 0));
    filterPanel.add(destButton,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(14, 0, 42, 23), 0, 0));
  }

  void filterButton_actionPerformed(ActionEvent e) {
    String filter_str = filterTextField.getText();

    FilterRule filter = null;
    try {
      filter = FilterRunner.filterRuleFromString(filter_str);
      if (m_srcFile.isDirectory()) {
        FilterRunner.filterDirectory(filter, m_srcFile, m_destFile);
      }
      else {
        FilterRunner.filterSingleFile(filter, m_srcFile, m_destFile);
      }
      setVisible(false);
    }
    catch (Exception ex1) {
      //TODO: Add Error message.
    }


  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  void destButton_actionPerformed(ActionEvent e) {
    int option = m_fileChooser.showSaveDialog(this);
    if(option == m_fileChooser.APPROVE_OPTION ||
       m_fileChooser.getSelectedFile() == null)
      return;
    m_destFile = m_fileChooser.getSelectedFile();

  }

  void srcButton_actionPerformed(ActionEvent e) {

    int option = m_fileChooser.showOpenDialog(this);
    if(option == m_fileChooser.APPROVE_OPTION ||
       m_fileChooser.getSelectedFile() == null)
      return;
    m_srcFile = m_fileChooser.getSelectedFile();

  }
}
