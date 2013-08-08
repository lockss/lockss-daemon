/*
 * $Id: TestStringPool.java,v 1.2 2013-08-08 06:01:27 tlipkis Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestStringPool extends LockssTestCase {

  public void testIntern() {
    StringPool pool = new StringPool("name");
    assertNull(pool.intern(null));
    String s1 = new String("foo");
    String s2 = new String("foo");
    assertNotSame(s1, s2);
    String exp = pool.intern(s1);
    assertSame(s1, exp);

    assertNotSame(exp, s2);
    assertSame(exp, pool.intern(s2));
  }

  public void testSealed() {
    StringPool pool = new StringPool("name");
    pool.intern("str1");
    pool.intern("str2");
    pool.seal();

    assertNull(pool.intern(null));
    String s1 = new String("str1");
    String s2 = new String("str1");
    assertNotSame(s1, s2);
    String int1 = pool.intern(s1);
    assertNotSame(s1, int1);
    assertSame(int1, pool.intern(s2));

    String s3 = new String("str2");
    assertNotSame(s3, pool.intern(s3));
    String s4 = new String("strxxx");
    String s5 = new String("strxxx");
    assertSame(s4, pool.intern(s4));
    assertSame(s5, pool.intern(s5));
  }
}
