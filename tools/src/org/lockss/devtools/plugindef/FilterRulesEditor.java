/*
 * $Id: FilterRulesEditor.java,v 1.8 2006-09-06 16:38:41 thib_gc Exp $
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
import javax.swing.table.*;

import org.lockss.plugin.definable.DefinablePlugin.*;
import org.lockss.util.Logger;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class FilterRulesEditor extends JDialog implements EDPEditor {
  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  EDPCellData m_data;
  FilterRulesTableModel m_model = new FilterRulesTableModel();
  HashMap m_filters;
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JTable filtersTable = new JTable();
  JButton addButton = new JButton();
  JButton cancelButton = new JButton();
  JButton deleteButton = new JButton();
  JButton upButton = new JButton();
  JButton dnButton = new JButton();
  JScrollPane jScrollPane1 = new JScrollPane();

  protected static Logger logger = Logger.getLogger("FilterRulesEditor");

  public FilterRulesEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the filter rules editor";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Filter Rules Editor",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public FilterRulesEditor(Frame frame) {
    this(frame, "Assigned Filter Rules", false);
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    okButton.setText("OK");
    okButton.addActionListener(new FilterRulesEditor_okButton_actionAdapter(this));
    filtersTable.setRowSelectionAllowed(true);
    filtersTable.setPreferredSize(new Dimension(418, 200));
    filtersTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    filtersTable.setCellSelectionEnabled(true);
    filtersTable.setColumnSelectionAllowed(false);
    filtersTable.setModel(m_model);
    addButton.setToolTipText("Add a new filter for mime type.");
    addButton.setText("Add");
    addButton.addActionListener(new FilterRulesEditor_addButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new FilterRulesEditor_cancelButton_actionAdapter(this));
    deleteButton.setToolTipText("Delete the currently selected item.");
    deleteButton.setText("Delete");
    deleteButton.addActionListener(new FilterRulesEditor_deleteButton_actionAdapter(this));
    upButton.setText("Up");
    upButton.addActionListener(new FilterRulesEditor_upButton_actionAdapter(this));
    dnButton.setText("Down");
    dnButton.addActionListener(new FilterRulesEditor_dnButton_actionAdapter(this));
    panel1.setPreferredSize(new Dimension(418, 200));
    jScrollPane1.setMinimumSize(new Dimension(200, 80));
    jScrollPane1.setOpaque(true);
    buttonPanel.add(dnButton, null);
    buttonPanel.add(upButton, null);
    buttonPanel.add(addButton, null);
    buttonPanel.add(deleteButton, null);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    getContentPane().add(panel1);
    panel1.add(buttonPanel,  BorderLayout.SOUTH);
    panel1.add(jScrollPane1,  BorderLayout.CENTER);
    jScrollPane1.getViewport().add(filtersTable, null);

  }

  /**
   * setEDPData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
    m_filters = m_data.getPlugin().getAuFilters();
    m_model.updateTableData();
  }

  void okButton_actionPerformed(ActionEvent e) {
    int num_rows = filtersTable.getRowCount();
    EditableDefinablePlugin edp = m_data.getPlugin();
    for(int row =0; row < num_rows; row++) {
      String mime_type = (String)filtersTable.getValueAt(row, 0);
      String filter = (String)filtersTable.getValueAt(row, 1);

      try {
        edp.setAuFilter(mime_type, filter);
      }
      catch (InvalidDefinitionException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                                      "Save Filter Warning",
                                      JOptionPane.WARNING_MESSAGE);
      }
    }
    setVisible(false);
  }

  void addButton_actionPerformed(ActionEvent e) {
    m_model.addNewRow();
  }

  void deleteButton_actionPerformed(ActionEvent e) {
    int row = filtersTable.getSelectedRow();
    m_model.removeRowData(row);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  void dnButton_actionPerformed(ActionEvent e) {
    int row = filtersTable.getSelectedRow();
    m_model.moveData(row, row+1);
  }

  void upButton_actionPerformed(ActionEvent e) {
    int row = filtersTable.getSelectedRow();
    m_model.moveData(row, row-1);
  }

  class FilterRulesTableModel extends AbstractTableModel {
    String[] columnNames = {"Mime Type", "Filter Class"};
    Class[] columnClass = {String.class, String.class};
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
      Object obj = ( (Object[]) rowData.elementAt(row))[col];
      System.out.println("Got object: " + obj);
      return ( (Object[]) rowData.elementAt(row))[col];
    }

    public Class getColumnClass(int col) {
      return getValueAt(0,col).getClass();
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

    public void updateTableData() {
      // we need to get the stored filters
      Object[] row_data;
      rowData.removeAllElements();
      for(Iterator it = m_filters.keySet().iterator(); it.hasNext();) {
        row_data = new Object[2];
        row_data[0] = it.next();
        row_data[1] = m_filters.get(row_data[0]);
        rowData.add(row_data);
      }
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

    private void addNewRow() {
      Object[] row_data = new Object[2];
      if (rowData.size() < 1) { // add a new html filter
        row_data[0] = "text/html";
        row_data[1] = "Replace with Filter class name.";
      }
      else {
        row_data[0] = "Enter Mime type";
        row_data[1] = "Replace with Filter class Name.";
      }
      System.out.println("Adding new row.");
      rowData.add(row_data);
      fireTableDataChanged();
    }

    /**
     * moveData
     *
     * @param curRow Old row for the data
     * @param newRow New row for the data
     */
    private void moveData(int curRow, int newRow) {
      int lastRow = rowData.size() -1;
      if(curRow >=0 && curRow <= lastRow && newRow >=0 &&
        newRow <= lastRow) {
        Object[] curData = (Object[])rowData.elementAt(curRow);
        rowData.removeElementAt(curRow);
        rowData.insertElementAt(curData, newRow);
        fireTableDataChanged();
      }
    }
  }
}



class FilterRulesEditor_okButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRulesEditor adaptee;

  FilterRulesEditor_okButton_actionAdapter(FilterRulesEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class FilterRulesEditor_addButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRulesEditor adaptee;

  FilterRulesEditor_addButton_actionAdapter(FilterRulesEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.addButton_actionPerformed(e);
  }
}

class FilterRulesEditor_deleteButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRulesEditor adaptee;

  FilterRulesEditor_deleteButton_actionAdapter(FilterRulesEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.deleteButton_actionPerformed(e);
  }
}

class FilterRulesEditor_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRulesEditor adaptee;

  FilterRulesEditor_cancelButton_actionAdapter(FilterRulesEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}

class FilterRulesEditor_dnButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRulesEditor adaptee;

  FilterRulesEditor_dnButton_actionAdapter(FilterRulesEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.dnButton_actionPerformed(e);
  }
}

class FilterRulesEditor_upButton_actionAdapter implements java.awt.event.ActionListener {
  FilterRulesEditor adaptee;

  FilterRulesEditor_upButton_actionAdapter(FilterRulesEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.upButton_actionPerformed(e);
  }
}
