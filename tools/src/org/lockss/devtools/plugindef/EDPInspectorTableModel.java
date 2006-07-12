/*
 * $Id: EDPInspectorTableModel.java,v 1.13 2006-07-12 17:24:21 thib_gc Exp $
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

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;

public class EDPInspectorTableModel extends AbstractTableModel
  implements ChangeListener {

  static EDPInspectorCellEditor inspectorCellEditor = new EDPInspectorCellEditor();

  static JComboBox crawlTypeBox = new JComboBox(new String[] {
    "HTML Links",
    "OAI"
  });

  static DefaultCellEditor crawlTypeEditor = new DefaultCellEditor(crawlTypeBox);

  static final String[] cols = {
    "Plugin Field", "Assigned Value"};

  static final class InspectorEntry {
    String m_pluginKey;
    String m_title;
    TableCellEditor m_editor;

    InspectorEntry(String key, String title, TableCellEditor editor) {
      m_pluginKey = key;
      m_title = title;
      m_editor = editor;
    }
  }

  static final InspectorEntry[] inspectorEntries = {
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_NAME, "Plugin Name", null),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_IDENTIFIER, "Plugin ID", null),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_VERSION,
		       "Plugin Version", null),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_PROPS,
		       "Configuration Parameters", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_NOTES, "Plugin Notes",
		       inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_START_URL,
		       "Start URL Template", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_NAME, "AU Name Template",
		       inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_RULES, "Crawl Rules",
		       inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_CRAWL_WINDOW,
		       "Configurable Crawl Window Class", null),
    new InspectorEntry(EditableDefinablePlugin.AU_CRAWL_WINDOW_SER,
                       "Crawl Window", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_PAUSE_TIME,
		       "Pause Time Between Fetches", inspectorCellEditor),
    new InspectorEntry( EditableDefinablePlugin.AU_NEWCONTENT_CRAWL,
			"New Content Crawl Interval", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_CRAWL_DEPTH,
		       "Default Crawl Depth", null),
    new InspectorEntry(EditableDefinablePlugin.AU_FILTER_SUFFIX,
		       "Filter Class", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_EXCEPTION_HANDLER,
		       "Crawl Exception Class", null),
    new InspectorEntry(EditableDefinablePlugin.CM_EXCEPTION_LIST_KEY,
		       "Cache Exception Map", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.CM_CRAWL_TYPE,
		       "Crawl Type", crawlTypeEditor)
  };

  static final InspectorEntry[] requiredEntries = {
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_NAME, "Plugin Name", null),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_IDENTIFIER, "Plugin ID", null),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_VERSION,
		       "Plugin Version", null),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_PROPS,
		       "Configuration Parameters", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.PLUGIN_NOTES, "Plugin Notes",
		       inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_START_URL,
		       "Start URL Template", inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_NAME, "AU Name Template",
		       inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_RULES, "Crawl Rules",
		       inspectorCellEditor),
    new InspectorEntry(EditableDefinablePlugin.AU_PAUSE_TIME,
		       "Pause Time Between Fetches", inspectorCellEditor),
    new InspectorEntry( EditableDefinablePlugin.AU_NEWCONTENT_CRAWL,
			"New Content Crawl Interval", inspectorCellEditor)
  };

  boolean isExpertMode = false;

  Object[][] data;
  EditableDefinablePlugin m_plugin;

  public EDPInspectorTableModel(JFrame parentFrame) {
    inspectorCellEditor.initEditors(parentFrame);
    int numEntries = inspectorEntries.length;
    data = new Object[numEntries][cols.length];
    for (int i = 0; i < numEntries; i++) {
      data[i][0] = inspectorEntries[i].m_title;
    }
  }

  /**
   * getRowCount
   *
   * @return int
   */
  public int getRowCount() {
    if (isExpertMode) {
      return inspectorEntries.length;
    }
    else {
      return requiredEntries.length;
    }
  }

  /**
   * getColumnCount
   *
   * @return int
   */
  public int getColumnCount() {
    return cols.length;
  }

  public String getColumnName(int colIndex) {
    return cols[colIndex];
  }

  /**
   * getValueAt
   *
   * @param rowIndex int
   * @param columnIndex int
   * @return Object
   */
  public Object getValueAt(int rowIndex, int columnIndex) {

    Object obj = data[rowIndex][columnIndex];
    if (obj instanceof EDPCellData) {
      EDPCellData cell_data = (EDPCellData) obj;
      Object value = cell_data.getData();
      if (inspectorEntries[rowIndex].m_editor != inspectorCellEditor) {
	return value;
      }
      return cell_data;
    }
    return obj;
  }

  public void setValueAt(Object obj, int rowIndex, int columnIndex) {
    EDPCellData cell_data = (EDPCellData) data[rowIndex][columnIndex];
    if (inspectorEntries[rowIndex].m_editor != inspectorCellEditor) {
      // we handle the internal update here
      cell_data.updateStringData( (String) obj);
    }
    else{
	//notifies listeners that something has changed
	cell_data.updateOtherData( (String) obj);
    }
  }

  public boolean isCellEditable(int row, int column) {
    return column == 1 ? true : false;
  }

  public void setExpertMode(boolean isExpert) {
    isExpertMode = isExpert;
    fireTableDataChanged();
  }

  public void setPluginData(EditableDefinablePlugin edp) {
    m_plugin = edp;
    for (int row = 0; row < inspectorEntries.length; row++) {
      EDPCellData cell_data = new EDPCellData(edp,
					      inspectorEntries[row].m_pluginKey);
      cell_data.addChangeListener(this);
      data[row][1] = cell_data;
    }

    fireTableDataChanged();
  }

  public void setColumnSize(JTable table, int col) {
    TableColumn column = null;
    Component comp = null;
    int headerWidth = 0;
    int cellWidth = 0;
    String longestStr = "";
    String curString = "";

    for (int row = 0; row < inspectorEntries.length; row++) {
      curString = data[row][col].toString();
      if (curString.length() > longestStr.length()) {
	longestStr = curString;
      }
    }
    TableCellRenderer headerRenderer =
      table.getTableHeader().getDefaultRenderer();

    column = table.getColumnModel().getColumn(col);

    comp = headerRenderer.getTableCellRendererComponent(
							null, column.getHeaderValue(),
							false, false, 0, 0);
    headerWidth = comp.getPreferredSize().width;

    comp = table.getDefaultRenderer(getColumnClass(col)).
      getTableCellRendererComponent(
				    table, longestStr,
				    false, false, 0, col);
    cellWidth = comp.getPreferredSize().width;

    column.setPreferredWidth(Math.max(headerWidth, cellWidth));
  }

  public void setCellEditor(CellEditorJTable.CellEditorModel editorModel) {
    for (int row = 0; row < inspectorEntries.length; row++) {
      if (inspectorEntries[row].m_editor != null) {
	editorModel.addEditorForCell(row, 1, inspectorEntries[row].m_editor);
      }
    }
  }

  /**
   * stateChanged
   *
   * @param e ChangeEvent
   */
  public void stateChanged(ChangeEvent e) {
    //sets all dirty bits on
    m_plugin.setPluginState(PersistentPluginState.ALL_DIRTY_BITS_ON,null,"on");
    fireTableDataChanged();
  }
}
