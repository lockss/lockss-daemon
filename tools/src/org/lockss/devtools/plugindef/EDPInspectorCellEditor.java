/*
 * $Id: EDPInspectorCellEditor.java,v 1.2 2004-06-03 02:44:33 clairegriffin Exp $
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

import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.Component;
import java.awt.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class EDPInspectorCellEditor extends AbstractCellEditor
    implements TableCellEditor, ActionListener {

  protected static final String PICKER = "picker";
  protected static final String TEMPLATE = "template";
  protected static final String CRAWLRULE = "rules";
  protected static final String FILTERS = "filters";
  protected static final String EXCEPTIONS = "exceptions";
  protected static final String TIME = "time";

  JButton pickerButton = makeButton(PICKER);
  JButton templateButton = makeButton(TEMPLATE);
  JButton rulesButton = makeButton(CRAWLRULE);
  JButton filtersButton = makeButton(FILTERS);
  JButton exceptionsButton = makeButton(EXCEPTIONS);
  JButton timeButton = makeButton(TIME);

  ConfigParamDescrPicker paramPicker = new ConfigParamDescrPicker();
  PrintfEditor templateEditor = new PrintfEditor();
  CrawlRuleEditor rulesEditor = new CrawlRuleEditor();
  FilterRulesEditor filtersEditor = new FilterRulesEditor();
  ExceptionsMapEditor exceptionsEditor = new ExceptionsMapEditor();
  TimeEditor timeEditor = new TimeEditor();

  protected CellEditorEntry[] editorEntries = {
      null, // plugin name
      null, // plugin version
      new CellEditorEntry(PICKER, paramPicker, pickerButton), // configuration parameters
      new CellEditorEntry(TEMPLATE,templateEditor,templateButton),  // au name template
      new CellEditorEntry(TEMPLATE,templateEditor,templateButton), // start url template
      new CellEditorEntry(CRAWLRULE,rulesEditor,rulesButton), // crawl rules
      new CellEditorEntry(TIME,timeEditor, timeButton), // pause between fetch
      new CellEditorEntry(TIME,timeEditor, timeButton), // content crawl interval
      null, // crawl depth
      null, // crawl window class
      new CellEditorEntry(FILTERS,filtersEditor, filtersButton),// filter classes
      null, // crawl exception class
      new CellEditorEntry(EXCEPTIONS,exceptionsEditor,exceptionsButton) // exception remappings
  };

  public EDPInspectorCellEditor() {
  }

  public Object getCellEditorValue() {
    return "Edit...";
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

    CellEditorEntry entry = editorEntries[row];
    if(entry != null) {
      JDialog dialog = entry.m_dialog;
      ((EDPEditor)dialog).setCellData((EDPCellData) value);
      prepareDialog(dialog,table,row,column);
      return entry.m_button;
    }


    return null;
  }

  /**
   * actionPerformed
   *
   * @param e ActionEvent
   */
  public void actionPerformed(ActionEvent e) {
    for(int i=0; i < editorEntries.length; i++) {
      if(editorEntries[i] != null &&
         editorEntries[i].m_command.equals(e.getActionCommand())) {
        editorEntries[i].m_dialog.setVisible(true);
        fireEditingStopped();
      }
    }
   }

  JButton makeButton(String command) {
    JButton button = new JButton();
    button.setActionCommand(command);
    button.addActionListener(this);
    button.setBorderPainted(false);
    return button;
  }

  private void prepareDialog(JDialog dialog, JTable table, int row, int column) {
    table.setRowSelectionInterval(row, row);
    table.setColumnSelectionInterval(column,column);
    Rectangle r = table.getCellRect(row, column, true);
    Dimension dlgSize = dialog.getPreferredSize();
    dialog.setLocation(r.x  + dlgSize.width, r.y  + dlgSize.height);
    dialog.pack();
  }

  static class CellEditorEntry {
    JButton m_button;
    JDialog m_dialog;
    String  m_command;

    CellEditorEntry(String command, JDialog dialog, JButton button) {
      m_button =  button;
      m_dialog = dialog;
      m_command = command;
    }
  }
}
