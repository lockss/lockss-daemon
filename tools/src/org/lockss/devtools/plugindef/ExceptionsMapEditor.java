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
    getContentPane().add(panel1);
    panel1.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(addButton, null);
    buttonPanel.add(deleteButton, null);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    panel1.add(exceptionTable, BorderLayout.CENTER);
    exceptionTable.setModel(m_model);
  }

  void addButton_actionPerformed(ActionEvent e) {

  }

  void deleteButton_actionPerformed(ActionEvent e) {

  }

  void okButton_actionPerformed(ActionEvent e) {

  }

  void cancelButton_actionPerformed(ActionEvent e) {

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
  Class columnClass[] = {Integer.class, String.class};
  private Collection classNames;
  private Object[][] data;

  public ExceptionsTableModel() {
    data = new Object[0][2];
  }

  public int getColumnCount() {
    return columnNames.length;
  }

  public int getRowCount() {
    return classNames == null ? 0 : classNames.size();
  }

  public String getColumnName(int column) {
    return columnNames[column];
  }

  public Object getValueAt(int row, int col) {
    return data[row][col];
  }

  public void setValueAt(Object value, int row, int col) {
    data[row][col] = value;
  }

  public Class getColumnClass(int column) {
    return columnClass[column];
  }

  public boolean isCellEditable(int row, int col) {
    return true;
  }

  public void setTableData(HashMap map) {
    int num_rows = map.size();
    data = new Object[num_rows][columnNames.length];
    int row = 0;
    for(Iterator it = map.keySet().iterator(); it.hasNext();) {
      String error = (String) it.next();
      String errorClass = (String) map.get(error);
      data[row][0] = new Integer(error);
      data[row][1] = errorClass;
      ++row;
    }
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
