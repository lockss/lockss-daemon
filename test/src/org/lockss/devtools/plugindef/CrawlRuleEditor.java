/*
 * $Id: CrawlRuleEditor.java,v 1.3 2004-05-15 01:23:24 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class CrawlRuleEditor extends JDialog implements EDPEditor{
  JPanel rulesPanel = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel buttonPanel = new JPanel();
  JButton deleteButton = new JButton();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();

  CrawlRuleModel m_model = new CrawlRuleModel();
  JTable rulesTable = new JTable();
  JButton addButton = new JButton();

  public CrawlRuleEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public CrawlRuleEditor() {
    this(null, "", false);
  }

  private void jbInit() throws Exception {
    setSize(390,180);
    rulesPanel.setLayout(borderLayout1);
    deleteButton.setText("Delete");
    deleteButton.addActionListener(new CrawlRuleEditor_deleteButton_actionAdapter(this));
    okButton.setText("OK");
    okButton.addActionListener(new CrawlRuleEditor_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new CrawlRuleEditor_cancelButton_actionAdapter(this));
    rulesTable.setBorder(BorderFactory.createRaisedBevelBorder());
    rulesTable.setRowHeight(20);
    rulesTable.setModel(m_model);
    rulesPanel.setMinimumSize(new Dimension(125, 50));
    rulesPanel.setPreferredSize(new Dimension(380, 100));
    buttonPanel.setMinimumSize(new Dimension(125, 34));
    buttonPanel.setPreferredSize(new Dimension(380, 40));
    addButton.setText("Add");
    addButton.addActionListener(new CrawlRuleEditor_addButton_actionAdapter(this));
    getContentPane().add(rulesPanel);
    rulesPanel.add(rulesTable, BorderLayout.CENTER);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(addButton, null);
    buttonPanel.add(deleteButton, null);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
  }

  void deleteButton_actionPerformed(ActionEvent e) {
    CrawlRuleModel model = (CrawlRuleModel) rulesTable.getModel();

    int row = rulesTable.getSelectedRow();
    model.removeRowData(row);
  }

  void addButton_actionPerformed(ActionEvent e) {
    CrawlRuleModel model = (CrawlRuleModel) rulesTable.getModel();
    int row = rulesTable.getSelectedRow();
    model.addNewRow(row);
  }

  void okButton_actionPerformed(ActionEvent e) {
    CrawlRuleModel model = (CrawlRuleModel) rulesTable.getModel();
    model.updateData();

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
    m_model.setData(data);

    TableColumn col = rulesTable.getColumnModel().getColumn(0);
    JComboBox kind_box = new JComboBox(CrawlRuleTemplate.RULE_KIND_STRINGS);
    col.setCellEditor(new DefaultCellEditor(kind_box));
    col = rulesTable.getColumnModel().getColumn(1);
    col.setCellEditor(new CrawlRuleCellEditor());
  }

  class CrawlRuleModel extends AbstractTableModel {
    EDPCellData m_data;
    Collection m_rules = Collections.EMPTY_LIST;
    Vector m_tableData;

    CrawlRuleModel() {
      m_tableData = new Vector();
    }

    void setData(EDPCellData data) {
      m_data = data;
      m_rules = data.getPlugin().getAuCrawlRules();
      m_tableData.clear();
      Object[] entry;

      for (Iterator it = m_rules.iterator(); it.hasNext(); ) {
        CrawlRuleTemplate crt = new CrawlRuleTemplate((String) it.next());
        entry = new Object[2];
        entry[0] = crt.getKindString();
        entry[1] = crt;
        m_tableData.add(entry);
      }
    }

    void updateData() {
      Collection rules = new ArrayList(m_tableData.size());
      for(int i=0 ; i < m_tableData.size(); i++) {
        Object[] entry = (Object[])m_tableData.elementAt(i);
        String crawl_rule = ((CrawlRuleTemplate)entry[1]).getCrawlRuleString();
        if(crawl_rule != null) {
          rules.add(crawl_rule);
        }
      }
      m_data.getPlugin().setAuCrawlRules(rules);
    }

    /**
     * getColumnCount
     *
     * @return int
     */
    public int getColumnCount() {
      return 2;
    }

    /**
     * getRowCount
     *
     * @return int
     */
    public int getRowCount() {
      return m_tableData.size();
    }

    /**
     * getValueAt
     *
     * @param rowIndex int
     * @param columnIndex int
     * @return Object
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
       return ((Object[])m_tableData.elementAt(rowIndex))[columnIndex];
    }

    public boolean isCellEditable(int row, int col) {
      return true;
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {
      if(m_tableData.size() > row && row >=0) {
        Object[] data = (Object[]) m_tableData.get(row);
        data[col] = value;
      }
      fireTableCellUpdated(row, col);
    }

    public void addRowData(int row , Object[] data) {
      if(row < 0) {
        m_tableData.add(data);
      }
      else {
        m_tableData.add(row, data);
      }
      fireTableDataChanged();
    }

   /**
     * addRow
     *
     * @param row int
     */
    private void addNewRow(int row) {
      Object[] entry = new Object[2];
      CrawlRuleTemplate crt = new CrawlRuleTemplate();
      entry[0] = crt.getKindString();
      entry[1] = crt;
      addRowData(row, entry);
    }

    public void removeRowData(int row) {
      if(m_tableData.size() > row && row >=0) {
        m_tableData.remove(row);
      }
      fireTableDataChanged();
    }

  }

  class CrawlRuleCellEditor
      extends AbstractCellEditor
      implements TableCellEditor, ActionListener, ChangeListener {
    static final String CMD_STRING = "Edit";
    JButton m_button;
    PrintfEditor m_editor;
    EDPCellData m_data;

    CrawlRuleCellEditor() {
      m_button = makeButton(CMD_STRING);
      m_editor = new PrintfEditor();
    }

    public Object getCellEditorValue() {
      return m_data.getData();
    }

    /**
     * getTableCellEditorComponent
     *
     * @param table JTable
     * @param value Object
     * @param isSelected boolean
     * @param row int
     * @param column int
     * @return Component
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row,
                                                 int column) {

      Object obj = m_model.getValueAt(row, column);
      CrawlRuleTemplate template = (CrawlRuleTemplate) obj;
      m_data = new EDPCellData(m_model.m_data.getPlugin(),
                                         m_model.m_data.getKey(),
                                         template,
                                         null);
      m_data.addChangeListener(this);
      m_editor.setCellData(m_data);
      m_button.setText(template.getTemplateString());
      /* position dialog */
      return m_button;

    }

    /**
     * actionPerformed
     *
     * @param e ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
      if (CMD_STRING.equals(e.getActionCommand())) {
        m_editor.setVisible(true);
        fireEditingStopped();
      }
    }

    JButton makeButton(String command) {
      JButton button = new JButton();
      button.setActionCommand(command);
      button.addActionListener(this);
      button.setBorderPainted(false);
      return button;
    }

    /**
     * stateChanged
     *
     * @param e ChangeEvent
     */
    public void stateChanged(ChangeEvent e) {
      m_data = null;
      m_model.fireTableDataChanged();
    }
  }
}


class CrawlRuleEditor_deleteButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleEditor adaptee;

  CrawlRuleEditor_deleteButton_actionAdapter(CrawlRuleEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.deleteButton_actionPerformed(e);
  }
}

class CrawlRuleEditor_okButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleEditor adaptee;

  CrawlRuleEditor_okButton_actionAdapter(CrawlRuleEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class CrawlRuleEditor_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleEditor adaptee;

  CrawlRuleEditor_cancelButton_actionAdapter(CrawlRuleEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}

class CrawlRuleEditor_addButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleEditor adaptee;

  CrawlRuleEditor_addButton_actionAdapter(CrawlRuleEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.addButton_actionPerformed(e);
  }
}
