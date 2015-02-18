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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

import org.lockss.devtools.*;
import org.lockss.plugin.*;
import org.lockss.util.*;


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

  protected JCheckBox fileCheckBox;
  protected JTextField fileTextField;
  protected JButton fileButton;
  protected CrawlRuleTester crawlRuleTesterThread;

  private ArchivalUnit m_au;
  private CrawlRuleTester.MessageHandler m_msgHandler;
  JPanel btnPanel = new JPanel();
  JButton checkButton = new JButton();
  JButton closeButton = new JButton();
  JButton stopButton = new JButton();

  protected static Logger logger = Logger.getLogger("CrawlRuleTestResultsDialog");

  public CrawlRuleTestResultsDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch (Exception exc) {
      String logMessage = "Could not set up the crawl rule test results dialog";
      logger.critical(logMessage, exc);
      JOptionPane.showMessageDialog(frame,
                                    logMessage,
                                    "Crawl Rule Test Results Dialog",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  public CrawlRuleTestResultsDialog() {
    this(null, "", false);
  }

  public CrawlRuleTestResultsDialog(ArchivalUnit au) {
    this();
    m_au = au;
    String startUrl = (String)m_au.getStartUrls().iterator().next();
    startUrlTextField.setText(startUrl);
    checkButton.setEnabled(!StringUtil.isNullString(startUrl));
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
    startUrlTextField.setText("");
    startUrlTextField.addKeyListener(new CrawlRuleTestResultsDialog_startUrlTextField_keyAdapter(this));

    infoPanel.setMinimumSize(new Dimension(300, 100));
    infoPanel.setPreferredSize(new Dimension(400, 120));
    infoPanel.setLayout(gridBagLayout1);
    delayLabel.setText("Fetch Delay:");
    depthLabel.setText("Test Depth:");
    startUrlLabel.setRequestFocusEnabled(false);
    startUrlLabel.setToolTipText("");
    startUrlLabel.setText("Starting URL:");
    panel1.setPreferredSize(new Dimension(400, 400));
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
    closeButton.setText("Close");
    closeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeButton_actionPerformed(e);
      }
    });
    stopButton.setText("Stop");
    stopButton.setEnabled(false);
    stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        stopButton_actionPerformed(event);
      }
    });
    outputTextPane.setEditable(false);
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

    addFileChooser();

    panel1.add(outputScrollPane, BorderLayout.CENTER);
    outputScrollPane.getViewport().add(outputTextPane, null);
    panel1.add(btnPanel,  BorderLayout.SOUTH);
    btnPanel.add(checkButton, null);
    btnPanel.add(stopButton, null);
    btnPanel.add(closeButton, null);
  }

  void checkButton_actionPerformed(ActionEvent e) {
    String startUrl = startUrlTextField.getText();
    if (StringUtil.isNullString(startUrl)){
      JOptionPane.showMessageDialog(this,
                                    "Missing starting url.",
                                    "CrawlRule TestError",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

    String fileName = fileTextField.getText();
    if (fileCheckBox.isSelected() && StringUtil.isNullString(fileName)) {
      JOptionPane.showMessageDialog(this,
                                    "Please choose an output file.",
                                    "Output file not chosen",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      int depth = Integer.parseInt(depthTextField.getText());
      long delay = Integer.parseInt(delayTextField.getText()) * Constants.SECOND;
      outputTextPane.setText("");
      outputTextPane.update(outputTextPane.getGraphics());

      synchronized(this) {
        stop();
        m_msgHandler = new MyMessageHandler();
        crawlRuleTesterThread = new CrawlRuleTester(m_msgHandler, depth, delay,
                                                    startUrl, m_au);
        stopButton.setEnabled(true);
        crawlRuleTesterThread.start();
      }
    }
    catch (Exception ex) {
      String logMessage = "Error while checking crawl rules: "
        + ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
      logger.error(logMessage, ex);
      stop();
      JOptionPane.showMessageDialog(this,
                                    logMessage,
                                    "Crawl Rule Test: Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  synchronized void closeButton_actionPerformed(ActionEvent e) {
    stopButton_actionPerformed(e);
    setVisible(false);
  }

  synchronized void stopButton_actionPerformed(ActionEvent e) {
    if (crawlRuleTesterThread != null) {
      stop();
      JOptionPane.showMessageDialog(this,
                                    "The crawl rule test was interrupted.",
                                    "Stop",
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }

  protected synchronized void stop() {
    if (crawlRuleTesterThread != null) {
      crawlRuleTesterThread.interrupt();
      crawlRuleTesterThread = null;
      m_msgHandler.close();
      m_msgHandler = null;
      stopButton.setEnabled(false);
    }
  }

  void startUrlTextField_keyReleased(KeyEvent e) {
    String startUrl = startUrlTextField.getText();
    boolean has_startUrl = !StringUtil.isNullString(startUrl);
    checkButton.setEnabled(has_startUrl);

  }

  private class MyMessageHandler implements CrawlRuleTester.MessageHandler {

    private Writer writer;

    public MyMessageHandler() throws IOException {
      String fileName = fileTextField.getText();
      if (fileCheckBox.isSelected() && !StringUtil.isNullString(fileName)) {
        File file = new File(fileName);
        writer = new BufferedWriter(new FileWriter(file, file.exists()));
      }
    }

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
        outputTextPane.scrollToReference(message);

        try {
          if (writer != null) {
            writer.write(message);
          }
        }
        catch (IOException exc) {
          // FIXME: add logging statement
        }
      }
      catch (BadLocationException ex) {
        logger.debug("Error in outputMessage()", ex);
      }
    }

    public void close() {
      if (writer != null) {
        IOUtil.safeClose(writer);
      }
      stop();
    }

  }

  protected void addFileChooser() {
    Box fileBox = Box.createVerticalBox();
    Box first = Box.createHorizontalBox();
    Box second = Box.createHorizontalBox();

    // Add checkbox and label
    fileCheckBox = new JCheckBox();
    fileCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        boolean selected = fileCheckBox.isSelected();
        fileTextField.setEnabled(selected);
        fileButton.setEnabled(selected);
      }
    });
    first.add(fileCheckBox);
    first.add(new JLabel("Save the output to a file"));
    first.add(Box.createHorizontalGlue());

    // Add text field and button, and set up file chooser interaction
    fileTextField = new JTextField();
    fileTextField.setEnabled(false);
    second.add(fileTextField);
    fileButton = new JButton("Choose file");
    fileButton.setEnabled(false);
    fileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("plugin-tool-output.txt"));
        int returnVal = chooser.showOpenDialog(CrawlRuleTestResultsDialog.this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
          fileTextField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
      }
    });
    second.add(Box.createHorizontalStrut(10));
    second.add(fileButton);

    // Put panel together
    fileBox.add(first);
    fileBox.add(second);
    infoPanel.add(fileBox,
                  new GridBagConstraints(0, 2,
                                         4, 2,
                                         1.0, 1.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(5, 5, 5, 5),
                                         0, 0));
  }

}

class CrawlRuleTestResultsDialog_startUrlTextField_keyAdapter extends java.awt.event.KeyAdapter {
  CrawlRuleTestResultsDialog adaptee;

  CrawlRuleTestResultsDialog_startUrlTextField_keyAdapter(CrawlRuleTestResultsDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void keyReleased(KeyEvent e) {
    adaptee.startUrlTextField_keyReleased(e);
  }
}
