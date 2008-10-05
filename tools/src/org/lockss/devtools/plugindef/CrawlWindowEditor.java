/*
 * $Id: CrawlWindowEditor.java,v 1.8 2006-09-06 16:38:41 thib_gc Exp $
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

import org.lockss.daemon.*;
import org.lockss.util.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * <p>Title: </p>
 * <p>@author Rebecca Illowsky</p>
 * <p>@version 0.7</p>
 * <p> </p>
 *  not attributable
 *
 */

public class CrawlWindowEditor extends JDialog implements EDPEditor{


  private static final int ACTION       = 0;
  private static final int FROM_HOUR    = 1;
  private static final int FROM_MINUTE  = 2;
  private static final int FROM_AMPM    = 3;
  private static final int TO_HOUR      = 4;
  private static final int TO_MINUTE    = 5;
  private static final int TO_AMPM      = 6;
  private static final int TIMEZONE     = 7;


  JPanel windowsPanel = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel buttonPanel = new JPanel();
    // JButton deleteButton = new JButton();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();

  CrawlWindowModel m_model = new CrawlWindowModel();
  JTable windowsTable = new JTable(m_model);
    //  JButton addButton = new JButton();
  JComboBox m_actionBox     = new JComboBox(CrawlWindowTemplate.WINDOW_ACTION_STRINGS);
  JComboBox m_fromHourBox   = new JComboBox(CrawlWindowTemplate.WINDOW_HOUR_STRINGS);
  JComboBox m_fromMinuteBox = new JComboBox(CrawlWindowTemplate.WINDOW_MINUTE_STRINGS);
  JComboBox m_fromAmPmBox   = new JComboBox(CrawlWindowTemplate.WINDOW_AMPM_STRINGS);
  JComboBox m_toHourBox     = new JComboBox(CrawlWindowTemplate.WINDOW_HOUR_STRINGS);
  JComboBox m_toMinuteBox   = new JComboBox(CrawlWindowTemplate.WINDOW_MINUTE_STRINGS);
  JComboBox m_toAmPmBox     = new JComboBox(CrawlWindowTemplate.WINDOW_AMPM_STRINGS);
  JComboBox m_timezoneBox   = new JComboBox(CrawlWindowTemplate.WINDOW_TIMEZONE_STRINGS);


  protected static Logger logger = Logger.getLogger("CrawlWindowEditor");

    //  JButton upButton = new JButton();
    //JButton dnButton = new JButton();
  private Frame m_frame;
  JScrollPane windowsScrollPane = new JScrollPane();

