/*
 * $Id: EDPInspectorTableModel.java,v 1.19 2009-10-19 05:27:00 tlipkis Exp $
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
import java.util.ArrayList;

import javax.swing.event.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.devtools.plugindef.EditableDefinablePlugin.DynamicallyLoadedComponentException;
import org.lockss.plugin.definable.*;
import org.lockss.daemon.PluginException;

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

    InspectorEntry(String key, String title) {
      this(key, title, null);
    }

  }

  static final InspectorEntry[] inspectorEntries = {
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_NAME,
                       "Plugin Name"),
    new InspectorEntry(EditableDefinablePlugin.KEY_PLUGIN_IDENTIFIER,
                       "Plugin ID"),
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_VERSION,
		       "Plugin Version"),
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
		       "Configuration Parameters",
                       inspectorCellEditor),
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_NOTES,
                       "Plugin Notes",
		       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_START_URL,
		       "Start URL Template",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_NAME,
                       "AU Name Template",
		       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,
                       "Crawl Rules",
		       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME,
		       "Pause Time Between Fetches",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL,
                       "New Content Crawl Interval",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.SUFFIX_FILTER_RULE,
		       "Hash Filter Rules (obs.)",
                       inspectorCellEditor), 
    new InspectorEntry(DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY,
                       "Hash Filter Factories",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.SUFFIX_CRAWL_FILTER_FACTORY,
                       "Crawl Filter Factories",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH,
                       "Default Crawl Depth"),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW,
                       "Configurable Crawl Window Class"),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW_SER,
                       "Crawl Window",
                       inspectorCellEditor),
    new InspectorEntry(DefinablePlugin.KEY_EXCEPTION_HANDLER,
		       "Crawl Exception Class"),
    new InspectorEntry(DefinablePlugin.KEY_EXCEPTION_LIST,
		       "Cache Exception Map",
                       inspectorCellEditor),
    new InspectorEntry(DefinablePlugin.KEY_REQUIRED_DAEMON_VERSION,
                       "Required Daemon Version"),
    new InspectorEntry(DefinablePlugin.KEY_CRAWL_TYPE,
                       "Crawl Type",
                       crawlTypeEditor),
  };

  static final InspectorEntry[] requiredEntries = {
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_NAME,
                       "Plugin Name"),
    new InspectorEntry(EditableDefinablePlugin.KEY_PLUGIN_IDENTIFIER,
                       "Plugin ID"),
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_VERSION,
		       "Plugin Version"),
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
		       "Configuration Parameters",
                       inspectorCellEditor),
    new InspectorEntry(DefinablePlugin.KEY_PLUGIN_NOTES,
                       "Plugin Notes",
		       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_START_URL,
		       "Start URL Template",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_NAME,
                       "AU Name Template",
		       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,
                       "Crawl Rules",
		       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME,
		       "Pause Time Between Fetches",
                       inspectorCellEditor),
    new InspectorEntry(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL,
                       "New Content Crawl Interval",
                       inspectorCellEditor)
  };

  boolean isExpertMode = false;

  Object[][] data;
  EditableDefinablePlugin m_plugin;
  protected JFrame parentFrame;

  public EDPInspectorTableModel(JFrame parentFrame) {
    this.parentFrame = parentFrame;
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
    EDPCellData cellData = (EDPCellData) data[rowIndex][columnIndex];
    try {
      if (inspectorEntries[rowIndex].m_editor != inspectorCellEditor) {
        try {
          // we handle the internal update here
          cellData.updateStringData((String)obj);
        }
        catch (DynamicallyLoadedComponentException dlce) {
          if (handleDynamicallyLoadedComponentException(parentFrame, dlce)) {
            cellData.updateStringDataAnyway((String)obj);
          }
        }
      }
      else {
        //notifies listeners that something has changed
        cellData.updateOtherData( (String) obj);
      }
    }
    catch (PluginException.InvalidDefinition ide) {
      JOptionPane.showMessageDialog(parentFrame,
                                    ide.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  /* package */ static boolean handleDynamicallyLoadedComponentException(Component parentComponent,
                                                                         DynamicallyLoadedComponentException dlce) {
    Throwable cause = dlce.getCause();
    if (cause != null) {
      String errorMessage;
      if (cause instanceof ClassCastException) {
        errorMessage = "The class you have specified does not seem to be of the right type.";
      }
      else if (cause instanceof ClassNotFoundException) {
        errorMessage = "The class you have specified does not seem to be loadable under the current class path.";
      }
      else if (cause instanceof InstantiationError) {
        errorMessage = "The class you have specified seems to have caused an instantiation error.";
      }
      else if (cause instanceof IllegalAccessException) {
        errorMessage = "The class you have specified does not seem to have a public constructor.";
      }
      else {
        throw dlce; // rethrow
      }
      String[] messages = new String[] {
          errorMessage,
          "The internal error was of type " + cause.getClass().getName() + " with the following message:",
          " ",
          "\"" + StringUtils.abbreviate(dlce.getMessage(), 80) + "\"",
          " ",
          "Do you want to commit this value to the plugin anyway?",
      };
      int sel = JOptionPane.showConfirmDialog(parentComponent,
                                              messages,
                                              "Dynamically Loaded Component Exception",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.WARNING_MESSAGE);
      return sel == JOptionPane.YES_OPTION;
    }
    else {
      throw dlce; // rethrow
    }
  }

  public boolean isCellEditable(int row, int column) {
    return column == 1;
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

    comp = headerRenderer.getTableCellRendererComponent(null,
                                                        column.getHeaderValue(),
							false,
                                                        false,
                                                        0,
                                                        0);
    headerWidth = comp.getPreferredSize().width;

    comp = table.getDefaultRenderer(getColumnClass(col)).getTableCellRendererComponent(table,
                                                                                       longestStr,
                                                                                       false,
                                                                                       false,
                                                                                       0,
                                                                                       col);
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
