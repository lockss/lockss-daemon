/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.util.*;

import org.lockss.test.*;

public class TestArchivalUnitStatus extends LockssTestCase {
  public void testDoubleToPercent() {
    // These tests match those in run_stf/testsuite.py
    assertEquals("0.00", ArchivalUnitStatus.doubleToPercent(0.0));
    assertEquals("0.00", ArchivalUnitStatus.doubleToPercent(0.00001));
    assertEquals("0.00", ArchivalUnitStatus.doubleToPercent(0.00009));
    assertEquals("0.01", ArchivalUnitStatus.doubleToPercent(0.0001));
    assertEquals("99.99", ArchivalUnitStatus.doubleToPercent(0.999999999));
    assertEquals("100.00", ArchivalUnitStatus.doubleToPercent(1.0));
  }
}
