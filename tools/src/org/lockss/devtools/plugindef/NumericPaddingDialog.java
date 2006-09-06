/*
 * $Id: NumericPaddingDialog.java,v 1.4 2006-09-06 16:38:41 thib_gc Exp $
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
import javax.swing.border.*;

import org.lockss.util.Logger;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: SUL-LOCKSS</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class NumericPaddingDialog extends JDialog {
  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel padPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JTextField widthTextField = new JTextField();
  JRadioButton zeroRadioButton = new JRadioButton();
  JRadioButton noneRadioButton = new JRadioButton();
  JLabel fieldWidthLabel = new JLabel();
  JRadioButton spacesRadioButton = new JRadioButton();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JPanel paddingPanel = new JPanel();
  ButtonGroup buttonGroup1 = new ButtonGroup();
  TitledBorder titledBorder1;
  TitledBorder titledBorder2;

  protected static Logger logger = Logger.getLogger("NumericPaddingDialog");

  public NumericPaddingDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the numeric padding dialog";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Numeric Padding Dialog",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public NumericPaddingDialog() {
    this(null, "Insert Numberic Padding", true);
  }

  private void jbInit() throws Exception {
    titledBorder2 = new TitledBorder("");
    panel1.setLayout(borderLayout1);
    okButton.setToolTipText("Set padding for numberic field.");
    okButton.setSelected(true);
    okButton.setText("Set Padding");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    widthTextField.setMinimumSize(new Dimension(10, 19));
    widthTextField.setPreferredSize(new Dimension(10, 19));
    widthTextField.setRequestFocusEnabled(true);
    widthTextField.setToolTipText("");
    widthTextField.setText("0");
    widthTextField.setSelectionStart(1);
    zeroRadioButton.setToolTipText("Insert number with leading 0, if less than field width.(3 => 03).");
    zeroRadioButton.setSelected(false);
    zeroRadioButton.setText("pad with zeros");
    noneRadioButton.setToolTipText("Insert number as-is (3 => 3).");
    noneRadioButton.setSelected(true);
    noneRadioButton.setText("do not pad");
    fieldWidthLabel.setToolTipText("minimum number of digits");
    fieldWidthLabel.setText("field width:");
    spacesRadioButton.setToolTipText("Insert number with leading space, if less than field width.(3 => " +
        " 3).");
    spacesRadioButton.setText("pad with spaces");
    paddingPanel.setLayout(gridBagLayout2);
    paddingPanel.setBorder(titledBorder2);
    paddingPanel.setMinimumSize(new Dimension(300, 70));
    paddingPanel.setPreferredSize(new Dimension(380, 150));
    titledBorder2.setTitleFont(new java.awt.Font("DialogInput", 0, 12));
    titledBorder2.setTitle("Numeric Padding");
    getContentPane().add(panel1);
    panel1.add(padPanel, BorderLayout.CENTER);
    padPanel.add(paddingPanel, null);
    paddingPanel.add(noneRadioButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 4, 0, 0), 33, 0));
    paddingPanel.add(zeroRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 4, 0, 0), 8, 0));
    paddingPanel.add(spacesRadioButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 4, 2, 0), 2, 0));
    paddingPanel.add(widthTextField, new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
                                            , GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(0, 12, 0, 52), 30, 9));
    paddingPanel.add(fieldWidthLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.WEST,
                                            GridBagConstraints.NONE,
                                            new Insets(6, 57, 0, 0), 0, 0));
    panel1.add(buttonPanel,  BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonGroup1.add(noneRadioButton);
    buttonGroup1.add(zeroRadioButton);
    buttonGroup1.add(spacesRadioButton);
  }

  public boolean usePadding() {
    return !noneRadioButton.isSelected();
  }

  public boolean useZero() {
    return zeroRadioButton.isSelected();
  }

  public boolean useSpace() {
    return spacesRadioButton.isSelected();
  }

  public int getPaddingSize() {
    int width = 0;
    if (usePadding()) {
      String width_str = widthTextField.getText();
      if ( (width_str != null) && (width_str.length() > 0)) {
        width = Integer.parseInt(width_str);
      }
    }
    return width;
  }

  void okButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }
}
