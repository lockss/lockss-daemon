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
    setSize(400,260);
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
    parameterPanel.setLayout(gridBagLayout2);
    parameterLabel.setText("Parameters:");
    parameterLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    parameterTextArea.setMinimumSize(new Dimension(100, 25));
    parameterTextArea.setEditable(true);
    parameterTextArea.setText("");
    insertButton.setText("Insert Param");
    insertButton.addActionListener(new
        PrintfTemplateEditor_insertButton_actionAdapter(this));
    formatTextArea.setMinimumSize(new Dimension(100, 25));
    formatTextArea.setText("");
    parameterPanel.setMinimumSize(new Dimension(380, 40));
    parameterPanel.setPreferredSize(new Dimension(380, 40));
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
    parameterPanel.add(insertButton,
                       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.NONE,
                                              new Insets(5, 10, 5, 0), 0, 6));
    parameterPanel.add(paramComboBox,
                       new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                              , GridBagConstraints.CENTER,
                                              GridBagConstraints.HORIZONTAL,
                                              new Insets(5, 8, 5, 6), 232, 6));
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
      case ConfigParamDescr.TYPE_YEAR:
      case ConfigParamDescr.TYPE_POS_INT:
        format = "%d";
        break;
    }
    // add the combobox data value to the edit box
    int pos = formatTextArea.getCaretPosition();
    formatTextArea.insert(format, pos);
    pos = parameterTextArea.getCaretPosition();
    parameterTextArea.insert(", " + key, pos);

    // if the value is an integer - get the field specifics
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
