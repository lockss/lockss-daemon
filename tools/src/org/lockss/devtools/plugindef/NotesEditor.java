/*
 * $Id: NotesEditor.java,v 1.2 2006-06-26 17:46:56 thib_gc Exp $
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
import javax.swing.event.*;

import org.lockss.daemon.*;
import javax.swing.text.*;
import org.lockss.util.*;
import org.lockss.plugin.definable.*;


public class NotesEditor extends JDialog
  implements EDPEditor {
  public NotesEditor(Frame owner, String title) {
    super(owner, title, false);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  private EDPCellData m_data;
  JPanel buttonPanel = new JPanel();
  JPanel textPanel = new JPanel();
  JTextArea notesField = new JTextArea();
  GridLayout gridLayout1 = new GridLayout();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();

  public void setCellData(EDPCellData data) {
    this.m_data = data;
    // notesField.setText(data.getPlugin().getPluginNotes());
    notesField.setText((String)data.getData());
  }

  private void jbInit() throws Exception {
    notesField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    notesField.setToolTipText("");
    notesField.setText("");
    notesField.setLineWrap(true);
    notesField.setWrapStyleWord(true);
    notesField.setMinimumSize(new Dimension(300,200));
    notesField.setPreferredSize(new Dimension(300,200));
    textPanel.setLayout(gridLayout1);
    textPanel.setInputVerifier(null);
    okButton.setText("OK");
    okButton.addActionListener(new NotesEditor_okButton_actionAdapter(this));
    cancelButton.setSelectedIcon(null);
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new
				   TextFieldEditor_cancelButton_actionAdapter(this));
    this.getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    this.getContentPane().add(textPanel, java.awt.BorderLayout.CENTER);
    textPanel.add(notesField);
  }

  public void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
    dispose();
  }

  public void okButton_actionPerformed(ActionEvent e) {
    // m_data.getPlugin().setPluginNotes(notesField.getText());
    m_data.updateStringData(notesField.getText());
    setVisible(false);
    dispose();
  }
}


class NotesEditor_okButton_actionAdapter implements ActionListener {
  private NotesEditor adaptee;
  NotesEditor_okButton_actionAdapter(NotesEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}


class TextFieldEditor_cancelButton_actionAdapter implements ActionListener {
  private NotesEditor adaptee;
  TextFieldEditor_cancelButton_actionAdapter(NotesEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}
