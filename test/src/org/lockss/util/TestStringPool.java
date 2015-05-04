/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

  public void testInternList() {
    StringPool pool = new StringPool("name");
    List lst1 = ListUtil.list(new String("foo"), new String("bar"));
    List lst2 = ListUtil.list(new String("bar"), new String("foo"));
    ArrayList intLst1 = pool.internList(lst1);
    ArrayList intLst2 = pool.internList(lst2);
    assertNotSame(intLst1, intLst2);
    assertSame(intLst1.get(0), intLst2.get(1));
    assertSame(intLst1.get(1), intLst2.get(0));
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

  public void testMapKeys1() {
    StringPool pool = new StringPool("gene");
    ConfigurationUtil.setFromArgs("org.lockss.stringPool.gene.mapKeys",
				  "key1;key2");
    String v1 = "val1";
    String v2 = "val2";
    assertSame(v1, pool.internMapValue("keyxyz", v1));
    assertNotSame(v1, pool.internMapValue("keyxyz", new String(v1)));
    assertSame(v2, pool.internMapValue("key2", v2));
    assertSame(v2, pool.internMapValue("key2", new String(v2)));
  }

  // Same but create pool after config set, which is a different code path
  // in StringPool
  public void testMapKeys2() {
    ConfigurationUtil.setFromArgs("org.lockss.stringPool.kiddie.mapKeys",
				  "key1;key2");
    StringPool pool = new StringPool("kiddie");
    String v1 = "val1";
    String v2 = "val2";
    assertSame(v1, pool.internMapValue("keyxyz", v1));
    assertNotSame(v1, pool.internMapValue("keyxyz", new String(v1)));
    assertSame(v2, pool.internMapValue("key2", v2));
    assertSame(v2, pool.internMapValue("key2", new String(v2)));
  }

  public void testMapKeys3() {
    StringPool pool = StringPool.TDBAU_PROPS;
    String v1 = "val1";
    String v2 = "val2";
    assertSame(v1, pool.internMapValue("keyxyz", v1));
    assertNotSame(v1, pool.internMapValue("keyxyz", new String(v1)));
    assertSame(v2, pool.internMapValue("type", v2));
    assertSame(v2, pool.internMapValue("type", new String(v2)));

    ConfigurationUtil.setFromArgs("org.lockss.stringPool.TdbAu props.mapKeys",
				  "key1;key2");
    assertSame(v2, pool.internMapValue("type", v2));
    assertNotSame(v2, pool.internMapValue("type", new String(v2)));
  }
}
