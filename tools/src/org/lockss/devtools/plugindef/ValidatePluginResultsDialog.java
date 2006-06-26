/*
 * $Id: ValidatePluginResultsDialog.java,v 1.8 2006-06-26 17:46:56 thib_gc Exp $
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
import javax.swing.*;
import org.lockss.plugin.*;
import org.lockss.devtools.*;
import javax.swing.text.*;
import org.lockss.util.*;
import java.awt.event.*;
import java.beans.*;


/**********************************************************************
 *  class ValidatePluginResultsDialog creates a Dialog that conducts
 *  the validation tests and displays the output.
 *
 *  <p>ValidatePluginResultsDialog: </p>
 *  <p>@author Rebecca Illowsky</p>
 *  <p>@version 0.7</p>
 *  LOCKSS
 **********************************************************************/

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
  BorderLayout borderLayout1   = new BorderLayout();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JScrollPane outputScrollPane = new JScrollPane();
  JTextPane outputTextPane     = new JTextPane();

  private ArchivalUnit m_au;

  JPanel  btnPanel     = new JPanel();
  JButton checkButton  = new JButton();
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
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
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
    panel1.add(outputScrollPane, BorderLayout.CENTER);
    outputScrollPane.getViewport().add(outputTextPane, null);
    panel1.add(btnPanel,  BorderLayout.SOUTH);
    btnPanel.add(checkButton, null);
    btnPanel.add(cancelButton, null);
  }

  /*!
   *@abstract Conducts all of the validation tests
   *and outputs whether or not the plugin in valid
   */
  public void validatePlugin(){
      String output = "";
      boolean validated = true;
      int output_color = BLACK_TEXT;

      output += "\n\n";
      outputMessage(output,0);

      //Test 1:  Checks that the starting url is in the Crawl Rules
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
      if(startingUrl.indexOf(m_au.getProperties().getUrl(ArchivalUnit.AU_BASE_URL,null).toString()) == -1){
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

  void cancelButton_actionPerformed(ActionEvent e) {
      setVisible(false);
  }
}
