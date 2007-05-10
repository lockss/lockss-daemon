/*
 * $Id: PluginConfig.java,v 1.1 2007-05-10 23:41:53 tlipkis Exp $
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

import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.servlet.*;

import org.mortbay.html.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.jetty.*;

/** Manipulate plugin registries, title DBs, plugin keystores
 */
public class PluginConfig extends LockssServlet {

  public static final String PREFIX = Configuration.PREFIX + "ui.";

  private static final String KEY_PLUGIN_URLS = "pluginUrls";
  private static final String KEY_TITLEDB_URLS = "titleDbUrls";
  private static final String KEY_KEYSTORE_URL = "keystoreUrl";
  private static final String KEY_KEYSTORE_PASSWORD = "keystorePassword";
  private static final String KEY_USE_DEFAULT_PLUGINS =
    "useDefaultPluginRegistries";
//   private static final String KEY_USE_DEFAULT_KEYSTORE = "useDefaultKeystore";

  public static final String ACTION_UPDATE = "Update";

  private static final String footUrl =
    "Enter one URL per line.  " +
    "Deletions may not take effect until the next restart";

  static Logger log = Logger.getLogger("PluginConfig");

  protected ConfigManager configMgr;

  // Values read from form
  private List pluginUrls;
  private List titleDbUrls;
  private String keystoreUrl;
  private String keystorePassword;
  private boolean useDefaultPluginRegistries;
//   private boolean useDefaultKeystore;

  protected boolean isForm;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssApp().getConfigManager();
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

  protected void readCurrent() {
    Configuration config = configMgr.getCurrentConfig();
    pluginUrls = config.getList(PluginManager.PARAM_USER_PLUGIN_REGISTRIES);
    titleDbUrls = config.getList(ConfigManager.PARAM_USER_TITLE_DB_URLS);
    keystoreUrl = config.get(PluginManager.PARAM_USER_KEYSTORE_LOCATION);
    keystorePassword = config.get(PluginManager.PARAM_USER_KEYSTORE_PASSWORD);
    useDefaultPluginRegistries =
      config.getBoolean(PluginManager.PARAM_USE_DEFAULT_PLUGIN_REGISTRIES,
			PluginManager.DEFAULT_USE_DEFAULT_PLUGIN_REGISTRIES);
//     useDefaultKeystore =
//       config.getBoolean(PluginManager.PARAM_USE_DEFAULT_KEYSTORE,
// 			PluginManager.DEFAULT_USE_DEFAULT_KEYSTORE);
  }

  protected void readForm() {
    Configuration config = configMgr.getCurrentConfig();
    pluginUrls = readMultiLine(req.getParameter(KEY_PLUGIN_URLS));
    titleDbUrls = readMultiLine(req.getParameter(KEY_TITLEDB_URLS));
    keystoreUrl = req.getParameter(KEY_KEYSTORE_URL);
    keystorePassword = req.getParameter(KEY_KEYSTORE_PASSWORD);
    useDefaultPluginRegistries =
      !StringUtil.isNullString(req.getParameter(KEY_USE_DEFAULT_PLUGINS));
//     useDefaultKeystore =
//       !StringUtil.isNullString(req.getParameter(KEY_USE_DEFAULT_KEYSTORE));
  }



  /**
   * Convert a string of newline separated IP addresses to a vector of strings,
   * removing duplicates
   *
   * @param ipStr string to convert into a vector
   * @return vector of strings representing ip addresses
   */
  private List readMultiLine(String multiLine) {
    return StringUtil.breakAt(multiLine, '\n', 0, true, true);
  }


  protected void doUpdate() throws IOException {
    if (false /* errors */) {
      displayPage();
    } else {
      try {
	saveChanges();
	statusMsg = "Update successful";
      } catch (Exception e) {
	log.error("Error saving changes", e);
	errMsg = "Error: Couldn't save changes:<br>" + e.toString();
      }
      displayPage();
    }
  }

