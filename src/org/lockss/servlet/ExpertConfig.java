/*
 * $Id: ExpertConfig.java,v 1.3.6.1 2009-11-24 05:07:41 dshr Exp $
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

package org.lockss.servlet;

import java.io.*;
import java.util.*;
import java.util.List;

import javax.servlet.*;

import org.mortbay.html.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.jetty.*;
import org.lockss.account.*;

/** Allow user to edit arbitrary config params (subject to
 * org.lockss.config.expert.allow and org.lockss.config.expert.deny)
 */
public class ExpertConfig extends LockssServlet {

  public static final String PREFIX = Configuration.PREFIX + "ui.";

  private static final String KEY_TEXT = "expert_text";

  public static final String ACTION_UPDATE = "Update";

  private static final String foot1 =
    "Enter parameters, one per line, in the form<pre>\n" +
    "org.lockss.foo.bar = value</pre>";

  static Logger log = Logger.getLogger("ExpertConfig");

  protected ConfigManager configMgr;

  // Values read from form
  private String etext;

  protected Configuration displayConfig;

  protected boolean isForm;


  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssApp().getConfigManager();
  }

  // don't hold onto objects after request finished
  protected void resetLocals() {
    etext = null;
    displayConfig = null;
    super.resetLocals();
  }

  protected void lockssHandleRequest() throws IOException {
    String action = req.getParameter("action");
    isForm = !StringUtil.isNullString(action);

    if (isForm) {
      readForm();
    } else {
      readCurrent();
    }

    if (ACTION_UPDATE.equals(action)) {
      doUpdate();
    } else {
      displayPage();
    }
  }

  protected void readCurrent() throws IOException {
    try {
      File efile =
	configMgr.getCacheConfigFile(ConfigManager.CONFIG_FILE_EXPERT);
      etext = StringUtil.fromFile(efile);
    } catch (FileNotFoundException e) {
      etext = null;
    } catch (IOException e) {
      log.error("Error reading expert config file", e);
      errMsg = "Couldn't read current expert config file: " + e.getMessage();
      etext = null;
    }
  }

  protected void readForm() {
    etext = req.getParameter(KEY_TEXT);
  }

  protected void doUpdate() throws IOException {
    if (false /* errors */) {
      displayPage();
    } else {
      try {
	if (testAndSaveConfig()) {
	  statusMsg = "Update successful";
	}
      } catch (Exception e) {
	log.error("Error saving changes", e);
	errMsg = "Error: Couldn't save changes:<br>" + e.toString();
      }
      displayPage();
    }
  }

  /**
   * Display the ExpertConfig.
   */
  private void displayPage() throws IOException {
    // Create and start laying out page
    Page page = newPage();
    layoutErrorBlock(page);
//     ServletUtil.layoutExplanationBlock(page, "Explanation");
    page.add("<br>");

    // Create and layout form
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    Table table = new Table(0, "align=\"center\" cellspacing=\"2\"");
    table.newRow();
    table.newCell("align=center");
    table.add(makeTextArea("Configuration Parameters" + addFootnote(foot1),
			   KEY_TEXT,
			   etext));
    spaceRow(table);
    ServletUtil.layoutSubmitButton(this, table, ACTION_UPDATE);
    form.add(table);
    page.add(form);
    if (displayConfig != null) {
      Table configTab = new Table(0, "align=\"center\" cellspacing=\"2\"");
      spaceRow(configTab);
      configTab.newRow();
      configTab.addHeading("Resulting configuration", "align=center colspan=3");
      List<String> keys = new ArrayList<String>(displayConfig.keySet());
      Collections.sort(keys);
      for (String key : keys) {
	configTab.newRow();
	configTab.newCell("align=left");
	configTab.add(key);
	configTab.newCell("width=8");
	configTab.add("&nbsp;");
	configTab.newCell("align=left");
	configTab.add(displayConfig.get(key));
      }
      page.add(configTab);
    }

    // Finish laying out page
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  void spaceRow(Table table) {
    table.newRow();
    table.newCell();
    table.add("&nbsp;");
  }

  private static final int TEXT_COLUMNS = 60;
  private static final int TEXT_ROWS = 8;

  Composite makeTextArea(String title, String fieldName, String value) {
    Table table = new Table(0, "align=\"center\" cellpadding=\"0\"");
    TextArea urlArea = new MyTextArea(fieldName);
    urlArea.setSize(TEXT_COLUMNS, TEXT_ROWS);
    urlArea.add(value);
    setTabOrder(urlArea);
    table.newRow();
    table.addHeading(title, "align=\"center\"");
    table.newRow();
    table.newCell("align=center");
    table.add(urlArea);
//     table.newRow();
//     table.newCell("align=center");
    return table;
  }


  /**
   * Save the include and exclude lists to the access control file
   */
  protected boolean testAndSaveConfig() {
    File tmpfile = null;
    try {
      tmpfile = FileUtil.createTempFile("econfig_test", ".txt");
      StringUtil.toFile(tmpfile, etext);

      ConfigManager.KeyPredicate keyPred = configMgr.expertConfigKeyPredicate;
      String tmpUrl = tmpfile.toURL().toString();
      Configuration testConfig = configMgr.loadConfigFromFile(tmpUrl);
      List<String> illKeys = new ArrayList<String>();
      for (String key : testConfig.keySet()) {
	if (!keyPred.evaluate(key)) {
	  illKeys.add(key);
	}
      }
      UserAccount acct = getUserAccount();
      if (!illKeys.isEmpty()) {
	errMsg = "Error: Illegal parameter" + (illKeys.size() > 1 ? "s" : "")
	  + " in expert config: " + StringUtil.separatedString(illKeys, ", ");
	if (acct != null) {
	  acct.auditableEvent("used Expert Config but failed: " + etext);
	}
	return false;
      }
      log.info("saving expert config");
      configMgr.writeCacheConfigFile(etext,
				     ConfigManager.CONFIG_FILE_EXPERT,
				     false);
      statusMsg = "Update successful";
      displayConfig =
	configMgr.readCacheConfigFile(ConfigManager.CONFIG_FILE_EXPERT);
      if (acct != null) {
	acct.auditableEvent("used Expert Config successfully: " + etext);
      }

      return true;
    } catch (IOException e) {
      errMsg = "Not saved: " + e.toString();
      return false;
    } finally {
      if (tmpfile != null) {
	tmpfile.delete();
      }
    }
  }
}
