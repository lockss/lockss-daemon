package org.lockss.devtools.plugindef;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.lockss.daemon.*;

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
  JLabel paddingLabel = new JLabel();
  JRadioButton spacesRadioButton = new JRadioButton();
  JRadioButton zeroRadioButton = new JRadioButton();
  ButtonGroup buttonGroup1 = new ButtonGroup();
  GridBagLayout gridBagLayout2 = new GridBagLayout();


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
    setSize(new Dimension(421, 380));
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
    formatPanel.setMinimumSize(new Dimension(400, 270));
    formatPanel.setPreferredSize(new Dimension(400, 270));
    parameterPanel.setLayout(gridBagLayout2);
    parameterLabel.setText("Parameters:");
    parameterLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    parameterTextArea.setMinimumSize(new Dimension(100, 25));
    parameterTextArea.setEditable(true);
    parameterTextArea.setText("");
    insertButton.setToolTipText("insert the format in the format string and add parameter to list.");
    insertButton.setText("Insert Parameter");
    insertButton.addActionListener(new
        PrintfTemplateEditor_insertButton_actionAdapter(this));
    formatTextArea.setMinimumSize(new Dimension(100, 25));
    formatTextArea.setText("");
    parameterPanel.setBorder(BorderFactory.createEtchedBorder());
    parameterPanel.setMinimumSize(new Dimension(380, 40));
    parameterPanel.setPreferredSize(new Dimension(380, 150));
    fieldWidthLabel.setToolTipText("minimum number of digits");
    fieldWidthLabel.setText("parameter field width:");
    widthTextField.setToolTipText("");
    widthTextField.setText("0");
    widthTextField.setSelectionStart(1);
    paddingLabel.setText("padding:");
    spacesRadioButton.setText("pad with space");
    zeroRadioButton.setText("pad with zeros");
    paramComboBox.addActionListener(new PrintfEditor_paramComboBox_actionAdapter(this));
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
        new Insets(6, 5, 0, 5), 386, 34));
    formatPanel.add(parameterTextArea,
                    new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0
                                           , GridBagConstraints.CENTER,
                                           GridBagConstraints.BOTH,
                                           new Insets(6, 5, 6, 5), 386, 34));
    this.getContentPane().add(parameterPanel, BorderLayout.CENTER);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonGroup.add(cancelButton);
    parameterPanel.add(paramComboBox,  new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 6, 0, 15), 197, 6));
    parameterPanel.add(widthTextField,  new GridBagConstraints(1, 1, 1, 2, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(21, 11, 38, 0), 21, 4));
    parameterPanel.add(insertButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 15, 0, 8), 0, 6));
    parameterPanel.add(fieldWidthLabel,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 15, 43, 0), 0, 0));
    parameterPanel.add(spacesRadioButton,  new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(9, 0, 0, 15), 6, 0));
    buttonGroup1.add(spacesRadioButton);
    buttonGroup1.add(zeroRadioButton);
    zeroRadioButton.setSelected(true);
    parameterPanel.add(paddingLabel,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 17, 43, 0), 0, 0));
    parameterPanel.add(zeroRadioButton,  new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 22, 15), 8, 0));
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
    String key = (String)paramComboBox.getSelectedItem();
    int type = ((Integer)paramKeys.get(key)).intValue();
    String format = "";

    switch(type) {
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
        if((width_str != null) && (width_str.length() > 0)) {
          width = Integer.parseInt(widthTextField.getText());
        }
        if(width > 0) {
          fbuf.append(".");
          if(is_zero) {
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
    setTemplate((PrintfTemplate)data.getData());
    // initialize the combobox
    paramComboBox.removeAllItems();
    paramKeys = data.getPlugin().getPrintfDescrs();
    if(paramKeys.size() > 0) {
      for (Iterator it = paramKeys.keySet().iterator(); it.hasNext(); ) {
        paramComboBox.addItem(it.next());
      }
      paramComboBox.setEnabled(true);
      paramComboBox.setSelectedIndex(0);
      paramComboBox.setToolTipText("Select a parameter to insert into the format string");
    }
    else { // deactivate the box and set a
      paramComboBox.setEnabled(false);
      paramComboBox.setToolTipText("No configuration parameters available.");
      setPadding(false);
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
    String key = (String)paramComboBox.getSelectedItem();
    Object param = paramKeys.get(key);
    if(param == null) {
      setPadding(false);
      return;
    }
    int type = ((Integer)param).intValue();
    if ( (type == ConfigParamDescr.TYPE_INT) ||
        (type == ConfigParamDescr.TYPE_POS_INT)) {
      setPadding(true);
    }
    else {
      setPadding(false);
    }

  }

  void setPadding(boolean paddingOn) {
    widthTextField.setVisible(paddingOn);
    fieldWidthLabel.setVisible(paddingOn);
    spacesRadioButton.setVisible(paddingOn);
    zeroRadioButton.setVisible(paddingOn);
    paddingLabel.setVisible(paddingOn);
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

class PrintfEditor_paramComboBox_actionAdapter implements java.awt.event.ActionListener {
  PrintfEditor adaptee;

  PrintfEditor_paramComboBox_actionAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.paramComboBox_actionPerformed(e);
  }
}