  public CrawlWindowEditor(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the crawl window editor";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Crawl Window Editor",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public CrawlWindowEditor(Frame frame) {
    this(frame, "Crawl Window Editor", false);
    m_frame = frame;
  }

  private void jbInit() throws Exception {
    windowsPanel.setLayout(borderLayout1);
    //  deleteButton.setText("Delete");
    //    deleteButton.addActionListener(new CrawlWindowEditor_deleteButton_actionAdapter(this));
    okButton.setText("OK");
    okButton.addActionListener(new CrawlWindowEditor_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new CrawlWindowEditor_cancelButton_actionAdapter(this));
    windowsTable.setBorder(BorderFactory.createRaisedBevelBorder());
    windowsTable.setRowHeight(20);
    windowsTable.setModel(m_model);
    windowsPanel.setMinimumSize(new Dimension(150, 50));
    windowsPanel.setPreferredSize(new Dimension(525, 100));
    buttonPanel.setMinimumSize(new Dimension(150, 34));
    buttonPanel.setPreferredSize(new Dimension(525, 40));

    // addButton.setText("Add");
    // addButton.addActionListener(new CrawlWindowEditor_addButton_actionAdapter(this));
    //  upButton.setText("Up");
    // upButton.addActionListener(new CrawlWindowEditor_upButton_actionAdapter(this));
    //  dnButton.setText("Down");
    //  dnButton.addActionListener(new CrawlWindowEditor_dnButton_actionAdapter(this));
    getContentPane().add(windowsPanel);
    windowsPanel.add(windowsScrollPane, BorderLayout.CENTER);
    windowsPanel.add(buttonPanel,  BorderLayout.SOUTH);
    windowsScrollPane.getViewport().add(windowsTable, null);
    // buttonPanel.add(upButton, null);
    // buttonPanel.add(dnButton, null);
    //  buttonPanel.add(addButton, null);
    //  buttonPanel.add(deleteButton, null);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);

  }
    /*
  void deleteButton_actionPerformed(ActionEvent e) {
    CrawlWindowModel model = (CrawlWindowModel) windowsTable.getModel();

    int row = windowsTable.getSelectedRow();
    model.removeRowData(row);
  }

  void addButton_actionPerformed(ActionEvent e) {
    CrawlWindowModel model = (CrawlWindowModel) windowsTable.getModel();
    int row = windowsTable.getSelectedRow();
    model.addNewRow(row);
  }
*/
  void okButton_actionPerformed(ActionEvent e) {
    CrawlWindowModel model = (CrawlWindowModel) windowsTable.getModel();
    model.updateData();

    setVisible(false);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }
    /*
  void dnButton_actionPerformed(ActionEvent e) {
    int row = windowsTable.getSelectedRow();

    CrawlWindowModel model = (CrawlWindowModel) windowsTable.getModel();
    model.moveData(row, row + 1);
  }

  void upButton_actionPerformed(ActionEvent e) {
    int row = windowsTable.getSelectedRow();

    CrawlWindowModel model = (CrawlWindowModel) windowsTable.getModel();
    model.moveData(row, row - 1);

  }*/

  /**
   * setCellData
   *
   * @param data DPCellData
   */
  public void setCellData(EDPCellData data) {
    m_model.setData(data);
    TableColumn col = windowsTable.getColumnModel().getColumn(ACTION);
    col.setCellEditor(new DefaultCellEditor(m_actionBox));
    col = windowsTable.getColumnModel().getColumn(FROM_HOUR);
    col.setCellEditor(new DefaultCellEditor(m_fromHourBox));
    col = windowsTable.getColumnModel().getColumn(FROM_MINUTE);
    col.setCellEditor(new DefaultCellEditor(m_fromMinuteBox));
    col = windowsTable.getColumnModel().getColumn(FROM_AMPM);
    col.setCellEditor(new DefaultCellEditor(m_fromAmPmBox));
    col = windowsTable.getColumnModel().getColumn(TO_HOUR);
    col.setCellEditor(new DefaultCellEditor(m_toHourBox));
    col = windowsTable.getColumnModel().getColumn(TO_MINUTE);
    col.setCellEditor(new DefaultCellEditor(m_toMinuteBox));
    col = windowsTable.getColumnModel().getColumn(TO_AMPM);
    col.setCellEditor(new DefaultCellEditor(m_toAmPmBox));
    col = windowsTable.getColumnModel().getColumn(TIMEZONE);
    col.setCellEditor(new DefaultCellEditor(m_timezoneBox));
  }

  class CrawlWindowModel extends AbstractTableModel {
    String[] m_columns = {"Action",
			  "From",
			  "",
			  "AM/PM",
			  "To",
			  "",
			  "AM/PM",
			  "Timezone"};

    EDPCellData m_data;
    CrawlWindow m_window;
    Vector m_tableData;

    CrawlWindowModel() {
      m_tableData = new Vector();
    }

    void setData(EDPCellData data) {
      m_data = data;
      m_window = data.getPlugin().getAuCrawlWindowSer();
      m_tableData.clear();
      Object[] entry;
      entry = new Object[m_columns.length];

      if(m_window!=null){
	  if(m_window instanceof CrawlWindows.Interval){
	      entry[ACTION] = CrawlWindowTemplate.WINDOW_ACTION_STRINGS[0];
	  }

	  else if(m_window instanceof CrawlWindows.Not){
	      entry[ACTION] = CrawlWindowTemplate.WINDOW_ACTION_STRINGS[1];
	      m_window = ((CrawlWindows.Not)m_window).getCrawlWindow();
	  }
	  else{
	      entry[ACTION] = CrawlWindowTemplate.WINDOW_ACTION_STRINGS[0];
	  }

	  Calendar start= ((CrawlWindows.Interval)m_window).getStartCalendar();
	  Calendar end  = ((CrawlWindows.Interval)m_window).getEndCalendar();
	  TimeZone timezone = ((CrawlWindows.Interval)m_window).getTimeZone();

	  int startHour = start.get(Calendar.HOUR_OF_DAY);
	  if(startHour > 12){
	      startHour -=12;
	      if(startHour == 0)
		  startHour = 12;
	      entry[FROM_AMPM] = CrawlWindowTemplate.WINDOW_AMPM_STRINGS[1];
	  }

	  else{
	      entry[FROM_AMPM] = CrawlWindowTemplate.WINDOW_AMPM_STRINGS[0];
	  }

	  entry[FROM_HOUR] = startHour + "";
	  entry[FROM_MINUTE] = CrawlWindowTemplate.WINDOW_MINUTE_STRINGS[start.get(Calendar.MINUTE)];

	  int endHour = end.get(Calendar.HOUR_OF_DAY);
	  if(endHour > 12){
	      endHour -=12;
	      if(endHour == 0)
		  endHour = 12;
	      entry[TO_AMPM] = CrawlWindowTemplate.WINDOW_AMPM_STRINGS[1];
	  }

	  else{
	      entry[TO_AMPM] = CrawlWindowTemplate.WINDOW_AMPM_STRINGS[0];
	  }

	  entry[TO_HOUR] = endHour + "";
	  entry[TO_MINUTE] = CrawlWindowTemplate.WINDOW_MINUTE_STRINGS[end.get(Calendar.MINUTE)];


	  entry[TIMEZONE] = timezone.getID();
      }

      else{
	  entry[ACTION]      = CrawlWindowTemplate.WINDOW_ACTION_STRINGS[0];
	  entry[FROM_HOUR]   = CrawlWindowTemplate.WINDOW_HOUR_STRINGS[0];
	  entry[FROM_MINUTE] = CrawlWindowTemplate.WINDOW_MINUTE_STRINGS[0];
	  entry[FROM_AMPM]   = CrawlWindowTemplate.WINDOW_AMPM_STRINGS[0];
	  entry[TO_HOUR]     = CrawlWindowTemplate.WINDOW_HOUR_STRINGS[11];
	  entry[TO_MINUTE]   = CrawlWindowTemplate.WINDOW_MINUTE_STRINGS[59];
	  entry[TO_AMPM]     = CrawlWindowTemplate.WINDOW_AMPM_STRINGS[1];
	  entry[TIMEZONE]    = CrawlWindowTemplate.WINDOW_TIMEZONE_STRINGS[0];
      }

      m_tableData.add(entry);

    }

    void updateData() {
	Calendar start = Calendar.getInstance();
	Calendar end   = Calendar.getInstance();
	TimeZone timezone;
	Object[] entry = (Object[])m_tableData.elementAt(0);

	boolean crawl = (entry[ACTION].toString() == "Crawl");

	int startHour = Integer.parseInt(entry[FROM_HOUR].toString());
	if(entry[FROM_AMPM].toString() == "PM")
	    startHour += 12;
	start.set(Calendar.HOUR_OF_DAY,startHour);
	start.set(Calendar.MINUTE,Integer.parseInt(entry[FROM_MINUTE].toString().substring(1)));

	int endHour = Integer.parseInt(entry[TO_HOUR].toString());
	if(entry[TO_AMPM].toString() == "PM")
	    endHour += 12;
	end.set(Calendar.HOUR_OF_DAY,endHour);
	end.set(Calendar.MINUTE,Integer.parseInt(entry[TO_MINUTE].toString().substring(1)));

	timezone = TimeZone.getTimeZone(entry[7].toString());

	CrawlWindows.Interval window = new CrawlWindows.Interval(start,end,CrawlWindows.TIME,timezone);
	if(crawl)
	    m_data.getPlugin().setAuCrawlWindowSer(window);
	else
	    m_data.getPlugin().setAuCrawlWindowSer(new CrawlWindows.Not(window));
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
	/* if(row < 0) {
        m_tableData.add(data);
      }
      else {
        m_tableData.add(row, data);
      }
      fireTableDataChanged();*/
    }

   /**
     * addRow
     *
     * @param row int
     */
    private void addNewRow(int row) {
    }

    public void removeRowData(int row) {
	/*   if(m_tableData.size() > row && row >=0) {
        m_tableData.remove(row);
      }
      fireTableDataChanged();*/
    }

    /**
     * moveData
     *
     * @param curRow Old row for the data
     * @param newRow New row for the data
     */
    private void moveData(int curRow, int newRow) {
	/* int lastRow = m_tableData.size() -1;
      if(curRow >=0 && curRow <= lastRow && newRow >=0 &&
        newRow <= lastRow) {
        Object[] curData = (Object[])m_tableData.elementAt(curRow);
        m_tableData.removeElementAt(curRow);
        m_tableData.insertElementAt(curData, newRow);
        fireTableDataChanged();
	}*/
    }
  }


  class CrawlWindowCellEditor
      extends AbstractCellEditor
      implements TableCellEditor, ActionListener, ChangeListener {
    static final String CMD_STRING = "Edit";
    JButton m_button;
    PrintfEditor m_editor;
    EDPCellData m_data;


    CrawlWindowCellEditor() {
      m_button = makeButton(CMD_STRING);
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
      CrawlWindowTemplate template = (CrawlWindowTemplate) obj;
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

/*
class CrawlWindowEditor_deleteButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlWindowEditor adaptee;

  CrawlWindowEditor_deleteButton_actionAdapter(CrawlWindowEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.deleteButton_actionPerformed(e);
  }
}
*/

class CrawlWindowEditor_okButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlWindowEditor adaptee;

  CrawlWindowEditor_okButton_actionAdapter(CrawlWindowEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class CrawlWindowEditor_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlWindowEditor adaptee;

  CrawlWindowEditor_cancelButton_actionAdapter(CrawlWindowEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}
/*
class CrawlWindowEditor_addButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlWindowEditor adaptee;

  CrawlWindowEditor_addButton_actionAdapter(CrawlWindowEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.addButton_actionPerformed(e);
  }
}

class CrawlWindowEditor_upButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlWindowEditor adaptee;

  CrawlWindowEditor_upButton_actionAdapter(CrawlWindowEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.upButton_actionPerformed(e);
  }
}

class CrawlWindowEditor_dnButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlWindowEditor adaptee;

  CrawlWindowEditor_dnButton_actionAdapter(CrawlWindowEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.dnButton_actionPerformed(e);
  }

}
*/
