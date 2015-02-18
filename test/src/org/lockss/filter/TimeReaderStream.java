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
import org.lockss.test.*;
import org.lockss.util.*;
import java.util.*;
import java.io.*;

public class TimeReaderStream extends LockssTiming {
  private static Logger log = Logger.getLogger("TimeReaderStream");

  static final int FILE_LINES = 10000;

  File file;

  public void setUp() throws Exception {
    super.setUp();
    file = FileTestUtil.tempFile("foo");
    writeFile(file);
  }

  public void testFoo() {
    log.info("file.encoding: " + System.getProperty("file.encoding"));
  }


  void writeFile(File file) throws Exception {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    PrintStream ps = new PrintStream(os);
    for (int ix = 0; ix < FILE_LINES; ix++) {
      ps.println("I am an Apteryx, a wingless bird with hairy feathers.");
    }
    ps.close();
  }

  public void testStream() throws Exception {
    time(file, "InputStream",
	 new Computation() {
	   public void execute() throws Exception {
	     InputStream is =
	       new BufferedInputStream(new FileInputStream(file));
	     readAll(is, true);
	     is.close();
	   }});
  }

  public void testStreamReader() throws Exception {
    time(file, "InputStreamReader",
	 new Computation() {
	   public void execute() throws Exception {
	     InputStream is =
	       new BufferedInputStream(new FileInputStream(file));
	     Reader rdr = newInputStreamReader(is);
	     readAll(rdr, true);
	     rdr.close();
	   }});
  }

  public void testStreamReaderStream() throws Exception {
    time(file, "InputStreamReaderStream",
	 new Computation() {
	   public void execute() throws Exception {
	     InputStream is =
	       new BufferedInputStream(new FileInputStream(file));
	     Reader rdr = newInputStreamReader(is);
	     InputStream is2 = new ReaderInputStream(rdr);
	     readAll(is2, true);
	     is2.close();
	   }});
  }

  public void testStreamReaderBufferedStream() throws Exception {
    time(file, "InputStreamReaderBufferedStream",
	 new Computation() {
	   public void execute() throws Exception {
	     InputStream is =
	       new BufferedInputStream(new FileInputStream(file));
	     Reader rdr = newInputStreamReader(is);
	     InputStream is2 = new BufferedInputStream(new ReaderInputStream(rdr));
	     readAll(is2, true);
	     is2.close();
	   }});
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TimeReaderStream.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
