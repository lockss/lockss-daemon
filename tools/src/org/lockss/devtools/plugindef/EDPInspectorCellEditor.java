/*
 * $Id: EDPInspectorCellEditor.java,v 1.8 2006-06-26 17:46:56 thib_gc Exp $
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
  protected static final String NOTES = "notes";
  protected static final String AUNAME = "auname";
  protected static final String STARTURL = "starturl";
  protected static final String CRAWLRULE = "rules";
  protected static final String CRAWLWINDOW = "window";
  protected static final String FILTERS = "filters";
  protected static final String EXCEPTIONS = "exceptions";
  protected static final String PAUSETIME = "pausetime";
  protected static final String CRAWLINTV = "crawlinterval";


  protected static final int PLUGIN_NAME = 0;
  protected static final int PLUGIN_ID = 1;
  protected static final int PLUGIN_VERSION = 2;
  protected static final int PLUGIN_PARAMS = 3;
  protected static final int PLUGIN_NOTES = 4;
  protected static final int PLUGIN_START_URL = 5;
  protected static final int PLUGIN_AUNAME = 6;
  protected static final int PLUGIN_CRAWLRULES = 7;
  protected static final int PLUGIN_CRAWLWINDOW = 8;
  protected static final int PLUGIN_PAUSETIME = 9;
  protected static final int PLUGIN_CRAWLINTV = 10;
  protected static final int PLUGIN_CRAWLDEPTH = 11;
  protected static final int PLUGIN_FILTER = 12;
  protected static final int PLUGIN_EXCEPTION = 13;
  protected static final int PLUGIN_EXMAP = 14;
  protected static final int NUMEDITORS = 15;

  protected CellEditorEntry[] editorEntries;

  public EDPInspectorCellEditor() {
  }

  public Object getCellEditorValue() {
    return "Edit...";
  }

  public void initEditors(JFrame parentFrame) {
    editorEntries = new CellEditorEntry[NUMEDITORS];

    // configuration parameters
    editorEntries[PLUGIN_PARAMS] =
      new CellEditorEntry(PICKER, new ConfigParamDescrPicker(parentFrame),
			  makeButton(PICKER));

    // plugin notes
    editorEntries[PLUGIN_NOTES] =
      new CellEditorEntry(NOTES, new NotesEditor(parentFrame, "Plugin Notes"),
			  makeButton(NOTES));

    // start url template
    editorEntries[PLUGIN_START_URL] =
      new CellEditorEntry(STARTURL,
			  new PrintfEditor(parentFrame,"Starting Url"),
			  makeButton(STARTURL));
    // au name template
    editorEntries[PLUGIN_AUNAME] =
      new CellEditorEntry(AUNAME,
			  new PrintfEditor(parentFrame, "AU Name"),
			  makeButton(AUNAME));
    // crawl rules
    editorEntries[PLUGIN_CRAWLRULES] =
      new CellEditorEntry(CRAWLRULE, new CrawlRuleEditor(parentFrame),
			  makeButton(CRAWLRULE));
    // crawl window
    editorEntries[PLUGIN_CRAWLWINDOW]=
	new CellEditorEntry(CRAWLWINDOW, new CrawlWindowEditor(parentFrame),
			    makeButton(CRAWLWINDOW));
    // pause between fetch
    editorEntries[PLUGIN_PAUSETIME] =
      new CellEditorEntry(PAUSETIME, new TimeEditor(parentFrame),
			  makeButton(PAUSETIME));
    // content crawl interval
    editorEntries[PLUGIN_CRAWLINTV] =
      new CellEditorEntry(CRAWLINTV, new TimeEditor(parentFrame),
			  makeButton(CRAWLINTV));
    // filter classes
    editorEntries[PLUGIN_FILTER] =
      new CellEditorEntry(FILTERS, new FilterRulesEditor(parentFrame),
			  makeButton(FILTERS));
    // exception remappings
    editorEntries[PLUGIN_EXMAP] =
      new CellEditorEntry(EXCEPTIONS, new ExceptionsMapEditor(parentFrame),
			  makeButton(EXCEPTIONS));

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
        JDialog dlg = editorEntries[i].m_dialog;
        dlg.setVisible(true);
        dlg.toFront();
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