  /**
   * Display the UpdateIps page.
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
    table.add(makeTextArea("Plugin Registry URLs&nbsp;" + addFootnote(footUrl),
			   KEY_PLUGIN_URLS,
			   StringUtil.terminatedSeparatedString(pluginUrls,
								"\n", "\n")));
    table.add(ServletUtil.checkbox(this, KEY_USE_DEFAULT_PLUGINS, "true",
				   "Include default plugin registries",
				   useDefaultPluginRegistries));

    spaceRow(table);
    table.newRow();
    table.newCell("align=center");
    table.add(makeTextArea("Title DB URLs&nbsp;" + addFootnote(footUrl),
			   KEY_TITLEDB_URLS,
			   StringUtil.terminatedSeparatedString(titleDbUrls,
								"\n", "\n")));
    spaceRow(table);
    table.newRow();
    table.newCell("align=center");
    table.add(makeInput("Plugin Certificate Keystore URL",
			KEY_KEYSTORE_URL, keystoreUrl));
    table.newRow();
    table.newCell("align=center");
    table.add(makeInput(Input.Password, "Keystore Password",
			KEY_KEYSTORE_PASSWORD, keystorePassword));
//     table.newRow();
//     table.newCell("align=center");
//     table.add(ServletUtil.checkbox(this, KEY_USE_DEFAULT_KEYSTORE, "true",
// 				   "Include default keystore",
// 				   useDefaultKeystore));
    spaceRow(table);
    ServletUtil.layoutSubmitButton(this, table, ACTION_UPDATE);
    form.add(table);
    page.add(form);

    // Finish laying out page
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  void spaceRow(Table table) {
    table.newRow();
    table.newCell();
    table.add("&nbsp;");
  }

  private static final int URL_COLUMNS = 60;
  private static final int URL_ROWS = 8;

  Composite makeInput(String title, String fieldName, String value) {
    return makeInput(Input.Text, title, fieldName, value);
  }

  Composite makeInput(String type, String title,
		      String fieldName, String value) {
    Table table = new Table(0, "align=\"center\" cellpadding=\"0\"");
    Input in = new Input(type, fieldName, value);
    in.setSize(URL_COLUMNS);
    setTabOrder(in);
    table.newRow();
    table.addHeading(title, "align=\"center\"");
    table.newRow();
    table.newCell("align=center");
    table.add(in);
    return table;
  }

  Composite makeTextArea(String title, String fieldName, String value) {
    Table table = new Table(0, "align=\"center\" cellpadding=\"0\"");
    TextArea urlArea = new MyTextArea(fieldName);
    urlArea.setSize(URL_COLUMNS, URL_ROWS);
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
  protected void saveChanges() throws IOException {
    Properties props = new Properties();
    addConfigProps(props);
    log.info("saving: " + props);
    configMgr.writeCacheConfigFile(props,
				   ConfigManager.CONFIG_FILE_PLUGIN_CONFIG,
				   "Plugin and title DBs");
  }

  protected void addConfigProps(Properties props) {
    String plugStr = StringUtil.separatedString(pluginUrls, ";");
    String titleStr = StringUtil.separatedString(titleDbUrls, ";");

    props.put(PluginManager.PARAM_USER_PLUGIN_REGISTRIES, plugStr);
    props.put(ConfigManager.PARAM_USER_TITLE_DB_URLS, titleStr);
    if (keystoreUrl != null) {
      props.put(PluginManager.PARAM_USER_KEYSTORE_LOCATION, keystoreUrl);
      props.put(PluginManager.PARAM_USER_KEYSTORE_PASSWORD,
		keystorePassword);
    }
    props.put(PluginManager.PARAM_USE_DEFAULT_PLUGIN_REGISTRIES,
	      Boolean.toString(useDefaultPluginRegistries));
//     props.put(PluginManager.PARAM_USE_DEFAULT_KEYSTORE,
// 	      Boolean.toString(useDefaultKeystore));
  }


}
