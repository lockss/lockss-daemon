/*
 * $Id$
 */

/*
n
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

import java.util.*;
import org.lockss.test.*;


/**
 * Test class for <code>org.lockss.scheduler.TimeBase</code>
 */

public class TestTimeBase extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.TimeBase.class
  };

  static Logger log = Logger.getLogger("TestTimeBase");

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConstants() {
    assertEquals(Long.MAX_VALUE, TimeBase.MAX);
  }

  public void testReal() {
    TimeBase.setReal();
    assertFalse(TimeBase.isSimulated());
    long now = TimeBase.nowMs();
    TimerUtil.guaranteedSleep(10);
    assertTrue(TimeBase.nowMs() > now);
  }

  public void testSimulated() throws Exception {
    TimeBase.setSimulated();
    assertTrue(TimeBase.isSimulated());
    assertEquals(0, TimeBase.nowMs());
    TimeBase.setSimulated(100);
    assertTrue(TimeBase.isSimulated());
    assertEquals(100, TimeBase.nowMs());
    TimeBase.step();
    assertEquals(101, TimeBase.nowMs());
    TimeBase.step(10);
    assertEquals(111, TimeBase.nowMs());
    assertEquals(new Date(111), TimeBase.nowDate());

    assertEquals(11, TimeBase.msSince(100));
    assertEquals(89, TimeBase.msUntil(200));

    TimeBase.setReal();
    assertFalse(TimeBase.isSimulated());
    TimeBase.setSimulated(100);
    assertTrue(TimeBase.isSimulated());

    TimeBase.setReal();
    assertFalse(TimeBase.isSimulated());
    TimeBase.setSimulated("1970/1/1 0:00:00");
    assertTrue(TimeBase.isSimulated());
    assertEquals(0, TimeBase.nowMs());
    TimeBase.setSimulated("1970/1/1 01:00:00");
    assertTrue(TimeBase.isSimulated());
    assertEquals(3600000, TimeBase.nowMs());
    TimeBase.setSimulated("1970/1/2 01:00:00");
    assertTrue(TimeBase.isSimulated());
    assertEquals(90000000, TimeBase.nowMs());
  }

  public void testCalendar() throws Exception {
    TimeBase.setReal();
    Calendar tbcal = TimeBase.nowCalendar();
    Calendar cal = Calendar.getInstance();
    int tbyear = tbcal.get(Calendar.YEAR);
    int year = cal.get(Calendar.YEAR);
    assertTrue(year == tbyear || (year == tbyear + 1 &&
				  tbcal.get(Calendar.MONTH) == 11 &&
				  cal.get(Calendar.MONTH) == 0));

    TimeBase.setSimulated("1970/6/15 01:00:00");
    Calendar simtbcal = TimeBase.nowCalendar();
    assertEquals(1970, simtbcal.get(Calendar.YEAR));
    assertEquals(5, simtbcal.get(Calendar.MONTH));
  }

}
