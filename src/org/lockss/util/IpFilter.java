/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.app.*;

/**
 * IpFilter accepts or rejects IP addresses based on an include list and an
 * exclude list of address masks.  To be accepted, a candidate IP address
 * must match at least one mask on the include list, and must not match any
 * patterns on the exclude list.  IPv4 masks are specified as either a
 * complete address, an address with 1-3 final stars (<i>eg</i>10.*.*.*) or
 * in CIDR notation (<i>eg</i>10.0.0.0/8).  IPv6 masks are specified as
 * either a complete address or in CIDR notation (<i>eg</i>ffff:1234::/32).
 * IPv4-mapped addresses are not allowed.
 */
public class IpFilter {
  private static Logger log = Logger.getLogger("IpFilter");

  private Mask[] inclFilters;
  private Mask[] exclFilters;

  /** Return the union of two filter lists */
  public static String unionFilters(String filters1, String filters2) {
    List<String> lst1 = breakStr(filters1);
    List<String> lst2 = breakStr(filters2);
    for (String s2 : lst2) {
      if (!lst1.contains(s2)) {
	lst1.add(s2);
      }
    }
    return StringUtil.separatedString(lst1, Constants.LIST_DELIM);
  }

  /** Set include and exclude access lists from LIST_DELIM-separated strings.
   */
  public void setFilters(String includeList, String excludeList)
      throws MalformedException {
    setFilters(breakStr(includeList), breakStr(excludeList));
  }

  private static List<String> breakStr(String str) {
    return StringUtil.breakAt(str, Constants.LIST_DELIM_CHAR, 0, true, true);
  }

  /** Set include and exclude access lists from lists of Mask strings.
   * Elements that start with # are comments, ignored.
   * Malformed entries in the include list are ignored (and logged),
   * malformed entries in the exclude list cause failure, as omitting an
   * exclude entry could be dangerous.
   * @throws MalformedException
   */
  public void setFilters(List inclVec, List exclVec)
      throws MalformedException {
    inclFilters = parseMasks(inclVec, "include", false);
    exclFilters = parseMasks(exclVec, "exclude", true);
//      logFilters();
  }


  Mask[] parseMasks(List<String> masks, String msg, boolean malformedIsFatal)
      throws MalformedException {
    List<Mask> res = new ArrayList<Mask>();
    for (String str : masks) {
      if (str.startsWith("#")) {
	continue;
      }
      try {
	res.add(newMask(str));
      } catch (MalformedException e) {
	log.warning("Malformed IP filter in " + msg + " list, ignoring: " +
		    e.getMalformedIp(), e);
	if (malformedIsFatal) {
	  throw e;
	}
      }
    }
    return (res.toArray(new Mask[0]));
  }


  /** Search for matching element in filter list
   */
  private boolean isInList(Addr ip, Mask[] list) {
    for (int i = 0; i < list.length; i++) {
      if (list[i].match(ip)) {
	return true;
      }
    }
    return false;
  }

