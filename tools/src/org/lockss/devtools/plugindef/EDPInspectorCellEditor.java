/*
 * $Id$
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

  protected static final String BUTTON_PARAMS = "params";
  protected static final String BUTTON_NOTES = "notes";
  protected static final String BUTTON_STARTURL = "starturl";
  protected static final String BUTTON_AUNAME = "auname";
  protected static final String BUTTON_CRAWLRULES = "rules";
  protected static final String BUTTON_PAUSETIME = "pausetime";
  protected static final String BUTTON_CRAWLINTV = "crawlinterval";
  protected static final String BUTTON_FILTERRULES = "filterrules";
  protected static final String BUTTON_HASH_FILTERFACTORIES =
    "hashfilterfactories";
  protected static final String BUTTON_CRAWL_FILTERFACTORIES =
    "crawlfilterfactories";
  protected static final String BUTTON_CRAWLWINDOWSER = "windowser";
  protected static final String BUTTON_EXCEPTIONS = "exceptions";


  protected static final int INDEX_NAME = 0;
  protected static final int INDEX_ID = 1;
  protected static final int INDEX_VERSION = 2;
  protected static final int INDEX_PARAMS = 3;
  protected static final int INDEX_NOTES = 4;
  protected static final int INDEX_STARTURL = 5;
  protected static final int INDEX_AUNAME = 6;
  protected static final int INDEX_CRAWLRULES = 7;
  protected static final int INDEX_PAUSETIME = 8;
  protected static final int INDEX_CRAWLINTV = 9;
  protected static final int INDEX_FILTERRULES = 10;
  protected static final int INDEX_HASH_FILTERFACTORIES = 11;
  protected static final int INDEX_CRAWL_FILTERFACTORIES = 12;
  protected static final int INDEX_REFETCHDEPTH = 13;
  protected static final int INDEX_CRAWLWINDOW = 14;
  protected static final int INDEX_CRAWLWINDOWSER = 15;
  protected static final int INDEX_EXCEPTIONS = 16;
  protected static final int INDEX_EXMAP = 17;
  protected static final int INDEX_REQUIREDDAEMONVERSION = 18;
  protected static final int NUMEDITORS = 19;

  protected CellEditorEntry[] editorEntries;

  public EDPInspectorCellEditor() {
  }

  public Object getCellEditorValue() {
    return "Edit...";
  }

  public void initEditors(JFrame parentFrame) {
    editorEntries = new CellEditorEntry[NUMEDITORS];

    // configuration parameters
    editorEntries[INDEX_PARAMS] =
      new CellEditorEntry(BUTTON_PARAMS, new ConfigParamDescrPicker(parentFrame),
			  makeButton(BUTTON_PARAMS));

    // plugin notes
    editorEntries[INDEX_NOTES] =
      new CellEditorEntry(BUTTON_NOTES, new NotesEditor(parentFrame, "Plugin Notes"),
			  makeButton(BUTTON_NOTES));

    // start url template
    editorEntries[INDEX_STARTURL] =
      new CellEditorEntry(BUTTON_STARTURL,
			  new PrintfEditor(parentFrame,"Starting Url"),
			  makeButton(BUTTON_STARTURL));
    // au name template
    editorEntries[INDEX_AUNAME] =
      new CellEditorEntry(BUTTON_AUNAME,
			  new PrintfEditor(parentFrame, "AU Name"),
			  makeButton(BUTTON_AUNAME));
    // crawl rules
    editorEntries[INDEX_CRAWLRULES] =
      new CellEditorEntry(BUTTON_CRAWLRULES, new CrawlRuleEditor(parentFrame),
			  makeButton(BUTTON_CRAWLRULES));
    // crawl window
    editorEntries[INDEX_CRAWLWINDOWSER]=
	new CellEditorEntry(BUTTON_CRAWLWINDOWSER, new CrawlWindowEditor(parentFrame),
			    makeButton(BUTTON_CRAWLWINDOWSER));
    // pause between fetch
    editorEntries[INDEX_PAUSETIME] =
      new CellEditorEntry(BUTTON_PAUSETIME, new TimeEditor(parentFrame),
			  makeButton(BUTTON_PAUSETIME));
    // content crawl interval
    editorEntries[INDEX_CRAWLINTV] =
      new CellEditorEntry(BUTTON_CRAWLINTV, new TimeEditor(parentFrame),
			  makeButton(BUTTON_CRAWLINTV));
    // filter rule classes
    editorEntries[INDEX_FILTERRULES] =
      new CellEditorEntry(BUTTON_FILTERRULES,
                          new MimeTypeEditor(new MimeTypeEditor.FilterRuleEditorBuilder(), parentFrame),
                          makeButton(BUTTON_FILTERRULES));
    // hash filter factory classes
    editorEntries[INDEX_HASH_FILTERFACTORIES] =
      new CellEditorEntry(BUTTON_HASH_FILTERFACTORIES,
                          new MimeTypeEditor(new MimeTypeEditor.HashFilterFactoryEditorBuilder(), parentFrame),
                          makeButton(BUTTON_HASH_FILTERFACTORIES));
    // crawl filter factory classes
    editorEntries[INDEX_CRAWL_FILTERFACTORIES] =
      new CellEditorEntry(BUTTON_CRAWL_FILTERFACTORIES,
                          new MimeTypeEditor(new MimeTypeEditor.CrawlFilterFactoryEditorBuilder(), parentFrame),
                          makeButton(BUTTON_CRAWL_FILTERFACTORIES));
    // exception remappings
    editorEntries[INDEX_EXMAP] =
      new CellEditorEntry(BUTTON_EXCEPTIONS, new ExceptionsMapEditor(parentFrame),
			  makeButton(BUTTON_EXCEPTIONS));

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
  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column) {

    CellEditorEntry entry = editorEntries[row];
    if (entry != null) {
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
