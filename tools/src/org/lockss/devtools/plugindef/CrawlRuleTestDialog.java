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
import org.lockss.plugin.ArchivalUnit.*;

/**
 * <p>Title: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 * <p> </p>
 *  not attributable
 *
 */

public class CrawlRuleTestDialog extends JDialog {
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

  public CrawlRuleTestDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public CrawlRuleTestDialog(Frame frame) {
    this(frame, "", false);
  }

  /**
   * CrawlRuleTestDialog
   *
   * @param editableDefinablePlugin EditableDefinablePlugin
   */
  CrawlRuleTestDialog(Frame parent, EditableDefinablePlugin plugin) {
    this(parent,"Configure Crawl Rule Test", false);
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
    okButton.setText("Check AU");
    okButton.addActionListener(new CrawlRuleTestDialog_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new
        CrawlRuleTestDialog_cancelButton_actionAdapter(this));
    buttonPanel.setBorder(null);
    buttonPanel.setMinimumSize(new Dimension(300, 35));
    buttonPanel.setPreferredSize(new Dimension(400, 35));
    parameterPanel.setBorder(parameterBorder);
    parameterPanel.setLayout(gridBagLayout2);
    parameterBorder.setTitleFont(new java.awt.Font("DialogInput", 0, 12));
    parameterBorder.setTitle("Archival Unit Configuration");
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
      JTextField field = new JTextField("");

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
      props.put(key, value);
    }
    Configuration config = ConfigManager.fromProperties(props);
    ArchivalUnit au = null;
    try {
      au = m_plugin.createAu(config);
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this,"Unable to create an Archival Unit.\n"
                                    +"Invalid Parameter",
                                    "CrawlRule Test Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
    return au;
  }

  private void testAu(ArchivalUnit au) {
    CrawlRuleTestResultsDialog test_dlg =
        new CrawlRuleTestResultsDialog(au);
    //Dimension dlgSize = test_dlg.getPreferredSize();
    Point pos = this.getLocationOnScreen();
    test_dlg.setLocation(pos.x, pos.y);
    //test_dlg.setLocation(r.x  + dlgSize.width, r.y  + dlgSize.height);
    test_dlg.pack();
    setVisible(false);
    test_dlg.show();
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }
}

class CrawlRuleTestDialog_okButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleTestDialog adaptee;

  CrawlRuleTestDialog_okButton_actionAdapter(CrawlRuleTestDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.okButton_actionPerformed(e);
  }
}

class CrawlRuleTestDialog_cancelButton_actionAdapter implements java.awt.event.ActionListener {
  CrawlRuleTestDialog adaptee;

  CrawlRuleTestDialog_cancelButton_actionAdapter(CrawlRuleTestDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancelButton_actionPerformed(e);
  }
}