  /** Return true if ip addr string is allowed by filters
   */
  public boolean isIpAllowed(String ipstr)
      throws MalformedException {
    Addr ip = newAddr(ipstr);
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
    StringBuilder sb = new StringBuilder();
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

  // Patterns used to quickly distinguish IPv4 from IPv6 addresses & masks,
  // and to ensure they are numeric.  More stringent syntax checks are
  // performed in the Mask4 and Mask6 constructors.

  private static Pattern IPV4_ADDR = Pattern.compile("[\\d*.]+");
  // IPv6 addresses can end with "%<zone index>"
  private static Pattern IPV6_ADDR = Pattern.compile("([\\p{XDigit}:]+)(%.*)?");

  private static Pattern IPV4_MASK =
    Pattern.compile("[\\d*.]+(?:/[\\d.]+)?");
  // This one is also used to separate addr from CIDR
  private static Pattern IPV6_MASK =
    Pattern.compile("([\\p{XDigit}:]+)(?:/([\\d.]+))?");

  /** Create a new IPv4 or IPv6 Address object */
  public static Addr newAddr(String str) throws MalformedException {
    Matcher m4 = IPV4_ADDR.matcher(str);
    if (m4.matches()) {
      return new Addr4(str);
    }
    Matcher m6 = IPV6_ADDR.matcher(str);
    if (m6.matches()) {

      // Remove any zone index.  IPAddr.getByName() may throw if given an
      // invalid zone (e.g., a non-numeric string that doesn't match a
      // local interface name).  That shouldn't happen in normal use, but
      // the zone index isn't needed and it's easier to remove it here than
      // to ensure the unit tests use a valid string.

      String str1 = m6.group(1);
      if (!str.equals(str1)) {
	log.debug3("Stripped addr6 " + str + " to " + str1);
      }
      return new Addr6(str1);
    }
    // Produce better message if matches mask pattern
    if (IPV4_MASK.matcher(str).matches() ||
	IPV6_MASK.matcher(str).matches()) {
      throw new MalformedException("Mask not allowed", str);
    }
    throw new MalformedException("Unknown IP address format", str);
  }

  /** Create a new IPv4 or IPv6 Mask object */
  public static Mask newMask(String str) throws MalformedException {
    Matcher m4 = IPV4_MASK.matcher(str);
    if (m4.matches()) {
      return new Mask4(str, true);
    }
    Matcher m6 = IPV6_MASK.matcher(str);
    if (m6.matches()) {
      return new Mask6(m6.group(1), m6.group(2), true);
    }
    throw new MalformedException("Unknown IP address format", str);
  }

  /** An IP address and mask */
  public interface Mask {
    public boolean match(Addr ip);
    public int getMaskBits();
  }

  /**  An IP address */
  public interface Addr extends Mask {
  }


  /** An IPv4 address and mask */
  public static class Mask4 implements Mask {

    protected int addr = 0;
    protected int mask = -1;
    protected int maskBits = 32;

    /**/
    /** Constructor for an IP address possibly including a mask
     */
    private Mask4(String s, boolean maskOk) throws MalformedException {
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
	      maskBits = n * 8;
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
	      maskBits = Integer.parseInt(tok);
	      if (maskBits < 0 || maskBits > 32) {
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
      switch (maskBits) {
      case 0: mask = 0; break;
      case 32: mask = -1; break;
      default: mask = -(1 << (32 - maskBits)); break;
      }
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
      if (obj instanceof Mask4) {
	Mask4 ip = (Mask4)obj;
	return (addr == ip.addr) && (mask == ip.mask);
      }
      return false;
    }

    public int hashCode() {
      return 3*addr + mask;
    }

    /** Return true if ip is equal to this, or if this is a mask that
     * matches ip.
     */
    public boolean match(Addr ip) {
      if (ip instanceof Addr4) {
	return match((Addr4)ip);
      }
      return false;
    }

    public boolean match(Addr4 ip) {
      return (ip.addr & mask) == addr;
    }

    /** Return number of one bits in mask
     */
    public int getMaskBits() {
      return maskBits;
    }

    /** Return string representation
     */
    public String toString () {
      StringBuilder sb = new StringBuilder(19);
      for (int i = 24; i >= 0; i -= 8) {
	sb.append(Integer.toString((addr >>> i) & 0xff));
	if (i > 0) {
	  sb.append(".");
	}
      }
      if (mask != -1) {
	sb.append("/");
	sb.append(Integer.toString(maskBits));
      }
      return sb.toString();
    }
  }

  /** An IPv6 address and mask */
  public static class Mask6 implements Mask {

    IPAddr inet;
    protected byte[] addr;
    protected int maskBits;

    /**/
    /** Constructor for an IPv6 address possibly including a mask
     */
    private Mask6(String ipstr, String bitStr, boolean maskOk)
	throws MalformedException {
      try {
	inet = IPAddr.getByName(ipstr);
	if (bitStr != null) {
	  if (!maskOk) {
	    throw new MalformedException("Mask not allowed",
					 ipstr + "/" + bitStr);
	  }
	  maskBits = Integer.parseInt(bitStr);
	  if (maskBits < 0 || maskBits > 128) {
	    throw new MalformedException("Illegal CIDR notation",
					 ipstr + "/" + bitStr);
	  }
	} else {
	  maskBits = 128;
	}
	int nBytes = (maskBits + 7) / 8;
	byte[] tmp = inet.getAddress();
	if (maskBits == 128) {
	  // all bits are significant
	  addr = tmp;
	} else {
	  // error if any masked out bytes are nonzero
	  for (int ix = nBytes; ix < 16; ix++) {
	    if (tmp[ix] != 0) {
	      throw new MalformedException("Illegal CIDR notation",
					   ipstr + "/" + bitStr);
	    }
	  }
	  int oddBits = maskBits % 8;
	  if (oddBits != 0) {
	    int mask = -(1 << (8 - oddBits));
	    // error if any masked out bits in last byte are nonzero
	    if ((tmp[nBytes-1] & ~mask) != 0) {
	      throw new MalformedException("Illegal CIDR notation",
					   ipstr + "/" + bitStr);
	    }
	  }
	  // save only the significant bytes
	  addr = new byte[nBytes];
	  System.arraycopy(tmp, 0, addr, 0, nBytes);
	}
      } catch (UnknownHostException e) {
	throw new MalformedException("Unparseable IPv6 address", ipstr);
      }
    }

    /** Return true if obj equal this.
     */
    public boolean equals(Object obj) {
      if (obj instanceof Mask6) {
	Mask6 other = (Mask6)obj;
 	return (maskBits == other.maskBits && inet.equals(other.inet));
      }
      return false;
    }

    public int hashCode() {
      return 5*inet.hashCode() + 3*maskBits + 1;
    }

    /** Return true if ip is equal to this, or if this is a mask that
     * matches ip.
     */
    public boolean match(Addr ip) {
      if (ip instanceof Addr6) {
	return match((Addr6)ip);
      }
      return false;
    }


    public boolean match(Addr6 ip) {
      switch (maskBits) {
      case 0: return true;
      case 128:
	// all bits are significant
	return Arrays.equals(addr, ip.addr);
      default:
	int nBytes = maskBits / 8;
	// compare masked-in whole bytes
	for (int ix = 0; ix < nBytes; ix++) {
	  if (addr[ix] != ip.addr[ix]) {
	    return false;
	  }
	}
	int oddBits = maskBits % 8;
	if (oddBits != 0) {
	  int mask = -(1 << (8 - oddBits));
	  // compare masked-in bits in last byte
	  return (0xff & (ip.addr[nBytes] & mask)) == addr[nBytes];
	}
	return true;
      }
    }

    /** Return number of one bits in mask
     */
    public int getMaskBits() {
      return maskBits;
    }

    /** Return string representation
     */
    public String toString () {
      StringBuilder sb = new StringBuilder(19);
      sb.append(inet.getHostAddress());
      if (maskBits != 128) {
	sb.append("/");
	sb.append(Integer.toString(maskBits));
      }
      return sb.toString();
    }
  }

  /** IPv4 address */
  public static class Addr4 extends Mask4 implements Addr {
    /**
     * Constructor for single IP address (no mask)
     */
    private Addr4(String ipAddr) throws MalformedException {
      super(ipAddr, false);
    }
  }

  /** IPv6 address */
  public static class Addr6 extends Mask6 implements Addr {
    /**
     * Constructor for single IP address (no mask)
     */
    private Addr6(String ipAddr) throws MalformedException {
      super(ipAddr, null, false);
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
