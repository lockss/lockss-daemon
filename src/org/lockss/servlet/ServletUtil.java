/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.*;
import org.apache.commons.lang3.mutable.*;
import org.apache.commons.lang3.time.FastDateFormat;
import org.lockss.config.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.jetty.Button;
import org.lockss.jetty.MyTextArea;
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.repository.*;
import org.lockss.servlet.BatchAuConfig.Verb;
import org.lockss.util.*;
import org.mortbay.html.*;

public class ServletUtil {

  /**
   * <p>Keeps a link and an accompanying explanation of its purpose
   * conveniently together, for use in menus.</p>
   * @author Thib Guicherd-Callin
   * @see ServletUtil#layoutMenu(Page, Iterator)
   */
  public static class LinkWithExplanation {

    /**
     * <p>The text explaining what the linki does or where it goes.</p>
     */
    private String explanation;

    /**
     * <p>A link string.</p>
     */
    private String link;

    /**
     * <p>Builds a new link with explanation pair.</p>
     * @param link        A link string. May be a fragment of HTML
     *                    (most likely an anchor tag) or anything that
     *                    can be used in lieu of a link (for instance,
     *                    a fragment of gray text if the link is
     *                    disabled).
     * @param explanation Some text explaining the link (purpose,
     *                    target).
     */
    public LinkWithExplanation(String link, String explanation) {
      this.link = link;
      this.explanation = explanation;
    }

    /**
     * <p>Retrieves this link's explanation.</p>
     * @return This link's explanation string.
     */
    protected String getExplanation() {
      return explanation;
    }

    /**
     * <p>Retrieves this link's content.</p>
     * @return This link's content string.
     */
    protected String getLink() {
      return link;
    }

  }

  private static final Logger log = Logger.getLogger(ServletUtil.class);

  static final String PREFIX = Configuration.PREFIX + "ui.";

  /** Groups names not to display in header */
  static final String PARAM_DONT_DISPLAY_GROUPS = PREFIX + "dontDisplayGroups";
  static final List DEFAULT_DONT_DISPLAY_GROUPS =
    ConfigManager.DEFAULT_DAEMON_GROUP_LIST;

  /** Disabled servlets; list of servlet name or name:explanation */
  static final String PARAM_DISABLED_SERVLETS = PREFIX + "disabledServlets";
  static final List DEFAULT_DISABLED_SERVLETS = Collections.EMPTY_LIST;

  /** URL of third party logo image */
  static final String PARAM_THIRD_PARTY_LOGO_IMAGE = PREFIX + "logo.img";
  
  /** loading spinner image for subscription management page **/
  static final String LOADING_SPINNER = "images/ajax-loader.gif";
  
  /** URL of third party logo link */
  static final String PARAM_THIRD_PARTY_LOGO_LINK = PREFIX + "logo.link";

  /** If true, Identity IP will be displayed in header along with hostname */
  static final String PARAM_DISPLAY_IP_ADDR = PREFIX + "displayIpAddr";
  static final boolean DEFAULT_DISPLAY_IP_ADDR = false;

  /** If true, page title will start with hostname.  Useful with multiple
   * tabs open on different LOCKSS boxes. */
  static final String PARAM_HOSTNAME_IN_TITLE = PREFIX + "hostNameInTitle";
  static final boolean DEFAULT_HOSTNAME_IN_TITLE = false;

  /** Format to display date/time in headers */
  public static final DateFormat headerDf =
    new SimpleDateFormat("HH:mm:ss MM/dd/yy");

  static final Image IMAGE_LOGO_LARGE =
    image("lockss-logo-large.gif", 160, 160, 0);

  static final Image IMAGE_LOGO_SMALL =
    image("lockss-logo-small.gif", 80, 81, 0);

  /* private */static final Image IMAGE_TM =
    image("tm.gif", 16, 16, 0);

  private static final String ALIGN_CENTER = "align=\"center\""; /* (a) */

  private static final String ALIGN_LEFT = "align=\"left\""; /* (a) */

  private static final String ALIGN_RIGHT = "align=\"right\""; /* (a) */

  private static final String ALLOWDENY_CELL_ATTRIBUTES =
    ALIGN_CENTER;

  private static final int ALLOWDENY_COLUMNS = 30;

  private static final int ALLOWDENY_LINES = 15;

  private static final String ALLOWDENY_ROW_ATTRIBUTES =
    "bgcolor=\"#cccccc\"";

  private static final String ALLOWDENY_TABLE_ATTRIBUTES =
    "align=\"center\" cellpadding=\"0\"";

  private static final int ALLOWDENY_TABLE_BORDER = 1;

  private static final String ALLOWDENYERRORS_AFTER =
    "</font><br>";

  private static final String ALLOWDENYERRORS_BEFORE =
    "<font color=\"red\">";

  private static final String AUBUTTONS_TABLE_ATTRIBUTES = "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int AUBUTTONS_TABLE_BORDER = 0;

  private static final String AUPROPS_CELL_ATTRIBUTES =
    "colspan=\"2\" align=\"center\"";

  private static final String AUPROPS_PREFIX = "lfp.";

  private static final String AUPROPS_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int AUPROPS_TABLE_BORDER = 0;

  private static final String AUSTATUS_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int AUSTATUS_TABLE_BORDER = 0;

  private static final String AUSUMMARY_BUTTONCELL_ATTRIBUTES =
    "align=\"right\" valign=\"center\"";

  private static final String AUSUMMARY_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int AUSUMMARY_TABLE_BORDER = 0;

  private static final String AUSUMMARY_TEXTCELL_ATTRIBUTES =
    "valign=\"center\"";

  private static final String BACKLINK_AFTER =
    "</center>";

  private static final String BACKLINK_BEFORE =
    "<center>";

  private static final String CHOOSEAUS_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int CHOOSEAUS_BORDER = 0;

  private static final String CHOOSEAUS_CELL_ATTRIBUTES =
    "align=\"right\" valign=\"center\"";

  private static final String CHOOSEAUS_NONOPERABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int CHOOSEAUS_NONOPERABLE_BORDER = 0;

  private static final String CHOOSESETS_BUTTONROW_ATTRIBUTES =
    "align=\"center\" colspan=\"2\"";

  private static final String CHOOSESETS_CELL_ATTRIBUTES =
    "valign=\"center\"";

  private static final String CHOOSESETS_CHECKBOX_ATTRIBUTES =
    "align=\"right\" valign=\"center\"";

  private static final String CHOOSESETS_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int CHOOSESETS_TABLE_BORDER = 0;

  private static final String ENABLEPORT_CELL_ATTRIBUTES =
    ALIGN_CENTER;

  private static final String ENABLEPORT_TABLE_ATTRIBUTES =
    "align=\"center\" cellpadding=\"4\"";

  private static final int ENABLEPORT_TABLE_BORDER = 0;

  private static final String ERRORBLOCK_ERROR_AFTER =
    "</font></center><br>";

  private static final String ERRORBLOCK_ERROR_BEFORE =
    "<center><font color=\"red\" size=\"+1\">";

  private static final String ERRORBLOCK_STATUS_AFTER =
    "</font></center><br>";

  private static final String ERRORBLOCK_STATUS_BEFORE =
    "<center><font size=\"+1\">";

  private static final String EXPLANATION_CELL_ATTRIBUTES =
    ALIGN_CENTER;

  private static final String EXPLANATION_TABLE_ATTRIBUTES =
    "width=\"85%\"";

  private static final int EXPLANATION_TABLE_BORDER = 0;

  private static final String FOOTER_ATTRIBUTES =
    "cellspacing=\"0\" cellpadding=\"0\" align=\"center\"";

  private static final int FOOTER_BORDER = 0;

  private static final String HEADER_HEADING_AFTER =
    "</b></font>";

  private static final String HEADER_HEADING_BEFORE =
    "<font size=\"+2\"><b>";

  private static final String HEADER_TABLE_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"0\" width=\"100%\"";

  private static final int HEADER_TABLE_BORDER = 0;

  private static final Image IMAGE_LOCKSS_RED =
    image("lockss-type-red.gif", 595, 31, 0);

  private static final String MENU_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"4\" align=\"center\"";

  private static final String MENU_ITEM_AFTER =
    "</font>";

  private static final String MENU_ITEM_BEFORE =
    "<font size=\"+1\">";

  private static final String MENU_ROW_ATTRIBUTES =
    "valign=\"top\"";

  private static final int MENU_TABLE_BORDER = 0;

  private static final String NAVTABLE_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"0\"";

  private static final int NAVTABLE_TABLE_BORDER = 0;

  private static final String NOTES_BEGIN =
    "<p><b>Notes:</b>";

  private static final String NOTES_LIST_AFTER =
    "</font></ol>";

  private static final String NOTES_LIST_BEFORE =
    "<ol><font size=\"-1\">";

  private static final String PAGE_BGCOLOR =
    Constants.COLOR_WHITE;

  private static final String REPOCHOICE_CELL_ATTRIBUTES =
    "colspan=\"4\" align=\"center\"";

  private static final String REPOCHOICE_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int REPOCHOICE_TABLE_BORDER = 0;

  private static final String REPOTABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int REPOTABLE_BORDER = 0;

  private static final String REPOTABLE_ROW_ATTRIBUTES =
    ALIGN_CENTER;

  private static final String RESTORE_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int RESTORE_TABLE_BORDER = 0;

  private static final String SELECTALL_ATTRIBUTES =
    "cellspacing=\"4\" cellpadding=\"0\"";

  private static final int SELECTALL_BORDER = 0;

  private static final String SPACE = "&nbsp;"; /* (a) */

  private static final String CENTRE_CLOSE =
    "</center>";

  private static final String CENTRE_OPEN =
      "<center>";

  private static final String BREAK =
      "<br/>";

  private static final boolean BTN_NEWLINE_DEFAULT = true;
  private static final boolean BTN_CENTRE_DEFAULT = true;

  private static volatile String thirdPartyLogo;
  private static volatile String thirdPartyLogoLink;
  private static volatile boolean displayIpAddr = DEFAULT_DISPLAY_IP_ADDR;
  static volatile boolean hostNameInTitle = DEFAULT_HOSTNAME_IN_TITLE;

