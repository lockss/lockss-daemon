/*
 * $Id: TestFilterUtil.java,v 1.3 2007-02-06 00:52:27 tlipkis Exp $
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

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for org.lockss.filter.FilterUtil
 */
public class TestFilterUtil extends LockssTestCase {

  public void testGetReader() {
    InputStream in = new StringInputStream("123");
    InputStreamReader rdr;
    Charset def = Charset.forName(Constants.DEFAULT_ENCODING);
    Charset utf = Charset.forName("UTF-8");
    rdr = (InputStreamReader)FilterUtil.getReader(in, null);
    assertTrue(def.aliases().contains(rdr.getEncoding()));
    rdr = (InputStreamReader)FilterUtil.getReader(in, "ISO-8859-1");
    assertTrue(def.aliases().contains(rdr.getEncoding()));
    rdr = (InputStreamReader)FilterUtil.getReader(in, "utf-8");
    assertTrue(utf.aliases().contains(rdr.getEncoding()));
    rdr = (InputStreamReader)FilterUtil.getReader(in, "NoSuchCharset");
    assertTrue(def.aliases().contains(rdr.getEncoding()));
  }

  public void testGetReaderShortcut() throws Exception {
    StringReader rdr = new StringReader("foo");
    ReaderInputStream in = new ReaderInputStream(rdr);
    Reader r2 = FilterUtil.getReader(in, null);
    assertSame(rdr, r2);
    assertReaderMatchesString("foo", r2);
  }
}
