/*
 * $Id: MimeTypeEditor.java,v 1.4 2009-10-20 22:38:16 tlipkis Exp $
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

import org.apache.commons.lang.WordUtils;
import org.lockss.devtools.plugindef.EditableDefinablePlugin.DynamicallyLoadedComponentException;
import org.lockss.daemon.*;
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

public class MimeTypeEditor extends JDialog implements EDPEditor {
  
  public interface MimeTypeEditorBuilder {
    
    String getValueName();
    
    String getValueClassName();
    
    Map getMap(EditableDefinablePlugin plugin);
    
    void put(EditableDefinablePlugin plugin,
             String mimeType,
             String mimeTypeValue)
        throws DynamicallyLoadedComponentException, PluginException.InvalidDefinition;
    
    void checkValue(EditableDefinablePlugin plugin,
		    String mimeType,
		    String mimeTypeValue)
        throws DynamicallyLoadedComponentException, PluginException.InvalidDefinition;
    
    void clear(EditableDefinablePlugin plugin);
  }
  
  public static class FilterRuleEditorBuilder implements MimeTypeEditorBuilder {
    
    public String getValueName() {
      return "filter rule";
    }
    
    public String getValueClassName() {
      return "FilterRule";
    }
    
    public Map getMap(EditableDefinablePlugin plugin) {
      return Collections.unmodifiableMap(plugin.getHashFilterRules());
    }
    
    public void put(EditableDefinablePlugin plugin,
                    String mimeType,
                    String mimeTypeValue)
        throws DynamicallyLoadedComponentException, PluginException.InvalidDefinition {
      plugin.setHashFilterRule(mimeType, mimeTypeValue);
    }
    
    public void checkValue(EditableDefinablePlugin plugin,
			   String mimeType,
			   String mimeTypeValue)
        throws DynamicallyLoadedComponentException, 
	       PluginException.InvalidDefinition {
      plugin.checkHashFilterRule(mimeType, mimeTypeValue);
    }
    
    public void clear(EditableDefinablePlugin plugin) {
      plugin.clearHashFilterRules();
    }
  }
  
  public static class HashFilterFactoryEditorBuilder
    implements MimeTypeEditorBuilder {
    
    public String getValueName() {
      return "hash filter factory";
    }
    
    public String getValueClassName() {
      return "HashFilterFactory";
    }
    
    public Map getMap(EditableDefinablePlugin plugin) {
      return Collections.unmodifiableMap(plugin.getHashFilterFactories());
    }
    
    public void put(EditableDefinablePlugin plugin,
                    String mimeType,
                    String mimeTypeValue)
        throws DynamicallyLoadedComponentException, PluginException.InvalidDefinition {
      plugin.setHashFilterFactory(mimeType, mimeTypeValue);
    }

    public void checkValue(EditableDefinablePlugin plugin,
			   String mimeType,
			   String mimeTypeValue)
        throws DynamicallyLoadedComponentException, 
	       PluginException.InvalidDefinition {
      plugin.checkHashFilterFactory(mimeType, mimeTypeValue);
    }
    
    public void clear(EditableDefinablePlugin plugin) {
      plugin.clearHashFilterFactories();
    }
  }
  
  public static class CrawlFilterFactoryEditorBuilder
    implements MimeTypeEditorBuilder {
    
    public String getValueName() {
      return "crawl filter factory";
    }
    
    public String getValueClassName() {
      return "CrawlFilterFactory";
    }
    
    public Map getMap(EditableDefinablePlugin plugin) {
      return Collections.unmodifiableMap(plugin.getCrawlFilterFactories());
    }
    
    public void put(EditableDefinablePlugin plugin,
                    String mimeType,
                    String mimeTypeValue)
        throws DynamicallyLoadedComponentException, PluginException.InvalidDefinition {
      plugin.setCrawlFilterFactory(mimeType, mimeTypeValue);
    }

    public void checkValue(EditableDefinablePlugin plugin,
			   String mimeType,
			   String mimeTypeValue)
        throws DynamicallyLoadedComponentException, 
	       PluginException.InvalidDefinition {
      plugin.checkCrawlFilterFactory(mimeType, mimeTypeValue);
    }
    
    public void clear(EditableDefinablePlugin plugin) {
      plugin.clearCrawlFilterFactories();
    }
  }
  
  protected MimeTypeEditorBuilder mimeTypeEditorBuilder;
  
  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  EDPCellData m_data;
  MimeTypeTableModel m_model;
  Map m_filters;
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

  public MimeTypeEditor(MimeTypeEditorBuilder mimeTypeEditorBuilder,
                        Frame frame,
                        String title,
                        boolean modal) {
    super(frame, title, modal);
    this.mimeTypeEditorBuilder = mimeTypeEditorBuilder;
    this.m_model = new MimeTypeTableModel();
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the " + mimeTypeEditorBuilder.getValueName() + " editor";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    WordUtils.capitalize(mimeTypeEditorBuilder.getValueName()) + " Editor",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public MimeTypeEditor(MimeTypeEditorBuilder mimeTypeEditorBuilder,
                        Frame frame) {
    this(mimeTypeEditorBuilder,
         frame,
         "Assigned " + WordUtils.capitalize(mimeTypeEditorBuilder.getValueName()),
         false);
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    okButton.setText("OK");
    okButton.addActionListener(new MimeTypeEditor_okButton_actionAdapter(this));
    filtersTable.setRowSelectionAllowed(true);
    filtersTable.setPreferredSize(new Dimension(418, 200));
    filtersTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    filtersTable.setCellSelectionEnabled(true);
    filtersTable.setColumnSelectionAllowed(false);
    filtersTable.setModel(m_model);
    addButton.setToolTipText("Add a new " + mimeTypeEditorBuilder.getValueName() + " for a MIME type");
    addButton.setText("Add");
    addButton.addActionListener(new MimeTypeEditor_addButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new MimeTypeEditor_cancelButton_actionAdapter(this));
    deleteButton.setToolTipText("Delete the currently selected item.");
    deleteButton.setText("Delete");
    deleteButton.addActionListener(new MimeTypeEditor_deleteButton_actionAdapter(this));
    upButton.setText("Up");
    upButton.addActionListener(new MimeTypeEditor_upButton_actionAdapter(this));
    dnButton.setText("Down");
    dnButton.addActionListener(new MimeTypeEditor_dnButton_actionAdapter(this));
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
    m_filters = mimeTypeEditorBuilder.getMap(m_data.getPlugin());
    m_model.updateTableData();
  }

  void okButton_actionPerformed(ActionEvent e) {
    int num_rows = filtersTable.getRowCount();
    EditableDefinablePlugin edp = m_data.getPlugin();
    for (int row = 0 ; row < num_rows ; row++) {
      String mimeType = (String)filtersTable.getValueAt(row, 0);
      String mimeTypeValue = (String)filtersTable.getValueAt(row, 1);

      try {
        mimeTypeEditorBuilder.checkValue(edp, mimeType, mimeTypeValue);
      }
      catch (DynamicallyLoadedComponentException dlce) {
        String logMessage = "Failed to set the " + mimeTypeEditorBuilder.getValueName()
                            + " for MIME type " + mimeType + " to " + mimeTypeValue;
        logger.error(logMessage, dlce);
        if (!EDPInspectorTableModel.handleDynamicallyLoadedComponentException(this, dlce)) {
	  return;
	} else {
          logger.debug("User override; allow " + mimeTypeValue);
	}
      }
    }
    mimeTypeEditorBuilder.clear(edp);
    for (int row = 0 ; row < num_rows ; row++) {
      String mimeType = (String)filtersTable.getValueAt(row, 0);
      String mimeTypeValue = (String)filtersTable.getValueAt(row, 1);

      try {
        mimeTypeEditorBuilder.put(edp, mimeType, mimeTypeValue);
      }
      catch (DynamicallyLoadedComponentException dlce) {
        String logMessage = "Internal error; MIME type " + mimeType
	  + " not set to " + mimeTypeValue;
        logger.error(logMessage, dlce);
      }
      catch (PluginException.InvalidDefinition ex) {
        JOptionPane.showMessageDialog(this,
                                      ex.getMessage(),
                                      WordUtils.capitalize(mimeTypeEditorBuilder.getValueName()) + " Warning",
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

  class MimeTypeTableModel extends AbstractTableModel {

    public MimeTypeTableModel() {
      columnNames = new String[] {
          "MIME Type",
          mimeTypeEditorBuilder.getValueClassName() + " Class Name",
      };
    }
    
    String[] columnNames;
    Class[] columnClass = { String.class, String.class, };
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
      logger.debug3("Got object: " + obj);
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
      for (Iterator it = m_filters.keySet().iterator() ; it.hasNext() ; ) {
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
      }
      else {
        row_data[0] = "Enter MIME type";
      }
      row_data[1] = "Replace with " + mimeTypeEditorBuilder.getValueClassName() + " class name";
      logger.debug3("Adding new row");
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



class MimeTypeEditor_okButton_actionAdapter implements java.awt.event.ActionListener {
  MimeTypeEditor adaptee;

  MimeTypeEditor_okButton_actionAdapter(MimeTypeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class MimeTypeEditor_addButton_actionAdapter implements java.awt.event.ActionListener {
  MimeTypeEditor adaptee;

  MimeTypeEditor_addButton_actionAdapter(MimeTypeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.addButton_actionPerformed(e);
  }
}

class MimeTypeEditor_deleteButton_actionAdapter implements java.awt.event.ActionListener {
  MimeTypeEditor adaptee;

  MimeTypeEditor_deleteButton_actionAdapter(MimeTypeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.deleteButton_actionPerformed(e);
  }
}

class MimeTypeEditor_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  MimeTypeEditor adaptee;

  MimeTypeEditor_cancelButton_actionAdapter(MimeTypeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}

class MimeTypeEditor_dnButton_actionAdapter implements java.awt.event.ActionListener {
  MimeTypeEditor adaptee;

  MimeTypeEditor_dnButton_actionAdapter(MimeTypeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.dnButton_actionPerformed(e);
  }
}

class MimeTypeEditor_upButton_actionAdapter implements java.awt.event.ActionListener {
  MimeTypeEditor adaptee;

  MimeTypeEditor_upButton_actionAdapter(MimeTypeEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.upButton_actionPerformed(e);
  }
}
