/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import java.util.*;
import java.io.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.filter.*;

public class TimeFilterRule extends LockssTiming {
  private static Logger log = Logger.getLogger("TimeFilterRule");

  static final int FILE_LINES = 10000;
  static final String enc = "ISO8859_1";
  //  static final String enc = "UTF8";

  String NOFILT =   "No filter  ";
  String NULLFILT = "Null filter";
  String FILTTEXT = "Filter text";
  String FILTHTML = "Filter html";

  static Class filterClass = null;
  static FilterRule rule = null;

  File file;

  public void setUp() throws Exception {
    super.setUp();
    System.setProperty("file.encoding", "ISO8859_1");
    file = FileTestUtil.tempFile("foo");
    if (rule == null) {
      rule = new TimedFilterRule();
    }
  }

  void writeTextFile(File file) throws Exception {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    PrintStream ps = new PrintStream(os);
    for (int ix = 0; ix < FILE_LINES; ix++) {
      ps.println("I am an Apteryx, a wingless bird with hairy feathers.");
    }
    ps.close();
  }

  void writeHtmlFile(File file) throws Exception {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    PrintStream ps = new PrintStream(os);
    for (int ix = 0; ix < FILE_LINES; ix++) {
      ps.println("I am an Apteryx, a wingless bird with hairy feathers.");
      ps.println("I am &nbsp;  an Apteryx, <a wingless> bird.");
      ps.println("foo <table x=y> a bunch of table stuff </table>");
    }
    ps.close();
  }

  public void testNoFilter() throws Exception {
    writeTextFile(file);
    time(file, NOFILT,
	 new Computation() {
	   public void execute() throws Exception {
	     InputStream is =
	       new BufferedInputStream(new FileInputStream(file));
	     incrBytesProcessed(readAll(is, true));
	     is.close();
	   }});
  }

  public void testNullFilter() throws Exception {
    writeTextFile(file);
    time(file, NULLFILT,
	 new Computation() {
	   public void execute() throws Exception {
	     Reader reader = newInputStreamReader(new BufferedInputStream(
                        new FileInputStream(file)));
	     incrBytesProcessed(readAll(reader, true));
	   }});
  }

  public void testNoHtml() throws Exception {
    writeTextFile(file);
    time(file, FILTTEXT,
	 new Computation() {
	   public void execute() throws Exception {
             Reader reader = rule.createFilteredReader(newInputStreamReader(
                 new FileInputStream(file)));
	     incrBytesProcessed(readAll(reader, true));
	   }});
  }

  public void testHtml() throws Exception {
    writeHtmlFile(file);
    time(file, FILTHTML,
	 new Computation() {
	   public void execute() throws Exception {
             Reader reader = rule.createFilteredReader(newInputStreamReader(
                 new FileInputStream(file)));
             incrBytesProcessed(readAll(reader, true));
	   }});
  }

  static boolean setRule(String name) {
    Class filterClass = null;
    if (StringUtil.isNullString(name)) {
      return true;
    }
    try {
      filterClass = Class.forName(name);
      if (!FilterRule.class.isAssignableFrom(filterClass)) {
	log.error("Not a FilterRule: " + name);
	return false;
      }
      rule = (FilterRule)filterClass.newInstance();
      return true;
    } catch (Exception e) {
      log.error("Can't create rule: " + e.toString());
      return false;
    }
  }

  public static void main(String[] argv) {
    boolean ok = true;
    if (argv.length >= 1) {
      ok = setRule(argv[0]);
    }
    if (ok) {
      String[] testCaseList = { TimeFilterRule.class.getName()};
      junit.textui.TestRunner.main(testCaseList);
    }
  }

  public class TimedFilterRule implements FilterRule {
    public static final String CITATION_STRING =
      "This article has been cited by other articles:";
    public static final String MEDLINE_STRING = "[Medline]";

    public Reader createFilteredReader(Reader reader) {

      List tagList =
	ListUtil.list(
		      new HtmlTagFilter.TagPair("<script", "</script>", true),
		      new HtmlTagFilter.TagPair("<table", "</table>", true),
		      new HtmlTagFilter.TagPair("<", ">")
		      );
      Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);
      Reader medFilter = new StringFilter(tagFilter, MEDLINE_STRING);
      Reader citeFilter = new StringFilter(medFilter, CITATION_STRING);
      return new WhiteSpaceFilter(citeFilter);
    }
  }

}
