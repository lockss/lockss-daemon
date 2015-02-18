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

public class TimeHtmlTagFilter extends LockssTiming {
  private static Logger log = Logger.getLogger("TimeHtmlTagFilter");

  static final int FILE_LINES = 10000;

  String NOFILT =   "No filter  ";
  String NULLFILT = "Null filter";
  String FILTTEXT = "Filter text";
  String FILTHTML = "Filter html";

  static Class filterClass = null;
  HtmlTagFilter.TagPair tagpair = null;

  File file;

  public void setUp() throws Exception {
    super.setUp();
    file = FileTestUtil.tempFile("foo");
    tagpair = new HtmlTagFilter.TagPair("<foo", "</foo>", true);
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
      ps.println("foo <foo x=y> a bunch of table stuff </foo>");
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
	     Reader rdr = newInputStreamReader(new BufferedInputStream(new FileInputStream(file)));
	     incrBytesProcessed(readAll(rdr, true));
	     rdr.close();
	   }});
  }

  public void testNoHtml() throws Exception {
    writeTextFile(file);
    time(file, FILTTEXT,
	 new Computation() {
	   public void execute() throws Exception {
	     Reader rdr = new HtmlTagFilter(newInputStreamReader(new FileInputStream(file)),
					    tagpair);
	     incrBytesProcessed(readAll(rdr, true));
	     rdr.close();
	   }});
  }

  public void testHtml() throws Exception {
    writeHtmlFile(file);
    time(file, FILTHTML,
	 new Computation() {
	   public void execute() throws Exception {
	     Reader rdr = new HtmlTagFilter(newInputStreamReader(new FileInputStream(file)),
					    tagpair);
	     incrBytesProcessed(readAll(rdr, true));
	     rdr.close();
	   }});
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TimeHtmlTagFilter.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
