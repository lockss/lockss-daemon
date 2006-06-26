/*
 * $Id: TimeEditor.java,v 1.4 2006-06-26 17:46:56 thib_gc Exp $
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

import org.lockss.util.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class TimeEditor extends JDialog implements EDPEditor {
  JPanel mainPanel = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  private EDPCellData m_cellData;
  JPanel timePanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  JLabel weekLabel = new JLabel();
  JLabel dayLabel = new JLabel();
  JLabel hourLabel = new JLabel();
  JLabel minLabel = new JLabel();
  JLabel secLabel = new JLabel();
  JLabel millisLabel = new JLabel();
  JTextField weekTextField = new JTextField();
  JTextField dayTextField = new JTextField();
  JTextField hourTextField = new JTextField();
  JTextField minTextField = new JTextField();
  JTextField secTextField = new JTextField();
  JTextField millisTextField = new JTextField();
  TitledBorder titledBorder1;
  GridBagLayout gridBagLayout1 = new GridBagLayout();

  public TimeEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public TimeEditor(Frame frame) {
    this(frame, "Time Editor", false);
  }

  private void jbInit() throws Exception {
    titledBorder1 = new TitledBorder("");
    mainPanel.setLayout(borderLayout1);
    okButton.setText("OK");
    okButton.addActionListener(new TimeEditor_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new TimeEditor_cancelButton_actionAdapter(this));
    timePanel.setLayout(gridBagLayout1);
    weekLabel.setText("Week");
    dayLabel.setText("Day");
    hourLabel.setText("Hour");
    minLabel.setText("Min");
    secLabel.setText("Sec");
    millisLabel.setText("Ms");
    weekTextField.setPreferredSize(new Dimension(64, 20));
    weekTextField.setText("0");
    dayTextField.setText("0");
    dayTextField.setPreferredSize(new Dimension(64, 20));
    hourTextField.setText("0");
    hourTextField.setPreferredSize(new Dimension(64, 20));
    minTextField.setText("0");
    minTextField.setPreferredSize(new Dimension(64, 20));
    secTextField.setText("0");
    secTextField.setPreferredSize(new Dimension(64, 20));
    millisTextField.setText("0");
    millisTextField.setPreferredSize(new Dimension(60, 20));
    timePanel.setBorder(BorderFactory.createEtchedBorder());
    timePanel.setMaximumSize(new Dimension(2147483647, 2147483647));
    timePanel.setPreferredSize(new Dimension(120, 180));
    mainPanel.setMinimumSize(new Dimension(120, 230));
    mainPanel.setOpaque(true);
    mainPanel.setPreferredSize(new Dimension(120, 230));
    mainPanel.setVerifyInputWhenFocusTarget(true);
    getContentPane().add(mainPanel);
    mainPanel.add(timePanel, BorderLayout.CENTER);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    timePanel.add(weekTextField,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
    timePanel.add(dayTextField,  new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
    timePanel.add(hourTextField,  new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
    timePanel.add(minTextField,  new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
    timePanel.add(secTextField,  new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
    timePanel.add(millisTextField,  new GridBagConstraints(0, 5, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 12, 0), 4, 0));

    timePanel.add(weekLabel,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 15, 0, 36), 0, 0));
    timePanel.add(dayLabel,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 15, 0, 36), 9, 0));
    timePanel.add(hourLabel,  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 15, 0, 36), 3, 0));
    timePanel.add(minLabel,   new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 15, 0, 36), 0, 0));
    timePanel.add(secLabel,  new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 15, 0, 36), 6, 0));
    timePanel.add(millisLabel,  new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 15, 12, 36), 15, 0));
    mainPanel.add(buttonPanel,  BorderLayout.SOUTH);
  }

  /**
   * setCellData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_cellData = data;
    long millis = ( (Long) data.getData()).longValue();
    long time_remaining = millis;
    long cur_interval = 0;
    cur_interval = time_remaining / Constants.WEEK;
    if (cur_interval > 0) {
      weekTextField.setText(String.valueOf(cur_interval));
      time_remaining -= cur_interval * Constants.WEEK;
    }
    else {
      weekTextField.setText("0");
    }

    cur_interval = time_remaining / Constants.DAY;
    if (cur_interval > 0) {
      dayTextField.setText(String.valueOf(cur_interval));
      time_remaining -= cur_interval * Constants.DAY;
    }
    else {
      dayTextField.setText("0");
    }

    cur_interval = time_remaining / Constants.HOUR;
    if (cur_interval > 0) {
      hourTextField.setText(String.valueOf(cur_interval));
      time_remaining -= cur_interval * Constants.HOUR;
    }
    else {
      hourTextField.setText("0");
    }

    cur_interval = time_remaining / Constants.MINUTE;
    if (cur_interval > 0) {
      minTextField.setText(String.valueOf(cur_interval));
      time_remaining -= cur_interval * Constants.MINUTE;
    }
    else {
      minTextField.setText("0");
    }

    cur_interval = time_remaining / Constants.SECOND;
    if (cur_interval > 0) {
      secTextField.setText(String.valueOf(cur_interval));
      time_remaining -= cur_interval * Constants.SECOND;
    }
    else {
      secTextField.setText("0");
    }

    millisTextField.setText(String.valueOf(time_remaining));
  }

  void storeCellData() {
    long cur_time = 0;
    String time_str = weekTextField.getText();
    if(!time_str.equals(""))
      cur_time += Long.parseLong(time_str) * Constants.WEEK;
    time_str = dayTextField.getText();
    if(!time_str.equals(""))
      cur_time += Long.parseLong(time_str)* Constants.DAY;
    time_str = hourTextField.getText();
    if(!time_str.equals(""))
      cur_time += Long.parseLong(time_str)* Constants.HOUR;
    time_str = minTextField.getText();
    if(!time_str.equals(""))
      cur_time += Long.parseLong(time_str)* Constants.MINUTE;
    time_str = secTextField.getText();
    if(!time_str.equals(""))
      cur_time += Long.parseLong(time_str)* Constants.SECOND;
    time_str = millisTextField.getText();
    if(!time_str.equals(""))
      cur_time += Long.parseLong(time_str);
    m_cellData.updateStringData(String.valueOf(cur_time));
  }

  public static long strToMilliseconds(String str) {
    long time = -1;
    long cur_interval = 0;
    StringTokenizer tokenizer = new StringTokenizer(str,"SMHDW",true);
    while(tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      if(token.equals("S")) {
        time += cur_interval * Constants.SECOND;
      }
      else if (token.equals("M")) {
        time += cur_interval * Constants.MINUTE;
      }
      else if (token.equals("H")) {
        time += cur_interval * Constants.HOUR;
      }
      else if (token.equals("D")) {
        time += cur_interval * Constants.DAY;
      }
      else if (token.equals("W")) {
        time += cur_interval * Constants.WEEK;
      }
      else {
        cur_interval = Long.parseLong(token);
      }
    }
    return time;
  }

  public static String millisToString(long millis) {
    StringBuffer time_str = new StringBuffer();
    long time_remaining = millis;
    long cur_interval = 0;
    cur_interval = time_remaining/Constants.WEEK;
    if(cur_interval > 0) {
      time_str.append(cur_interval);
      time_str.append(" W");
      time_remaining -= cur_interval * Constants.WEEK;
    }

    cur_interval = time_remaining/Constants.DAY;
    if(cur_interval > 0) {
      time_str.append(cur_interval);
      time_str.append(" D");
      time_remaining -= cur_interval * Constants.DAY;
    }

    cur_interval = time_remaining/Constants.HOUR;
    if(cur_interval > 0) {
      time_str.append(cur_interval);
      time_str.append(" H");
      time_remaining -= cur_interval * Constants.HOUR;
    }

    cur_interval = time_remaining/Constants.MINUTE;
    if(cur_interval > 0) {
      time_str.append(cur_interval);
      time_str.append(" M");
      time_remaining -= cur_interval * Constants.MINUTE;
    }

    cur_interval = time_remaining/Constants.SECOND;
    if(cur_interval > 0) {
      time_str.append(cur_interval);
      time_str.append(" S");
      time_remaining -= cur_interval * Constants.SECOND;
    }
    if(time_remaining > 0) {
      time_str.append(time_remaining);
    }

    return time_str.toString();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    //ignore dialog fields
    setVisible(false);
  }

  void okButton_actionPerformed(ActionEvent e) {
    storeCellData();
    setVisible(false);
  }


}

class TimeEditor_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  TimeEditor adaptee;

  TimeEditor_cancelButton_actionAdapter(TimeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}

class TimeEditor_okButton_actionAdapter implements java.awt.event.ActionListener {
  TimeEditor adaptee;

  TimeEditor_okButton_actionAdapter(TimeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}
