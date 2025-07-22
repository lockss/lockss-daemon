/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.daemon;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.lang3.StringUtils;

import org.lockss.util.*;
import static org.lockss.daemon.ConfigParamDescr.*;

/**
 * Types of AU config params (ConfigParamDescr) and AU config functions.
 * Replacement for (and compatible with) ConfigParamDescr.TYPE_XXX
 */
public enum AuParamType {
  String(TYPE_STRING) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "String");
      return val;
    }
  },
  Int(TYPE_INT) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "Int");
      try {
        return Integer.valueOf(val);
      } catch (NumberFormatException nfe) {
        throw new InvalidFormatException("Value of type Int must parse as a valid integer", nfe);
      }
    }
  },
  PosInt(TYPE_POS_INT) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "PosInt");
      try {
        Object res = Integer.valueOf(val);
        if (((Integer)res).intValue() >= 0) {
          return res;
        } else {
          throw new InvalidFormatException("Value of type PosInt must be non-negative: " + val);
        }
      } catch (NumberFormatException nfe) {
        throw new InvalidFormatException("Value of type PosInt must parse as a valid integer", nfe);
      }
    }},
  Long(TYPE_LONG) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "Long");
      try {
        return java.lang.Long.valueOf(val);
      } catch (NumberFormatException nfe) {
        throw new InvalidFormatException("Value of type Long must parse as a valid long integer", nfe);
      }
    }},
  TimeInterval(TYPE_TIME_INTERVAL) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "TimeInterval");
      try {
        return StringUtil.parseTimeInterval(val);
      } catch (NumberFormatException nfe) {
        throw new InvalidFormatException("Value of type TimeInterval must parse as a valid time interval", nfe);
      }
    }},
  Url(TYPE_URL) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "Url");
      try {
        return new URL(val);
      } catch (MalformedURLException mue) {
        throw new InvalidFormatException("Value of type Url must parse as a valid URL", mue);
      }
    }},
  Year(TYPE_YEAR) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "Year");
      try {
        Object res = Integer.valueOf(val);
        int ival = ((Integer)res).intValue();
        if ((1000 <= ival && ival <= 9999) || ival == 0) {
          return res;
        } else {
          throw new InvalidFormatException("Value of type Year must be four digits, or set to 0: " + val);
        }
      } catch (NumberFormatException nfe) {
        throw new InvalidFormatException("Value of type Year must parse as a valid integer", nfe);
      }
    }},
  Boolean(TYPE_BOOLEAN) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "Boolean");
      switch (val.toLowerCase()) {
        case "true": case "yes": case "on": case "1": {
          return java.lang.Boolean.TRUE;
        }
        case "false": case "no": case "off": case "0": {
          return java.lang.Boolean.FALSE;
        }
        default: {
          throw new InvalidFormatException("Value of type Boolean must be a valid boolean string: " + val);
        }
      }
    }},
  Range(TYPE_RANGE) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "Range");
      Vector<String> pair = StringUtil.breakAt(val, '-', 2, true, true);
      // Turn range "S" into "S-S".
      if (pair.size() == 1 && val.indexOf("-") < 0) {
        pair.add(pair.firstElement());
      }
      if (pair.size() != 2) {
        throw new InvalidFormatException("Value of type Range must be two strings separated by a hyphen, or a single string: " + val);
      }
      String s_min = (String)pair.firstElement();
      String s_max = (String)pair.lastElement();
      if (s_min.compareTo(s_max) > 0) {
        throw new InvalidFormatException("Value of type Range must denote a non-decreasing string range: " + val);
      }
      return pair;
    }},
  NumRange(TYPE_NUM_RANGE) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "NumRange");
      Vector<String> pair = StringUtil.breakAt(val, '-', 2, true, true);
      // Turn range "N" into "N-N".
      if (pair.size() == 1 && val.indexOf("-") < 0) {
        pair.add(pair.firstElement());
      }
      if (pair.size() != 2) {
        throw new InvalidFormatException("Value of type NumRange must be two integers separated by a hyphen, or a single integer: " + val);
      }
      String s_min = (String)pair.firstElement();
      String s_max = (String)pair.lastElement();
      try {
        Long l_min = parseLongOrNull(s_min);
        Long l_max = parseLongOrNull(s_max);
        if (l_min.compareTo(l_max) <= 0) {
          Vector<Long> res = new Vector<>();
          res.add(l_min);
          res.add(l_max);
          return res;
        }
        throw new InvalidFormatException("Value of type NumRange must denote a non-decreasing numeric range: " + val);
      } catch (NumberFormatException nfe) {
        if (s_min.compareTo(s_max) <= 0) {
          return pair;
        }
        throw new InvalidFormatException("Value of type NumRange must denote a non-decreasing string range if not a valid non-decreasing numeric range: " + val);
      }
    }},
  Set(TYPE_SET) {
    public Object parse(String val) throws InvalidFormatException {
      return expandSetMacros(val);
    }},
  UserPasswd(TYPE_USER_PASSWD) {
    public Object parse(String val) throws InvalidFormatException {
      throwIfEmpty(val, "UserPasswd");
      List<String> lst = Arrays.asList(StringUtils.substringBefore(val, ':'),
                                       StringUtils.substringAfter(val, ':'));
      if (StringUtil.isNullString(lst.get(0)) || StringUtil.isNullString(lst.get(1))) {
        throw new InvalidFormatException("Value of type UserPasswd must consist of two non-empty strings separated by a colon: " + val);
      }
      return lst;
    }};

  private static Logger log = Logger.getLogger(AuParamType.class);

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
  
  protected static void throwIfEmpty(String str, String type) throws InvalidFormatException {
    if (StringUtil.isNullString(str)) {
      throw new InvalidFormatException(java.lang.String.format("Value of type %s must be non-empty", type));
    }
  }

  /** Thrown by {@link AuParamType#parse(String)} methods for invalid input.
   * This replaces the old {@code ConfigParamDescr.InvalidFormatException}. */
  public static class InvalidFormatException extends Exception {

    public InvalidFormatException(String msg) {
      super(msg);
    }

    public InvalidFormatException(String msg, Throwable thr) {
      super(msg, thr);
    }

    public InvalidFormatException(Throwable thr) {
      super(thr);
    }

  }

}
