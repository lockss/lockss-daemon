/*
 * $Id$
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

package org.lockss.util;

import java.util.Iterator;

import org.lockss.test.LockssTestCase;
import org.lockss.util.PrintfUtil.*;

/**
 * <p>Tests the <code>org.lockss.util.PrintfUtil</code> class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestPrintfUtil extends LockssTestCase {

  public void testStringToPrintf() throws Exception {
    PrintfData printfData = PrintfUtil.stringToPrintf("\"foo%dbar%sbaz%%\", my_int, my_str");

    assertEquals("foo%dbar%sbaz%%",
                 printfData.getFormat());

    Iterator /* of String */ iter = printfData.getArguments().iterator();
    assertEquals("my_int", iter.next());
    assertEquals("my_str", iter.next());
  }

  public void testPrintfToString() throws Exception {
    PrintfData printfData = new PrintfData();
    printfData.setFormat("foo%dbar%sbaz%%");
    printfData.addArgument("my_int");
    printfData.addArgument("my_str");
    assertEquals("\"foo%dbar%sbaz%%\", my_int, my_str",
                 PrintfUtil.printfToString(printfData));
  }

  public void testPrintfToElements() throws Exception {
    PrintfData printfData = new PrintfData();
    printfData.setFormat("foo%dbar%sbaz%%");
    printfData.addArgument("my_int");
    printfData.addArgument("my_str");
    PrintfElement[] printfElements = PrintfUtil.printfToElements(printfData);

    assertEquals(6, printfElements.length);
    PrintfElement[] expected = new PrintfElement[] {
      new PrintfElement(PrintfElement.NONE, "foo"),
      new PrintfElement("%d", "my_int"),
      new PrintfElement(PrintfElement.NONE, "bar"),
      new PrintfElement("%s", "my_str"),
      new PrintfElement(PrintfElement.NONE, "baz"),
      new PrintfElement(PrintfElement.NONE, "%%"),
    };

    for (int ix = 0 ; ix < expected.length ; ++ix) {
      assertEquals(expected[ix].getFormat(), printfElements[ix].getFormat());
      assertEquals(expected[ix].getElement(), printfElements[ix].getElement());
    }
  }

}
