/*
 * $Id: TestBoyerMoore.java,v 1.1 2005-05-17 23:11:46 troberts Exp $
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

package org.lockss.util;

import java.util.*;
import org.lockss.test.*;

public class TestBoyerMoore extends LockssTestCase {

  public void testSearchNotFound() {
    String test1 = "This is a test string";
    BoyerMoore bm = new BoyerMoore("No match");
    assertEquals(-1, bm.search(test1.toCharArray()));
    assertEquals(0, bm.partialMatch());
  }

  public void testSearchFound() {
    String test1 = "This is a test string";
    BoyerMoore bm = new BoyerMoore("test");
    assertEquals(10, bm.search(test1.toCharArray()));
    assertEquals(0, bm.partialMatch());
  }

  public void testSearchNotFoundPartial() {
    String test1 = "This is a test string";
    BoyerMoore bm = new BoyerMoore("stringsarefun");
    assertEquals(-1, bm.search(test1.toCharArray()));
    assertEquals(6, bm.partialMatch());
  }

  public void testSearchFoundPartial() {
    String test1 = "This is a test stringte";
    BoyerMoore bm = new BoyerMoore("test");
    assertEquals(10, bm.search(test1.toCharArray()));
    //shouldn't flag a partial
    assertEquals(0, bm.partialMatch());
  }

}
