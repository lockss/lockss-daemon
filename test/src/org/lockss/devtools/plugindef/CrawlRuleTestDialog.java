package org.lockss.devtools.plugindef;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.lockss.daemon.*;
import org.lockss.devtools.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

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
  JPanel infoPanel = new JPanel();
  JButton okButton = new JButton();
  JButton cancelButton = new JButton();
  JLabel startUrlLabel = new JLabel();
  JTextField startUrlTextField = new JTextField();
  JLabel depthLabel = new JLabel();
  JTextField depthTextField = new JTextField();
  JLabel delayLabel = new JLabel();
  JTextField delayTextField = new JTextField();
  JFileChooser fileChooser = new JFileChooser();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
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

  public CrawlRuleTestDialog() {
    this(null, "", false);
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
    int height = infoPanel.getPreferredSize().height +
        parameterPanel.getPreferredSize().height +
        buttonPanel.getPreferredSize().height;

    panel1.setPreferredSize(new Dimension(400, height));
    pack();
  }

  private void jbInit() throws Exception {
    parameterBorder = new TitledBorder("");
    infoBorder = new TitledBorder("");
    panel1.setLayout(borderLayout1);
    okButton.setText("OK");
    okButton.addActionListener(new CrawlRuleTestDialog_okButton_actionAdapter(this));
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new
        CrawlRuleTestDialog_cancelButton_actionAdapter(this));
    infoPanel.setBorder(infoBorder);
    infoPanel.setMinimumSize(new Dimension(300, 80));
    infoPanel.setPreferredSize(new Dimension(400, 90));
    infoPanel.setLayout(gridBagLayout1);
    buttonPanel.setBorder(null);
    buttonPanel.setMinimumSize(new Dimension(300, 35));
    buttonPanel.setPreferredSize(new Dimension(400, 35));
    startUrlLabel.setRequestFocusEnabled(false);
    startUrlLabel.setToolTipText("");
    startUrlLabel.setText("Starting URL:");
    startUrlTextField.setMinimumSize(new Dimension(200, 19));
    startUrlTextField.setPreferredSize(new Dimension(290, 19));
    startUrlTextField.setToolTipText("Enter URL from which to start check.");
    startUrlTextField.setText("http://");
    depthLabel.setText("Test Depth:");
    depthTextField.setMinimumSize(new Dimension(200, 19));
    depthTextField.setPreferredSize(new Dimension(100, 19));
    depthTextField.setToolTipText("Enter crawl depth.");
    depthTextField.setText("1");
    depthTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    delayLabel.setText("Fetch Delay:");
    delayTextField.setText("6");
    delayTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    delayTextField.setPreferredSize(new Dimension(100, 19));
    delayTextField.setToolTipText("Enter delay in milliseconds.");
    delayTextField.setMinimumSize(new Dimension(200, 19));
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
    panel1.add(infoPanel, BorderLayout.NORTH);
    infoPanel.add(startUrlLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 0, 0), 0, 0));
    infoPanel.add(startUrlTextField,
                  new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0
                                         , GridBagConstraints.WEST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(10, 7, 0, 13), 0, 0));
    infoPanel.add(depthLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 5, 0), 0, 0));
    infoPanel.add(delayLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 24, 5, 0), 0, 0));
    infoPanel.add(delayTextField, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(10, 13, 5, 13), -11, 0));
    infoPanel.add(depthTextField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(10, 7, 5, 0), -11, 0));
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
    try {
      Properties props = new Properties();
      String startUrl = startUrlTextField.getText();
      int depth = Integer.parseInt(depthTextField.getText());
      long delay = Integer.parseInt(delayTextField.getText()) *
          Constants.SECOND;
      for (Iterator it = m_descrMap.keySet().iterator(); it.hasNext();) {
        String key = (String) it.next();
        String value = ((JTextField)m_descrMap.get(key)).getText();
        props.put(key, value);
      }
      Configuration config = ConfigurationUtil.fromProps(props);
      ArchivalUnit au = m_plugin.createAu(config);
      int option = fileChooser.showSaveDialog(this);
      if(option == fileChooser.APPROVE_OPTION ||
         fileChooser.getSelectedFile() != null) {
        String fileName = fileChooser.getSelectedFile().getAbsolutePath();

        CrawlRuleTester tester = new CrawlRuleTester(fileName, depth, delay,
            startUrl, au.getCrawlSpec());

        setVisible(false);
        tester.runTest();
      }
    }

    catch(Exception ex) {
      ex.printStackTrace();
      // Alert here.
    }
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
