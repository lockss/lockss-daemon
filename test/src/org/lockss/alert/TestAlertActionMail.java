/*
 * $Id: TestAlertActionMail.java,v 1.1 2004-07-12 06:09:41 tlipkis Exp $
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

package org.lockss.alert;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.alert.AlertActionMail
 */
public class TestAlertActionMail extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAlertActionMail");

  public void testEquals() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    AlertActionMail a2 = new AlertActionMail("to@here");
    AlertActionMail a3 = new AlertActionMail("to@there");
    assertEquals(a1, a2);
    assertEquals(a2, a1);
    assertNotEquals(a1, a3);
    assertNotEquals(a3, a1);
  }

  public void testHash() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    AlertActionMail a2 = new AlertActionMail("to@here");
    AlertActionMail a3 = new AlertActionMail("to@there");
    assertEquals(a1.hashCode(), a2.hashCode());
  }
}
