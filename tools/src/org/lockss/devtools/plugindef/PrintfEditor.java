package org.lockss.devtools.plugindef;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.lockss.daemon.*;

public class PrintfEditor extends JDialog
    implements EDPEditor {
  protected PrintfTemplate originalTemplate;
  protected transient PrintfTemplate editTemplate;
  private EDPCellData m_data;
  private HashMap paramKeys;
  private HashMap matchesKeys = new HashMap();
  static char[] RESERVED_CHARS = {'[','\\','^','$','.','|','?','*','+','(',')'};
  static String RESERVED_STRING = new String(RESERVED_CHARS);

  JPanel formatPanel = new JPanel();
  ButtonGroup buttonGroup = new ButtonGroup();
  JPanel buttonPanel = new JPanel();
  JButton cancelButton = new JButton();
  JButton saveButton = new JButton();
  JLabel formatLabel = new JLabel();
  FlowLayout flowLayout1 = new FlowLayout();
  JTextArea formatTextArea = new JTextArea();
  JPanel parameterPanel = new JPanel();
  JLabel parameterLabel = new JLabel();
  JTextArea parameterTextArea = new JTextArea();
  JButton insertButton = new JButton();
  JComboBox paramComboBox = new JComboBox();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  ButtonGroup buttonGroup1 = new ButtonGroup();

  TitledBorder titledBorder2;
  GridBagLayout gridBagLayout3 = new GridBagLayout();
  JComboBox matchComboBox = new JComboBox();
  GridBagLayout gridBagLayout4 = new GridBagLayout();
  JButton insertMatchButton = new JButton();
  JPanel matchPanel = new JPanel();
  JPanel InsertPanel = new JPanel();
  GridLayout gridLayout1 = new GridLayout();

  public PrintfEditor() {
    originalTemplate = new PrintfTemplate();
    editTemplate = new PrintfTemplate();

    try {
      jbInit();
      initMatches();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initMatches() {
    matchesKeys.put("String Literal", "");
    matchesKeys.put("Any Number", "[0-9][0-9]*");
    matchesKeys.put("Anything", ".*");
    matchesKeys.put("Start", "^");
    matchesKeys.put("End", "$");
    for(Iterator it = matchesKeys.keySet().iterator(); it.hasNext();) {
      matchComboBox.addItem(it.next());
    }
  }

  private void jbInit() throws Exception {

    saveButton.setText("Save");
    saveButton.addActionListener(new
                                 PrintfTemplateEditor_saveButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new
                                   PrintfTemplateEditor_cancelButton_actionAdapter(this));
    this.setTitle("Template Editor");
    formatPanel.setLayout(gridBagLayout1);
    formatLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    formatLabel.setText("Format String:");
    buttonPanel.setLayout(flowLayout1);
    formatPanel.setBorder(BorderFactory.createEtchedBorder());
    formatPanel.setMinimumSize(new Dimension(100, 160));
    formatPanel.setPreferredSize(new Dimension(380, 160));
    parameterPanel.setLayout(gridBagLayout3);
    parameterLabel.setText("Parameters:");
    parameterLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    parameterTextArea.setMinimumSize(new Dimension(100, 25));
    parameterTextArea.setPreferredSize(new Dimension(200, 25));
    parameterTextArea.setEditable(true);
    parameterTextArea.setText("");
    insertButton.setMaximumSize(new Dimension(136, 20));
    insertButton.setMinimumSize(new Dimension(136, 20));
    insertButton.setPreferredSize(new Dimension(136, 20));
    insertButton.setToolTipText(
        "insert the format in the format string and add parameter to list.");
    insertButton.setText("Insert Parameter");
    insertButton.addActionListener(new
                                   PrintfTemplateEditor_insertButton_actionAdapter(this));
    formatTextArea.setMinimumSize(new Dimension(100, 25));
    formatTextArea.setPreferredSize(new Dimension(200, 15));
    formatTextArea.setText("");
    parameterPanel.setBorder(null);
    parameterPanel.setMinimumSize(new Dimension(60, 60));
    parameterPanel.setPreferredSize(new Dimension(300, 60));
    insertMatchButton.addActionListener(new
       PrintfTemplateEditor_insertMatchButton_actionAdapter(this));
    insertMatchButton.setText("Insert Match");
    insertMatchButton.setToolTipText("insert the match in the format string and add parameter to list.");
    insertMatchButton.setPreferredSize(new Dimension(136, 20));
    insertMatchButton.setMinimumSize(new Dimension(136, 20));
    insertMatchButton.setMaximumSize(new Dimension(136, 20));
    matchPanel.setPreferredSize(new Dimension(300, 100));
    matchPanel.setBorder(null);
    matchPanel.setMinimumSize(new Dimension(60, 60));
    matchPanel.setLayout(gridBagLayout4);
    InsertPanel.setLayout(gridLayout1);
    gridLayout1.setColumns(1);
    gridLayout1.setRows(2);
    gridLayout1.setVgap(0);
    InsertPanel.setBorder(BorderFactory.createEtchedBorder());
    InsertPanel.setMinimumSize(new Dimension(100, 100));
    InsertPanel.setPreferredSize(new Dimension(380, 120));
    parameterPanel.add(paramComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(4, 8, 0, 5), 190, 11));
    parameterPanel.add(insertButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.NONE,
                                              new Insets(4, 6, 0, 0), 0, 10));
    buttonPanel.add(cancelButton, null);
    buttonPanel.add(saveButton, null);
    matchPanel.add(matchComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(4, 8, 0, 5), 190, 11));
    matchPanel.add(insertMatchButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.NONE,
                                              new Insets(4, 6, 0, 0), 0, 10));
    this.getContentPane().add(InsertPanel,  BorderLayout.CENTER);
    InsertPanel.add(parameterPanel, null);
    InsertPanel.add(matchPanel, null);
    this.getContentPane().add(formatPanel,  BorderLayout.NORTH);
    formatPanel.add(parameterLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(7, 5, 0, 5), 309, 0));
    formatPanel.add(formatLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(4, 5, 0, 5), 288, 0));
    formatPanel.add(formatTextArea, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
        , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(6, 5, 0, 5), 300, 34));
    formatPanel.add(parameterTextArea,
                    new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0
                                           , GridBagConstraints.CENTER,
                                           GridBagConstraints.BOTH,
                                           new Insets(6, 5, 6, 5), 300, 34));
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonGroup.add(cancelButton);

  }

  void saveButton_actionPerformed(ActionEvent e) {
    String format = formatTextArea.getText();
    String parameters = parameterTextArea.getText();
    originalTemplate.setFormat(format);
    originalTemplate.setParameters(parameters);
    m_data.updateTemplateData(originalTemplate);
    setVisible(false);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  void insertButton_actionPerformed(ActionEvent e) {
    String key = (String) paramComboBox.getSelectedItem();
    int type = ( (Integer) paramKeys.get(key)).intValue();
    String format = "";

    switch (type) {
      case ConfigParamDescr.TYPE_STRING:
      case ConfigParamDescr.TYPE_URL:
      case ConfigParamDescr.TYPE_BOOLEAN:
        format = "%s";
        break;
      case ConfigParamDescr.TYPE_INT:
      case ConfigParamDescr.TYPE_POS_INT:
        NumericPaddingDialog dialog = new NumericPaddingDialog();
        Point pos = this.getLocationOnScreen();
        dialog.setLocation(pos.x, pos.y);
        dialog.pack();
        dialog.show();
        StringBuffer fbuf = new StringBuffer("%");
        int width = dialog.getPaddingSize();
        boolean is_zero = dialog.useZero();
        if (width > 0) {
          fbuf.append(".");
          if (is_zero) {
            fbuf.append(0);
          }
          fbuf.append(width);
        }
        fbuf.append("d");
        format = fbuf.toString();
        break;
      case ConfigParamDescr.TYPE_YEAR:
        format = "%d";
    }
    // add the combobox data value to the edit box
    int pos = formatTextArea.getCaretPosition();
    formatTextArea.insert(format, pos);
    pos = parameterTextArea.getCaretPosition();
    parameterTextArea.insert(", " + key, pos);
  }


  void insertMatchButton_actionPerformed(ActionEvent e) {
    String key = (String) matchComboBox.getSelectedItem();
    String format = (String)matchesKeys.get(key);
    if(key.equals("String Literal")) {
     format = escapeReservedChars((String) JOptionPane.showInputDialog(this,
         "Enter the string you wish to match",
         "String Literal Input",
         JOptionPane.OK_CANCEL_OPTION));
    }

    // add the combobox data value to the edit box
    int pos = formatTextArea.getCaretPosition();
    formatTextArea.insert(format, pos);
  }


  /**
   * Return a copy of the string with all reserved regexp chars
   * escaped by backslash.
   * @param str the string to add escapes to
   * @return String return a string with escapes or "" if str is null
   */
  public static String escapeReservedChars(String str) {
  if(str == null) return "";
    StringBuffer sb = new StringBuffer();
    for(int ci = 0; ci < str.length(); ci++) {
      char ch = str.charAt(ci);
      if(RESERVED_STRING.indexOf(ch) >=0) {
        sb.append('\\');
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  /**
   * setEDPData
   *
   * @param edp EditableDefinablePlugin
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
    setTemplate( (PrintfTemplate) data.getData());
    // initialize the combobox
    paramComboBox.removeAllItems();
    paramKeys = data.getPlugin().getPrintfDescrs();
    if (paramKeys.size() > 0) {
      for (Iterator it = paramKeys.keySet().iterator(); it.hasNext(); ) {
        paramComboBox.addItem(it.next());
      }
      paramComboBox.setEnabled(true);
      paramComboBox.setSelectedIndex(0);
      paramComboBox.setToolTipText(
          "Select a parameter to insert into the format string");
      insertButton.setEnabled(true);
    }
    else { // deactivate the box and set a
      insertButton.setEnabled(false);
      paramComboBox.setEnabled(false);
      paramComboBox.setToolTipText("No configuration parameters available.");
    }
    if(data.getKey().equals(EditableDefinablePlugin.AU_RULES)) {
      matchPanel.setVisible(true);
    }
    else {
      matchPanel.setVisible(false);
    }
  }

  protected void setTemplate(PrintfTemplate template) {
    originalTemplate = template;
    editTemplate = new PrintfTemplate();
    if (template != null) {
      editTemplate.setFormat(template.m_format);
      editTemplate.setTokens(template.m_tokens);
    }
    formatTextArea.setText(template.m_format);
    parameterTextArea.setText(template.getTokenString());
  }

}

class PrintfTemplateEditor_saveButton_actionAdapter
    implements java.awt.event.ActionListener {
  PrintfEditor adaptee;

  PrintfTemplateEditor_saveButton_actionAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.saveButton_actionPerformed(e);
  }
}

class PrintfTemplateEditor_cancelButton_actionAdapter
    implements java.awt.event.ActionListener {
  PrintfEditor adaptee;

  PrintfTemplateEditor_cancelButton_actionAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}

class PrintfTemplateEditor_insertButton_actionAdapter
    implements java.awt.event.ActionListener {
  PrintfEditor adaptee;

  PrintfTemplateEditor_insertButton_actionAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.insertButton_actionPerformed(e);
  }
}

class PrintfTemplateEditor_insertMatchButton_actionAdapter
    implements java.awt.event.ActionListener {
  PrintfEditor adaptee;

  PrintfTemplateEditor_insertMatchButton_actionAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.insertMatchButton_actionPerformed(e);
  }
}
