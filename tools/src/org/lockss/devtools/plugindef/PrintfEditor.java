package org.lockss.devtools.plugindef;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.lockss.daemon.*;
import javax.swing.border.*;

public class PrintfEditor extends JDialog implements EDPEditor {
  protected PrintfTemplate originalTemplate;
  protected transient PrintfTemplate editTemplate;
  private EDPCellData m_data;
  private HashMap paramKeys;

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
  JLabel fieldWidthLabel = new JLabel();
  JTextField widthTextField = new JTextField();
  JRadioButton spacesRadioButton = new JRadioButton();
  JRadioButton zeroRadioButton = new JRadioButton();
  ButtonGroup buttonGroup1 = new ButtonGroup();
  JPanel paddingPanel = new JPanel();
  JRadioButton noneRadioButton = new JRadioButton();
  TitledBorder paddingBorder;
  TitledBorder titledBorder2;
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  GridBagLayout gridBagLayout3 = new GridBagLayout();

  public PrintfEditor() {
    originalTemplate = new PrintfTemplate();
    editTemplate = new PrintfTemplate();

    try {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    paddingBorder = new TitledBorder("");
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
    parameterPanel.setBorder(BorderFactory.createEtchedBorder());
    parameterPanel.setMinimumSize(new Dimension(100, 160));
    parameterPanel.setPreferredSize(new Dimension(370, 160));
    fieldWidthLabel.setToolTipText("minimum number of digits");
    fieldWidthLabel.setText("field width:");
    widthTextField.setMinimumSize(new Dimension(10, 19));
    widthTextField.setPreferredSize(new Dimension(10, 19));
    widthTextField.setRequestFocusEnabled(true);
    widthTextField.setToolTipText("");
    widthTextField.setText("0");
    widthTextField.setSelectionStart(1);
    spacesRadioButton.setToolTipText(
        "Insert number with leading space, if less than field width.(3 => " +
        " 3).");
    spacesRadioButton.setText("pad with spaces");
    zeroRadioButton.setToolTipText(
        "Insert number with leading 0, if less than field width.(3 => 03).");
    zeroRadioButton.setSelected(false);
    zeroRadioButton.setText("pad with zeros");
    paramComboBox.addActionListener(new
                                    PrintfEditor_paramComboBox_actionAdapter(this));
    paddingPanel.setLayout(gridBagLayout2);
    noneRadioButton.setToolTipText("Insert number as-is (3 => 3).");
    noneRadioButton.setSelected(true);
    noneRadioButton.setText("do not pad");
    paddingPanel.setBorder(paddingBorder);
    paddingPanel.setMinimumSize(new Dimension(300, 70));
    paddingPanel.setPreferredSize(new Dimension(380, 150));
    paddingBorder.setTitleFont(new java.awt.Font("DialogInput", 0, 12));
    paddingBorder.setTitle("Numeric Padding");
    paddingPanel.add(noneRadioButton,
                     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 4, 0, 0), 33, 0));
    paddingPanel.add(zeroRadioButton,
                     new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 4, 0, 0), 8, 0));
    paddingPanel.add(spacesRadioButton,
                     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 4, 2, 0), 2, 0));
    paddingPanel.add(widthTextField,
                     new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
                                            , GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(0, 12, 0, 52), 30, 9));
    paddingPanel.add(fieldWidthLabel,
                     new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                            , GridBagConstraints.WEST,
                                            GridBagConstraints.NONE,
                                            new Insets(6, 57, 0, 0), 0, 0));
    parameterPanel.add(paramComboBox,
                       new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(4, 8, 0, 5), 190, 11));
    parameterPanel.add(paddingPanel,
                       new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.BOTH,
                                              new Insets(6, 6, 7, 5), -8, -51));
    buttonPanel.add(cancelButton, null);
    buttonPanel.add(saveButton, null);
    this.getContentPane().add(formatPanel, BorderLayout.NORTH);
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
    this.getContentPane().add(parameterPanel, BorderLayout.CENTER);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonGroup.add(cancelButton);
    buttonGroup1.add(spacesRadioButton);
    buttonGroup1.add(zeroRadioButton);
    parameterPanel.add(insertButton,
                       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.NONE,
                                              new Insets(4, 6, 0, 0), 0, 10));
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
        StringBuffer fbuf = new StringBuffer("%");
        boolean is_zero = zeroRadioButton.isSelected();
        String width_str = widthTextField.getText();
        int width = 0;
        if ( (width_str != null) && (width_str.length() > 0)) {
          width = Integer.parseInt(widthTextField.getText());
        }
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
      paddingPanel.setVisible(false);
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

  void paramComboBox_actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    String key = (String) paramComboBox.getSelectedItem();
    Object param = paramKeys.get(key);
    if (param == null) {
      paddingPanel.setVisible(false);
      return;
    }
    int type = ( (Integer) param).intValue();
    if ( (type == ConfigParamDescr.TYPE_INT) ||
        (type == ConfigParamDescr.TYPE_POS_INT)) {
      paddingPanel.setVisible(true);
    }
    else {
      paddingPanel.setVisible(false);
    }

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

class PrintfEditor_paramComboBox_actionAdapter
    implements java.awt.event.ActionListener {
  PrintfEditor adaptee;

  PrintfEditor_paramComboBox_actionAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(ActionEvent e) {
    adaptee.paramComboBox_actionPerformed(e);
  }
}
