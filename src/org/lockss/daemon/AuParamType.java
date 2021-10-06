/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import static org.lockss.daemon.ConfigParamDescr.*;

/**
 * Types of AU config params (ConfigParamDescr) and AU config functions.
 * Replacement for (and compatible with) ConfigParamDescr.TYPE_XXX
 */
public enum AuParamType {
  String(TYPE_STRING) {
    public Object parse(String val) throws InvalidFormatException {
      if (!StringUtil.isNullString(val)) {
	return val;
      } else {
	throw new InvalidFormatException("Invalid String: " + val);
      }
    }
  },
  Int(TYPE_INT) {
    public Object parse(String val) throws InvalidFormatException {
      try {
	return Integer.valueOf(val);
      } catch (NumberFormatException nfe) {
	throw new InvalidFormatException("Invalid Int: " + val);
      }
    }
  },
  PosInt(TYPE_POS_INT) {
    public Object parse(String val) throws InvalidFormatException {
      try {
	Object res = Integer.valueOf(val);
	if (((Integer)res).intValue() >= 0) {
	  return res;
	} else {
	  throw new InvalidFormatException("Invalid Positive Int: " + val);
	}
      } catch (NumberFormatException nfe) {
	throw new InvalidFormatException("Invalid Positive Int: " + val);
      }
    }},
  Long(TYPE_LONG) {
    public Object parse(String val) throws InvalidFormatException {
      try {
	return java.lang.Long.valueOf(val);
      } catch (NumberFormatException nfe) {
	throw new InvalidFormatException("Invalid Long: " + val);
      }
    }},
  TimeInterval(TYPE_TIME_INTERVAL) {
    public Object parse(String val) throws InvalidFormatException {
      try {
	return StringUtil.parseTimeInterval(val);
      } catch (NumberFormatException nfe) {
	throw new InvalidFormatException("Invalid time interval: " + val);
      }
    }},
  Url(TYPE_URL) {
    public Object parse(String val) throws InvalidFormatException {
      try {
	return new URL(val);
      } catch (MalformedURLException ex) {
	throw new InvalidFormatException("Invalid URL: " + val, ex);
      }
    }},
  Year(TYPE_YEAR) {
    public Object parse(String val) throws InvalidFormatException {
      if (val.length() == 4 || "0".equals(val)) {
	try {
	  int i_val = Integer.parseInt(val);
	  if (i_val >= 0) {
	    return Integer.valueOf(val);
	  }
	} catch (NumberFormatException fe) {
	  // Fall through to throw statement below
	}
      }
      throw new InvalidFormatException("Invalid Year: " + val);
    }},
  Boolean(TYPE_BOOLEAN) {
    public Object parse(String val) throws InvalidFormatException {
      if (val.equalsIgnoreCase("true") ||
	  val.equalsIgnoreCase("yes") ||
	  val.equalsIgnoreCase("on") ||
	  val.equalsIgnoreCase("1")) {
	return java.lang.Boolean.TRUE;
      }
      else if (val.equalsIgnoreCase("false") ||
	       val.equalsIgnoreCase("no") ||
	       val.equalsIgnoreCase("off") ||
	       val.equalsIgnoreCase("0")) {
	return java.lang.Boolean.FALSE;
      } else {
	throw new InvalidFormatException("Invalid Boolean: " + val);
      }
    }},
  Range(TYPE_RANGE) {
    public Object parse(String val) throws InvalidFormatException {
      Vector pair = StringUtil.breakAt(val, '-', 2, true, true);
      // Turn range "S" into "S-S".
      if (pair.size() == 1 && val.indexOf("-") < 0) {
	pair.add(pair.firstElement());
      }
      if (pair.size() != 2) {
	throw new InvalidFormatException("Invalid Range: " + val);
      }
      String s_min = (String)pair.firstElement();
      String s_max = (String)pair.lastElement();
      if ( !(s_min.compareTo(s_max) <= 0) ) {
	throw new InvalidFormatException("Invalid Range: " + val);
      }
      return pair;
    }},
  NumRange(TYPE_NUM_RANGE) {
    public Object parse(String val) throws InvalidFormatException {
      Vector pair = StringUtil.breakAt(val,'-',2,true, true);

      // Turn range "N" into "N-N".
      if (pair.size() == 1 && val.indexOf("-") < 0) {
	pair.add(pair.firstElement());
      }
      if (pair.size() != 2) {
	throw new InvalidFormatException("Invalid Range: " + val);
      }
      String s_min = (String)pair.firstElement();
      String s_max = (String)pair.lastElement();
      try {
	Long l_min = parseLongOrNull(s_min);
	Long l_max = parseLongOrNull(s_max);
	if (l_min.compareTo(l_max) <= 0) {
	  pair.setElementAt(l_min, 0);
	  pair.setElementAt(l_max, 1);
	  return pair;
	}
      } catch (NumberFormatException ex1) {
	if (s_min.compareTo(s_max) <= 0) {
	  return pair;
	}
      }
      throw new InvalidFormatException("Invalid Numeric Range: " + val);
    }},
  Set(TYPE_SET) {
    public Object parse(String val) throws InvalidFormatException {
      return expandSetMacros(val);
    }},
  UserPasswd(TYPE_USER_PASSWD) {
    public Object parse(String val) throws InvalidFormatException {
      if (!StringUtil.isNullString(val)) {
	List<String> lst = StringUtil.breakAt(val, ':', -1, true, false);
	if (lst.size() != 2) { // FIXME
//	  throw new InvalidFormatException("User:Passwd must consist of two" +
//					   "strings separated by a colon: " +
//					   val);
	}
	return lst;
      } else {
	throw new InvalidFormatException("Invalid String: " + val);
      }
    }};

