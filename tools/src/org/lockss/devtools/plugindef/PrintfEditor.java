/*
 * $Id: PrintfEditor.java,v 1.28 2006-10-25 22:15:03 thib_gc Exp $
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import org.lockss.daemon.*;
import javax.swing.text.*;
import org.lockss.util.*;
import org.lockss.plugin.definable.*;


public class PrintfEditor extends JDialog implements EDPEditor, ConfigParamListener {
  protected PrintfTemplate originalTemplate;
  protected PrintfTemplate editableTemplate;
  private EDPCellData m_data;
  private HashMap paramKeys;
  private HashMap matchesKeys = new HashMap();
  static char[] RESERVED_CHARS = {'[','\\','^','$','.','|','?','*','+','(',')'};
  static String RESERVED_STRING = new String(RESERVED_CHARS);
  static SimpleAttributeSet PLAIN_ATTR = new SimpleAttributeSet();
  static {
    StyleConstants.setForeground(PLAIN_ATTR, Color.black);
    StyleConstants.setBold(PLAIN_ATTR, false);
    StyleConstants.setFontFamily(PLAIN_ATTR, "Helvetica");
    StyleConstants.setFontSize(PLAIN_ATTR, 14);
  }
  int numParameters = 0;

  JPanel printfPanel = new JPanel();
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
  JComboBox matchComboBox = new JComboBox();
  JButton insertMatchButton = new JButton();
  JPanel matchPanel = new JPanel();
  JPanel InsertPanel = new JPanel();
  GridLayout gridLayout1 = new GridLayout();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  GridBagLayout gridBagLayout3 = new GridBagLayout();
  JTabbedPane printfTabPane = new JTabbedPane();
  JTextPane editorPane = new JTextPane();
  JScrollPane editorPanel = new JScrollPane();
  int selectedPane = 0;
  private boolean m_isCrawlRuleEditor = false;
  private static final String STRING_LITERAL = "String Literal";

  protected static Logger logger = Logger.getLogger("PrintfEditor");

  public PrintfEditor(Frame frame, String title) {
    super(frame, title, false);

    originalTemplate = new PrintfTemplate();
    editableTemplate = new PrintfTemplate();
    try {
      jbInit();
      pack();
      initMatches();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the printf editor";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Printf Editor",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  private void initMatches() {
    matchesKeys.put(STRING_LITERAL, "");
    matchesKeys.put("Any number", "[0-9]+");
    matchesKeys.put("Anything", ".*");
    matchesKeys.put("Start", "^");
    matchesKeys.put("End", "$");
    matchesKeys.put("Single path component", "[^/]+");

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
    this.setTitle(this.getTitle() + " Template Editor");
    printfPanel.setLayout(gridBagLayout1);
    formatLabel.setFont(new java.awt.Font("DialogInput", 0, 12));
    formatLabel.setText("Format String:");
    buttonPanel.setLayout(flowLayout1);
    printfPanel.setBorder(BorderFactory.createEtchedBorder());
    printfPanel.setMinimumSize(new Dimension(100, 160));
    printfPanel.setPreferredSize(new Dimension(380, 160));
    parameterPanel.setLayout(gridBagLayout2);
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
    parameterPanel.setMinimumSize(new Dimension(60, 40));
    parameterPanel.setPreferredSize(new Dimension(300, 40));
    insertMatchButton.addActionListener(new
       PrintfTemplateEditor_insertMatchButton_actionAdapter(this));
    insertMatchButton.setText("Insert Match");
    insertMatchButton.setToolTipText("insert the match in the format string and add parameter to list.");
    insertMatchButton.setPreferredSize(new Dimension(136, 20));
    insertMatchButton.setMinimumSize(new Dimension(136, 20));
    insertMatchButton.setMaximumSize(new Dimension(136, 20));
    matchPanel.setPreferredSize(new Dimension(300, 40));
    matchPanel.setBorder(null);
    matchPanel.setMinimumSize(new Dimension(60, 60));
    matchPanel.setLayout(gridBagLayout3);
    InsertPanel.setLayout(gridLayout1);
    gridLayout1.setColumns(1);
    gridLayout1.setRows(2);
    gridLayout1.setVgap(0);
    InsertPanel.setBorder(BorderFactory.createEtchedBorder());
    InsertPanel.setMinimumSize(new Dimension(100, 100));
    InsertPanel.setPreferredSize(new Dimension(380, 120));
    editorPane.setText("");
    editorPane.addKeyListener(new PrintfEditor_editorPane_keyAdapter(this));
    printfTabPane.addChangeListener(new PrintfEditor_printfTabPane_changeAdapter(this));
    parameterPanel.add(insertButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(8, 6, 13, 8), 0, 10));
    parameterPanel.add(paramComboBox, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 13, 0), 258, 11));
    paramComboBox.setRenderer(new MyCellRenderer());
    InsertPanel.add(matchPanel, null);
    InsertPanel.add(parameterPanel, null);
    buttonPanel.add(cancelButton, null);
    buttonPanel.add(saveButton, null);
    this.getContentPane().add(printfTabPane, BorderLayout.NORTH);
    this.getContentPane().add(InsertPanel,  BorderLayout.CENTER);
    matchPanel.add(insertMatchButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(8, 6, 13, 8), 0, 10));
    matchPanel.add(matchComboBox, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 13, 0), 258, 11));
    printfPanel.add(parameterLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(7, 5, 0, 5), 309, 0));
    printfPanel.add(formatLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 5, 0, 5), 288, 0));
    printfPanel.add(formatTextArea, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 5, 0, 5), 300, 34));
    printfPanel.add(parameterTextArea, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 5, 6, 5), 300, 34));
    printfTabPane.addTab("Editor View", null, editorPanel,"View in Editor");
    printfTabPane.addTab("Printf View", null, printfPanel,"Vies as Printf");
    editorPane.setCharacterAttributes(PLAIN_ATTR, true);
    editorPane.addStyle("PLAIN",editorPane.getLogicalStyle());
    editorPanel.getViewport().add(editorPane, null);
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    buttonGroup.add(cancelButton);
  }
  /**
   * notifiyParamsChanged
   */
  public void notifiyParamsChanged() {
    updateParams(m_data);
  }

  /**
   * setEDPData
   *
   * @param data EDPCellData
   */
  public void setCellData(EDPCellData data) {
    m_data = data;
    paramKeys = data.getPlugin().getPrintfDescrs(!m_isCrawlRuleEditor);
    data.getPlugin().addParamListener(this);
    setTemplate( (PrintfTemplate) data.getData());
    m_isCrawlRuleEditor = data.getKey().equals(DefinableArchivalUnit.AU_RULES_KEY);
    // initialize the combobox
    updateParams(data);
    if (m_isCrawlRuleEditor) {
      matchPanel.setVisible(true);
    }
    else {
      matchPanel.setVisible(false);
    }
  }

  void saveButton_actionPerformed(ActionEvent e) {
    updateEditableTemplate(selectedPane);
    originalTemplate.setFormat(editableTemplate.m_format);
    originalTemplate.setTokens(editableTemplate.m_tokens);
    m_data.updateTemplateData(originalTemplate);
    setVisible(false);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  void printfTabPane_stateChanged(ChangeEvent e) {
    updateEditableTemplate(selectedPane);
    selectedPane = printfTabPane.getSelectedIndex();
    updatePane(selectedPane);
  }

  void updateEditableTemplate(int pane) {
    switch (pane) {
      case 0: // use the editor to update the template
        updateTemplateFromEditor(editableTemplate);
       break;
      case 1: // use the printf text areas to update the template.
        updateTemplateFromPrintf();
        break;
    }
  }

  void insertButton_actionPerformed(ActionEvent e) {
    Object selected = paramComboBox.getSelectedItem();
    ConfigParamDescr descr;
    String key;
    int type = 0;
    String format = "";
    if(selected instanceof ConfigParamDescr) {
      descr = (ConfigParamDescr) selected;
      key = descr.getKey();
      type = descr.getType();
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
          dialog.setVisible(true);
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
          if(key.startsWith(DefinableArchivalUnit.AU_SHORT_YEAR_PREFIX)) {
            format = "%02d";
          }
          else {
            format = "%d";
          }
          break;
        case ConfigParamDescr.TYPE_RANGE:
        case ConfigParamDescr.TYPE_NUM_RANGE:
        case ConfigParamDescr.TYPE_SET:
          format = "%s";
          break;
      }
      if (selectedPane == 0) {
        insertParameter(descr, format, editorPane.getSelectionStart());
      }
      else if (selectedPane == 1) {
        // add the combobox data value to the edit box
        int pos = formatTextArea.getCaretPosition();
        formatTextArea.insert(format, pos);

        pos = parameterTextArea.getCaretPosition();
        parameterTextArea.insert(", " + key, pos);
      }
    }
    else {
      key = selected.toString();
      format = escapePrintfChars( (String) JOptionPane.showInputDialog(this,
          "Enter the string you wish to input",
          "String Literal Input",
          JOptionPane.OK_CANCEL_OPTION));
      if (StringUtil.isNullString(format)) {
        return;
      }
      if(selectedPane == 0) {
        insertText(format, PLAIN_ATTR, editorPane.getSelectionStart());
      }
      else if (selectedPane == 1) {
        // add the combobox data value to the edit box
        formatTextArea.insert(format, formatTextArea.getCaretPosition());
      }
    }
  }


  void insertMatchButton_actionPerformed(ActionEvent e) {
    String key = (String) matchComboBox.getSelectedItem();
    String format = (String)matchesKeys.get(key);
    if (key.equals(STRING_LITERAL)) {
      format = escapeReservedChars((String) JOptionPane.showInputDialog(this,
                                                                        "Enter the string you wish to match",
                                                                        "String Literal Input",
                                                                        JOptionPane.OK_CANCEL_OPTION));
      if (StringUtil.isNullString(format)) {
        return;
      }
    }
    if (selectedPane == 0) {
      insertText(format, PLAIN_ATTR, editorPane.getSelectionStart());
    }
    else {
      // add the combobox data value to the edit box
      int pos = formatTextArea.getCaretPosition();
      formatTextArea.insert(format, pos);
    }
  }

  void editorPane_keyPressed(KeyEvent e) {
    StyledDocument doc = editorPane.getStyledDocument();
    int pos = editorPane.getCaretPosition();
    int code = e.getKeyCode();
    Element el;
    switch(code) {
      case KeyEvent.VK_BACK_SPACE:
      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_KP_LEFT:
        if(pos == 0) return;
        // we want to get the element to the left of position.
        el = doc.getCharacterElement(pos-1);
        break;
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_KP_RIGHT:
        // we want to get the element to the right of position.
        el = doc.getCharacterElement(pos + 1);
        break;
      default:
        return; // bail we don't handle it.
    }
    AttributeSet attr = el.getAttributes();
    String el_name = (String) attr.getAttribute(StyleConstants.NameAttribute);
    int el_range = el.getEndOffset() - el.getStartOffset()-1;
    if (el_name.startsWith("Parameter") &&
        StyleConstants.getComponent(attr) != null) {
      try {
        switch (code) {
          case KeyEvent.VK_BACK_SPACE:
          case KeyEvent.VK_DELETE:
            doc.remove(el.getStartOffset(), el_range);
            break;
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_KP_LEFT:
            editorPane.setCaretPosition(pos - el_range);
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_KP_RIGHT:
            editorPane.setCaretPosition(pos + (el_range));
            break;
        }
      }
      catch (BadLocationException ex) {
      }

    }
  }


  private void insertParameter(ConfigParamDescr descr, String format, int pos) {
    try {
      StyledDocument doc = (StyledDocument) editorPane.getDocument();

      // The component must first be wrapped in a style
      Style style = doc.addStyle("Parameter-" + numParameters, null);
      JLabel label = new JLabel(descr.getDisplayName());
      label.setAlignmentY(0.8f); // make sure we line up
      label.setFont(new Font("Helvetica", Font.PLAIN, 14));
      label.setForeground(Color.BLUE);
      label.setName(descr.getKey());
      label.setToolTipText("key: " + descr.getKey() + "    format: " + format);
      StyleConstants.setComponent(style, label);
      doc.insertString(pos, format, style);
      numParameters++;
    }
    catch (BadLocationException e) {
    }
  }

  private void insertText(String text, AttributeSet set, int pos) {
    try {
      editorPane.getDocument().insertString(pos, text, set);
    }
    catch (BadLocationException ex) {
    }
  }

  private void appendText(String text, AttributeSet set) {
    insertText(text, set, editorPane.getDocument().getLength());
  }

  private void updateTemplateFromPrintf() {
    String format = formatTextArea.getText();
    String parameters = parameterTextArea.getText();
    editableTemplate.setFormat(format);
    editableTemplate.setParameters(parameters);
  }

  private void updateTemplateFromEditor(PrintfTemplate template) {
    ArrayList params = new ArrayList();
    String format = null;
    int text_length = editorPane.getDocument().getLength();
    try {
      format = editorPane.getDocument().getText(0,text_length);
    }
    catch (BadLocationException ex1) {
    }
    Element section_el = editorPane.getDocument().getDefaultRootElement();
    // Get number of paragraphs.
    int num_para = section_el.getElementCount();
    for (int p_count = 0; p_count < num_para; p_count++) {
      Element para_el = section_el.getElement(p_count);
      // Enumerate the content elements
      int num_cont = para_el.getElementCount();
      for (int c_count = 0; c_count < num_cont; c_count++) {
        Element content_el = para_el.getElement(c_count);
        AttributeSet attr = content_el.getAttributes();
        // Get the name of the style applied to this content element; may be null
        String sn = (String) attr.getAttribute(StyleConstants.NameAttribute);
        // Check if style name match
        if (sn != null && sn.startsWith("Parameter")) {
          // we extract the label.
          JLabel l = (JLabel) StyleConstants.getComponent(attr);
          if (l != null) {
            params.add(l.getName());
          }
        }
      }
    }

    template.setFormat(format);
    template.setTokens(params);
  }

  protected void setTemplate(PrintfTemplate template) {
    originalTemplate = template;
    editableTemplate.setFormat(template.m_format);
    editableTemplate.setTokens(template.m_tokens);
    updatePane(selectedPane);
  }


  private void updateParams(EDPCellData data) {
    paramComboBox.removeAllItems();
    paramKeys = data.getPlugin().getPrintfDescrs(!m_isCrawlRuleEditor);
    if (!m_isCrawlRuleEditor) {
      paramComboBox.addItem(STRING_LITERAL);
    }

    for (Iterator it = paramKeys.values().iterator() ; it.hasNext() ; ) {
      ConfigParamDescr descr = (ConfigParamDescr)it.next();
      int type = descr.getType();
      if(!m_isCrawlRuleEditor && (type == ConfigParamDescr.TYPE_SET
         || type == ConfigParamDescr.TYPE_RANGE))
        continue;
      paramComboBox.addItem(descr);
    }
    paramComboBox.setEnabled(true);
    paramComboBox.setSelectedIndex(0);
    paramComboBox.setToolTipText(
        "Select a parameter to insert into the format string");
    insertButton.setEnabled(true);
  }


  /**
   * updatePane
   *
   * @param sel int
   */
  private void updatePane(int sel) {
    switch(sel) {
      case 0:   // editor view
        updateEditorView();
        break;
      case 1:   // printf view
        updatePrintfView();
       break;
    }
  }

  private void updatePrintfView() {
    formatTextArea.setText(editableTemplate.m_format);
    parameterTextArea.setText(editableTemplate.getTokenString());
  }

  private void updateEditorView() {
    editorPane.setText("");
    numParameters = 0;
    try {
      java.util.List elements = editableTemplate.getPrintfElements();
      for(Iterator it = elements.iterator(); it.hasNext(); ) {
        PrintfUtil.PrintfElement el = (PrintfUtil.PrintfElement) it.next();
        if(el.getFormat().equals(PrintfUtil.PrintfElement.FORMAT_NONE)) {
          appendText(el.getElement(), PLAIN_ATTR);
        }
        else {
         insertParameter((ConfigParamDescr)paramKeys.get(el.getElement()),
                         el.getFormat(),
                         editorPane.getDocument().getLength());
        }
      }
    }
    catch(Exception ex) {
      JOptionPane.showMessageDialog(this, "Invalid Format: " + ex.getMessage(),
                                    "Invalid Printf Format",
                                    JOptionPane.ERROR_MESSAGE);

      selectedPane = 1;
      printfTabPane.setSelectedIndex(selectedPane);
      updatePane(selectedPane);
    }
  }

  /**
   * Return a copy of the string with all reserved regexp chars
   * escaped by backslash.
   * @param str the string to add escapes to
   * @return String return a string with escapes or "" if str is null
   */
  private String escapeReservedChars(String str) {
  if(str == null) return "";
    StringBuffer sb = new StringBuffer();
    for(int ci = 0; ci < str.length(); ci++) {
      char ch = str.charAt(ci);
      if(RESERVED_STRING.indexOf(ch) >=0) {
        sb.append('\\');
      }
      sb.append(ch);
    }
    return escapePrintfChars(sb.toString());
  }

  private String escapePrintfChars(String str) {
    if(str == null) return "";
    StringBuffer sb = new StringBuffer();
    for(int ci = 0; ci < str.length(); ci++) {
      char ch = str.charAt(ci);
      if(ch == '%') {
        sb.append('%');
      }
      sb.append(ch);
    }
    return sb.toString();
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

class PrintfEditor_printfTabPane_changeAdapter implements javax.swing.event.ChangeListener {
  PrintfEditor adaptee;

  PrintfEditor_printfTabPane_changeAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void stateChanged(ChangeEvent e) {
    adaptee.printfTabPane_stateChanged(e);
  }
}

class PrintfEditor_editorPane_keyAdapter extends java.awt.event.KeyAdapter {
  PrintfEditor adaptee;

  PrintfEditor_editorPane_keyAdapter(PrintfEditor adaptee) {
    this.adaptee = adaptee;
  }
  public void keyPressed(KeyEvent e) {
    adaptee.editorPane_keyPressed(e);
  }
}

class MyCellRenderer extends JLabel implements ListCellRenderer {
    public MyCellRenderer() {
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {

      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      }
      else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      if(value instanceof ConfigParamDescr) {
        ConfigParamDescr descr = (ConfigParamDescr) value;
        setText(descr.getDisplayName());
      }
      else {
        setText(value.toString());
      }
      return this;
    }
}


