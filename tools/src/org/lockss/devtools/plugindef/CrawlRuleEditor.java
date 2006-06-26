/*
 * $Id: CrawlRuleEditor.java,v 1.7 2006-06-26 17:46:56 thib_gc Exp $
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
  JTable rulesTable = new JTable(m_model);
  JButton addButton = new JButton();
  JComboBox m_kindBox = new JComboBox(CrawlRuleTemplate.RULE_KIND_STRINGS);
  JButton upButton = new JButton();
  JButton dnButton = new JButton();
  private Frame m_frame;
  JScrollPane rulesScrollPane = new JScrollPane();

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

  public CrawlRuleEditor(Frame frame) {
    this(frame, "Crawl Rule Editor", false);
    m_frame = frame;
  }

  private void jbInit() throws Exception {
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
    rulesPanel.setMinimumSize(new Dimension(150, 50));
    rulesPanel.setPreferredSize(new Dimension(440, 100));
    buttonPanel.setMinimumSize(new Dimension(150, 34));
    buttonPanel.setPreferredSize(new Dimension(440, 40));
    addButton.setText("Add");
    addButton.addActionListener(new CrawlRuleEditor_addButton_actionAdapter(this));
    upButton.setText("Up");
    upButton.addActionListener(new CrawlRuleEditor_upButton_actionAdapter(this));
    dnButton.setText("Down");
    dnButton.addActionListener(new CrawlRuleEditor_dnButton_actionAdapter(this));
    getContentPane().add(rulesPanel);
    rulesPanel.add(rulesScrollPane, BorderLayout.CENTER);
    rulesPanel.add(buttonPanel,  BorderLayout.SOUTH);
    rulesScrollPane.getViewport().add(rulesTable, null);
    buttonPanel.add(upButton, null);
    buttonPanel.add(dnButton, null);
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

  void dnButton_actionPerformed(ActionEvent e) {
    int row = rulesTable.getSelectedRow();

    CrawlRuleModel model = (CrawlRuleModel) rulesTable.getModel();
    model.moveData(row, row + 1);
  }

  void upButton_actionPerformed(ActionEvent e) {
    int row = rulesTable.getSelectedRow();

    CrawlRuleModel model = (CrawlRuleModel) rulesTable.getModel();
    model.moveData(row, row - 1);

  }

  /**
   * setCellData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_model.setData(data);
    TableColumn col = rulesTable.getColumnModel().getColumn(0);
    col.setCellEditor(new DefaultCellEditor(m_kindBox));
    col = rulesTable.getColumnModel().getColumn(1);
    col.setCellEditor(new CrawlRuleCellEditor());
  }

  class CrawlRuleModel extends AbstractTableModel {
    String[] m_columns = {"Action", "Pattern"};

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
        entry = new Object[m_columns.length];
        entry[0] = crt.getKindString();
        entry[1] = crt;
        m_tableData.add(entry);
      }
    }

    void updateData() {
      Collection rules = new ArrayList(m_tableData.size());
      for(int i=0 ; i < m_tableData.size(); i++) {
        Object[] entry = (Object[])m_tableData.elementAt(i);
        CrawlRuleTemplate crt = (CrawlRuleTemplate)entry[1];
        // update the kind string;
        crt.setRuleKind((String)entry[0]);
        String crawl_rule = crt.getCrawlRuleString();
        if(crt.getRuleKind() > 0 && crawl_rule != null) {
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
      return m_columns.length;
    }

    public String getColumnName(int col) {
      return m_columns[col];
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

   public void setValueAt(Object value, int row, int col) {
      if(m_tableData.size() > row && row >=0) {
        Object[] data = (Object[]) m_tableData.get(row);
        data[col] = value;
      }
      fireTableCellUpdated(row, col);
    }

    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
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

    /**
     * moveData
     *
     * @param curRow Old row for the data
     * @param newRow New row for the data
     */
    private void moveData(int curRow, int newRow) {
      int lastRow = m_tableData.size() -1;
      if(curRow >=0 && curRow <= lastRow && newRow >=0 &&
        newRow <= lastRow) {
        Object[] curData = (Object[])m_tableData.elementAt(curRow);
        m_tableData.removeElementAt(curRow);
        m_tableData.insertElementAt(curData, newRow);
        fireTableDataChanged();
      }
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
      m_editor = new PrintfEditor(m_frame, "Crawl Rule");
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
      Rectangle r = table.getCellRect(row, column, true);
      Dimension dlgSize = m_editor.getPreferredSize();
      m_editor.setLocation(r.x  + dlgSize.width, r.y  + dlgSize.height);
      m_editor.pack();
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

class CrawlRuleEditor_upButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleEditor adaptee;

  CrawlRuleEditor_upButton_actionAdapter(CrawlRuleEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.upButton_actionPerformed(e);
  }
}

class CrawlRuleEditor_dnButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleEditor adaptee;

  CrawlRuleEditor_dnButton_actionAdapter(CrawlRuleEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.dnButton_actionPerformed(e);
  }

}
