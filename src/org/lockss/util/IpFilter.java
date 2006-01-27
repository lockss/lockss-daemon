/*
 * $Id: IpFilter.java,v 1.8.10.1 2006-01-27 04:24:40 tlipkis Exp $
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

package org.lockss.util;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.app.*;

/**
 * IpFilter objects represent either an IP address or a range of addresses
 * (addr and mask).
 * Static methods support a single pair of include and exclude filter arrays,
 * and lookups.
 */
public class IpFilter {
  private static Logger log = Logger.getLogger("IpFilter");

  private Mask[] inclFilters;
  private Mask[] exclFilters;

  /** Return the union of two filter lists */
  public static String unionFilters(String filters1, String filters2) {
    List lst1 =
      StringUtil.breakAt(filters1, Constants.LIST_DELIM_CHAR, 0, true, true);
    List lst2 =
      StringUtil.breakAt(filters2, Constants.LIST_DELIM_CHAR, 0, true, true);
    for (Iterator iter = lst2.iterator(); iter.hasNext(); ) {
      Object o = iter.next();
      if (!lst1.contains(o)) {
	lst1.add(o);
      }
    }
    return StringUtil.separatedString(lst1, Constants.LIST_DELIM);
  }

  /** Set include and exclude access lists from LIST_DELIM-separated strings.
   */
  public void setFilters(String includeList, String excludeList)
      throws MalformedException {
    setFilters(StringUtil.breakAt(includeList, Constants.LIST_DELIM_CHAR),
	       StringUtil.breakAt(excludeList, Constants.LIST_DELIM_CHAR));
  }

  /** Set include and exclude access lists from vectors of Mask strings.
   * Malformed entries in the include list are ignored (and logged),
   * malformed entries in the exclude list cause failure, as omitting an
   * exclude entry could be dangerous.
   * @throws MalformedException
   */
  public void setFilters(Vector inclVec, Vector exclVec)
      throws MalformedException {
    inclFilters = new Mask[inclVec.size()];
    exclFilters = new Mask[exclVec.size()];
    int i = 0;
    for (Iterator iter = inclVec.iterator(); iter.hasNext();) {
      try {
	// must be separate statements to ensure i doesn't get incremented
	// if Mask constructor throws
	Mask mask = new Mask((String)iter.next(), true);
	inclFilters[i++] = mask;
      } catch (MalformedException e) {
	log.warning("Malformed IP filter in include list, ignoring: " +
		    e.getMalformedIp(), e);
	// make the array shorter by one.
	Mask[] tmp = new Mask[inclFilters.length - 1];
	System.arraycopy(inclFilters, 0, tmp, 0, i);
	inclFilters = tmp;
      }
    }
    i = 0;
    for (Iterator iter = exclVec.iterator(); iter.hasNext();) {
      exclFilters[i++] = new Mask((String)iter.next(), true);
    }
//      logFilters();
  }

  /** Search for matching element in filter list
   */
  private boolean isInList(Mask ip, Mask[] list) {
    for (int i = 0; i < list.length; i++) {
      if (ip.match(list[i])) {
	return true;
      }
    }
    return false;
  }

  /** Return true if ip addr string is allowed by filters
   */
  public boolean isIpAllowed(String ipstr)
      throws MalformedException {
    Addr ip = new Addr(ipstr);
    return isIpAllowed(ip);
  }

  /** Return true if Addr instance is allowed by filters
   */
  public boolean isIpAllowed(Addr ip) {
    if (inclFilters == null) return false;
    if (!isInList(ip, inclFilters)) return false;
    if (isInList(ip, exclFilters)) return false;
    return true;
  }

  private void logFilters() {
    StringBuffer sb = new StringBuffer();
    sb.append("Include: ");
    for (int i = 0; i < inclFilters.length; i++) {
      sb.append(inclFilters[i]);
      sb.append(" ");
    }
    sb.append("\nExclude: ");
    for (int i = 0; i < exclFilters.length; i++) {
      sb.append(exclFilters[i]);
      sb.append(" ");
    }
    sb.append("\n");
    System.err.print(sb.toString());
  }

