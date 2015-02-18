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
