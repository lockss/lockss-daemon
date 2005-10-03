/*
 * $Id: ServletUtil.java,v 1.6 2005-10-03 17:36:38 thib_gc Exp $
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
import java.util.Date;
import java.util.Iterator;

import org.lockss.util.Constants;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;
import org.mortbay.html.*;

public class ServletUtil {

  /** Format to display date/time in headers */
  public static final DateFormat headerDf =
    new SimpleDateFormat("HH:mm:ss MM/dd/yy");

  static final Image IMAGE_LOGO_LARGE =
    makeImage("lockss-logo-large.gif", 160, 160, 0);

  static final Image IMAGE_LOGO_SMALL =
    makeImage("lockss-logo-small.gif", 80, 81, 0);

  /* private */static final Image IMAGE_TM =
    makeImage("tm.gif", 16, 16, 0);

  static final String PAGE_BGCOLOR = "#FFFFFF";

  private static final String FOOTER_ATTRIBUTES =
    "cellspacing=\"0\" cellpadding=\"0\" align=\"center\"";

  private static final int FOOTER_BORDER = 0;

  private static final String HEADER_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"0\" width=\"100%\"";

  private static final int HEADER_BORDER = 0;

  private static final String HEADER_HEADING_AFTER =
    "</b></font>";

  private static final String HEADER_HEADING_BEFORE = 
    "<font size=\"+2\"><b>";

  private static final Image IMAGE_LOCKSS_RED =
    makeImage("lockss-type-red.gif", 595, 31, 0);
  
  private static final String MENU_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"4\" align=\"center\"";
  
  private static final int MENU_BORDER = 0;

  private static final String MENU_ITEM_AFTER =
    "</font>";

  private static final String MENU_ITEM_BEFORE =
    "<font size=\"+1\">";

  private static final String MENU_ROW_ATTRIBUTES =
    "valign=\"top\"";

  private static final String NAVTABLE_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"0\"";

  private static final int NAVTABLE_BORDER = 0;
  
  private static final String NOTES_BEGIN =
    "<p><b>Notes:</b>";

  private static final String NOTES_LIST_AFTER =
    "</font></ol>";

  private static final String NOTES_LIST_BEFORE =
    "<ol><font size=\"-1\">";

  // Common page footer
  public static void layoutFooter(Page page,
                                  Iterator notesIterator,
                                  String versionInfo) {
    Composite comp = new Composite();

    comp.add(NOTES_BEGIN);
    comp.add(NOTES_LIST_BEFORE);
    for (int nth = 1 ; notesIterator.hasNext() ; nth++) {
      layoutFootnote(comp, (String)notesIterator.next(), nth);
    }
    comp.add(NOTES_LIST_AFTER);

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
                                  Date startDate, Iterator servletDescrIterator) {
    Composite comp = new Composite();
    Table table = new Table(HEADER_BORDER, HEADER_ATTRIBUTES);
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
    layoutNavTable(servlet, table, servletDescrIterator, machineNameClientAddr);
    comp.add(table);
    comp.add("<br>");
    page.add(comp);
  }
  
  public static void layoutMenu(LockssServlet servlet,
                                Page page,
                                ServletDescr[] sd) {
    Table table = new Table(MENU_BORDER, MENU_ATTRIBUTES);
    for (int ii = 0; ii < sd.length; ii++) {
      if (sd[ii] != null) {
        ServletDescr desc = sd[ii];
        table.newRow(MENU_ROW_ATTRIBUTES);
        table.newCell();
        table.add(MENU_ITEM_BEFORE);
        table.add(servlet.srvLink(desc, desc.heading));
        table.add(MENU_ITEM_AFTER);
        table.newCell();
        table.add(desc.getExplanation());
      }
    }
    page.add(table);
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
  
  private static void layoutFootnote(Composite comp,
                                     String footnote,
                                     int nth) {
    comp.add("<li value=\"" + nth + "\">");
    comp.add("<a name=\"foottag" + nth + "\">");
    comp.add(footnote);
    comp.add("</a>");
  }
  
  // Build servlet navigation table
  private static void layoutNavTable(LockssServlet servlet,
                                     Table outerTable,
                                     Iterator servletDescrIterator,
                                     String machineNameClientAddr) {
    final String NAVTABLE_CELL_WIDTH = "width=\"15\"";
    
    Table navTable = new Table(NAVTABLE_BORDER, NAVTABLE_ATTRIBUTES);
    boolean clientTitle = false;

    while (servletDescrIterator.hasNext()) {
      ServletDescr d = (ServletDescr)servletDescrIterator.next();
      if (servlet.isServletInNav(d)) {
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
  
}
