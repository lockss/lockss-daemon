package org.lockss.devtools.plugindef;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.ListSelectionModel;
import java.util.Vector;
import javax.swing.table.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class CellEditorJTable extends JTable {
  protected CellEditorModel m_model = new CellEditorModel();

  public CellEditorJTable() {
    super();
  }

  public CellEditorJTable(TableModel p0) {
    super(p0);
  }

  public CellEditorJTable(TableModel p0, TableColumnModel p1) {
    super(p0, p1);
  }

  public CellEditorJTable(TableModel p0, TableColumnModel p1, ListSelectionModel p2) {
    super(p0, p1, p2);
  }

  public CellEditorJTable(int p0, int p1) {
    super(p0, p1);
  }

  public CellEditorJTable(Vector p0, Vector p1) {
    super(p0, p1);
  }

  public CellEditorJTable(Object[][] p0, Object[] p1) {
    super(p0, p1);
  }

  // the part that makes this a cell editor
  public void setCellEditorModel(CellEditorModel model) {
    m_model = model;
  }

  public CellEditorModel getCellEditorModel() {
    return m_model;
  }


  public TableCellEditor getCellEditor(int row, int column) {
    TableCellEditor editor = null;

    // if a editor is assigned in our CellEditor we return it.
    if(m_model != null) {
      editor = m_model.getEditorForCell(row, column);
      if(editor != null) {
        return editor;
      }
    }
    // otherwise just let the standard behavior prevail.
    return super.getCellEditor(row, column);
  }

  public static class CellEditorModel {
    private Hashtable editors = new Hashtable();

    public CellEditorModel() {

    }

    public void addEditorForCell(int row, int column, TableCellEditor editor) {
      editors.put(makeKey(row,column),editor);
    }

    public void removeEditorForCell(int row, int column) {
      editors.remove(makeKey(row,column));
    }

    public TableCellEditor getEditorForCell(int row, int column) {
      return (TableCellEditor) editors.get(makeKey(row, column));
    }

    private String makeKey(int row, int column) {
      return row + ":" + column;
    }
  }
}
