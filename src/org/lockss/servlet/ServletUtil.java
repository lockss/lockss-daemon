/*
 * $Id: ServletUtil.java,v 1.5 2005-09-30 22:25:01 thib_gc Exp $
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
  
  private static final String HEADING_NULL = "Cache Administration";
  
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
  
  public static void layoutHeader(Page page,
                                  String heading,
                                  boolean isLargeLogo,
                                  String machineName,
                                  Date startDate) {
    if (heading == null) {
      heading = HEADING_NULL;
    }

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
    // Date startDate = getLockssApp().getStartDate();
    String since =
      StringUtil.timeIntervalToString(TimeBase.msSince(startDate.getTime()));
    table.add(machineName + " at " + headerDf.format(new Date()) + ", up " + since);

    table.newCell("valign=\"center\" align=\"center\" width=\"20%\"");
    //table.add(getNavTable());
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
  
  private static void layoutFootnote(Composite comp,
                                     String footnote,
                                     int nth) {
    comp.add("<li value=\"" + nth + "\">");
    comp.add("<a name=\"foottag" + nth + "\">");
    comp.add(footnote);
    comp.add("</a>");
  }
  
}
