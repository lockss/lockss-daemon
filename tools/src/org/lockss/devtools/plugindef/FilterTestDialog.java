/*
 * $Id: FilterTestDialog.java,v 1.7 2006-09-06 16:38:41 thib_gc Exp $
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

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.lockss.devtools.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Stanford University Libraries - LOCKSS</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class FilterTestDialog extends JDialog {
  private static final String FILTER_NAME_KEY    = "filtername";
  private static final String FILTER_SOURCE_KEY  = "filtersource";
  private static final String FILTER_DEST_KEY    = "filterdest";

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

  protected static Logger logger = Logger.getLogger("FilterTestDialog");

  public FilterTestDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the filter test dialog";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Filter Test Dialog",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public FilterTestDialog(Frame frame, EditableDefinablePlugin plugin) {
    this(frame, "Fiter Runner", false);
    m_plugin = plugin;
    filterTextField.setText(m_plugin.getPluginState().getFilterFieldValue(FILTER_NAME_KEY));
    sourceTextField.setText(m_plugin.getPluginState().getFilterFieldValue(FILTER_SOURCE_KEY));
    destTextField.setText(m_plugin.getPluginState().getFilterFieldValue(FILTER_DEST_KEY));

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
    sourceLabel.setText("Source:");
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
    String filter_str         = filterTextField.getText();
    String filter_source_str  = sourceTextField.getText();
    String filter_dest_str    = destTextField.getText();

    FilterRule filter = null;
    try {
      filter = FilterRunner.filterRuleFromString(filter_str);
      if (m_srcFile.isDirectory()) {
        FilterRunner.filterDirectory(filter, m_srcFile, m_destFile);
      }
      else {
        FilterRunner.filterSingleFile(filter, m_srcFile, m_destFile);
      }
      //saves the text field input in order to display
      //the next time the filters are openned.  (Not tested)
      m_plugin.setPluginState(PersistentPluginState.FILTERS,
			      FILTER_NAME_KEY,
			      filter_str);
      m_plugin.setPluginState(PersistentPluginState.FILTERS,
			      FILTER_SOURCE_KEY,
			      filter_source_str);
      m_plugin.setPluginState(PersistentPluginState.FILTERS,
			      FILTER_DEST_KEY,
			      filter_dest_str);
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
    if(option == JFileChooser.APPROVE_OPTION ||
       m_fileChooser.getSelectedFile() == null)
      return;
    m_destFile = m_fileChooser.getSelectedFile();

  }

  void srcButton_actionPerformed(ActionEvent e) {

    int option = m_fileChooser.showOpenDialog(this);
    if(option == JFileChooser.APPROVE_OPTION ||
       m_fileChooser.getSelectedFile() == null)
      return;
    m_srcFile = m_fileChooser.getSelectedFile();

  }
}