  private static Map<String,String> disabledServlets = new HashMap();

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(ServeContent.PREFIX)) {
      ServeContent.setConfig(config, oldConfig, diffs);
    }
    if (diffs.contains(HashCUS.PREFIX)) {
      HashCUS.setConfig(config, oldConfig, diffs);
    }
    if (diffs.contains(PREFIX)) {
      thirdPartyLogo = config.get(PARAM_THIRD_PARTY_LOGO_IMAGE);
      if (thirdPartyLogo != null) {
	thirdPartyLogoLink = config.get(PARAM_THIRD_PARTY_LOGO_LINK);
      }
      displayIpAddr = config.getBoolean(PARAM_DISPLAY_IP_ADDR,
					DEFAULT_DISPLAY_IP_ADDR);
      hostNameInTitle = config.getBoolean(PARAM_HOSTNAME_IN_TITLE,
					  DEFAULT_HOSTNAME_IN_TITLE);
      if (diffs.contains(PARAM_DISABLED_SERVLETS)) {
	List<String> dis = config.getList(PARAM_DISABLED_SERVLETS,
					  DEFAULT_DISABLED_SERVLETS);
	disabledServlets.clear();
	for (String s : dis) {
	  String exp = "";
	  int pos = s.indexOf(':');
	  if (pos > 0) {
	    exp = s.substring(pos + 1);
	    s = s.substring(0, pos);
	  }
	  disabledServlets.put(s, exp);
	}
      }
    }
  }

  public static String servletDisabledReason(String servlet) {
    return disabledServlets.get(servlet);
  }

  public static boolean isHostNameInTitle() {
    return hostNameInTitle;
  }

  /** Return the URL of this machine's config backup file to download */
  // This is a crock.  It's called from RemoteAPI, which has no servlet
  // instance and thus can't use LockssServlet.srvURL().
  public static String backupFileUrl(String hostname) {
    ServletDescr backupServlet = AdminServletManager.SERVLET_BATCH_AU_CONFIG;
    int port = CurrentConfig.getIntParam(AdminServletManager.PARAM_PORT,
					 AdminServletManager.DEFAULT_PORT);
    StringBuffer sb = new StringBuffer();
    sb.append("http://");
    sb.append(hostname);
    sb.append(":");
    sb.append(port);
    sb.append("/");
    sb.append(backupServlet.getPath());
    sb.append("?");
    sb.append(BatchAuConfig.ACTION_TAG);
    sb.append("=");
    sb.append(BatchAuConfig.ACTION_BACKUP);
    return sb.toString();
  }

  public static void addNotes(Composite comp, Iterator notesIterator) {
    if (notesIterator != null && notesIterator.hasNext()) {
      // if there are footnotes
      comp.add(NOTES_BEGIN);
      comp.add(NOTES_LIST_BEFORE);
      for (int nth = 1 ; notesIterator.hasNext() ; nth++) {
        layoutFootnote(comp, (String)notesIterator.next(), nth);
      }
      comp.add(NOTES_LIST_AFTER);
    }
  }

  // Common page footer
  public static void doLayoutFooter(Page page,
                                  Iterator notesIterator,
                                  String versionInfo) {
    Composite comp = new Composite();

    addNotes(comp, notesIterator);

    comp.add("<p>");

    Table table = new Table(FOOTER_BORDER, FOOTER_ATTRIBUTES);
    table.newRow("valign=\"top\"");
    table.newCell();
    table.add(IMAGE_LOCKSS_RED);
    table.newCell();
    table.add(IMAGE_TM);
    table.newRow();
    table.newCell("colspan=\"2\"");
    table.add("<center><font size=\"-1\">" + versionInfo + "</font></center>");
    comp.add(table);
    page.add(comp);
  }

  public static Page doNewPage(String pageTitle,
                               boolean isFramed) {
    Page page = new Page();
    layoutPageHeaders(page);

    if (isFramed) {
      page.addHeader("<base target=\"_top\">");
    }
    page.title(pageTitle);

    page.attribute("bgcolor", PAGE_BGCOLOR);
    return page;
  }

  /** Add html tags to grey the text */
  public static String gray(String txt) {
    return gray(txt, true);
  }

  public static Image image(String file,
                            int width,
                            int height,
                            int border) {
    return new Image("/images/" + file, width, height, border);
  }

  /** create an image that will display the tooltip on mouse hover */
  public static Image image(String file,
                            int width,
                            int height,
                            int border,
                            String tooltip) {
    Image img = image(file, width, height, border);
    img.alt(tooltip);			// some browsers (IE) use alt tag
    img.attribute("title", tooltip);	// some (Mozilla) use title tag
    return img;
  }

  public static void layoutAuId(Composite comp,
                                AuProxy au,
                                String auIdName) {
    comp.add(new Input(Input.Hidden,
                       auIdName,
                       au != null ? au.getAuId() : ""));
  }

  public static void layoutAuPropsButtons(LockssServlet servlet,
                                          Composite comp,
                                          Iterator actionIter,
                                          String actionTag) {
    // Make a one-cell table
    Table btnsTbl = new Table(AUBUTTONS_TABLE_BORDER, AUBUTTONS_TABLE_ATTRIBUTES);
    btnsTbl.newRow();
    btnsTbl.newCell(ALIGN_CENTER);

    while (actionIter.hasNext()) {
      Object act = actionIter.next();
      if (act instanceof String) {
        btnsTbl.add(servlet.setTabOrder(
            new Input(Input.Submit, actionTag, (String)act)));
      }
      else {
        if (act instanceof Element) {
          Element ele = (Element)act;
          servlet.setTabOrder(ele);
        }
        btnsTbl.add(act);
      }

      if (actionIter.hasNext()) {
        btnsTbl.add(SPACE);
      }
    }

    comp.add(btnsTbl);
  }

  public static void layoutAuPropsTable(LockssServlet servlet,
                                        Composite comp,
                                        Collection configParamDescrs,
                                        Collection defKeys,
                                        Configuration initVals,
                                        Collection noEditKeys,
                                        boolean isNew,
                                        Collection editKeys,
                                        boolean editable) {

    Table tbl = new Table(AUPROPS_TABLE_BORDER, AUPROPS_TABLE_ATTRIBUTES);

    tbl.newRow();
    tbl.newCell(AUPROPS_CELL_ATTRIBUTES);
    tbl.add("Archival Unit Definition");
    layoutAuPropRows(servlet,
                    tbl,
                    configParamDescrs.iterator(),
                    defKeys,
                    initVals,
                    isNew ? CollectionUtils.subtract(defKeys, noEditKeys) : null);

    tbl.newRow();
    if (editKeys.isEmpty()) {
      if (!isNew && editable) {
        // nothing
      }
    }
    else {
      tbl.newRow();
      tbl.newCell(AUPROPS_CELL_ATTRIBUTES);
      tbl.add("Other Parameters");
      layoutAuPropRows(servlet,
                       tbl,
                       configParamDescrs.iterator(),
                       editKeys,
                       initVals,
                       editable ? editKeys : null);
    }

    comp.add(tbl);
  }

  public static void layoutAuStatus(LockssServlet servlet,
				    Page page,
				    List<BatchAuStatus.Entry> auStatusList) {
    Set userMessages = new HashSet();
    Table tbl = new Table(AUSTATUS_TABLE_BORDER, AUSTATUS_TABLE_ATTRIBUTES);
    tbl.addHeading("Status");
    tbl.addHeading("Archival Unit");
    for (BatchAuStatus.Entry stat : auStatusList) {
      tbl.newRow();
      tbl.newCell();
      tbl.add(SPACE);
      tbl.add(stat.getStatus());
      tbl.add(SPACE);
      tbl.newCell();
      String name = stat.getName();
      tbl.add(name != null ? encodeText(name) : stat.getAuId());
      String exp = stat.getExplanation();
      String umsg = stat.getUserMessage();
      if (exp != null || umsg != null) {
	StringBuilder sb = new StringBuilder();
	if (exp != null) {
	  sb.append(exp);
	}
	if (umsg != null) {
	  sb.append("See note");
	  sb.append(servlet.addFootnote(umsg));
	}
        tbl.newCell();
        tbl.add(sb.toString());
      }
      if (stat.getUserMessage() != null) {
	userMessages.add(stat.getUserMessage());
      }
    }
    if (!userMessages.isEmpty()) {
      layoutExplanationBlock(page,
			     "<font color=\"red\" size=\"+1\">" +
			     "Some of the titles you just configured may require additional action.  Please see the notes at the bottom of this page." +
			     "</font>");
    }
    page.add(tbl);
  }

  /**
   * <p>Lays out an HTML form onto the given page, with a button to
   * add an AU, and many buttons to restore, reactivate or edit
   * AUs.</p>
   * @param servlet             The servlet building the form.
   * @param buttonNumber        The servlet's button counter.
   * @param remoteApi           A reference to the remote API.
   * @param page                The page onto which the form will be
   *                            built.
   * @param formUrl             The form's POST URL.
   * @param formId              The form's identifier.
   * @param tableId             The table's identifier.
   * @param hiddenActionName    The action parameter name.
   * @param activeAuProxyIter   An iterator of {@link AuProxy}
   *                            instances for the active AUs.
   * @param inactiveAuProxyIter An iterator of {@link AuProxy}
   *                            instances for the inactive AUs.
   * @param auIdName            The AU ID parameter name.
   * @param addAction           The "add" action name.
   * @param restoreAction       The "restore" action name.
   * @param reactivateAction    The "reactivate" action name.
   * @param editAction          The "edit" action name.
   */
  public static void layoutAuSummary(LockssServlet servlet,
                                     MutableInt buttonNumber,
                                     RemoteApi remoteApi,
                                     Page page,
                                     String formUrl,
                                     String formId,
                                     String tableId,
                                     String hiddenActionName,
                                     Iterator activeAuProxyIter,
                                     Iterator inactiveAuProxyIter,
                                     String auIdName,
                                     String addAction,
                                     String restoreAction,
                                     String reactivateAction,
                                     String editAction) {
    // Start form
    Form frm = newForm(formUrl);
    frm.attribute("id", formId);
    frm.add(new Input(Input.Hidden, hiddenActionName));
    frm.add(new Input(Input.Hidden, auIdName, ""));

    // Start table
    Table tbl = new Table(AUSUMMARY_TABLE_BORDER, AUSUMMARY_TABLE_ATTRIBUTES);
    tbl.attribute("id", tableId);
    tbl.newRow();
    tbl.newCell(AUSUMMARY_BUTTONCELL_ATTRIBUTES);
    tbl.add(submitButton(servlet, buttonNumber, "Add", addAction));
    tbl.newCell(AUSUMMARY_TEXTCELL_ATTRIBUTES);
    tbl.add("Add new Archival Unit");

    // Layout rows
    layoutAuSummaryRows(servlet, buttonNumber, remoteApi, tbl,
        activeAuProxyIter, auIdName, restoreAction,
        reactivateAction, editAction);
    layoutAuSummaryRows(servlet, buttonNumber, remoteApi, tbl,
        inactiveAuProxyIter, auIdName, restoreAction,
        reactivateAction, editAction);

    // End
    frm.add(tbl);
    page.add(frm);
  }

  public static void layoutBackLink(Composite comp,
                                    String destinationLink) {
    comp.add(BACKLINK_BEFORE);
    comp.add(destinationLink);
    comp.add(BACKLINK_AFTER);
  }

  public static void layoutChooseSets(String url,
                                      Page page,
                                      Composite chooseSets,
                                      String hiddenActionName,
                                      String hiddenVerbName,
                                      Verb verb) {
    Form frm = newForm(url);
    frm.add(new Input(Input.Hidden, hiddenActionName));
    frm.add(new Input(Input.Hidden, hiddenVerbName, verb.valStr));
    frm.add(chooseSets);
    page.add(frm);
  }

  public static void layoutEnablePortRow(LockssServlet servlet,
                                         Table table,
                                         String enableFieldName,
                                         boolean defaultEnable,
                                         String enableDescription,
                                         String enableFootnote,
                                         String filterFootnote,
                                         String portFieldName,
                                         String defaultPort,
                                         List usablePorts) {
    layoutEnablePortRow(servlet,
			table,
			enableFieldName,
			defaultEnable,
			enableDescription,
			enableFootnote,
			filterFootnote,
			portFieldName,
			null,
			defaultPort,
			null,
			usablePorts);
  }

  private static final String SSL_FOOT =
    "SSL port used internally on loopback interface.  -1 to disable";

  public static void layoutEnablePortRow(LockssServlet servlet,
                                         Table table,
                                         String enableFieldName,
                                         boolean defaultEnable,
                                         String enableDescription,
                                         String enableFootnote,
                                         String filterFootnote,
                                         String portFieldName,
                                         String sslPortFieldName,
                                         String defaultPort,
                                         String defaultSslPort,
                                         List usablePorts) {

    // Start row
    table.newRow();

    Input portElem = new Input(Input.Text, portFieldName, defaultPort);
    Input sslPortElem = null;
    // "enable" element
    Input enaElem = new Input(Input.Checkbox, enableFieldName, "1");
    if (defaultEnable) {
      enaElem.check();
    } else {
      portElem.attribute("disabled", "true");
    }
    String portFieldId = "id_" + portFieldName;
    String sslPortFieldId = null;
    if (sslPortFieldName != null) {
      sslPortElem = new Input(Input.Text, sslPortFieldName, defaultSslPort);
      if (!defaultEnable) {
	sslPortElem.attribute("disabled", "true");
      }
      sslPortFieldId = "id_" + sslPortFieldName;
      sslPortElem.setSize(6);
      sslPortElem.attribute("id", sslPortFieldId);
    }    
    enaElem.attribute("onchange",
		      "selectEnable(this,'" +
		      portFieldId + "','" + sslPortFieldId + "')");
    servlet.setTabOrder(enaElem);

    table.newCell("align=\"right\" valign=\"bottom\"");
    table.add(enaElem);
    table.add("Enable " + enableDescription);
    table.add(servlet.addFootnote(enableFootnote));
    table.add(" on port&nbsp;");

    // "port" element
    portElem.setSize(6);
    portElem.attribute("id", portFieldId);
    servlet.setTabOrder(portElem);
    table.add(portElem);
    if (sslPortElem != null) {
      table.newCell("align=\"left\" valign=\"bottom\"");
      table.add("SSL port&nbsp;");
      table.add(servlet.addFootnote(SSL_FOOT));
      table.add("&nbsp;");
      servlet.setTabOrder(sslPortElem);
      table.add(sslPortElem);
    }

    // List of usable ports
    if (usablePorts != null) {
      table.add("<br>");
      if (usablePorts.isEmpty()) {
        table.add("(No available ports)");
        table.add(servlet.addFootnote(enableFootnote));
      }
      else {
        table.add("Available ports");
        table.add(servlet.addFootnote(filterFootnote));
        table.add(": ");
        table.add(StringUtil.separatedString(usablePorts, ", "));
      }
    }
  }

  public static void layoutErrorBlock(Composite composite,
                                      String errMsg) {
    layoutErrorBlock(composite, errMsg, null);
  }

  public static void layoutErrorBlock(Composite composite,
                                      String errMsg,
                                      String statusMsg) {
    Composite block = new Composite();
    if (errMsg != null) {
      block.add(ERRORBLOCK_ERROR_BEFORE);
      block.add(encodeText(errMsg));
      block.add(ERRORBLOCK_ERROR_AFTER);
    }
    if (statusMsg != null) {
      block.add(ERRORBLOCK_STATUS_BEFORE);
      block.add(encodeText(statusMsg));
      block.add(ERRORBLOCK_STATUS_AFTER);
    }
    composite.add(block);
  }

  public static void layoutExplanationBlock(Composite composite,
                                            String text) {
    Table exp = new Table(EXPLANATION_TABLE_BORDER, EXPLANATION_TABLE_ATTRIBUTES);
    exp.center();
    exp.newCell(EXPLANATION_CELL_ATTRIBUTES);
    exp.add(text);
    composite.add(exp);
  }

  static void addBold(Composite comp, String str) {
    comp.add("<b>");
    comp.add(str);
    comp.add("</b>");
  }

  static boolean shouldDisplayGroups(List groups) {
    List dontGroups = CurrentConfig.getList(PARAM_DONT_DISPLAY_GROUPS,
					    DEFAULT_DONT_DISPLAY_GROUPS);
    return groups != null && !CollectionUtils.containsAny(groups, dontGroups);
  }

  public static void layoutHeader(LockssServlet servlet,
                                  Page page,
                                  String heading,
                                  boolean isLargeLogo,
                                  String machineName,
                                  String machineIpAddr,
                                  Date startDate,
                                  Iterator descrIterator) {
    Composite comp = new Composite();
    Table table = new Table(HEADER_TABLE_BORDER, HEADER_TABLE_ATTRIBUTES);
    Image logo = ((isLargeLogo && thirdPartyLogo == null)
		  ? IMAGE_LOGO_LARGE
		  : IMAGE_LOGO_SMALL);
    table.newRow();
    table.newCell("valign=\"top\" align=\"center\" width=\"20%\"");
    table.add(new Link(Constants.LOCKSS_HOME_URL, logo));
    table.add(IMAGE_TM);
    if (thirdPartyLogo != null) {
      Image img = new Image(thirdPartyLogo);
      img.border(0);
      table.add(new Link(thirdPartyLogoLink, img));
    }

    table.newCell("valign=\"top\" align=\"center\" width=\"60%\"");
    table.add("<br>");
    table.add(HEADER_HEADING_BEFORE);
    table.add(heading);
    table.add(HEADER_HEADING_AFTER);
    table.add("<br>");

    addBold(table, machineName);
    if (displayIpAddr && machineIpAddr != null) {
      table.add(" (");
      table.add(machineIpAddr);
      table.add(")");
    }
    List<String> groups = ConfigManager.getPlatformGroupList();
    if (shouldDisplayGroups(groups)) {
      table.add("&nbsp;&nbsp;(");
      if (groups.size() == 1) {
	addBold(table, groups.get(0));
	table.add(" group");
      } else {
	table.add(StringUtil.separatedDelimitedString(groups, ", ",
						      "<b>", "</b>"));
	table.add(" groups");
      }
      table.add(")");
    }
    table.add("<br>");

    String since =
      StringUtil.timeIntervalToString(TimeBase.msSince(startDate.getTime()));
    table.add(headerDf.format(new Date()));
    table.add(", up ");
    table.add(since);

    table.newCell("valign=\"center\" align=\"center\" width=\"20%\"");
    layoutNavTable(servlet, table, descrIterator);
    comp.add(table);
    comp.add("<br>");
    page.add(comp);
  }

  public static void layoutIpAllowDenyTable(LockssServlet servlet,
                                            Composite composite,
                                            List<String> allow,
                                            List<String> deny,
                                            String ipFootnote,
                                            List<String> allowErrs,
                                            List<String> denyErrs,
                                            String allowName,
                                            String denyName) {
    Table table = new Table(ALLOWDENY_TABLE_BORDER, ALLOWDENY_TABLE_ATTRIBUTES);

    table.newRow(ALLOWDENY_ROW_ATTRIBUTES);
    table.newCell(ALLOWDENY_CELL_ATTRIBUTES);
    table.add("<font size=\"+1\">Allow Access"
              + servlet.addFootnote(ipFootnote)
              + "&nbsp;</font>");

    table.newCell(ALLOWDENY_CELL_ATTRIBUTES);
    table.add("<font size=\"+1\">Deny Access"
              + servlet.addFootnote(ipFootnote)
              + "&nbsp;</font>");

    if ((allowErrs != null && allowErrs.size() > 0) ||
        (denyErrs != null && denyErrs.size() > 0)) {
      table.newRow();
      table.newCell();
      layoutIpAllowDenyErrors(table, allowErrs);
      table.newCell();
      layoutIpAllowDenyErrors(table, denyErrs);
    }

    String allowStr = StringUtil.terminatedSeparatedString(allow, "\n", "\n");
    String denyStr = StringUtil.terminatedSeparatedString(deny, "\n", "\n");

    TextArea incArea = new MyTextArea(allowName);
    incArea.setSize(ALLOWDENY_COLUMNS, ALLOWDENY_LINES);
    incArea.add(allowStr);

    TextArea excArea = new MyTextArea(denyName);
    excArea.setSize(ALLOWDENY_COLUMNS, ALLOWDENY_LINES);
    excArea.add(denyStr);

    table.newRow();
    table.newCell(ALLOWDENY_CELL_ATTRIBUTES);
    servlet.setTabOrder(incArea);
    table.add(incArea);
    table.newCell(ALLOWDENY_CELL_ATTRIBUTES);
    servlet.setTabOrder(excArea);
    table.add(excArea);
    composite.add(table);
  }

  public static void layoutMenu(Page page,
                                Iterator<LinkWithExplanation> linkIterator) {
    Table table = new Table(MENU_TABLE_BORDER, MENU_ATTRIBUTES);
    while (linkIterator.hasNext()) {
      LinkWithExplanation link = linkIterator.next();
      table.newRow(MENU_ROW_ATTRIBUTES);
      table.newCell();
      table.add(MENU_ITEM_BEFORE);
      table.add(link.getLink());
      table.add(MENU_ITEM_AFTER);
      table.newCell();
      table.add(link.getExplanation());
    }
    page.add(table);
  }

  public static void layoutPluginId(Composite comp,
                                    PluginProxy plugin,
                                    String pluginIdName) {
    comp.add(new Input(Input.Hidden,
                       pluginIdName,
                       plugin.getPluginId()));
  }

  public static void layoutRepoChoice(LockssServlet servlet,
                                      Composite comp,
                                      RemoteApi remoteApi,
                                      String repoFootnote,
                                      String repoTag) {
    RepositoryManager repoMgr =
      servlet.getLockssDaemon().getRepositoryManager();

    Table tbl = new Table(REPOCHOICE_TABLE_BORDER, REPOCHOICE_TABLE_ATTRIBUTES);
    List repos = remoteApi.getRepositoryList();
    boolean isChoice = repos.size() > 1;

    tbl.newRow();
    tbl.newCell(REPOCHOICE_CELL_ATTRIBUTES);
    if (isChoice) {
      tbl.add("Select Repository");
      tbl.add(servlet.addFootnote(repoFootnote));
    } else {
      tbl.add("Disk Space");
    }
    tbl.newRow();
    tbl.addHeading("Repository");
    tbl.addHeading("Size");
    tbl.addHeading("Free");
    tbl.addHeading("%Full");

    Map repoMap = remoteApi.getRepositoryMap();
    String mostFree = remoteApi.findLeastFullRepository(repoMap);
    for (Iterator iter = repoMap.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry entry = (Map.Entry)iter.next();
      String repo = (String)entry.getKey();
      PlatformUtil.DF df = (PlatformUtil.DF)entry.getValue();

      tbl.newRow();
      tbl.newCell(ALIGN_LEFT); // "Repository"
      if (isChoice) {
	tbl.add(radioButton(servlet, repoTag, repo, repo == mostFree));
      } else {
	tbl.add(repo);
      }
      addDfToRow(repoMgr, df, tbl);
    }

    comp.add(tbl);
  }

  static void addDfToRow(RepositoryManager repoMgr,
			 PlatformUtil.DF df, Table tbl) {
    if (df != null) {
      tbl.newCell(ALIGN_RIGHT); // "Size"
      tbl.add(SPACE);
      tbl.add(StringUtil.sizeKBToString(df.getSize()));
      tbl.newCell(ALIGN_RIGHT); // "Free"
      tbl.add(SPACE);
      tbl.add(diskSpaceColor(repoMgr, df,
			     StringUtil.sizeKBToString(df.getAvail())));
      tbl.newCell(ALIGN_RIGHT); // "%Full"
      tbl.add(SPACE);
      tbl.add(diskSpaceColor(repoMgr, df, df.getPercentString()));
    }
  }

  private static final Format backupFileDf =
    FastDateFormat.getInstance("HH:mm:ss MM/dd/yyyy");

  public static void layoutBackup(LockssServlet servlet,
				  Page page,
				  RemoteApi remoteApi,
				  String hiddenActionName,
				  String hiddenVerbName,
				  Verb verb,
				  MutableInt buttonNumber,
				  String backupFileButtonAction) {
    Form frm = newForm(servlet.srvURL(servlet.myServletDescr()));
    frm.add(new Input(Input.Hidden, hiddenActionName));
    frm.add(new Input(Input.Hidden, hiddenVerbName, verb.valStr));
    frm.add(new Input(Input.Hidden, "create"));
    String expl;
    Table tbl = new Table(RESTORE_TABLE_BORDER, RESTORE_TABLE_ATTRIBUTES);
    tbl.newRow();
    tbl.newCell(ALIGN_RIGHT);
    Element retrieveButton = submitButton(servlet, buttonNumber,
					  "Retrieve", backupFileButtonAction,
					  "create", "");
    tbl.add(retrieveButton);
    try {
      File permFile = remoteApi.getBackupFile();
      if (permFile.exists()) {
	tbl.newCell(ALIGN_LEFT);
	tbl.add(StringUtil.sizeToString(permFile.length()));
	tbl.add(" file created ");
	tbl.add(backupFileDf.format(permFile.lastModified()));
      } else {
	tbl.newCell(ALIGN_LEFT);
	tbl.add("(No backup file on disk)");
	retrieveButton.attribute("disabled", "true");
      }
    } catch (IOException e) {
      log.error("Error finding config backup file", e);
      tbl.newCell(ALIGN_LEFT);
      tbl.add("(Backup file not retrievable)");
      retrieveButton.attribute("disabled", "true");
    }
    tbl.newRow();
    tbl.newCell(ALIGN_RIGHT);
    tbl.add(submitButton(servlet, buttonNumber,
			 "Retrieve", backupFileButtonAction,
			 "create", "true"));
    tbl.newCell(ALIGN_LEFT);
    tbl.add("newly created file");
    frm.add(tbl);

    layoutExplanationBlock(page, "Retrieve the most recent backup file or create and retrieve a new one");
    page.add(frm);
  }

  public static void layoutRestore(LockssServlet servlet,
                                   Page page,
                                   String hiddenActionName,
                                   String hiddenVerbName,
                                   Verb verb,
                                   String backupFileFieldName,
                                   MutableInt buttonNumber,
                                   String backupFileButtonAction) {
    Form frm = newForm(servlet.srvURL(servlet.myServletDescr()));
    frm.attribute("enctype", "multipart/form-data");
    frm.add(new Input(Input.Hidden, hiddenActionName));
    frm.add(new Input(Input.Hidden, hiddenVerbName, verb.valStr));
    Table tbl = new Table(RESTORE_TABLE_BORDER, RESTORE_TABLE_ATTRIBUTES);
    tbl.newRow();
    tbl.newCell(ALIGN_CENTER);
    tbl.add("Enter name of AU configuration backup file");
    tbl.newRow();
    tbl.newCell(ALIGN_CENTER);
    tbl.add(new Input(Input.File, backupFileFieldName));
    tbl.newRow();
    tbl.newCell(ALIGN_CENTER);
    tbl.add(submitButton(servlet, buttonNumber, "Restore", backupFileButtonAction));
    frm.add(tbl);
    page.add(frm);
  }

  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
                                        String value,
                                        String label) {
    layoutSubmitButton(servlet, composite, "action", value, label);
  }

  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
                                        String key,
                                        String value,
                                        String label) {
    layoutSubmitButton(servlet, composite, key, value, label, BTN_NEWLINE_DEFAULT, BTN_CENTRE_DEFAULT);
  }

  /**
   * Layout a submit button with a preceding newline (HTML line break) if
   * specified.
   */
  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
                                        String key,
                                        String value,
                                        String label,
                                        boolean newline,
                                        boolean centre) {
    layoutButton(servlet, composite, key, value, Input.Submit, label, newline, centre);
  }

  public static void layoutResetButton(LockssServlet servlet,
                                       Composite composite,
                                       String key,
                                       String value,
                                       String label,
                                       boolean newline,
                                       boolean centre) {
    layoutButton(servlet, composite, key, value, Input.Reset, label, newline, centre);
  }

  /**
   * Layout a button of the specified type, optionally preceded by a line break.
   * The button will be an HTML <button> element so that it can be
   * internationalized (that is, it can have a distinct value and label). The
   * button element is not provided by the Jetty library, so in order to set
   * the tab order on it, we must either create one or increment the tabindex
   * independently of the setTabOrder method.
   *
   * @param servlet the servlet containing the button
   * @param composite the object to add the button to
   * @param key the name of the button
   * @param value the value of the button
   * @param type the type of the button
   * @param label the localised display label for the button
   * @param lineBreak whether to insert a line break before the button
   * @param centre whether to centre the button
   */
  public static void layoutButton(LockssServlet servlet,
                                  Composite composite,
                                  String key,
                                  String value,
                                  String type,
                                  String label,
                                  boolean lineBreak,
                                  boolean centre) {
    Button button = new Button(key, value, type, label);
    servlet.setTabOrder(button);
    if (lineBreak) composite.add(BREAK);
    if (centre) composite.add(CENTRE_OPEN);
    composite.add(button);
    if (centre) composite.add(CENTRE_CLOSE);
  }


  public static Composite makeChooseAus(LockssServlet servlet,
                                        Iterator basEntryIter,
                                        Verb verb,
                                        List repos,
                                        Map auConfs,
                                        String keyAuid,
                                        String keyRepo,
                                        String repoChoiceFootnote,
                                        String buttonText,
                                        MutableInt buttonNumber,
                                        boolean isLong) {
    boolean isAdd = verb.isAdd;
    boolean repoFlg = isAdd && repos.size() > 1;
    int reposSize = repoFlg ? repos.size() : 0;
    int maxCols = reposSize + (isAdd ? 3 : 2);
    Block repoFootElement = null;

    Table tbl = new Table(CHOOSEAUS_BORDER, CHOOSEAUS_ATTRIBUTES);

    if (isLong) {
      tbl.newRow();
      tbl.newCell(ALIGN_CENTER + " colspan=\"" + maxCols + "\"");
      tbl.add(submitButton(servlet, buttonNumber, buttonText, verb.action()));
    }

    tbl.newRow();
    tbl.newCell(ALIGN_LEFT + " colspan=\"99\"");
    Block selAllBlock1 = tbl.cell();

    tbl.newRow();
    tbl.addHeading(verb.cap + "?", ALIGN_RIGHT + " rowspan=\"2\"");
    if (repoFlg) {
      tbl.addHeading("Disk", ALIGN_CENTER + " colspan=\"" + reposSize + "\"");
      repoFootElement = tbl.cell();
    }
    tbl.addHeading("Archival Unit", ALIGN_CENTER + " rowspan=\"2\"");
    if (isAdd) {
      tbl.addHeading("Est. size (MB)", ALIGN_CENTER + " rowspan=\"2\"");
    }

    tbl.newRow(); // for rowspan=2 above
    for (int ix = 1; ix <= reposSize; ++ix) {
      tbl.addHeading(Integer.toString(ix), ALIGN_CENTER);
    }

    boolean isAnyAssignedRepo = false;
    boolean isAnyNotAssignedRepo = false;
    while (basEntryIter.hasNext()) {
      // Get next entry
      BatchAuStatus.Entry rs = (BatchAuStatus.Entry)basEntryIter.next();
      if (rs.isOk()) {
        String auid = rs.getAuId();
        tbl.newRow();

        tbl.newCell(CHOOSEAUS_CELL_ATTRIBUTES);
        auConfs.put(auid, rs.getConfig());
        Element cb = checkbox(servlet, keyAuid, auid, false);
        cb.attribute("onClick", "clickAu(event, this, this.form);");
        cb.attribute("class", "doall");
        tbl.add(cb);

	List existingRepoNames = rs.getRepoNames();
	String firstRepo = null;
	if (existingRepoNames != null && !existingRepoNames.isEmpty()) {
	  firstRepo = (String)existingRepoNames.get(0);
	  isAnyAssignedRepo = true;
	} else {
	  isAnyNotAssignedRepo = true;
	}
        if (repoFlg) {
          int ix = 1;
          for (Iterator riter = repos.iterator(); riter.hasNext(); ++ix) {
            String repo = (String)riter.next();
            tbl.newCell(ALIGN_CENTER);
            if (firstRepo == null || repo.equals(firstRepo)) {
	      Element rb = radioButton(servlet, keyRepo + "_" + auid,
				       Integer.toString(ix), null,
				       firstRepo != null,
				       PropUtil.fromArgs("class", "doall"));
	      tbl.add(rb);
	    }
          }
        }
        else if (firstRepo != null) {
	  // The Select On Disk button looks for entries with a
	  // defaultChecked radio button.  If no repo choice, add a hidden
	  // button just for that.
	  Block div = new Block(Block.Div, "style=\"display:none\"");
	  div.add(radioButton(servlet, keyRepo + "_" + auid,
			      "1", null, true));
	  tbl.add(div);
        }

        tbl.newCell();
        tbl.add(encodeText(rs.getName()));
        TitleConfig tc = rs.getTitleConfig();
        long est;
        if (isAdd && tc != null && (est = tc.getEstimatedSize()) != 0) {
          tbl.newCell(ALIGN_RIGHT);
          long mb = (est + (512 * 1024)) / (1024 * 1024);
          tbl.add(Long.toString(Math.max(mb, 1L)));
        }
      }
    }

    boolean includeOnDiskButton = isAnyAssignedRepo && isAnyNotAssignedRepo;
    selAllBlock1.add(layoutSelectAllButton(servlet, includeOnDiskButton));

    if (isLong) {
      tbl.newRow();
      tbl.newCell(ALIGN_LEFT + " colspan=\"99\"");
      tbl.add(layoutSelectAllButton(servlet, includeOnDiskButton));
    }

    if (repoFootElement != null && isAnyAssignedRepo) {
      repoFootElement.add(servlet.addFootnote(repoChoiceFootnote));
    }

    tbl.newRow();
    tbl.newCell(ALIGN_CENTER + " colspan=\"" + maxCols + "\"");
    tbl.add(submitButton(servlet, buttonNumber, buttonText, verb.action()));

    return tbl;
  }

  public static Composite makeChooseSets(LockssServlet servlet,
                                         RemoteApi remoteApi,
                                         Iterator titleSetIterator,
                                         Verb verb,
                                         String checkboxGroup,
                                         boolean doGray,
                                         MutableBoolean isAnySelectable,
                                         String submitText,
                                         String submitAction,
                                         MutableInt buttonNumber,
                                         int atLeast) {
    int actualRows = 0;
    isAnySelectable.setValue(false);
    Composite topRow;

    // Create table
    Table tbl = new Table(CHOOSESETS_TABLE_BORDER, CHOOSESETS_TABLE_ATTRIBUTES);

    // Create top row
    tbl.newRow();
    topRow = tbl.row();
    tbl.newCell(CHOOSESETS_BUTTONROW_ATTRIBUTES);
    tbl.add(submitButton(servlet, buttonNumber, submitText, submitAction));

    // Iterate over title sets
    while (titleSetIterator.hasNext()) {
      TitleSet set = (TitleSet)titleSetIterator.next();
      if (verb.isTsAppropriateFor(set)) {
        int numOk = verb.countAusInSetForVerb(remoteApi, set);
        if (numOk > 0 || doGray) {
          ++actualRows;
          tbl.newRow();
          tbl.newCell(CHOOSESETS_CHECKBOX_ATTRIBUTES);
          if (numOk > 0) {
            isAnySelectable.setValue(true);
            tbl.add(checkbox(servlet, checkboxGroup,
			     set.getId(), false));
          }
          tbl.newCell(CHOOSESETS_CELL_ATTRIBUTES);
          String txt = encodeText(set.getName()) + " (" + numOk + ")";
          tbl.add(numOk > 0 ? txt : gray(txt));
        }
      }
    }

    if (isAnySelectable.booleanValue()) {
      // Remove top row if unneeded
      if (actualRows < atLeast) {
        topRow.reset();
      }

      // Add bottom row
      tbl.newRow();
      tbl.newCell(CHOOSESETS_BUTTONROW_ATTRIBUTES);
      tbl.add(submitButton(servlet, buttonNumber, submitText, submitAction));
    }

    return tbl;
  }

  public static Composite makeNonOperableAuTable(String heading,
                                                 Iterator basEntryIter) {
      Composite comp = new Block(Block.Center);
      comp.add("<br>");
      comp.add(heading);

      Table tbl = new Table(CHOOSEAUS_NONOPERABLE_BORDER, CHOOSEAUS_NONOPERABLE_ATTRIBUTES);
      tbl.addHeading("Archival Unit");
      tbl.addHeading("Reason");

      while (basEntryIter.hasNext()) {
        BatchAuStatus.Entry rs = (BatchAuStatus.Entry)basEntryIter.next();
        if (!rs.isOk()) {
          tbl.newRow();
          tbl.newCell();
          tbl.add(encodeText(rs.getName()));
          tbl.newCell();
          tbl.add(rs.getExplanation());
        }
      }

      comp.add(tbl);
      return comp;
  }

  public static Element makeRepoTable(LockssServlet servlet,
				      RemoteApi remoteApi,
                                      Map<String,PlatformUtil.DF> repoMap,
                                      String keyDefaultRepo) {
    RepositoryManager repoMgr =
      servlet.getLockssDaemon().getRepositoryManager();
    boolean isChoice = repoMap.size() > 1;

    Table tbl = new Table(REPOTABLE_BORDER, REPOTABLE_ATTRIBUTES);
    tbl.newRow();
    if (isChoice) {
      tbl.addHeading("Available Disks", "colspan=\"6\"");
    } else {
      tbl.addHeading("Disk Space", "colspan=\"4\"");
    }
    tbl.newRow();
    if (isChoice) {
      tbl.addHeading("Use");
      tbl.addHeading("Disk");
    }
    tbl.addHeading("Location");
    tbl.addHeading("Size");
    tbl.addHeading("Free");
    tbl.addHeading("%Full");

    String mostFree =
      isChoice ? remoteApi.findLeastFullRepository(repoMap) : null;
    int ix = 0;
    // Populate repo key table
    for (Map.Entry<String,PlatformUtil.DF> entry : repoMap.entrySet()) {
      ix++;
      String repo = entry.getKey();
      PlatformUtil.DF df = entry.getValue();

      // Populate row for entry
      tbl.newRow(REPOTABLE_ROW_ATTRIBUTES);
      if (isChoice) {
	tbl.newCell(ALIGN_CENTER); // "Default"
	Element cb = checkbox(servlet, keyDefaultRepo, Integer.toString(ix),
			      repo == mostFree);
        cb.attribute("onChange", "resetRepoSelections();");
	tbl.add(cb);
	tbl.newCell(ALIGN_RIGHT); // "Disk"
	tbl.add(Integer.toString(ix) + "." + SPACE);
      }
      tbl.newCell(ALIGN_LEFT); // "Location"
      tbl.add(repo);
      addDfToRow(repoMgr, df, tbl);
    }

    tbl.newRow();
    tbl.newCell("colspan=\"6\"");
    tbl.add(Break.rule);
    return tbl;
  }

  static String diskSpaceColor(RepositoryManager repoMgr,
			       PlatformUtil.DF df, String s) {
    String color = null;
    if (df.isFullerThan(repoMgr.getDiskFullThreshold())) {
      color = Constants.COLOR_RED;
    } else if (df.isFullerThan(repoMgr.getDiskWarnThreshold())) {
      color = Constants.COLOR_ORANGE;
    }      
    if (color != null) {
      return "<font color=\"" + color + "\">" + s + "</font>";
    } else {
      return s;
    }
  }

  /**
   * <p>Begins a new {@link Table} suited for multiple "enable port"
   * rows.</p>
   * @return A new {@link Table} instance.
   * @see #layoutEnablePortRow
   */
  public static Table newEnablePortTable() {
    return new Table(ENABLEPORT_TABLE_BORDER, ENABLEPORT_TABLE_ATTRIBUTES);
  }

  /**
   * <p>Begins a new POST form with the given URL.</p>
   * @param url The form's URL.
   * @return A new form.
   */
  public static Form newForm(String url) {
    Form form = new Form(url);
    form.method("POST");
    return form;
  }

  /** Return a button that invokes the javascript submit routine with the
   * specified action */
  public static Element submitButton(LockssServlet servlet,
                                     MutableInt buttonNumber,
                                     String label,
                                     String action) {
    return submitButton(servlet, buttonNumber, label, action, null, null);
  }

  /** Return a (possibly labelled) checkbox.
   * @param servlet The servlet associated with the checkbox.
   * @param key     Form key to which result set is assigned.
   * @param value   Value included in result set if box checked.
   * @param checked If true, the checkbox is initially checked.
   * @return A checkbox {@link Element}.
   */
  public static Element checkbox(LockssServlet servlet,
                                  String key,
                                  String value,
                                  boolean checked) {
    return checkbox(servlet, key, value, null, checked);
  }

  /** Return a (possibly labelled) checkbox.
   * @param servlet The servlet associated with the checkbox.
   * @param key     Form key to which result set is assigned
   * @param value   Value included in result set if box checked
   * @param text    Appears to right of checkbox if non null
   * @param checked If true, box is initially checked
   * @return a checkbox Element
   */
  public static Element checkbox(LockssServlet servlet,
                                  String key,
                                  String value,
                                  String text,
                                  boolean checked) {
    return checkbox(servlet, key, value, text, checked, true);
  }

  /** Return a (possibly labelled) checkbox.
   * @param servlet The servlet associated with the checkbox.
   * @param key     Form key to which result set is assigned
   * @param value   Value included in result set if box checked
   * @param text    Appears to right of checkbox if non null
   * @param checked If true, box is initially checked
   * @param enabled If true, box is enabled; otherwise disabled and text grayed
   * @return a checkbox Element
   */
  public static Element checkbox(LockssServlet servlet,
      String key,
      String value,
      String text,
      boolean checked,
      boolean enabled) {
    Input in = new Input(Input.Checkbox, key, value);
    if (!enabled) { in.attribute("disabled", "true"); }
    if (checked) { in.check(); }
    servlet.setTabOrder(in);
    if (StringUtil.isNullString(text)) {
      return in;
    }
    else {
      Composite c = new Composite();
      c.add(in); c.add(" "); 
      c.add(enabled ? text : gray(text));
      return c;
    }
  }
  
  private static String encodeAttr(String str) {
    return HtmlUtil.encode(str, HtmlUtil.ENCODE_ATTR);
  }

  private static String encodeText(String str) {
    return HtmlUtil.encode(str, HtmlUtil.ENCODE_TEXT);
  }

  private static String encodeTextArea(String str) {
    return HtmlUtil.encode(str, HtmlUtil.ENCODE_TEXTAREA);
  }

  /** Add html tags to grey the text if isGrey is true */
  private static String gray(String txt, boolean isGray) {
    if (isGray)
      return "<font color=\"gray\">" + txt + "</font>";
    else
      return txt;
  }


  /** A pattern for finding whitespace in a string. Immutable and thread-safe. */
  static final Pattern whitespacePattern = Pattern.compile("\\s+");

  /**
   * Constructs a filename for the file represented by the CachedUrl. The
   * filename consists of the name of the ArchivalUnit, followed by an
   * underscore and then the name of the file being downloaded. The filename
   * should have no spaces, so whitespace is replaced by underscores.
   * If <tt>withPath</tt> is true, the slash characters in the filename are also
   * translated to underscores.
   *
   * @param au the ArchivalUnit being accessed
   * @param cu the CachedUrl representing the AU resource
   * @param withPath whether to include the path component of the URL
   * @return a filename string with no spaces
   */
  static String makeContentFilename(ArchivalUnit au, CachedUrl cu, boolean withPath) {
    String auName = whitespacePattern.matcher(au.getName()).replaceAll("_");
    String filename;
    try {
      // Get the filename path from the URL, without any query string
      filename = new URL(cu.getUrl()).getPath();
      // Remove path component if requested
      if (!withPath) {
        int n = filename.lastIndexOf("/");
        filename = filename.substring(n<0?0:n+1);
      }
    } catch (MalformedURLException e) {
      filename = "unknown";
    }
    return auName+"_"+filename;
  }

  /**
   * Gets the original filename for the file represented by the CachedUrl.
   * This is the preferred method of getting a filename, as it honours the
   * original
   *
   * @param cu the CachedUrl representing the AU resource
   * @param quoted if true, the result will be enclosed in double quotes
   * @return the original filename string
   */
  static String getContentOriginalFilename(CachedUrl cu, boolean quoted) {
    String basename = StringUtil.basename(cu.getUrl());
    if (basename != null && quoted) {
      return  "\"" + basename + "\"";
    } else {
      return basename;
    }
  }

  /** Return a button that invokes javascript when clicked. */
  private static Input javascriptButton(LockssServlet servlet,
                                        String label,
                                        String js) {
    Input btn = new Input("button", null);
    btn.attribute("value", label);
    servlet.setTabOrder(btn);
    btn.attribute("onClick", js);
    return btn;
  }

  private static void layoutAuPropRows(LockssServlet servlet,
                                       Table tbl,
                                       Iterator configParamDescrIter,
                                       Collection keys,
                                       Configuration initVals,
                                       Collection editableKeys) {

    while (configParamDescrIter.hasNext()) {
      ConfigParamDescr descr = (ConfigParamDescr)configParamDescrIter.next();
      if (keys.contains(descr.getKey())) {
        String key = descr.getKey();

        tbl.newRow();
        tbl.newCell();
        tbl.add(encodeText(descr.getDisplayName()));
        tbl.add(servlet.addFootnote(encodeText(descr.getDescription())));
        tbl.add(": ");

        tbl.newCell();
        String val = initVals != null ? initVals.get(key) : null;

        if (editableKeys != null && editableKeys.contains(key)) {
          Input in = new Input(Input.Text,
                               AUPROPS_PREFIX + descr.getKey(),
                               encodeAttr(val));
          if (descr.getSize() != 0) {
            in.setSize(descr.getSize());
          }
          servlet.setTabOrder(in);
          tbl.add(in);
        }
        else {
          tbl.add(encodeText(val));
          tbl.add(new Input(Input.Hidden,
                            AUPROPS_PREFIX + descr.getKey(),
                            encodeAttr(val)));
        }
      }
    }
  }

  /**
   * <p>Lays out summary rows in the AU summary table, each row being
   * either "restore", "reactivate" or "edit" depending on the AU.</p>
   * @param servlet          The servlet building the form.
   * @param buttonNumber     The servlet's button counter.
   * @param remoteApi        A reference to the remote API.
   * @param tbl              The table into which rows will be added.
   * @param auProxyIter      An iterator of {@link AuProxy}
   *                         instances for the AUs.
   * @param auIdName         The AU ID parameter name.
   * @param restoreAction    The "restore" action name.
   * @param reactivateAction The "reactivate" action name.
   * @param editAction       The "edit" action name.

   * @see #layoutAuSummary(LockssServlet, MutableInt, RemoteApi, Page, String, String, String, String, Iterator, Iterator, String, String, String, String, String)
   */
  private static void layoutAuSummaryRows(LockssServlet servlet,
                                          MutableInt buttonNumber,
                                          RemoteApi remoteApi,
                                          Table tbl,
                                          Iterator auProxyIter,
                                          String auIdName,
                                          String restoreAction,
                                          String reactivateAction,
                                          String editAction) {
    while (auProxyIter.hasNext()) {
      AuProxy au = (AuProxy)auProxyIter.next();
      Configuration cfg = remoteApi.getStoredAuConfiguration(au);
      boolean isGray = true;
      String act;

      if (cfg.isEmpty()) {
        act = restoreAction;
      }
      else if (cfg.getBoolean(PluginManager.AU_PARAM_DISABLED, false)) {
        act = reactivateAction;
      }
      else {
        act = editAction;
        isGray = false;
      }

      tbl.newRow();
      tbl.newCell(AUSUMMARY_BUTTONCELL_ATTRIBUTES);
      tbl.add(submitButton(servlet, buttonNumber, act, act, auIdName, au.getAuId()));
      tbl.newCell(AUSUMMARY_TEXTCELL_ATTRIBUTES);
      tbl.add(gray(encodeText(au.getName()), isGray));
    }
  }

  private static void layoutFootnote(Composite comp,
                                     String footnote,
                                     int nth) {
    comp.add("<li value=\"" + nth + "\">");
    comp.add("<a name=\"foottag" + nth + "\">");
    comp.add(footnote);
    comp.add("</a></li>");
  }

  private static void layoutIpAllowDenyErrors(Composite composite,
                                              List<String> errs) {
    int size;
    if (errs != null && (size = errs.size()) > 0) {
      composite.add(ALLOWDENYERRORS_BEFORE);
      composite.add(Integer.toString(size));
      composite.add(size == 1 ? " error/warning" : " errors/warnings");
      composite.add(ALLOWDENYERRORS_AFTER);
      for (Iterator iter = errs.iterator() ; iter.hasNext() ; ) {
        composite.add((String)iter.next());
        composite.add("<br>");
      }
    }
    else {
      composite.add("&nbsp");
    }
  }

  // Build servlet navigation table
  private static void layoutNavTable(LockssServlet servlet,
                                     Table outerTable,
                                     Iterator descrIterator) {
    final String NAVTABLE_CELL_WIDTH = "width=\"15\"";

    Table navTable = new Table(NAVTABLE_TABLE_BORDER, NAVTABLE_ATTRIBUTES);

    while (descrIterator.hasNext()) {
      ServletDescr d = (ServletDescr)descrIterator.next();
      navTable.newRow();
      navTable.newCell("colspan=\"3\"");
      if (false /*isThisServlet(d)*/) {
        navTable.add("<font size=\"-1\" color=\"green\">");
      } else {
        navTable.add("<font size=\"-1\">");
      }
      navTable.add(servlet.conditionalSrvLink(d, d.getNavHeading(servlet),
					      servlet.isServletLinkInNav(d)));
      navTable.add("</font>");
    }
    navTable.add("</font>");
    outerTable.add(navTable);
  }

  private static void layoutPageHeaders(Page page) {
//     page.addHeader("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
//     page.addHeader("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    page.addHeader("<link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\" />");
    page.addHeader(  "<style type=\"text/css\"> <!--\n"
                   + "sup {font-weight: normal; vertical-align: super; }\n"
                   + "a.colhead, a.colhead:link, a.colhead:visited { text-decoration: none; font-weight: bold; color: blue; }\n"
                   + "td.colhead { font-weight: bold; background: #e0e0e0; }\n"
                   + "div.resize { resize: both; overflow: auto; }\n"
                   + "--> </style>");
  }

  private static Composite layoutSelectAllButton(LockssServlet servlet,
						 boolean includeOnDiskButton) {
    Table tbl = new Table(SELECTALL_BORDER, SELECTALL_ATTRIBUTES);
    tbl.newRow();
    tbl.newCell();
    tbl.add(javascriptButton(servlet, "Select All", "selectAll(this.form, 'all');"));
//     tbl.newRow();
//     tbl.newCell(ALIGN_RIGHT);
    tbl.add(javascriptButton(servlet, "Clear All", "selectAll(this.form, 'clear');"));
    if (includeOnDiskButton) {
//       tbl.newRow();
//       tbl.newCell(ALIGN_RIGHT);
      tbl.add(javascriptButton(servlet, "On Disk", "selectAll(this.form, 'inRepo');"));
    }
    return tbl;
  }

  public static Composite layoutSelectAu(LockssServlet servlet, String key,
					 String preselId) {
    Select sel = new Select(key, false);
    sel.add("", preselId == null, "");
    PluginManager pluginMgr = servlet.getLockssDaemon().getPluginManager();
    for (Iterator iter = pluginMgr.getAllAus().iterator(); iter.hasNext(); ) {
      ArchivalUnit au0 = (ArchivalUnit)iter.next();
      String id = au0.getAuId();
      sel.add(encodeAttr(au0.getName()), id.equals(preselId), id);
    }
    return sel;
  }

  private static String multiline(String str) {
    return str.replaceAll("\n", "<br>");
  }

  public static Element radioButton(LockssServlet servlet,
				    String key,
				    String value,
				    boolean checked) {
    return radioButton(servlet, key, value, value, checked);
  }

  public static Element radioButton(LockssServlet servlet,
      				    String key,
      				    String value,
      				    boolean checked,
      				    boolean enabled) {
    return radioButton(servlet, key, value, value, checked, enabled);
  }

  public static Element radioButton(LockssServlet servlet,
				    String key,
				    String value,
				    String text,
				    boolean checked) {
    return radioButton(servlet, key, value, text, checked, null);
  }

  public static Element radioButton(LockssServlet servlet,
	                            String key,
	                            String value,
	                            String text,
	                            boolean checked,
	                            boolean enabled) {
    return radioButton(servlet, key, value, text, checked, enabled, null);
}

  public static Element radioButton(LockssServlet servlet,
				    String key,
				    String value,
				    String text,
				    boolean checked,
				    Properties attrs) {
    return radioButton(servlet, key, value, text, checked, true, attrs);
  }

  public static Element radioButton(LockssServlet servlet,
	     			    String key,
	     			    String value,
	     			    String text,
	     			    boolean checked,
	     			    boolean enabled,
	     			    Properties attrs) {
    Composite c = new Composite();
    Input in = new Input(Input.Radio, key, value);
    if (!enabled) { in.attribute("disabled", "true"); }
    if (checked) { in.check(); }
    servlet.setTabOrder(in);
    addAttrs(in, attrs);
    c.add(in); c.add(" ");
    c.add(enabled ? text : gray(text));
    return c;
  }

  static void addAttrs(Element elem, Properties attrs) {
    if (attrs != null) {
      for (Iterator iter = attrs.keySet().iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	String val = attrs.getProperty(key);
	elem.attribute(key, val);
      }
    }
  }

  /** Return a button that invokes the javascript submit routine with the
   * specified action, first storing the value in the specified form
   * prop. */
  private static Element submitButton(LockssServlet servlet,
                                      MutableInt buttonNumber,
                                      String label,
                                      String action,
                                      String prop,
                                      String value) {
    buttonNumber.add(1);
    StringBuffer sb = new StringBuffer(40);
    sb.append("lockssButton(this, '");
    sb.append(action);
    sb.append("'");
    if (prop != null && value != null) {
      sb.append(", '");
      sb.append(prop);
      sb.append("', '");
      sb.append(value);
      sb.append("'");
    }
    sb.append(")");
    Input btn = javascriptButton(servlet, label, sb.toString());
    btn.attribute("id", "lsb." + buttonNumber);
    return btn;
  }

  public static Element centeredBlock(Element ele) {
    Block blk = new Block(Block.Div);
    blk.attribute("align", "center");
    blk.add(ele);
    return blk;
  }

  public static Element notStartedWarning() {
    Composite warning = new Composite();
    warning.add("<center><font color=red size=+1>");
    warning.add("This LOCKSS box is still starting.  Table contents may be incomplete.");
    warning.add("</font></center><br>");
    return warning;
  }

  public static Element removeElementWithId(String id) {
    Composite ele = new Composite();
    ele.add("<script type=\"text/javascript\">\n");
    ele.add("<!--\nremoveElementId(\"");
    ele.add(id);
    ele.add("\")\n//-->\n</script>");
    return ele;
  }

  /** Return an index of all the manifest pages.  Used by the ProxyHandler;
   * here because it's convenient and easier to test */
  public static Element manifestIndex(LockssDaemon daemon, String hostname) {
    PluginManager pluginMgr = daemon.getPluginManager();
    StringBuffer sb = new StringBuffer();
    sb.append(HEADER_HEADING_BEFORE);
    sb.append("Volume Manifests on ");
    sb.append(hostname);
    sb.append(HEADER_HEADING_AFTER);
    return manifestIndex(pluginMgr, pluginMgr.getAllAus(), sb.toString());
  }

  /** Return an index of manifest pages for the given AUs. */
  public static Element manifestIndex(LockssDaemon daemon,
				      Collection<ArchivalUnit> aus) {
    return manifestIndex(daemon.getPluginManager(), aus, null);
  }

  /** Return an index of manifest pages for the given AUs. */
  public static Element manifestIndex(LockssDaemon daemon,
				      Collection<ArchivalUnit> aus,
				      String header) {
    return manifestIndex(daemon.getPluginManager(), aus, header);
  }

  /** Transform applied to URLs of manifest pages */
  public interface ManifestUrlTransform {
    public Object transformUrl(String url, ArchivalUnit au);
  }

  /** Return an index of manifest pages for the given AUs. */
  public static Element manifestIndex(PluginManager pluginMgr,
				      Collection<ArchivalUnit> aus,
				      String header) {
    return manifestIndex(pluginMgr,
			 aus,
			 header,
			 new ManifestUrlTransform(){
			   public Object transformUrl(String url,
						      ArchivalUnit au) {
			     return new Link(url, url);
			   }},
			 true);
  }

  public static Element manifestIndex(PluginManager pluginMgr,
				      Collection<ArchivalUnit> aus,
				      String header,
				      ManifestUrlTransform xform,
				      boolean checkCollected) {
    return manifestIndex(pluginMgr,
			 aus, null,
			 header,
			 xform,
			 checkCollected);
  }

  public static Element manifestIndex(PluginManager pluginMgr,
				      Collection<ArchivalUnit> aus,
				      Predicate pred,
				      String header,
				      ManifestUrlTransform xform,
				      boolean checkCollected) {
    Table tbl = new Table(AUSUMMARY_TABLE_BORDER,
			  "cellspacing=\"4\" cellpadding=\"0\"");
    if (header != null) {
      tbl.newRow();
      tbl.newCell("align=\"center\" colspan=\"3\"");
      tbl.add(header);
    }
    if (!pluginMgr.areAusStarted()) {
      tbl.newRow();
      tbl.newCell("align=\"center\" colspan=\"3\"");
      tbl.add(ServletUtil.notStartedWarning());
    }
    tbl.newRow();
    tbl.addHeading("Archival Unit", "align=left");
    tbl.newCell("width=8");
    tbl.add("&nbsp;");
    tbl.addHeading("Manifest", "align=left");
    for (ArchivalUnit au : aus) {
      if (pred != null && !pred.evaluate(au)) {
	continue;
      }
      tbl.newRow();
      tbl.newCell(ALIGN_LEFT);
      tbl.add(encodeText(au.getName()));
      tbl.newCell("width=8");
      tbl.add("&nbsp;");
      tbl.newCell(ALIGN_LEFT);
      try {
	Collection<String> urls = au.getAccessUrls();
	for (Iterator uiter = urls.iterator(); uiter.hasNext(); ) {
	  String url = (String)uiter.next();
	  tbl.add(xform.transformUrl(url, au));
	  if (checkCollected && !AuUtil.hasCrawled(au)) {
	    tbl.add(" (not fully collected)");
	  }
	  if (uiter.hasNext()) {
	    tbl.add("<br>");
	  }
	}
      } catch (RuntimeException e) {
	tbl.add("Plugin error: " + e.getMessage());
      }
    }
    return tbl;
  }

  /** Interface to link rewriting */
  public interface LinkTransform {
    public String rewrite(String url);
  }

  /**
   * Creates the table-containing tabs used to divide the display of content.
   * 
   * WP: The tabs for the subscription Add page are manages differently.
   * 
   * @param alphabetLetterCount
   *          An int with the count of the letters of the alphabet to be used.
   * @param lettersPerTabCount
   *          An int with the count of the letters per tab to be used.
   * @param columnHeaderNames
   *          A List<String> with the names of the column headers.
   * @param rowTitleCssClass
   *          A String with the CSS class to use for the row title.
   * @param columnHeaderCssClasses
   *          A List<String> with the CSS classes to use for the column headers.
   * @param tabLetterPopulationMap
   *          A Map<String, Boolean> with the indication of whether a letter
   *          used in a display tab has content.
   * @param tabsDiv
   *          A Block with the tabs container.
   * @return a Map<String, Table> with the tabs tables mapped by the initial
   *         letters they cover.
   */
  public static Map<String, Table> createTabsWithTable(int alphabetLetterCount,
      int lettersPerTabCount, List<String> columnHeaderNames,
      String rowTitleCssClass, List<String> columnHeaderCssClasses,
      Map<String, Boolean> tabLetterPopulationMap, Block tabsDiv,
      String action) {
    final String DEBUG_HEADER = "createTabsWithTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the map of tab letters.
    Map<Character, Character> tabLetters =
	createTabLettersMap(alphabetLetterCount, lettersPerTabCount);

    // Create the spans required by jQuery to build the desired tabs.
    org.mortbay.html.List tabList =
	createTabList(lettersPerTabCount, tabLetters, tabLetterPopulationMap, tabsDiv, action);

    // The start and end letters of a tab letter group.
    Map.Entry<Character, Character> letterPair;
    Character startLetter;
    Table divTable = null;
    Block tabDiv;
    Map<String, Table> divTableMap = new HashMap<String, Table>();
    Character tabLetter;

    Iterator<Map.Entry<Character, Character>> iterator =
	tabLetters.entrySet().iterator();

    // Loop through all the tabs letter groups.
    while (iterator.hasNext()) {
      // Get the first letter in the tab letter group.
      letterPair = iterator.next();
      startLetter = letterPair.getKey();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "startLetter = " + startLetter);

      if (isTabPopulated(lettersPerTabCount, startLetter,
	  tabLetterPopulationMap)) {
	// Create the table for the tab.
	divTable = createTabTable(startLetter.toString(), columnHeaderNames,
	    rowTitleCssClass, columnHeaderCssClasses);
      } else {
	divTable = new Table(0, "class=\"status-table\"");
      }

      // Create the tab for this letter group.
      tabDiv = new Block("div", "id=\"" + startLetter.toString() + "\"");

      // Map the tab table by the first letter.
      divTableMap.put(startLetter.toString(), divTable);

      // Loop through all the other letters in the tab.
      for (int j = 1; j < lettersPerTabCount; j++) {
        tabLetter = (char) (startLetter + j);
        if (log.isDebug3())
          log.debug3(DEBUG_HEADER + "j = " + j + ", tabLetter = " + tabLetter);

        // Map the tab table by this letter.
        divTableMap.put(tabLetter.toString(), divTable);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return divTableMap;
  }

  /**
   * Provides a map with a set of tabs start and end letters.
   * 
   * @param alphabetLetterCount
   *          An int with the count of the letters of the alphabet to be used.
   * @param lettersPerTabCount
   *          An int with the count of the letters per tab to be used.
   * @return a Map<Character, Character> with the tabs start and end letters.
   */
  private static Map<Character, Character> createTabLettersMap(
      int alphabetLetterCount, int lettersPerTabCount) {
    final String DEBUG_HEADER = "createTabLettersMap(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Initialize the resulting map, sorted by its natural order.
    Map<Character, Character> tabLetters = new TreeMap<Character, Character>();

    // The character position of the first letter ('A').
    int firstLetterCharacterPosition = 65;

    // The character position of the last letter.
    int lastLetterCharacterPosition = firstLetterCharacterPosition
	+ alphabetLetterCount - 1;

    // Determine how many tabs there are.
    int numberOfTabs = alphabetLetterCount / lettersPerTabCount;

    if (alphabetLetterCount % lettersPerTabCount != 0) {
      numberOfTabs++;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "numberOfTabs = " + numberOfTabs);

    // Loop through all the tabs.
    for (int i = 0; i < numberOfTabs; i++) {
      // The first letter of the tab.
      Character startLetter =
	  (char) (firstLetterCharacterPosition + i * lettersPerTabCount);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "startLetter = " + startLetter);

      // The last letter of the tab.
      Character endLetter =
	  (char) (Math.min(startLetter + lettersPerTabCount - 1,
	      		   lastLetterCharacterPosition));
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endLetter = " + endLetter);

      // Add the pair of letters in the tab to the map.
      tabLetters.put(startLetter, endLetter);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return tabLetters;
  }

  /**
   * Creates the spans required by jQuery to build the desired tabs.
   * 
   * WP: Made some changes to the tabs on the add subscription page 
   *     in order to load tabs content only when they are opened.
   * 
   * @param lettersPerTabCount
   *          An int with the count of the letters per tab to be used.
   * @param tabLetters
   *          A Map<Character, Character> with the tabs start and end letters.
   * @param tabLetterPopulationMap
   *          A Map<String, Boolean> with the indication of whether a letter
   *          used in a display tab has content.
   * @return an org.mortbay.html.List with the spans required by jQuery to build
   *         the desired tabs.
   */
  private static org.mortbay.html.List createTabList(int lettersPerTabCount,
      Map<Character, Character> tabLetters,
      Map<String, Boolean> tabLetterPopulationMap,
      Block tabsDiv,
      String action) {
    final String DEBUG_HEADER = "createTabList(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    org.mortbay.html.List tabList =
	new org.mortbay.html.List(org.mortbay.html.List.Unordered);

    // The start and end letters of a tab letter group.
    Map.Entry<Character, Character> letterPair;
    Character startLetter;
    Character endLetter;
    Block tabSpan;
    Link tabLink;
    Composite tabListItem;

    Iterator<Map.Entry<Character, Character>> iterator =
	tabLetters.entrySet().iterator();

    int tabCount = 1;
    List<Block> loadingDivs = new ArrayList<Block>();
    
    // Loop through all the tab letter groups.
    while (iterator.hasNext()) {
      // Get the start and end letters of the tab letter group.
      letterPair = iterator.next();
      startLetter = (Character) letterPair.getKey();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "startLetter = " + startLetter);

      endLetter = (Character) letterPair.getValue();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "endLetter = " + endLetter);

      // Initialize the tab.
      if (isTabPopulated(lettersPerTabCount, startLetter,
	  tabLetterPopulationMap)) {
	tabSpan = new Block(Block.Bold);
      } else {
	tabSpan = new Block(Block.Span);
      }

      // Add the label.
      if (!startLetter.equals(endLetter)) {
        tabSpan.add(startLetter + " - " + endLetter);
      } else {
        tabSpan.add(startLetter);
      }

      // Set up the tab link.
      tabLink = new Link("SubscriptionManagement?lockssAction=" + action + "&start=" + startLetter + "&amp;end=" + endLetter);

      tabLink.add(tabSpan);
      
      // Add the tab to the list.
      tabListItem = tabList.newItem();
      tabListItem.add(tabLink);
      
      // Add loading spinner image
      Block loadingDiv = new Block(Block.Div, "id='ui-tabs-" + tabCount++ + "'");
      Image loadingImage = new Image(LOADING_SPINNER);
      loadingImage.alt("Loading...");
      loadingDiv.add(loadingImage);
      loadingDiv.add(" Loading...");
      loadingDivs.add(loadingDiv);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    
    tabsDiv.add(tabList);
    for(Block loadingDiv : loadingDivs){
      tabsDiv.add(loadingDiv);
    }
    
    return tabList;
  }

  /**
   * Provides an indication of whether a tab is populated with content.
   * 
   * @param lettersPerTabCount
   *          An int with the count of the letters per tab to be used.
   * @param startLetter
   *          A Character with the first tab letter.
   * @param tabLetterPopulationMap
   *          A Map<String, Boolean> with the indication of whether a letter
   *          used in a display tab has content.
   * @return a boolean with <code>true</code> if the tab is populated with
   *         content, or <code>false</code> otherwise.
   */
  static boolean isTabPopulated(int lettersPerTabCount, Character startLetter,
      Map<String, Boolean> tabLetterPopulationMap) {
    final String DEBUG_HEADER = "isTabPopulated(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    boolean tabIsPopulated = false;

    // Loop through all the other letters in the tab.
    for (int j = 0; j < lettersPerTabCount; j++) {
      Character tabLetter = (char) (startLetter + j);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "j = " + j + ", tabLetter = " + tabLetter);

      // Check whether the letter is populated.
      if (tabLetterPopulationMap.get(tabLetter.toString()) != null) {
	// Yes: The tab is populated.
	tabIsPopulated = true;
	break;
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tabIsPopulated = " + tabIsPopulated);
    return tabIsPopulated;
  }

  /**
   * Creates the table for a tab.
   * 
   * WP: Need to be public to be called by SubscriptionManagement.populateTab
   * 
   * @param letter
   *          A String with the start letter of the tab group.
   * @param columnHeaderNames
   *          A List<String> with the names of the column headers.
   * @param rowTitleCssClass
   *          A String with the CSS class to use for the row title.
   * @param columnHeaderCssClasses
   *          A List<String> with the CSS classes to use for the column headers.
   * @return a Table to be added to the page.
   */
  public static Table createTabTable(String letter,
      List<String> columnHeaderNames, String rowTitleCssClass,
      List<String> columnHeaderCssClasses) {
    final String DEBUG_HEADER = "createTabTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "letter = " + letter);

    Table divTable = new Table(0, "class=\"status-table\"");
    divTable.newRow();
    if (StringUtil.isNullString(rowTitleCssClass)) {
      divTable.addCell("");
    } else {
      divTable.addCell("", "class=\"" + rowTitleCssClass + "\"");
    }

    Block columnHeader;
    Iterator<String> columnIterator = columnHeaderNames.listIterator();

    Iterator<String> columnCssIterator = null;

    if (columnHeaderCssClasses != null && columnHeaderCssClasses.size() > 0) {
      columnCssIterator = columnHeaderCssClasses.listIterator();
    }

    String cssClass = null;

    // Loop through all the columns in the table.
    while (columnIterator.hasNext()) {
      // Create the column header.
      columnHeader = new Block(Block.Bold);
      columnHeader.add(columnIterator.next());

      // Get the column CSS class, if any.
      if (columnCssIterator != null && columnCssIterator.hasNext()) {
	cssClass = columnCssIterator.next();
      }

      // Add the column header to the table.
      if (StringUtil.isNullString(cssClass)) {
	divTable.addCell(columnHeader);
      } else {
	divTable.addCell(columnHeader, "class=\"" + cssClass + "\"");
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return divTable;
  }

  /**
   * Provides a CSS class based on the row number and the size of a row block.
   * 
   * @param rowIndex
   *          An int with the row number.
   * @param blockSize
   *          An int with the size of a row block.
   * @return a String with the CSS class for the row.
   */
  public static String rowCss(int rowIndex, int blockSize) {
    return (rowIndex % (2 * blockSize) < blockSize) ? "even-row" : "odd-row";
  }
}
