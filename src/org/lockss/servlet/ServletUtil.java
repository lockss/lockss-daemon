/*
 * $Id: ServletUtil.java,v 1.23 2006-01-13 23:59:54 thib_gc Exp $
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
import java.text.*;
import java.util.*;
import java.util.List;

import org.lockss.daemon.*;
import org.lockss.jetty.MyTextArea;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.remote.RemoteApi.BatchAuStatus.Entry;
import org.lockss.servlet.BatchAuConfig.Verb;
import org.lockss.util.*;
import org.lockss.config.*;
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

  private static final String AUSTATUS_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int AUSTATUS_TABLE_BORDER = 0;

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

  private static final String COLOR_WHITE = "#ffffff"; /* (a) */

  private static final String ERRORBLOCK_ERROR_AFTER =
    "</font></center><br>";

  private static final String ERRORBLOCK_ERROR_BEFORE =
    "<center><font color=\"red\" size=\"+1\">";

  private static final String ERRORBLOCK_STATUS_AFTER =
    "<center><font size=\"+1\">";

  private static final String ERRORBLOCK_STATUS_BEFORE =
    "<center><font color=\"red\" size=\"+1\">";

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
    COLOR_WHITE;

  private static final String PORT_ATTRIBUTES =
    ALIGN_CENTER;

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

  /** Return the URL of this machine's config backup file to download */
  // This is a crock.  It's called from RemoteAPI, which has no servlet
  // instance and thus can't use LockssServlet.srvURL().
  public static String backupFileUrl(String hostname) {
    ServletDescr backupServlet = LockssServlet.SERVLET_BATCH_AU_CONFIG;
    int port = CurrentConfig.getIntParam(LocalServletManager.PARAM_PORT,
					 LocalServletManager.DEFAULT_PORT);
    StringBuffer sb = new StringBuffer();
    sb.append("http://");
    sb.append(hostname);
    sb.append(":");
    sb.append(port);
    sb.append("/");
    sb.append(backupServlet.getName());
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
    img.alt(tooltip);		 	// some browsers (IE) use alt tag
    img.attribute("title", tooltip);	// some (Mozilla) use title tag
    return img;
  }

  public static void layoutAuStatus(Page page,
                                    Iterator auStatusEntryIter) {
    Table tbl = new Table(AUSTATUS_TABLE_BORDER, AUSTATUS_TABLE_ATTRIBUTES);
    tbl.addHeading("Status");
    tbl.addHeading("Archival Unit");
    while (auStatusEntryIter.hasNext()) {
      Entry stat = (Entry)auStatusEntryIter.next();
      tbl.newRow();
      tbl.newCell();
      tbl.add(SPACE);
      tbl.add(stat.getStatus());
      tbl.add(SPACE);
      tbl.newCell();
      String name = stat.getName();
      tbl.add(name != null ? name : stat.getAuId());
      if (stat.getExplanation() != null) {
        tbl.newCell();
        tbl.add(stat.getExplanation());
      }
    }
    page.add(tbl);
  }

  public static void layoutBackLink(LockssServlet servlet,
                                    Page page,
                                    String destination) {
    page.add(BACKLINK_BEFORE);
    page.add(servlet.srvLink(servlet.myServletDescr(),
                             "Back to " + destination));
    page.add(BACKLINK_AFTER);
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
    table.newCell(PORT_ATTRIBUTES);

    // "enable" element
    Input enaElem = new Input(Input.Checkbox, enableFieldName, "1");
    if (defaultEnable) {
      enaElem.check();
    }
    servlet.setTabOrder(enaElem);
    table.add(enaElem);
    table.add("Enable " + enableDescription);
    table.add(servlet.addFootnote(enableFootnote));
    table.add(" on port&nbsp;");

    // "port" element
    Input portElem = new Input(Input.Text, portFieldName, defaultPort);
    portElem.setSize(6);
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
    Image logo = isLargeLogo ? IMAGE_LOGO_LARGE : IMAGE_LOGO_SMALL;

    table.newRow();
    table.newCell("valign=\"top\" align=\"center\" width=\"20%\"");
    table.add(new Link(Constants.LOCKSS_HOME_URL, logo));
    table.add(IMAGE_TM);

    table.newCell("valign=\"top\" align=\"center\" width=\"60%\"");
    table.add("<br>");
    table.add(HEADER_HEADING_BEFORE);
    table.add(heading);
    table.add(HEADER_HEADING_AFTER);
    table.add("<br>");

    String since =
      StringUtil.timeIntervalToString(TimeBase.msSince(startDate.getTime()));
    table.add(machineName + " at " + headerDf.format(new Date()) + ", up " + since);

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

  public static void layoutRestore(LockssServlet servlet,
                                   Page page,
                                   String hiddenActionName,
                                   String hiddenVerbName,
                                   Verb verb,
                                   String backupFileFieldName,
                                   MutableInteger buttonNumber,
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
    buttonNumber.add(1);
    tbl.add(submitButton(servlet, buttonNumber.intValue(),
        "Restore", backupFileButtonAction));
    frm.add(tbl);
    page.add(frm);
  }

  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
                                        String value) {
    Input submit = new Input(Input.Submit, "action", value);
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
                                        MutableInteger buttonNumber,
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
      buttonNumber.add(1);
      tbl.add(submitButton(servlet, buttonNumber.intValue(),
          buttonText, verb.action()));
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
        tbl.add(rs.getName());
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
    buttonNumber.add(1);
    tbl.add(submitButton(servlet, buttonNumber.intValue(),
        buttonText, verb.action()));

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
                                         MutableInteger buttonNumber,
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
    buttonNumber.add(1);
    tbl.add(submitButton(servlet, buttonNumber.intValue(),
        submitText, submitAction));

    // Iterate over title sets
    while (titleSetIterator.hasNext()) {
      TitleSet set = (TitleSet)titleSetIterator.next();
      if (verb.isTsAppropriateFor(set)) {
        BatchAuStatus bas = verb.findAusInSetForVerb(remoteApi, set);
        int numOk = 0;
        for (Iterator iter = bas.getStatusList().iterator(); iter.hasNext(); ) {
          if (((Entry)iter.next()).isOk()) { numOk++; }
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
      buttonNumber.add(1);
      tbl.add(submitButton(servlet, buttonNumber.intValue(),
          submitText, submitAction));
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
          tbl.add(rs.getName());
          tbl.newCell();
          tbl.add(rs.getExplanation());
        }
      }

      comp.add(tbl);
      return comp;
  }

  public static Element makeRepoTable(LockssServlet servlet,
                                      Iterator repoIter,
                                      String keyDefaultRepo) {
    Table tbl = new Table(REPOTABLE_BORDER, REPOTABLE_ATTRIBUTES);
    tbl.newRow();
    tbl.addHeading("Available Disks", "colspan=\"6\"");
    tbl.newRow();
    tbl.addHeading("Default");
    tbl.addHeading("Disk");
    tbl.addHeading("Location");
    tbl.addHeading("Size");
    tbl.addHeading("Free");
    tbl.addHeading("%Full");

    // Populate repo key table
    for (int ix = 1 ; repoIter.hasNext() ; ++ix) {
      // Get entry
      Map.Entry entry = (Map.Entry)repoIter.next();
      String repo = (String)entry.getKey();
      PlatformInfo.DF df = (PlatformInfo.DF)entry.getValue();

      // Populate row for entry
      tbl.newRow(REPOTABLE_ROW_ATTRIBUTES);
      tbl.newCell(ALIGN_CENTER); // "Default"
      tbl.add(radioButton(servlet, keyDefaultRepo, Integer.toString(ix),
                  null, ix == 1));
      tbl.newCell(ALIGN_RIGHT); // "Disk"
      tbl.add(Integer.toString(ix) + "." + SPACE);
      tbl.newCell(ALIGN_LEFT); // "Location"
      tbl.add(repo);
      if (df != null) {
        tbl.newCell(ALIGN_RIGHT); // "Size"
        tbl.add(SPACE);
        tbl.add(StringUtil.sizeKBToString(df.getSize()));
        tbl.newCell(ALIGN_RIGHT); // "Free"
        tbl.add(SPACE);
        tbl.add(StringUtil.sizeKBToString(df.getAvail()));
        tbl.newCell(ALIGN_RIGHT); // "%Full"
        tbl.add(SPACE);
        tbl.add(df.getPercentString());
      }
    }

    tbl.newRow();
    tbl.newCell("colspan=\"6\"");
    tbl.add(Break.rule);
    return tbl;
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

  /** Return a (possibly labelled) checkbox.
   * @param servlet The servlet associated with the checkbox.
   * @param key     Form key to which result set is assigned.
   * @param value   Value included in result set if box checked.
   * @param checked If true, the checkbox is initially checked.
   * @return A checkbox {@link Element}.
   */
  private static Element checkbox(LockssServlet servlet,
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
  private static Element checkbox(LockssServlet servlet,
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
      composite.add(size == 1 ? " entry has" : " entries have");
      composite.add(" errors:");
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
    boolean clientTitle = false;

    while (descrIterator.hasNext()) {
      ServletDescr d = (ServletDescr)descrIterator.next();
      navTable.newRow();
      if (d.isPerClient()) {
        if (!clientTitle) {
          // Insert client name before first per-client servlet
          navTable.newCell(NAVTABLE_CELL_WIDTH);
          navTable.newCell("colspan=\"2\"");
          navTable.add("<b>" + machineNameClientAddr + "</b>");
          navTable.newRow();
          clientTitle = true;
        }
        navTable.newCell(NAVTABLE_CELL_WIDTH);
        navTable.newCell(NAVTABLE_CELL_WIDTH);
        navTable.newCell();
      } else {
        navTable.newCell("colspan=\"3\"");
      }
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
    page.add("<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">");
    page.addHeader("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    page.addHeader("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    page.addHeader("<link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\" />");
    page.addHeader("<style type=\"text/css\"><!--\n" +
        "sup {font-weight: normal; vertical-align: super}\n" +
        "A.colhead, A.colhead:link, A.colhead:visited { text-decoration: none ; font-weight: bold ; color: blue }\n" +
        "TD.colhead { font-weight: bold; background : #e0e0e0 }\n" +
        "--> </style>");
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

  private static String multiline(String str) {
    return str.replaceAll("\n", "<br>");
  }

  private static Element radioButton(LockssServlet servlet,
                                     String key,
                                     String value,
                                     boolean checked) {
    return radioButton(servlet, key, value, value, checked);
  }

  private static Element radioButton(LockssServlet servlet,
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
   * specified action */
  private static Element submitButton(LockssServlet servlet,
                                      int buttonNumber,
                                      String label,
                                      String action) {
    return submitButton(servlet, buttonNumber, label, action, null, null);
  }

  /** Return a button that invokes the javascript submit routine with the
   * specified action, first storing the value in the specified form
   * prop. */
  private static Element submitButton(LockssServlet servlet,
                                      int buttonNumber,
                                      String label,
                                      String action,
                                      String prop,
                                      String value) {
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

}
