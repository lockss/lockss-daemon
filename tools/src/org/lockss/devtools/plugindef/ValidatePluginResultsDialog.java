package org.lockss.devtools.plugindef;

import java.awt.*;
import javax.swing.*;
import org.lockss.plugin.*;
import org.lockss.devtools.*;
import javax.swing.text.*;
import org.lockss.util.*;
import java.awt.event.*;
import java.beans.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: SUL-LOCKSS</p>
 * @author Rebecca Illowsky
 * @version 2.0
 */

public class ValidatePluginResultsDialog extends JDialog {
  static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
  static SimpleAttributeSet BLACK = new SimpleAttributeSet();
  static SimpleAttributeSet RED = new SimpleAttributeSet();
  static SimpleAttributeSet GREEN = new SimpleAttributeSet();
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

    StyleConstants.setForeground(GREEN, new Color(0,150,50));
    StyleConstants.setFontFamily(GREEN, "Helvetica");
    StyleConstants.setFontSize(GREEN, 14);
  }

  private static final int RED_TEXT = 0;
  private static final int GREEN_TEXT = 2;
  private static final int BLACK_TEXT = 3;
  private static final int BOLD_BLACK_TEXT = 4;

  SimpleAttributeSet[] m_attributes = {
     RED, RED, GREEN, BLACK, BOLD_BLACK, BOLD_BLACK
  };

  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
    //  JTextField delayTextField = new JTextField();
    // JTextField depthTextField = new JTextField();
    // JTextField startUrlTextField = new JTextField();
    //  JPanel infoPanel = new JPanel();
    // JLabel delayLabel = new JLabel();
    // JLabel depthLabel = new JLabel();
    //JLabel startUrlLabel = new JLabel();
  JScrollPane outputScrollPane = new JScrollPane();
  JTextPane outputTextPane = new JTextPane();

  private ArchivalUnit m_au;
    // private ValidatePluginer.MessageHandler m_msgHandler = new myMessageHandler();
  JPanel btnPanel = new JPanel();
  JButton checkButton = new JButton();
  JButton cancelButton = new JButton();

  public ValidatePluginResultsDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public ValidatePluginResultsDialog() {
    this(null, "", false);
  }

  public ValidatePluginResultsDialog(ArchivalUnit au) {
      this();  
      m_au = au;

      validatePlugin();

    //startUrlTextField.setText(startUrl);
    //checkButton.setEnabled(!StringUtil.isNullString(startUrl));
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    /*    delayTextField.setText("6");
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
    startUrlTextField.addKeyListener(new ValidatePluginResultsDialog_startUrlTextField_keyAdapter(this));
    infoPanel.setMinimumSize(new Dimension(300, 80));
    infoPanel.setPreferredSize(new Dimension(400, 90));
    infoPanel.setLayout(gridBagLayout1);
    delayLabel.setText("Fetch Delay:");
    depthLabel.setText("Test Depth:");
    startUrlLabel.setRequestFocusEnabled(false);
    startUrlLabel.setToolTipText("");
    startUrlLabel.setText("Starting URL:");*/
    panel1.setPreferredSize(new Dimension(400, 400));
    outputScrollPane.setMinimumSize(new Dimension(100, 100));
    outputScrollPane.setPreferredSize(new Dimension(400, 400));
    outputScrollPane.setToolTipText("");
    this.setTitle("Validate Plugin");
    checkButton.setText("OK");
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
    outputTextPane.setEditable(false);
    getContentPane().add(panel1);
    //   panel1.add(infoPanel, BorderLayout.NORTH);
    /*  infoPanel.add(startUrlLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
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
        new Insets(10, 7, 5, 0), -11, 0));*/
    panel1.add(outputScrollPane, BorderLayout.CENTER);
    outputScrollPane.getViewport().add(outputTextPane, null);
    panel1.add(btnPanel,  BorderLayout.SOUTH);
    btnPanel.add(checkButton, null);
    btnPanel.add(cancelButton, null);
  }

  /*!
   *@abstract Conducts all of the validation tests
   *and returns the concationation of their strings
   */
  public void validatePlugin(){
      String output = "";
      boolean validated = true;
      int output_color = BLACK_TEXT;

      output += "\n\n";
      outputMessage(output,0);

      //Test 1
      validated &= testStartingUrl();
  

      //Additional Tests...

      
      if(validated){
	  output += "Validation Succeeded!\n\n\n";
	  output_color = GREEN_TEXT;
      }
      else{
	  output += "Validation Failed!\n\n\n";
	  output_color = RED_TEXT;
      }

      outputMessage(output,output_color);
  }

  public boolean testStartingUrl(){
      String output = "";
      String startingUrl = (String)m_au.getNewContentCrawlUrls().get(0);
      boolean succeeded = true;
      int output_color;
      
      output += "Validating Starting Url (" + startingUrl + ")...\n";
      
      //Checks if the Crawl Rules have been set
      if(m_au.getCrawlSpec().isRuleNull()){
	  succeeded = false;
	  output += "  Validation Error:      Crawl Rules have not been set for plugin\n";
      }
      
      //Checks if the Starting Url has been set
      if(startingUrl == null || startingUrl.trim().equals("")){
	  succeeded = false;
	  output+= "  Validation Error:       Starting Url has not been set\n";
      }

      //Checks if the base url is included in the starting url
      if(startingUrl.indexOf(m_au.getProperties().getUrl(m_au.AU_BASE_URL,null).toString()) == -1){
	  output+= "  Validation Warning:  Base Url is not included in Starting Url\n";
      }

      //Checks if the starting url would be included in the crawl rules
      //(if the crawl rules are null, shouldBeCached() returns true)
      if(!m_au.shouldBeCached(startingUrl)){
	  succeeded = false;
	  output += "  Validation Error:      Starting Url is not included in Crawl Rules\n";
      }
      
      if(succeeded){
	  output += "...Succeeded";
	  output_color = GREEN_TEXT;
      }
      else{
	  output += "...Failed";
	  output_color = RED_TEXT;
      }

      output += "\n\n";

      outputMessage(output,output_color);

      return succeeded;
  }
    
  public void outputMessage(String message, int messageType) {
      try {
	  outputTextPane.getDocument().insertString(
	    outputTextPane.getDocument().getLength(), message,
            m_attributes[messageType]);
	  outputTextPane.scrollToReference(message);
      }
      catch (BadLocationException ex) {
	  ex.printStackTrace();
      }

  }

  void checkButton_actionPerformed(ActionEvent e) {
      setVisible(false);
  }

    /*
    String startUrl = startUrlTextField.getText();
    if(StringUtil.isNullString(startUrl)){
      JOptionPane.showMessageDialog(this,
                                    "Missing starting url.",
                                    "CrawlRule TestError",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    int depth = Integer.parseInt(depthTextField.getText());
    long delay = Integer.parseInt(delayTextField.getText()) * Constants.SECOND;
    outputTextPane.setText("");
    outputTextPane.update(outputTextPane.getGraphics());
    try {
	//    ValidatePluginer tester = new ValidatePluginer(m_msgHandler, depth, delay,
	//     startUrl, m_au.getCrawlSpec());
	//   tester.start();
    }
    catch (Exception ex) {
      String msg = ex.getCause() !=
          null ? ex.getCause().getMessage() : ex.getMessage();
      JOptionPane.showMessageDialog(this,
                                    "Error occured while checking crawl rules:\n"
                                    + msg,
                                    "CrawlRule Test Error",
                                    JOptionPane.ERROR_MESSAGE);
      ex.printStackTrace();
    }
  }
    */
  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }
    /*
  void startUrlTextField_keyReleased(KeyEvent e) {
    String startUrl = startUrlTextField.getText();
    boolean has_startUrl = !StringUtil.isNullString(startUrl);
    checkButton.setEnabled(has_startUrl);

    }*/
    /*
  private class myMessageHandler implements ValidatePluginer.MessageHandler {
    /**
     * outputMessage
     *
     * @param message String
     * @param messageType int
     *//*
    public void outputMessage(String message, int messageType) {
      try {
        outputTextPane.getDocument().insertString(
            outputTextPane.getDocument().getLength(), message,
            m_attributes[messageType]);
        outputTextPane.scrollToReference(message);
      }
      catch (BadLocationException ex) {
        ex.printStackTrace();
      }
    }
    }*/

}

/*
class ValidatePluginResultsDialog_startUrlTextField_keyAdapter extends java.awt.event.KeyAdapter {
  ValidatePluginResultsDialog adaptee;

  ValidatePluginResultsDialog_startUrlTextField_keyAdapter(ValidatePluginResultsDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void keyReleased(KeyEvent e) {
    adaptee.startUrlTextField_keyReleased(e);
  }
}
*/
