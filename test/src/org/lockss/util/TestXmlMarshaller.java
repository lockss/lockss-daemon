/*
 * $Id: TestXmlMarshaller.java,v 1.1 2004-02-07 06:45:57 eaalto Exp $
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
import java.io.File;
import org.lockss.test.LockssTestCase;

/**
 * This is the test class for org.lockss.util.XmlMarshaller
 */
public class TestXmlMarshaller extends LockssTestCase {
  XmlMarshaller marshaller;
  String tempDirPath;

  static final String FILE_NAME = "testFile";
  static final String MAPPING_FILE_NAME = "/org/lockss/util/marshaltest.xml";

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    marshaller = new XmlMarshaller();
  }

  public void testMarshallString() throws Exception {
    Tester tester = new Tester();
    tester.setInt(123);
    tester.setString("test");
    tester.setList(ListUtil.list("entry1", "entry2"));

    marshaller.store(tempDirPath, FILE_NAME, tester, MAPPING_FILE_NAME);

    File xmlFile = new File(tempDirPath + FILE_NAME);
    assertTrue(xmlFile.exists());

    tester = (Tester)marshaller.load(tempDirPath + FILE_NAME,
        Tester.class, MAPPING_FILE_NAME);
    assertEquals(123, tester.getInt());
    assertEquals("test", tester.getString());
    assertIsomorphic(ListUtil.list("entry1", "entry2"), tester.getList());
  }

  public static class Tester {
    String testStr;
    int testInt;
    List testList;

    public Tester() { }

    public String getString() {
      return testStr;
    }

    public void setString(String newStr) {
      testStr = newStr;
    }

    public int getInt() {
      return testInt;
    }

    public void setInt(int newInt) {
      testInt = newInt;
    }

    public List getList() {
      return testList;
    }

    public void setList(List newList) {
      testList = newList;
    }
  }

}