  private static Logger log = Logger.getLogger("AuParamType");

  static AuParamType[] vals = new AuParamType[MAX_TYPE + 1];
  static {
    for (AuParamType aupt : AuParamType.values()) {
      vals[aupt.typeInt] = aupt;
    }
  }
  final int typeInt;

  public static Long parseLongOrNull(String s) throws NumberFormatException {
    if (s == null) {
      return null;
    }
    return java.lang.Long.valueOf(s);
  }

  /** Parse a String, return a type-dependent value type */
  abstract public Object parse(String val) throws InvalidFormatException;

  /** Convert from ConfigParamDescr.TYPE_XXX to enum */
  public static AuParamType fromTypeInt(int n) {
    try {
      AuParamType ret = vals[n];
      if (ret != null) {
	return ret;
      }
      throw new IllegalArgumentException("No AuParamType with value " + n);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("No AuParamType with value " + n);
    }
  }

  private AuParamType(int cpdType) {
    this.typeInt = cpdType;
  }

  /** Convert enum to ConfigParamDescr.TYPE_XXX */
  public int typeInt() {
    return typeInt;
  }

  // Pattern matches an integer range macro within a set
  private static final String SET_MACRO_RANGE =
    RegexpUtil.quotemeta(SET_RANGE_OPEN) +
    "\\s*(\\d+)\\s*-\\s*(\\d+)\\s*" +
    RegexpUtil.quotemeta(SET_RANGE_CLOSE);

  private static Pattern SET_MACRO_RANGE_PAT =
  Pattern.compile(SET_MACRO_RANGE);


  List<String> expandSetMacros(String setSpec) {
    List<String> raw = StringUtil.breakAt(setSpec,',', MAX_SET_SIZE,
					  true, true);
    // Avoid cost of pattern matches and list copy in the usual case of no
    // range macros
    for (String ele : raw) {
      if (ele.startsWith(SET_RANGE_OPEN) && ele.endsWith(SET_RANGE_CLOSE)) {
	return expandSetMacros0(raw);
      }
    }
    return raw;
  }

  private List<String> expandSetMacros0(List<String> raw) {
    int size = raw.size();
    List<String> res = new ArrayList<String>(size + 50);
    for (String ele : raw) {
      Matcher m1 = SET_MACRO_RANGE_PAT.matcher(ele);
      if (m1.matches()) {
	try {
	  int beg = Integer.valueOf(m1.group(1));
	  int end = Integer.valueOf(m1.group(2));
	  for (int ix = beg; ix <= end; ix++) {
	    if (size++ > MAX_SET_SIZE) {
	      log.warning("Set value has more than " + MAX_SET_SIZE +
			  " elements; only the first " + MAX_SET_SIZE +
			  " will be used: " + raw);
	      return res;
	    }
	    res.add(Integer.toString(ix));
	  }
	} catch (RuntimeException e) {
	  log.warning("Suspicious Set range macro: " + ele + " not expanded",
		      e);
	  res.add(ele);
	}
      } else {
	res.add(ele);
      }
    }
    return res;
  }

  /** Throws by parse() methods for invalid input.  This is replacing
   * {@link ConfigParamDescr.InvalidFormatException} */
  public static class InvalidFormatException
    extends ConfigParamDescr.InvalidFormatException {

    private Throwable nestedException;

    public InvalidFormatException(String msg) {
      super(msg);
    }

    public InvalidFormatException(String msg, Throwable e) {
      super(msg + (e.getMessage() == null ? "" : (": " + e.getMessage())));
      this.nestedException = e;
    }

    public Throwable getNestedException() {
      return nestedException;
    }
  }

}
