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

package org.lockss.crawler;
import java.util.*;
import java.io.*;

import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.test.*;

public class TestPermissionRecord extends LockssTestCase {

  private String URL = "http://www.example.com/index.html";
  private String HOST = "www.example.com";

  public void setUp() throws Exception {
    super.setUp();
  }

  public void testNulls() {
    try {
      new PermissionRecord(null, "host");
      fail("Null url should throw");
    } catch (IllegalArgumentException e) {
    }
    try {
      new PermissionRecord("url", null);
      fail("Null host should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testAccessors() {
    PermissionRecord record = new PermissionRecord(URL, HOST);
    assertEquals(URL, record.getUrl());
    assertEquals(HOST, record.getHost());
  }

  public void testStatus() {
    PermissionRecord record = new PermissionRecord(URL, HOST);
    assertEquals(record.getStatus(),
		 PermissionStatus.PERMISSION_UNCHECKED);
    record.setStatus(PermissionStatus.PERMISSION_OK);
    assertEquals(PermissionStatus.PERMISSION_OK, record.getStatus());
  }

}
