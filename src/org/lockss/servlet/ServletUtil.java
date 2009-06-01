/*
 * $Id: ServletUtil.java,v 1.59 2009-06-01 07:53:32 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;
import java.util.*;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.*;
import org.apache.commons.collections.map.*;
import org.apache.commons.lang.mutable.*;
import org.lockss.config.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
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

  static Logger log = Logger.getLogger("ServletUtil");

  /** Groups names not to display in header */
  static final String PARAM_DONT_DISPLAY_GROUPS =
    Configuration.PREFIX + "ui.dontDisplayGroups";
  static final List DEFAULT_DONT_DISPLAY_GROUPS =
    ConfigManager.DEFAULT_DAEMON_GROUP_LIST;

  /** Disabled servlets; list of servlet name or name:explanation */
  static final String PARAM_DISABLED_SERVLETS =
    Configuration.PREFIX + "ui.disabledServlets";
  static final List DEFAULT_DISABLED_SERVLETS = Collections.EMPTY_LIST;

  /** URL of third party logo image */
  static final String PARAM_THIRD_PARTY_LOGO_IMAGE =
    Configuration.PREFIX + "ui.logo.img";

  /** URL of third party logo link */
  static final String PARAM_THIRD_PARTY_LOGO_LINK =
    Configuration.PREFIX + "ui.logo.link";

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

  private static final String DOCTYPE =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">";

  private static final String ENABLEPORT_CELL_ATTRIBUTES =
    ALIGN_CENTER;

  private static final String ENABLEPORT_TABLE_ATTRIBUTES = "align=\"center\" cellpadding=\"10\"";

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

  private static final String SUBMIT_AFTER =
    "</center>";

  private static final String SUBMIT_BEFORE =
    "<br><center>";

  private static String thirdPartyLogo;
  private static String thirdPartyLogoLink;

  private static Map<String,String> disabledServlets = new HashMap();

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(ServeContent.PREFIX)) {
      ServeContent.setConfig(config, oldConfig, diffs);
    }
    thirdPartyLogo = config.get(PARAM_THIRD_PARTY_LOGO_IMAGE);
    if (thirdPartyLogo != null) {
      thirdPartyLogoLink = config.get(PARAM_THIRD_PARTY_LOGO_LINK);
    }
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

  public static String servletDisabledReason(String servlet) {
    return disabledServlets.get(servlet);
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

  // Common page footer
  public static void doLayoutFooter(Page page,
                                  Iterator notesIterator,
                                  String versionInfo) {
    Composite comp = new Composite();

    if (notesIterator!= null && notesIterator.hasNext()) {
      // if there are footnotes
      comp.add(NOTES_BEGIN);
      comp.add(NOTES_LIST_BEFORE);
      for (int nth = 1 ; notesIterator.hasNext() ; nth++) {
        layoutFootnote(comp, (String)notesIterator.next(), nth);
      }
      comp.add(NOTES_LIST_AFTER);
    }

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
    // Start row
    table.newRow();

    // Start line
    table.newCell(ENABLEPORT_CELL_ATTRIBUTES);

    Input portElem = new Input(Input.Text, portFieldName, defaultPort);

    // "enable" element
    Input enaElem = new Input(Input.Checkbox, enableFieldName, "1");
    if (defaultEnable) {
      enaElem.check();
    } else {
      portElem.attribute("disabled", "true");
    }
    String portFieldId = "id_" + portFieldName;
    enaElem.attribute("onchange",
		      "selectEnable(this,'" + portFieldId + "')");
    servlet.setTabOrder(enaElem);
    table.add(enaElem);
    table.add("Enable " + enableDescription);
    table.add(servlet.addFootnote(enableFootnote));
    table.add(" on port&nbsp;");

    // "port" element
    portElem.setSize(6);
    portElem.attribute("id", portFieldId);
    servlet.setTabOrder(portElem);
    table.add(portElem);

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
      block.add(multiline(errMsg));
      block.add(ERRORBLOCK_ERROR_AFTER);
    }
    if (statusMsg != null) {
      block.add(ERRORBLOCK_STATUS_BEFORE);
      block.add(multiline(statusMsg));
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
                                  String machineNameClientAddr,
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
    layoutNavTable(servlet, table, descrIterator, machineNameClientAddr);
    comp.add(table);
    comp.add("<br>");
    page.add(comp);
  }

  public static void layoutIpAllowDenyTable(LockssServlet servlet,
                                            Composite composite,
                                            Vector allow,
                                            Vector deny,
                                            String ipFootnote,
                                            Vector allowErrs,
                                            Vector denyErrs,
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
                                Iterator linkIterator) {
    Table table = new Table(MENU_TABLE_BORDER, MENU_ATTRIBUTES);
    while (linkIterator.hasNext()) {
      LinkWithExplanation link = (LinkWithExplanation)linkIterator.next();
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

    Table tbl = new Table(REPOCHOICE_TABLE_BORDER, REPOCHOICE_TABLE_ATTRIBUTES);    List repos = remoteApi.getRepositoryList();
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
    tbl.add(new Input(Input.File, "AuConfigBackupContents"));
    tbl.newRow();
    tbl.newCell(ALIGN_CENTER);
    tbl.add(submitButton(servlet, buttonNumber, "Restore", backupFileButtonAction));
    frm.add(tbl);
    page.add(frm);
  }

  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
                                        String value) {
    layoutSubmitButton(servlet, composite, "action", value);
  }

  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
					String key,
                                        String value) {
    Input submit = new Input(Input.Submit, key, value);
    servlet.setTabOrder(submit);
    composite.add(SUBMIT_BEFORE + submit + SUBMIT_AFTER);
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
    tbl.newCell();
    tbl.add(layoutSelectAllButton(servlet));

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
    while (basEntryIter.hasNext()) {
      // Get next entry
      BatchAuStatus.Entry rs = (BatchAuStatus.Entry)basEntryIter.next();
      if (rs.isOk()) {
        String auid = rs.getAuId();
        tbl.newRow();

        tbl.newCell(CHOOSEAUS_CELL_ATTRIBUTES);
        auConfs.put(auid, rs.getConfig());
        Element cb = checkbox(servlet, keyAuid, auid, false);
        cb.attribute("onClick", "if (this.checked) selectRepo(this, this.form);");
        tbl.add(cb);

        if (repoFlg) {
          List existingRepoNames = rs.getRepoNames();
          String firstRepo = null;
          if (existingRepoNames != null && !existingRepoNames.isEmpty()) {
            firstRepo = (String)existingRepoNames.get(0);
            isAnyAssignedRepo = true;
          }

          int ix = 1;
          for (Iterator riter = repos.iterator(); riter.hasNext(); ++ix) {
            String repo = (String)riter.next();
            tbl.newCell(ALIGN_CENTER);
            if (firstRepo == null) {
              tbl.add(radioButton(servlet, keyRepo + "_" + auid,
                  Integer.toString(ix), null, false));
            }
            else if (repo.equals(firstRepo)) {
              tbl.add(radioButton(servlet, keyRepo + "_" + auid,
                  Integer.toString(ix), null, true));
            }
            else {
              // nothing
            }
          }
        }
        else if (reposSize > 0) {
          tbl.newCell("colspan=\"" + reposSize + "\"");
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

    if (isLong) {
      tbl.newRow();
      tbl.newCell();
      tbl.add(layoutSelectAllButton(servlet));
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
        BatchAuStatus bas = verb.findAusInSetForVerb(remoteApi, set);
        int numOk = 0;
        for (Iterator iter = bas.getStatusList().iterator(); iter.hasNext(); ) {
          if (((BatchAuStatus.Entry)iter.next()).isOk()) { numOk++; }
        }
        if (numOk > 0 || doGray) {
          ++actualRows;
          tbl.newRow();
          tbl.newCell(CHOOSESETS_CHECKBOX_ATTRIBUTES);
          if (numOk > 0) {
            isAnySelectable.setValue(true);
            tbl.add(checkbox(servlet, checkboxGroup, set.getName(), false));
          }
          tbl.newCell(CHOOSESETS_CELL_ATTRIBUTES);
          String txt = set.getName() + " (" + numOk + ")";
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
                                      Map repoMap,
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
      tbl.addHeading("Default");
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
    for (Iterator iter = repoMap.entrySet().iterator(); iter.hasNext(); ) {
      ix++;
      Map.Entry entry = (Map.Entry)iter.next();
      String repo = (String)entry.getKey();
      PlatformUtil.DF df = (PlatformUtil.DF)entry.getValue();

      // Populate row for entry
      tbl.newRow(REPOTABLE_ROW_ATTRIBUTES);
      if (isChoice) {
	tbl.newCell(ALIGN_CENTER); // "Default"
	tbl.add(radioButton(servlet, keyDefaultRepo, Integer.toString(ix),
			    null, repo == mostFree));
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

  public static void writePage(HttpServletResponse response,
                               Page page)
      throws IOException {
    response.setContentType("text/html");
    response.getWriter().println(DOCTYPE);
    page.write(response.getWriter());
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
    Input in = new Input(Input.Checkbox, key, value);
    if (checked) { in.check(); }
    servlet.setTabOrder(in);
    if (StringUtil.isNullString(text)) {
      return in;
    }
    else {
      Composite c = new Composite();
      c.add(in); c.add(" "); c.add(text);
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
        tbl.add(HtmlUtil.encode(descr.getDisplayName(), HtmlUtil.ENCODE_TEXT));
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
      tbl.add(gray(HtmlUtil.encode(au.getName(), HtmlUtil.ENCODE_TEXT), isGray));
    }
  }

  private static void layoutFootnote(Composite comp,
                                     String footnote,
                                     int nth) {
    comp.add("<li value=\"" + nth + "\">");
    comp.add("<a name=\"foottag" + nth + "\">");
    comp.add(footnote);
    comp.add("</a>");
  }

  private static void layoutIpAllowDenyErrors(Composite composite,
                                              Vector errs) {
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
                                     Iterator descrIterator,
                                     String machineNameClientAddr) {
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
      navTable.add(servlet.conditionalSrvLink(d, d.heading, servlet.isServletLinkInNav(d)));
      navTable.add("</font>");
    }
    navTable.add("</font>");
    outerTable.add(navTable);
  }

  private static void layoutPageHeaders(Page page) {
    page.addHeader("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    page.addHeader("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    page.addHeader("<link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\" />");
    page.addHeader(  "<style type=\"text/css\"> <!--\n"
                   + "sup {font-weight: normal; vertical-align: super; }\n"
                   + "a.colhead, a.colhead:link, a.colhead:visited { text-decoration: none; font-weight: bold; color: blue; }\n"
                   + "td.colhead { font-weight: bold; background: #e0e0e0; }\n"
                   + "--> </style>");
  }

  private static Composite layoutSelectAllButton(LockssServlet servlet) {
    Table tbl = new Table(SELECTALL_BORDER, SELECTALL_ATTRIBUTES);
    tbl.newRow();
    tbl.newCell(ALIGN_RIGHT);
    tbl.add(javascriptButton(servlet, "Select All", "selectAll(this.form, 0);"));
    tbl.newRow();
    tbl.newCell(ALIGN_RIGHT);
    tbl.add(javascriptButton(servlet, "Clear All", "selectAll(this.form, 1);"));
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
      sel.add(au0.getName(), id.equals(preselId), id);
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
				    String text,
				    boolean checked) {
    Composite c = new Composite();
    Input in = new Input(Input.Radio, key, value);
    if (checked) { in.check(); }
    servlet.setTabOrder(in);
    c.add(in); c.add(" "); c.add(text);
    return c;
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
  public static Element manifestIndex(LockssDaemon daemon, Collection aus) {
    return manifestIndex(daemon.getPluginManager(), aus, null);
  }

  /** Return an index of manifest pages for the given AUs. */
  public static Element manifestIndex(LockssDaemon daemon,
				      Collection aus, String header) {
    return manifestIndex(daemon.getPluginManager(), aus, header);
  }

  public interface ManifestUrlTransform {
    public Object transformUrl(String url);
  }

  /** Return an index of manifest pages for the given AUs. */
  public static Element manifestIndex(PluginManager pluginMgr,
				      Collection aus, String header) {
    return manifestIndex(pluginMgr,
			 aus,
			 header,
			 new ManifestUrlTransform(){
			   public Object transformUrl(String url) {
			     return new Link(url, url);
			   }},
			 true);
  }

  public static Element manifestIndex(PluginManager pluginMgr,
				      Collection aus, String header,
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
    tbl.addHeading("Archival Unit");
    tbl.newCell("width=8");
    tbl.add("&nbsp;");
    tbl.addHeading("Manifest");
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      ArchivalUnit au = (ArchivalUnit)iter.next();
      CrawlSpec spec = au.getCrawlSpec();
      tbl.newRow();
      tbl.newCell(ALIGN_LEFT);
      tbl.add(au.getName());
      tbl.newCell("width=8");
      tbl.add("&nbsp;");
      tbl.newCell(ALIGN_LEFT);
      if (spec instanceof SpiderCrawlSpec) {
	List urls = ((SpiderCrawlSpec)spec).getStartingUrls();
	for (Iterator uiter = urls.iterator(); uiter.hasNext(); ) {
	  String url = (String)uiter.next();
	  tbl.add(xform.transformUrl(url));
	  if (checkCollected && AuUtil.getAuState(au).getLastCrawlTime() < 0) {
	    tbl.add(" (not fully collected)");
	  }
	  if (uiter.hasNext()) {
	    tbl.add("<br>");
	  }
	}
      } else if (spec instanceof OaiCrawlSpec) {
	tbl.add("(OAI)");
      } else {
	tbl.add("(Unknown CrawlSpec type)");
      }
    }
    return tbl;
  }

  /** Interface to link rewriting */
  public interface LinkTransform {
    public String rewrite(String url);
  }
}
