package org.lockss.devtools.plugindef;

import java.awt.*;
import javax.swing.*;
import org.lockss.plugin.*;
import org.lockss.devtools.*;
import javax.swing.text.*;
import org.lockss.util.*;
import java.awt.event.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: SUL-LOCKSS</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class CrawlRuleTestResultsDialog extends JDialog {
  static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
  static SimpleAttributeSet BLACK = new SimpleAttributeSet();
  static SimpleAttributeSet RED = new SimpleAttributeSet();
  static {
    StyleConstants.setForeground(BOLD_BLACK, Color.black);
    StyleConstants.setBold(BOLD_BLACK, true);
    StyleConstants.setFontFamily(BOLD_BLACK, "Helvetica");
    StyleConstants.setFontSize(BOLD_BLACK, 14);

    StyleConstants.setForeground(BLACK, Color.black);
    StyleConstants.setFontFamily(BLACK, "Helvetica");
    StyleConstants.setFontSize(BLACK, 14);

    StyleConstants.setForeground(RED, Color.red);
    StyleConstants.setFontFamily(RED, "Helvetica");
    StyleConstants.setFontSize(RED, 14);
  }

  SimpleAttributeSet[] m_attributes = {
     RED, RED, BLACK, BOLD_BLACK, BOLD_BLACK
  };

  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JTextField delayTextField = new JTextField();
  JTextField depthTextField = new JTextField();
  JTextField startUrlTextField = new JTextField();
  JPanel infoPanel = new JPanel();
  JLabel delayLabel = new JLabel();
  JLabel depthLabel = new JLabel();
  JLabel startUrlLabel = new JLabel();
  JScrollPane outputScrollPane = new JScrollPane();
  JTextPane outputTextPane = new JTextPane();

  private ArchivalUnit m_au;
  private CrawlRuleTester.MessageHandler m_msgHandler = new myMessageHandler();
  JPanel btnPanel = new JPanel();
  JButton checkButton = new JButton();
  JButton cancelButton = new JButton();

  public CrawlRuleTestResultsDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public CrawlRuleTestResultsDialog() {
    this(null, "", false);
  }

  public CrawlRuleTestResultsDialog(ArchivalUnit au) {
    this();
    m_au = au;
    startUrlTextField.setText(m_au.getManifestPage());
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    delayTextField.setText("6");
    delayTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    delayTextField.setPreferredSize(new Dimension(100, 19));
    delayTextField.setToolTipText("Enter delay in milliseconds.");
    delayTextField.setMinimumSize(new Dimension(200, 19));
    depthTextField.setMinimumSize(new Dimension(200, 19));
    depthTextField.setPreferredSize(new Dimension(100, 19));
    depthTextField.setToolTipText("Enter crawl depth.");
    depthTextField.setText("1");
    depthTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    startUrlTextField.setMinimumSize(new Dimension(200, 19));
    startUrlTextField.setPreferredSize(new Dimension(290, 19));
    startUrlTextField.setToolTipText("Enter URL from which to start check.");
    startUrlTextField.setText("http://");
    infoPanel.setMinimumSize(new Dimension(300, 80));
    infoPanel.setPreferredSize(new Dimension(400, 90));
    infoPanel.setLayout(gridBagLayout1);
    delayLabel.setText("Fetch Delay:");
    depthLabel.setText("Test Depth:");
    startUrlLabel.setRequestFocusEnabled(false);
    startUrlLabel.setToolTipText("");
    startUrlLabel.setText("Starting URL:");
    panel1.setPreferredSize(new Dimension(400, 400));
    outputTextPane.setEditable(false);
    outputScrollPane.setMinimumSize(new Dimension(100, 100));
    outputScrollPane.setPreferredSize(new Dimension(400, 400));
    outputScrollPane.setToolTipText("");
    this.setTitle("Crawl Rule Test Results");
    checkButton.setText("Check Url");
    checkButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        checkButton_actionPerformed(e);
      }
    });
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    getContentPane().add(panel1);
    panel1.add(infoPanel, BorderLayout.NORTH);
    infoPanel.add(startUrlLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
        , GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 0, 0), 0, 0));
    infoPanel.add(startUrlTextField, new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0
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
    panel1.add(outputScrollPane, BorderLayout.CENTER);
    outputScrollPane.getViewport().add(outputTextPane, null);
    panel1.add(btnPanel,  BorderLayout.SOUTH);
    btnPanel.add(checkButton, null);
    btnPanel.add(cancelButton, null);
  }

  void checkButton_actionPerformed(ActionEvent e) {
    String startUrl = startUrlTextField.getText();
    int depth = Integer.parseInt(depthTextField.getText());
    long delay = Integer.parseInt(delayTextField.getText()) * Constants.SECOND;
    try {
      CrawlRuleTester tester = new CrawlRuleTester(m_msgHandler, depth, delay,
          startUrl, m_au.getCrawlSpec());
      tester.runTest();
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
                                    "Unable to create an Archival Unit:\n"
                                    + ex.getMessage(),
                                    "CrawlRule Test Error",
                                    JOptionPane.ERROR_MESSAGE);
      ex.printStackTrace();
    }
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  private class myMessageHandler implements CrawlRuleTester.MessageHandler {
    /**
     * outputMessage
     *
     * @param message String
     * @param messageType int
     */
    public void outputMessage(String message, int messageType) {
      try {
        outputTextPane.getDocument().insertString(
            outputTextPane.getDocument().getLength(), message,
            m_attributes[messageType]);
      }
      catch (BadLocationException ex) {
        ex.printStackTrace();
      }
    }
  }

}
