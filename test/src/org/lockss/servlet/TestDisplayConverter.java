/*
 * $Id$
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

package org.lockss.servlet;

import java.util.*;
import java.text.*;

import org.lockss.test.*;
import org.lockss.util.*;
import static org.lockss.daemon.status.ColumnDescriptor.*;

public class TestDisplayConverter extends LockssTestCase {

  static Logger log = Logger.getLogger("TestDisplayConverter");

  private DisplayConverter dc;

  protected void setUp() throws Exception {
    super.setUp();
    dc = new GmtDisplayConverter();
  }

  String cds(Object val, int type) {
    return dc.convertDisplayString(val, type);
  }

  public void testInt() throws Exception {
    assertEquals("", cds(null, TYPE_INT));
    assertEquals("375", cds(375, TYPE_INT));
    assertEquals("-375", cds(-375, TYPE_INT));
    assertEquals("1375", cds(Integer.valueOf(1375), TYPE_INT));
    assertEquals("3752", cds(Long.valueOf(3752), TYPE_INT));
    assertEquals("-3752", cds(Long.valueOf(-3752), TYPE_INT));
    assertEquals("321.25", cds(321.25, TYPE_INT));
    assertEquals("-321.25", cds(-321.25, TYPE_INT));
    assertEquals("1321.5", cds(Double.valueOf(1321.5), TYPE_INT));
    assertEquals("32,112,345", cds(32112345, TYPE_INT));
    assertEquals("-32,112,345", cds(-32112345, TYPE_INT));
    // Decimal dropped on large numbers
    assertEquals("13,210,123", cds(13210123.5, TYPE_INT));
    assertEquals("-13,210,123", cds(-13210123.5, TYPE_INT));

    // non Number uses toString()
    assertEquals("strang", cds("strang", TYPE_INT));
    assertEquals("[37, 8]", cds(ListUtil.list(37,8), TYPE_INT));
  }

  // FLOAT currently hardwired to 1 decimal place
  public void testFloat() throws Exception {
    assertEquals("", cds(null, TYPE_FLOAT));
    assertEquals("375.0", cds(375, TYPE_FLOAT));
    assertEquals("-375.0", cds(-375, TYPE_FLOAT));
    assertEquals("3751.4", cds(3751.35, TYPE_FLOAT));
    assertEquals("1.2", cds(Double.valueOf(1.234567), TYPE_FLOAT));
    assertEquals("-1.2", cds(Double.valueOf(-1.234567), TYPE_FLOAT));
    assertEquals("3752.0", cds(Long.valueOf(3752), TYPE_FLOAT));
    assertEquals("32112345.0", cds(32112345, TYPE_FLOAT));
    assertEquals("-32112345.0", cds(-32112345, TYPE_FLOAT));

    // non float generates a warning then uses toString()
    assertEquals("Expect ClassCastException",
		 cds("Expect ClassCastException", TYPE_FLOAT));
    assertEquals("[37, 8]", cds(ListUtil.list(37,8), TYPE_FLOAT));
  }

  public void testPercent() throws Exception {
    assertEquals("", cds(null, TYPE_PERCENT));
    assertEquals("0%", cds(0, TYPE_PERCENT));
    assertEquals("0%", cds(0.0, TYPE_PERCENT));
    assertEquals("0%", cds(0.000001, TYPE_PERCENT));
    assertEquals("70%", cds(.7, TYPE_PERCENT));
    assertEquals("-30%", cds(-.3, TYPE_PERCENT));
    assertEquals("100%", cds(1, TYPE_PERCENT));
    assertEquals("100%", cds(1.0, TYPE_PERCENT));
    assertEquals("90%", cds(0.89999, TYPE_PERCENT));
    assertEquals("100%", cds(0.99999, TYPE_PERCENT));
    assertEquals("723%", cds(7.23, TYPE_PERCENT));
    assertEquals("724%", cds(7.239, TYPE_PERCENT));
    assertEquals("-724%", cds(-7.239, TYPE_PERCENT));

    // non number generates a warning then uses toString()
    assertEquals("Expect ClassCastException",
		 cds("Expect ClassCastException", TYPE_PERCENT));
    assertEquals("[37, 8]", cds(ListUtil.list(37,8), TYPE_PERCENT));
  }

  public void testAgreement() throws Exception {
    assertEquals("", cds(null, TYPE_AGREEMENT));
    assertEquals("0.00%", cds(0, TYPE_AGREEMENT));
    assertEquals("0.00%", cds(0.0, TYPE_AGREEMENT));
    assertEquals("0.00%", cds(0.000001, TYPE_AGREEMENT));
    assertEquals("69.99%", cds(.7, TYPE_AGREEMENT));
    assertEquals("75.00%", cds(.75, TYPE_AGREEMENT));
    assertEquals("-30.00%", cds(-.3, TYPE_AGREEMENT));
    assertEquals("100.00%", cds(1, TYPE_AGREEMENT));
    assertEquals("100.00%", cds(1.0, TYPE_AGREEMENT));
    assertEquals("89.99%", cds(0.89999999, TYPE_AGREEMENT));
    assertEquals("99.99%", cds(0.99999, TYPE_AGREEMENT));
    assertEquals("723.00%", cds(7.23, TYPE_AGREEMENT));

    // non number generates a warning then uses toString()
    assertEquals("Expect ClassCastException",
		 cds("Expect ClassCastException", TYPE_AGREEMENT));
    assertEquals("[37, 8]", cds(ListUtil.list(37,8), TYPE_AGREEMENT));
  }

  public void testDate() throws Exception {
    assertEquals("", cds(null, TYPE_DATE));
    assertEquals("never", cds(0, TYPE_DATE));
    assertEquals("never", cds(0.0, TYPE_DATE));
    long then =
      1234 * Constants.YEAR + 13 * Constants.HOUR + 505 * Constants.SECOND;
    assertEquals("13:08:25 03/08/03", cds(then, TYPE_DATE));
    assertEquals("13:08:25 03/08/03", cds(new Date(then), TYPE_DATE));
    assertEquals("13:08:25 03/08/03", cds(Deadline.at(then), TYPE_DATE));

    // non date generates a warning then uses toString()
    assertEquals("Expect ClassCastException",
		 cds("Expect ClassCastException", TYPE_DATE));
    assertEquals("[37, 8]", cds(ListUtil.list(37,8), TYPE_DATE));
  }

  public void testTimeInterval() throws Exception {
    assertEquals("", cds(null, TYPE_TIME_INTERVAL));
    assertEquals("0ms", cds(0, TYPE_TIME_INTERVAL));
    assertEquals("0ms", cds(0.0, TYPE_TIME_INTERVAL));
    assertEquals("15s", cds(15 * Constants.SECOND, TYPE_TIME_INTERVAL));
    assertEquals("4m20s", cds(260 * Constants.SECOND, TYPE_TIME_INTERVAL));
    assertEquals("7h14m0s", cds(7 * Constants.HOUR + 14 * Constants.MINUTE,
				TYPE_TIME_INTERVAL));
    assertEquals("7h14m10s",
		 cds(7 * Constants.HOUR + (14*60+10) * Constants.SECOND,
		     TYPE_TIME_INTERVAL));
    assertEquals("7d0h14m",
		 cds(7 * Constants.DAY + (14*60+10) * Constants.SECOND,
		     TYPE_TIME_INTERVAL));

    // non number generates a warning then uses toString()
    assertEquals("Expect ClassCastException",
		 cds("Expect ClassCastException", TYPE_TIME_INTERVAL));
    assertEquals("[37, 8]", cds(ListUtil.list(37,8), TYPE_TIME_INTERVAL));
  }

  // Use fixed timezone to get predictable strings
  class GmtDisplayConverter extends DisplayConverter {
    protected Format getTableDateFormat() {
      return DisplayConverter.TABLE_DATE_FORMATTER_GMT;
    }
  }
}
