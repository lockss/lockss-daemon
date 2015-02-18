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

package org.lockss.servlet;

import java.text.*;
import java.util.*;

import org.apache.commons.lang3.time.FastDateFormat;

import org.lockss.util.*;
import org.lockss.daemon.status.*;

/** Convert values to display strings for the UI.  Currently behavior is
 * fixed, or can be overridden by subclass. */

public class DisplayConverter {
  
  private static final Logger log = Logger.getLogger(DisplayConverter.class);

  // Thread-safe formatters.
  // FastDateFormat is thread-safe, NumberFormat & subclasses aren't.

  /** Local time date formatter. */
  public static final FastDateFormat TABLE_DATE_FORMATTER_LOCAL =
    FastDateFormat.getInstance("HH:mm:ss MM/dd/yy");

  /** Local time date formatter. */
  public static final FastDateFormat TABLE_DATE_FORMATTER_GMT =
    FastDateFormat.getInstance("HH:mm:ss MM/dd/yy",
			       TimeZone.getTimeZone("GMT"),
			       Locale.US);


  protected Format getTableDateFormat() {
    return TABLE_DATE_FORMATTER_LOCAL;
  }

  private static final ThreadLocal<NumberFormat> agmntFmt = 
    new ThreadLocal<NumberFormat> () {
    @Override protected NumberFormat initialValue() {
      return new DecimalFormat("0.00");
    }
  };

  static NumberFormat getAgreementFormat() {
    return agmntFmt.get();
  }

  private static final ThreadLocal<NumberFormat> floatFmt = 
    new ThreadLocal<NumberFormat> () {
    @Override protected NumberFormat initialValue() {
      return new DecimalFormat("0.0");
    }
  };

  static NumberFormat getFloatFormat() {
    return floatFmt.get();
  }

  private static final ThreadLocal<NumberFormat> bigIntFmt = 
    new ThreadLocal<NumberFormat> () {
    @Override protected NumberFormat initialValue() {
      NumberFormat fmt = NumberFormat.getInstance();
//       if (fmt instanceof DecimalFormat) {
//         ((DecimalFormat)fmt).setDecimalSeparatorAlwaysShown(true);
//       }
      return fmt;
    }
  };

  public NumberFormat getBigIntFormat() {
    return bigIntFmt.get();
  }

  /* DecimalFormat automatically applies half-even rounding to
   * values being formatted under Java < 1.6.  This is a workaround. */ 
  public String doubleToPercent(double d) {
    int i = (int)(d * 10000);
    double pc = i / 100.0;
    return getAgreementFormat().format(pc);
  }

  // turn a value into a display string
  public String convertDisplayString(Object val, int type) {
    if (val == null) {
      return "";
    }
    try {
      switch (type) {
      case ColumnDescriptor.TYPE_INT:
	if (val instanceof Number) {
	  long lv = ((Number)val).longValue();
	  if (lv >= 1000000 || lv <= -1000000) {
	    return getBigIntFormat().format(lv);
	  }
	}
	// fall thru
      case ColumnDescriptor.TYPE_STRING:
      default:
	return val.toString();
      case ColumnDescriptor.TYPE_FLOAT:
	return getFloatFormat().format(((Number)val).doubleValue());
      case ColumnDescriptor.TYPE_PERCENT:
	float fv = ((Number)val).floatValue();
	return Integer.toString(Math.round(fv * 100)) + "%";
      case ColumnDescriptor.TYPE_AGREEMENT:
	float av = ((Number)val).floatValue();
	return doubleToPercent(av) + "%";
      case ColumnDescriptor.TYPE_DATE:
	Date d;
	if (val instanceof Number) {
	  d = new Date(((Number)val).longValue());
	} else if (val instanceof Date) {
	  d = (Date)val;
	} else if (val instanceof Deadline) {
	  d = ((Deadline)val).getExpiration();
	} else {
	  return val.toString();
	}
	return dateString(d);
      case ColumnDescriptor.TYPE_IP_ADDRESS:
	return ((IPAddr)val).getHostAddress();
      case ColumnDescriptor.TYPE_TIME_INTERVAL:
	long millis = ((Number)val).longValue();
	return StringUtil.timeIntervalToString(millis);
      }
    } catch (NumberFormatException e) {
      log.warning("Bad number: " + val.toString(), e);
      return val.toString();
    } catch (ClassCastException e) {
      log.warning("Wrong type value: " + val.toString(), e);
      return val.toString();
    } catch (Exception e) {
      log.warning("Error formatting value: " + val.toString(), e);
      return val.toString();
    }
  }

  public String dateString(Date d) {
    long val = d.getTime();
    if (val == 0 || val == -1) {
      return "never";
    } else {
      return getTableDateFormat().format(d);
    }
  }

}