  /** Represents an IP address mask */
  public static class Mask {

    private int addr = 0;
    private int mask = -1;
    private int cidr = 32;

    /**/
    /** Constructor for an IP address possibly including a mask
     */
    public Mask(String s, boolean maskOk) throws MalformedException {
      s = s.trim();
      StringTokenizer en = new StringTokenizer(s, "./", true);
      boolean seenStar = false;
      int n = 0;
      int b;
      try {
	while (en.hasMoreTokens()) {
	  String tok = en.nextToken();

	  if ("*".equals(tok)) {
	    b = -1;
	  } else {
	    b = Integer.parseInt(tok);
	  }
	  if (b == -1) {
	    if (!seenStar) {
	      cidr = n * 8;
	    }
	    seenStar = true;
	    b = 0;
	  } else if (seenStar) {
	    throw new MalformedException("* in illegal position", s);
	  }
	  addr = (addr << 8) | b;
	  n++;
	  if (en.hasMoreTokens()) {
	    tok = en.nextToken();
	    if (".".equals(tok)) {
	      if (n < 4) {
		continue;
	      } else {
		throw new MalformedException("Too many dots", s);
	      }
	    }
	    if ("/".equals(tok)) {
	      if (n < 1 || seenStar || !en.hasMoreTokens()) {
		throw new MalformedException("Illegal CIDR notation", s);
	      }
	      tok = en.nextToken();
	      cidr = Integer.parseInt(tok);
	      if (cidr < 0 || cidr > 32) {
		throw new MalformedException("Illegal CIDR notation", s);
	      }
	      if (en.hasMoreTokens()) {
		throw new MalformedException("Junk at end", s);
	      }
	      // if fewer than 4 bytes, simulate enough 0 bytes
	      addr <<= (4 - n) * 8;
	      n = 4;
	    }
	  }
	}
      } catch (NumberFormatException e) {
	throw new MalformedException("Illegal number", s);
      }
      if (n != 4) {
	throw new MalformedException("Must have 4 bytes", s);
      }
      mask = (cidr == 32) ? -1 : -(1 << (32 - cidr));
      if (mask != -1 && !maskOk) {
	throw new MalformedException("Mask not allowed", s);
      }
      if ((addr & ~mask) != 0) {
	throw new MalformedException("Illegal CIDR notation", s);
      }
    }

    /** Return true if obj equal this.
     */
    public boolean equals(Object obj) {
      if (obj instanceof Mask) {
	Mask ip = (Mask)obj;
	return (addr == ip.addr) && (mask == ip.mask);
      }
      return false;
    }

    public int hashCode() {
      return 3*addr + mask;
    }

    /** Return true if ip is equal to this, or if either ip or this is a
     * mask that matches the other.
     */
    public boolean match(Mask ip) {
      if (ip.mask == -1) {
	return (ip.addr & mask) == addr;
      } else if (mask == -1) {
	return ip.match(this);
      } else {
	// both have mask, treat as equals()
	return equals(ip);
      }
    }

    /** Return string representation
     */
    public String toString () {
      StringBuffer sb = new StringBuffer(19);
      for (int i = 24; i >= 0; i -= 8) {
	sb.append(Integer.toString((addr >>> i) & 0xff));
	if (i > 0) {
	  sb.append(".");
	}
      }
      if (mask != -1) {
	sb.append("/");
	sb.append(Integer.toString(cidr));
      }
      return sb.toString();
    }
  }

  public static class Addr extends Mask {
    /**
     * Constructor for single IP address (no mask)
     */
    public Addr(String ipAddr) throws MalformedException {
      super(ipAddr, false);
    }
  }


  public static class MalformedException extends Exception {
    private String ip;
    public MalformedException (String msg, String ip) {
      super(msg);
      this.ip = ip;
    }

    public String getMalformedIp() {
      return ip;
    }

    public String toString() {
      return super.toString() +": \"" + ip + "\"";
    }
  }

}
