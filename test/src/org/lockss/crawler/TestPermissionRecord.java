/*
 * $Id: TestPermissionRecord.java,v 1.2 2004-07-16 22:49:29 dcfok Exp $
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
import org.lockss.test.*;

public class TestPermissionRecord extends LockssTestCase {
 
  private String permissionUrl = "http://www.example.com/index.html";
  private PermissionRecord record;

  public void setUp() throws Exception {
    super.setUp();
    record 
      = new PermissionRecord(permissionUrl, PermissionRecord.PERMISSION_OK);
  }
  
  public void testPrThrowsForNullUrl() {
    try{
      record = new PermissionRecord(null, PermissionRecord.PERMISSION_OK);
       fail("Constructing a PermissionRecord with a null permissionUrl"
	   +" should throw an IllegalArgumentException");
    }catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperUrl() {
    assertEquals(record.getPermissionUrl(), permissionUrl);
  }

  public void testReturnsProperStatus() {
    assertEquals(record.getPermissionStatus(), PermissionRecord.PERMISSION_OK);
  }

  public void testSetUrl() {
    String newUrl = "http://www.example.com/start.html";
    record.setPermissionUrl(newUrl);
    assertEquals(record.getPermissionUrl(), newUrl);
  }

  public void testSetStatus() {
    record.setPermissionStatus(PermissionRecord.PERMISSION_NOT_OK);
    assertEquals(record.getPermissionStatus(), PermissionRecord.PERMISSION_NOT_OK);
  }

}
