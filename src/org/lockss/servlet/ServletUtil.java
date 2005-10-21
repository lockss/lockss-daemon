/*
 * $Id: ServletUtil.java,v 1.17 2005-10-21 23:28:44 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import org.lockss.daemon.TitleSet;
import org.lockss.jetty.MyTextArea;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.remote.RemoteApi.BatchAuStatus.Entry;
import org.lockss.servlet.BatchAuConfig.Verb;
import org.lockss.util.*;
import org.mortbay.html.*;

public class ServletUtil {

  public static class LinkWithExplanation {

    private String explanation;

    private String link;

    public LinkWithExplanation(String link, String explanation) {
      this.link = link;
      this.explanation = explanation;
    }

    protected String getExplanation() {
      return explanation;
    }

    protected String getLink() {
      return link;
    }

  }

  /** Format to display date/time in headers */
  public static final DateFormat headerDf =
    new SimpleDateFormat("HH:mm:ss MM/dd/yy");

  static final Image IMAGE_LOGO_LARGE =
    makeImage("lockss-logo-large.gif", 160, 160, 0);

  static final Image IMAGE_LOGO_SMALL =
    makeImage("lockss-logo-small.gif", 80, 81, 0);

  /* private */static final Image IMAGE_TM =
    makeImage("tm.gif", 16, 16, 0);

  static final String PAGE_BGCOLOR =
    "#ffffff";

  private static final String ALLOWDENY_CELL_ATTRIBUTES =
    "align=\"center\"";

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

  private static final String CHOOSESETS_CELL_ATTRIBUTES = "valign=\"center\"";

  private static final String CHOOSESETS_CHECKBOX_ATTRIBUTES =
    "align=\"right\" valign=\"center\"";

  private static final String CHOOSESETS_TABLE_ATTRIBUTES =
    "align=\"center\" cellspacing=\"4\" cellpadding=\"0\"";

  private static final int CHOOSESETS_TABLE_BORDER = 0;

  private static final String ERRORBLOCK_ERROR_AFTER =
    "</font></center><br>";

  private static final String ERRORBLOCK_ERROR_BEFORE =
    "<center><font color=\"red\" size=\"+1\">";

  private static final String ERRORBLOCK_STATUS_AFTER =
    "<center><font size=\"+1\">";

  private static final String ERRORBLOCK_STATUS_BEFORE =
    "<center><font color=\"red\" size=\"+1\">";

  private static final String EXPLANATION_CELL_ATTRIBUTES =
    "align=\"center\"";

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
    makeImage("lockss-type-red.gif", 595, 31, 0);

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

  private static final String PORT_ATTRIBUTES =
    ALLOWDENY_CELL_ATTRIBUTES;

  private static final String SUBMIT_AFTER =
    "</center>";

  private static final String SUBMIT_BEFORE =
    "<br><center>";

//  public static void layoutChooseSets(LockssServlet servlet,
//                                      RemoteApi remoteApi,
//                                      Iterator setIterator,
//                                      String checkboxGroup,
//                                      Verb verb,
//                                      boolean doGray,
//                                      MutableInteger actualRows,
//                                      MutableBoolean isAnySelectable) {
//    actualRows.setValue(0);
//    isAnySelectable.setValue(false);
//
//    Table tbl = new Table(CHOOSESETS_TABLE_BORDER, CHOOSESETS_TABLE_ATTRIBUTES);
//    while (setIterator.hasNext()) {
//      TitleSet set = (TitleSet)setIterator.next();
//      if (verb.isTsAppropriateFor(set)) {
//        BatchAuStatus bas = verb.findAusInSetForVerb(remoteApi, set);
//        int numOk = 0;
//        for (Iterator iter = bas.getStatusList().iterator(); iter.hasNext(); ) {
//          if (((Entry)iter.next()).isOk()) { numOk++; }
//        }
//        if (numOk > 0 || doGray) {
//          actualRows.add(1);
//          tbl.newRow();
//          tbl.newCell(CHOOSESETS_CHECKBOX_ATTRIBUTES);
//          if (numOk > 0) {
//            isAnySelectable.setValue(true);
//            tbl.add(makeCheckbox(servlet, null, set.getName(), checkboxGroup, false));
//          }
//          tbl.newCell(CHOOSESETS_CELL_ATTRIBUTES);
//          String txt = set.getName() + " (" + numOk + ")";
//          tbl.add(numOk > 0 ? txt : gray(txt));
//        }
//      }
//    }
//  }

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

  // Common page footer
  public static void layoutFooter(Page page,
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

  public static void layoutSubmitButton(LockssServlet servlet,
                                        Composite composite,
                                        String value) {
    Input submit = new Input(Input.Submit, "action", value);
    servlet.setTabOrder(submit);
    composite.add(SUBMIT_BEFORE + submit + SUBMIT_AFTER);
  }

  public static Image makeImage(String file,
                                int width,
                                int height,
                                int border) {
    return new Image("/images/" + file, width, height, border);
  }

  /** create an image that will display the tooltip on mouse hover */
  public static Image makeImage(String file,
                                int width,
                                int height,
                                int border,
                                String tooltip) {
    Image img = makeImage(file, width, height, border);
    img.alt(tooltip);			// some browsers (IE) use alt tag
    img.attribute("title", tooltip);	// some (Mozilla) use title tag
    return img;
  }

  public static Form newForm(LockssServlet servlet) {
    Form form = new Form(servlet.srvURL(servlet.myServletDescr()));
    form.method("POST");
    return form;
  }

  public static Page newPage(String pageTitle,
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
  private static String gray(String txt) {
    return gray(txt, true);
  }

  /** Add html tags to grey the text if isGrey is true */
  private static String gray(String txt, boolean isGray) {
    if (isGray)
      return "<font color=\"gray\">" + txt + "</font>";
    else
      return txt;
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

  /** Return a (possibly labelled) checkbox.
   * @param label appears to right of checkbox if non null
   * @param value value included in result set if box checked
   * @param key form key to which result set is assigned
   * @param checked if true, box is initially checked
   * @return a checkbox Element
   */
  private static Element makeCheckbox(LockssServlet servlet,
                                      String label,
                                      String value,
                                      String key,
                                      boolean checked) {
    Input in = new Input(Input.Checkbox, key, value);
    if (checked) { in.check(); }
    servlet.setTabOrder(in);
    if (StringUtil.isNullString(label)) {
      return in;
    }
    else {
      Composite c = new Composite();
      c.add(in); c.add(" "); c.add(label);
      return c;
    }
  }

  private static String multiline(String str) {
    return str.replaceAll("\n", "<br>");
  }

}
