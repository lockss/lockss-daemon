/*
 * $Id: ServletUtil.java,v 1.2 2005-09-22 23:30:51 thib_gc Exp $
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

import org.mortbay.html.*;

public class ServletUtil {

  /* private */static final Image IMAGE_TM =
    makeImage("tm.gif", 16, 16, 0);

  private static final Image IMAGE_LOCKSS_RED =
    makeImage("lockss-type-red.gif", 595, 31, 0);

  private static final String FOOTER_ATTRIBUTES =
    "cellspacing=\"0\" cellpadding=\"0\" align=\"center\"";

  private static final int FOOTER_BORDER = 0;

  private static final String MENU_ATTRIBUTES =
    "cellspacing=\"2\" cellpadding=\"4\" align=\"center\"";

  private static final int MENU_BORDER = 0;
  
  private static final String MENU_ITEM_AFTER =
    "</font>";

  private static final String MENU_ITEM_BEFORE =
    "<font size=\"+1\">";

  private static final String MENU_ROW_ATTRIBUTES =
    "valign=\"top\"";

  // Common page footer
  public static void layoutFooter(LockssServlet servlet,
                                  Page page) {
    Composite comp = new Composite();
    String vDaemon = servlet.theApp.getVersionInfo();

    servlet.addNotes(comp);
    comp.add("<p>");

    Table table = new Table(FOOTER_BORDER, FOOTER_ATTRIBUTES);
    table.newRow("valign=\"top\"");
    table.newCell();
    table.add(IMAGE_LOCKSS_RED);
    table.newCell();
    table.add(IMAGE_TM);
    table.newRow();
    table.newCell("colspan=\"2\"");
    table.add("<center><font size=\"-1\">" + vDaemon + "</font></center>");
    comp.add(table);
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
  
}
