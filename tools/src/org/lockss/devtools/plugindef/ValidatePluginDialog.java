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

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;


/**********************************************************************
 *  class ValidatePluginDialog creates a Dialog that accepts user input
 *  on the user-specified configuration parameters, and creates an
 *  instance of ValidatePluginResultsDialog to conduct the validation
 *  tests and display output.
 *
 *  <p>ValidatePluginDialog: </p>
 *  <p>@author Rebecca Illowsky</p>
 *  <p>@version 0.7</p>
 *  LOCKSS
 **********************************************************************/

public class ValidatePluginDialog extends JDialog {
  public static final String DIRTY_BIT_VALIDATE_KEY = "validatebit";

  EditableDefinablePlugin m_plugin;
  HashMap m_descrMap;

  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  JFileChooser fileChooser = new JFileChooser();
  JPanel parameterPanel = new JPanel();
  TitledBorder parameterBorder;
  TitledBorder infoBorder;
  GridBagLayout gridBagLayout2 = new GridBagLayout();

  protected static Logger logger = Logger.getLogger("ValidatePluginDialog");

  public ValidatePluginDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the plugin validation dialog";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Plugin Validation Dialog",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public ValidatePluginDialog(Frame frame) {
    this(frame, "", false);
  }

  /**
   * ValidatePluginDialog
   * @param parent parent Frame
   * @param plugin EditableDefinablePlugin
   */
  ValidatePluginDialog(Frame parent, EditableDefinablePlugin plugin) {
    this(parent,"Validate Plugin Parameters", false);
    m_plugin = plugin;
    addConfigParamFields();
    Dimension dim = new Dimension();
    int height = parameterPanel.getPreferredSize().height +
        buttonPanel.getPreferredSize().height;

    panel1.setPreferredSize(new Dimension(400, height));
    pack();
  }

  private void jbInit() throws Exception {
    parameterBorder = new TitledBorder("");
    infoBorder = new TitledBorder("");
    panel1.setLayout(borderLayout1);
    okButton.setText("Validate");
    okButton.addActionListener(new ValidatePluginDialog_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new
        ValidatePluginDialog_cancelButton_actionAdapter(this));
    buttonPanel.setBorder(null);
    buttonPanel.setMinimumSize(new Dimension(300, 35));
    buttonPanel.setPreferredSize(new Dimension(400, 35));
    parameterPanel.setBorder(parameterBorder);
    parameterPanel.setLayout(gridBagLayout2);
    parameterBorder.setTitleFont(new java.awt.Font("DialogInput", 0, 12));
    parameterBorder.setTitle("Plugin Configuration");
    infoBorder.setTitleFont(new java.awt.Font("DialogInput", 0, 12));
    infoBorder.setTitle("Test Configuration");
    getContentPane().add(panel1);
    panel1.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    panel1.add(parameterPanel, BorderLayout.CENTER);
  }

  void addConfigParamFields() {
    Collection descrs = m_plugin.getConfigParamDescrs();
    m_descrMap = new HashMap(descrs.size());
    int p_count = 0;
    for (Iterator it = descrs.iterator(); it.hasNext(); p_count++) {
      ConfigParamDescr cpd = (ConfigParamDescr) it.next();
      JLabel label = new JLabel(cpd.getDisplayName(), JLabel.RIGHT);
      JTextField field = new JTextField(m_plugin.getPluginState().getConfigParameterValue(cpd.getKey()));
      label.setLabelFor(field);
      Dimension dim = new Dimension(cpd.getSize() * field.getFont().getSize(),
                              19);
      field.setMinimumSize(dim);
      field.setPreferredSize(dim);
      field.setToolTipText(cpd.getDescription());

      parameterPanel.add(label,
                         new GridBagConstraints(0, p_count, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.NONE,
                                                new Insets(10, 10, 0, 0), 0, 0));
      parameterPanel.add(field,
                         new GridBagConstraints(1, p_count, 1, 1, 1.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.NONE,
                                                new Insets(10, 7, 5, 0), -11, 0));
      m_descrMap.put(cpd.getKey(), field);
    }

    if(p_count <= 0) {
      JLabel label =
          new JLabel("Warning: Configuration parameters have not been assigned!");
      parameterPanel.add(label);
    }
  }

  void okButton_actionPerformed(ActionEvent e) {
    ArchivalUnit au = makeAu();
    if(au != null) {
      testAu(au);
    }
  }

  private ArchivalUnit makeAu() {
    Properties props = new Properties();
    for (Iterator it = m_descrMap.keySet().iterator(); it.hasNext(); ) {
      String key = (String) it.next();
      String value = ( (JTextField) m_descrMap.get(key)).getText();
      if(!StringUtil.isNullString(value)) {
        props.put(key, value);
	m_plugin.setPluginState(PersistentPluginState.CONFIG_PARAMETERS,key,value);
      }
    }
    Configuration config = ConfigManager.fromProperties(props);
    ArchivalUnit au = null;
    try {
      au = m_plugin.createAu(config);
      m_plugin.setPluginState(PersistentPluginState.DIRTY_BIT,DIRTY_BIT_VALIDATE_KEY,"off");
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this,"Unable to test plugin.\n"
                                    +"Invalid Parameter\n"
				    +"Error Description: \n  "
				    + (ex.toString()).substring(
				        (ex.toString()).indexOf('$')+1),
                                    "Vaildation Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
    return au;
  }

  private void testAu(ArchivalUnit au) {
    ValidatePluginResultsDialog test_dlg =
        new ValidatePluginResultsDialog(au);

    //Runs all of the tests to validate the plugin.
    //Outputs the results to outputTextPane in the
    //results dialog
    test_dlg.validatePlugin();

    //Dimension dlgSize = test_dlg.getPreferredSize();
    Point pos = this.getLocationOnScreen();
    test_dlg.setLocation(pos.x, pos.y);
    //test_dlg.setLocation(r.x  + dlgSize.width, r.y  + dlgSize.height);
    test_dlg.pack();
    setVisible(false);
    test_dlg.setVisible(true);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }
}

class ValidatePluginDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
  ValidatePluginDialog adaptee;

  ValidatePluginDialog_okButton_actionAdapter(ValidatePluginDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class ValidatePluginDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  ValidatePluginDialog adaptee;

  ValidatePluginDialog_cancelButton_actionAdapter(ValidatePluginDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}
