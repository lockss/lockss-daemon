package org.lockss.devtools.plugindef;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import org.lockss.plugin.definable.*;
import org.lockss.plugin.definable.DefinablePlugin.*;

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
  HashMap m_filters;
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JTable filtersTable = new JTable();
  JButton addButton = new JButton();
  JButton cancelButton = new JButton();
  JButton deleteButton = new JButton();

  public FilterRulesEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public FilterRulesEditor() {
    this(null, "", false);
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    okButton.setText("OK");
    okButton.addActionListener(new FilterRulesEditor_okButton_actionAdapter(this));
    filtersTable.setRowSelectionAllowed(true);
    filtersTable.setBorder(BorderFactory.createEtchedBorder());
    filtersTable.setCellSelectionEnabled(true);
    filtersTable.setColumnSelectionAllowed(false);
    filtersTable.setModel(new FilterRulesTableModel());
    addButton.setToolTipText("Add a new filter for mime type.");
    addButton.setText("Add");
    addButton.addActionListener(new FilterRulesEditor_addButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new FilterRulesEditor_cancelButton_actionAdapter(this));
    deleteButton.setToolTipText("Delete the currently selected item.");
    deleteButton.setText("Delete");
    deleteButton.addActionListener(new FilterRulesEditor_deleteButton_actionAdapter(this));
    buttonPanel.add(addButton, null);
    buttonPanel.add(deleteButton, null);
    getContentPane().add(panel1);
    panel1.add(buttonPanel,  BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    panel1.add(filtersTable, BorderLayout.CENTER);
    buttonPanel.add(cancelButton, null);

  }

  /**
   * setEDPData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
    m_filters = m_data.getPlugin().getAuFilters();
    FilterRulesTableModel model = (FilterRulesTableModel) filtersTable.getModel();

    model.updateTableData();
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
        //TODO: Add alert.
      }
    }
    setVisible(false);
  }

  void addButton_actionPerformed(ActionEvent e) {
    FilterRulesTableModel model = (FilterRulesTableModel) filtersTable.getModel();
    Object[] row_data = new Object[2];
    if(m_filters.size() < 1) { // add a new html filter
      row_data[0] = "text/html";
      row_data[1] = "Replace with Filter class name.";
    }
    else {
      row_data[0] = "Enter Mime type";
      row_data[1] = "Replace with Filter class Name.";
    }
    model.addRowData(row_data);
    model.fireTableDataChanged();
  }
  void deleteButton_actionPerformed(ActionEvent e) {
    int row = filtersTable.getSelectedRow();
    FilterRulesTableModel model = (FilterRulesTableModel) filtersTable.getModel();
    model.removeRowData(row);

  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }


  class FilterRulesTableModel extends AbstractTableModel {
    String[] columnNames = {"Mime Type", "Filter Class"};
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

    public void addRowData(Object[] data) {
      rowData.add(data);
    }

    public void updateTableData() {
      // we need to get the stored filters
      Object[] row_data;

      for(Iterator it = m_filters.keySet().iterator(); it.hasNext();) {
        row_data = new Object[2];
        row_data[0] = it.next();
        row_data[1] = m_filters.get(row_data[0]);
        addRowData(row_data);
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
