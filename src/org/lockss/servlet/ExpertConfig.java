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

package org.lockss.servlet;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

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
  static Logger log = Logger.getLogger("ExpertConfig");

  /** URL of parameter documentation page */
  public static final String PARAM_PARAM_DOC_URL =
    Configuration.PREFIX + "config.paramDocUrl";
  public static final String DEFAULT_PARAM_DOC_URL =
    "http://www.lockss.org/lockssdoc/gamma/daemon/paramdoc.html";

  /** Initial dimensions in characters of ExpertConfig textarea:
   * <code><i>width</i>,<i>height</i></code> or min:max range of
   * dimensions:
   * <code><i>min_width</i>,<i>min_height</i>:<i>max_width</i>,<i>max_height</i></code> */
  public static final String PARAM_TEXT_DIMENSIONS =
    Configuration.PREFIX + "config.textDimensions";
  public static final String DEFAULT_TEXT_DIMENSIONS = "60,8:100,40";

  private static final String KEY_TEXT = "expert_text";

  public static final String ACTION_UPDATE = "Update";
  public static final String I18N_ACTION_UPDATE = i18n.tr("Update");

  private static final String foot1 =
    "Enter <a href=\"@PARAMDOCURL@\">parameters</a>, one per line, in the form<pre>\n" +
    "org.lockss.foo.bar = value</pre>";

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

  protected String readExpertConfigFile()
      throws FileNotFoundException, IOException {
    File efile = configMgr.getCacheConfigFile(ConfigManager.CONFIG_FILE_EXPERT);
    return StringUtil.fromFile(efile);
  }

  protected void readCurrent() {
    try {
      etext = readExpertConfigFile();
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
    String ft =
      StringUtil.replaceString(foot1, "@PARAMDOCURL@",
			       CurrentConfig.getParam(PARAM_PARAM_DOC_URL,
						      DEFAULT_PARAM_DOC_URL));
    table.add(makeTextArea("Configuration Parameters" + addFootnote(ft),
			   KEY_TEXT, etext));
    spaceRow(table);
    ServletUtil.layoutSubmitButton(this, table, ACTION_UPDATE, I18N_ACTION_UPDATE);
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
    endPage(page);
  }

  void spaceRow(Table table) {
    table.newRow();
    table.newCell();
    table.add("&nbsp;");
  }

  Composite makeTextArea(String title, String fieldName, String value) {

    StringUtil.CharWidthHeight cwh = StringUtil.countWidthAndHeight(value);
    int cols = maxWidth <= 0 ? minWidth : between(cwh.getWidth(),
						  minWidth, maxWidth);
    int rows = maxHeight <= 0 ? minHeight : between(cwh.getHeight(),
						    minHeight, maxHeight);

    Table table = new Table(0, "align=\"center\" cellpadding=\"0\"");
    TextArea urlArea = new MyTextArea(fieldName);
    urlArea.attribute("class", "resize");
    urlArea.setSize(cols, rows);
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


  int between(int n, int lo, int hi) {
    if (n < lo) return lo;
    if (n > hi) return hi;
    return n;
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
      String tmpUrl = tmpfile.toURI().toURL().toString();
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
      String origText;
      try {
	origText = readExpertConfigFile();
      } catch (IOException e) {
	log.error("Error reading expert config file", e);
	origText = null;
      }
      log.info("saving expert config");
      configMgr.writeCacheConfigFile(etext,
				     ConfigManager.CONFIG_FILE_EXPERT,
				     false);
      statusMsg = "Update successful";
      displayConfig =
	configMgr.readCacheConfigFile(ConfigManager.CONFIG_FILE_EXPERT);
      raiseAlert(acct, origText, etext, displayConfig);
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

  protected void raiseAlert(UserAccount acct,
			    String orig, String cur,
			    Configuration newConfig) {
    if (acct != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("used Expert Config.\n\n");
      if (!StringUtil.isNullString(orig)) {
	sb.append("Differences:\n\n");
	sb.append(DiffUtil.diff_configText(orig, cur));
      } else {
	sb.append("New text:\n\n");
	sb.append(cur);
      }
      sb.append("\n\nCurrent Expert Config:\n\n");
      List<String> keys = new ArrayList<String>(newConfig.keySet());
      Collections.sort(keys);
      for (String key : keys) {
	sb.append(key);
	sb.append(" = ");
	sb.append(newConfig.get(key));
	sb.append("\n");
      }
      acct.auditableEvent(sb.toString());
    }
  }


  // Patterm to match text dimension spec.  ddd,ddd or ddd,ddd:ddd,ddd
  static Pattern DIMENSION_SPEC_PAT =
    Pattern.compile("(\\d+),(\\d+)(?::(\\d+),(\\d+))?");


  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(PARAM_TEXT_DIMENSIONS)) {
      String whspec =
	config.get(PARAM_TEXT_DIMENSIONS, DEFAULT_TEXT_DIMENSIONS);
      try {
	parseSpec(whspec);
      } catch (NumberFormatException e) {
	parseSpec(DEFAULT_TEXT_DIMENSIONS);
      }
      log.debug2("minWidth: " + minWidth + ", maxWidth: " + maxWidth +
		 ", minHeight: " + minHeight + ", maxHeight: " + maxHeight);
    }

  }
					       
  private static int minWidth;
  private static int maxWidth;
  private static int minHeight;
  private static int maxHeight;

  static void parseSpec(String s) throws NumberFormatException {
    Matcher m1 = DIMENSION_SPEC_PAT.matcher(s);
    if (m1.matches()) {
      minWidth = Integer.parseInt(m1.group(1));
      minHeight = Integer.parseInt(m1.group(2));
      if (m1.start(3) > 0) {
	maxWidth = Integer.parseInt(m1.group(3));
	maxHeight = Integer.parseInt(m1.group(4));
      } else {
	maxWidth = 0;
	maxHeight = 0;
      }
    } else {
      throw new NumberFormatException("Invalied dimension spec: '" + s + "'");
    }
  }
}
