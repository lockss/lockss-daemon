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

package org.lockss.tdb;

import java.util.*;

import org.lockss.test.LockssTestCase;

public class TestTitle extends LockssTestCase {

  public static final String NAME_VALUE = "Title Name";
  public static final String DOI_VALUE = "DOI Value";
  public static final String EISSN_VALUE = "eISSN Value";
  public static final String ISSN_VALUE = "ISSN Value";
  public static final String ISSNL_VALUE = "ISSN-L Value";
  public static final String TYPE_VALUE = "Type Value";
  
  public static final String FOO_KEY = "titlefookey";
  public static final String FOO_VALUE = "titlefooval";

  public void testKeys() throws Exception {
    assertEquals("doi", Title.DOI);
    assertEquals("eissn", Title.EISSN);
    assertEquals("issn", Title.ISSN);
    assertEquals("issnl", Title.ISSNL);
    assertEquals("name", Title.NAME);
    assertEquals("type", Title.TYPE);
  }
  
  public void testType() throws Exception {
    assertEquals(Title.TYPE_JOURNAL, Title.TYPE_DEFAULT);
    assertEquals("book", Title.TYPE_BOOK);
    assertEquals("journal", Title.TYPE_JOURNAL);
  }
  
  public void testEmpty() throws Exception {
    Publisher publisher = new Publisher();
    Title title = new Title(publisher);
    assertSame(publisher, title.getPublisher());
    assertNull(title.getName());
    assertNull(title.getDoi());
    assertNull(title.getEissn());
    assertNull(title.getIssn());
    assertNull(title.getIssnl());
    assertEquals(Title.TYPE_DEFAULT, title.getType());
    assertNull(title.getArbitraryValue(FOO_KEY));
  }
  
  public void testTitle() throws Exception {
    Publisher publisher = new Publisher();
    Map<String, String> map = new HashMap<String, String>();
    Title title = new Title(publisher, map);
    assertSame(publisher, title.getPublisher());
    map.put(Title.NAME, NAME_VALUE);
    assertEquals(NAME_VALUE, title.getName());
    map.put(Title.DOI, DOI_VALUE);
    assertEquals(DOI_VALUE, title.getDoi());
    map.put(Title.EISSN, EISSN_VALUE);
    assertEquals(EISSN_VALUE, title.getEissn());
    map.put(Title.ISSN, ISSN_VALUE);
    assertEquals(ISSN_VALUE, title.getIssn());
    map.put(Title.ISSNL, ISSNL_VALUE);
    assertEquals(ISSNL_VALUE, title.getIssnl());
    map.put(Title.TYPE, TYPE_VALUE);
    assertEquals(TYPE_VALUE, title.getType());
    map.put(FOO_KEY, FOO_VALUE);
    assertEquals(FOO_VALUE, title.getArbitraryValue(FOO_KEY));
    assertNull(title.getArbitraryValue("X" + FOO_KEY));
  }
  
}
