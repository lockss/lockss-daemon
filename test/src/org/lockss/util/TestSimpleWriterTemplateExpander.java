/*
 * $Id$
 */

/*

Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.SimpleWriterTemplateExpander
 */

public class TestSimpleWriterTemplateExpander extends LockssTestCase {

  SimpleWriterTemplateExpander t;

  public void testNull() {
    try {
      new SimpleWriterTemplateExpander(null, new StringWriter());
      fail("null template should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      new SimpleWriterTemplateExpander("foo", null);
      fail("null template should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  void assertNoTokens(String tmpl) throws IOException {
    StringWriter wrtr = new StringWriter();
    t = new SimpleWriterTemplateExpander(tmpl, wrtr);
    assertNull(t.nextToken());
    assertEquals(tmpl, wrtr.toString());
  }

  public void testNoTokens() throws IOException {
    assertNoTokens("12345");
    assertNoTokens("12@345");
    assertNoTokens("@12345");
    assertNoTokens("12345@");
    assertNoTokens("12@@345");
    assertNoTokens("12@not a token@345");
    assertNoTokens("12@not-token@345");

    t = new SimpleWriterTemplateExpander("123", new StringWriter());
    assertNull(t.nextToken());
    try {
      t.nextToken();
      fail("Calling nextToken() after it returns null should throw");
    } catch (IllegalStateException e) {
    }
  }

  void assertOne(String exp, String tmpl, String token, String repl)
      throws IOException {
    StringWriter wrtr = new StringWriter();
    t = new SimpleWriterTemplateExpander(tmpl, wrtr);
    assertEquals(token, t.nextToken());
    wrtr.write(repl);
    assertNull(t.nextToken());
    log.critical("out: " + wrtr.toString());
    assertEquals(exp, wrtr.toString());
  }

  void assertRep(String exp, String tmpl, Map<String,String> map)
      throws IOException {
    StringWriter wrtr = new StringWriter();
    t = new SimpleWriterTemplateExpander(tmpl, wrtr);
    String tok;
    while ((tok = t.nextToken()) != null) {
      wrtr.write(map.get(tok));
    }
    assertEquals(exp, wrtr.toString());
  }

  public void testOne() throws IOException {
    assertOne("foo123bar", "foo@v1@bar", "v1", "123");
    assertOne("123foobar", "@v1@foobar", "v1", "123");
    assertOne("4foobar", "@v1@foobar", "v1", "4");
    assertOne("foobar123", "foobar@v1@", "v1", "123");
  }  

  public void testMult() throws IOException {
    Map m = MapUtil.map("v1","up","v2","down");
    assertRep("fooupdownbarupupxyz", "foo@v1@@v2@bar@v1@@v1@xyz", m);
  }  
}
