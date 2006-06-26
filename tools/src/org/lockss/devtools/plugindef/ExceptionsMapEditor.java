/*
 * $Id: ExceptionsMapEditor.java,v 1.5 2006-06-26 17:46:56 thib_gc Exp $
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
import java.util.*;
import javax.swing.table.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class ExceptionsMapEditor extends JDialog implements EDPEditor {
  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel buttonPanel = new JPanel();
  JButton addButton = new JButton();
  JButton deleteButton = new JButton();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  JTable exceptionTable = new JTable();

  private EDPCellData m_data;
  ExceptionsTableModel m_model = new ExceptionsTableModel();
  JScrollPane jScrollPane1 = new JScrollPane();

  public ExceptionsMapEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public ExceptionsMapEditor(Frame frame) {
    this(frame, "Exception Mapping", false);
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    addButton.setText("Add");
    addButton.addActionListener(new ExceptionsMapEditor_addButton_actionAdapter(this));
    deleteButton.setText("Delete");
    deleteButton.addActionListener(new ExceptionsMapEditor_deleteButton_actionAdapter(this));
    okButton.setText("OK");
    okButton.addActionListener(new ExceptionsMapEditor_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new ExceptionsMapEditor_cancelButton_actionAdapter(this));
    panel1.setPreferredSize(new Dimension(300, 200));
    exceptionTable.setMaximumSize(new Dimension(0, 0));
    exceptionTable.setMinimumSize(new Dimension(418, 100));
    exceptionTable.setPreferredSize(new Dimension(418, 200));
    exceptionTable.setRowHeight(20);
    getContentPane().add(panel1);
    panel1.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(addButton, null);
    buttonPanel.add(deleteButton, null);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    panel1.add(jScrollPane1, BorderLayout.CENTER);
    jScrollPane1.getViewport().add(exceptionTable, null);
    exceptionTable.setModel(m_model);
  }

  void addButton_actionPerformed(ActionEvent e) {
    m_model.addNewRow();
  }

  void deleteButton_actionPerformed(ActionEvent e) {
    int row = exceptionTable.getSelectedRow();
    m_model.removeRowData(row);

  }

  void okButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  /**
   * setCellData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
    // set up the table
    m_model.setTableData((HashMap) data.getData());

    // set up the combo box
    Collection classes = data.getPlugin().getKnownCacheExceptions();

    TableColumn classColumn = exceptionTable.getColumnModel().getColumn(1);
    JComboBox comboBox = new JComboBox();
    for(Iterator it = classes.iterator(); it.hasNext();) {
      comboBox.addItem(it.next());
    }
    classColumn.setCellEditor(new DefaultCellEditor(comboBox));
  }
}

class ExceptionsTableModel extends AbstractTableModel {
  String columnNames[] = {"Return Code", "Exception Class"};
  Class columnClass[] = {String.class, String.class};
  Vector rowData = new Vector();

  public int getColumnCount() {
    return columnNames.length;
  }

  public int getRowCount() {
    return rowData.size();
  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  public Object getValueAt(int row, int col) {
     return ((Object[])rowData.elementAt(row))[col];
 }

 public Class getColumnClass(int column) {
   return columnClass[column];
 }

  public boolean isCellEditable(int row, int col) {
    return true;
  }

  public void setValueAt(Object value, int row, int col) {
    if(rowData.size() > row && row >=0) {
      Object[] data = (Object[]) rowData.get(row);
      data[col] = value;
    }
    fireTableCellUpdated(row, col);
  }

  public void setTableData(HashMap map) {
    Object[] row_data;
    rowData.removeAllElements();
    for(Iterator it = map.keySet().iterator(); it.hasNext();) {
      row_data = new Object[2];
      String error = (String) it.next();
      String errorClass = (String) map.get(error);
      row_data[0] = new Integer(error);
      row_data[1] = errorClass;
      rowData.add(row_data);
    }
    fireTableDataChanged();
  }

  /**
   * addNewRow
   */
  public void addNewRow() {
    Object[] row_data = new Object[2];
    row_data[0] = "Return Code";
    row_data[1] = "Error Class";

    rowData.add(row_data);
    fireTableDataChanged();
  }

  /**
   * removeRowData
   *
   * @param row int
   */
  public void removeRowData(int row) {
    if(rowData.size() > row && row >=0) {
      rowData.remove(row);
    }
    fireTableDataChanged();
  }

}

class ExceptionsMapEditor_addButton_actionAdapter implements java.awt.event.ActionListener {
  ExceptionsMapEditor adaptee;

  ExceptionsMapEditor_addButton_actionAdapter(ExceptionsMapEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.addButton_actionPerformed(e);
  }
}

class ExceptionsMapEditor_deleteButton_actionAdapter implements java.awt.event.ActionListener {
  ExceptionsMapEditor adaptee;

  ExceptionsMapEditor_deleteButton_actionAdapter(ExceptionsMapEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.deleteButton_actionPerformed(e);
  }
}

class ExceptionsMapEditor_okButton_actionAdapter implements java.awt.event.ActionListener {
  ExceptionsMapEditor adaptee;

  ExceptionsMapEditor_okButton_actionAdapter(ExceptionsMapEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class ExceptionsMapEditor_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  ExceptionsMapEditor adaptee;

  ExceptionsMapEditor_cancelButton_actionAdapter(ExceptionsMapEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}
