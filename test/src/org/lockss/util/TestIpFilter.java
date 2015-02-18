/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;
import static org.lockss.util.IpFilter.Mask;
import static org.lockss.util.IpFilter.Addr;
import static org.lockss.util.IpFilter.MalformedException;

/**
 * This is the test class for org.lockss.util.IpFilter
 */
public class TestIpFilter extends LockssTestCase {
  IpFilter filt;

  public void setUp() throws Exception {
    super.setUp();
    filt = new IpFilter();
  }

  
  IpFilter.Mask newMask(String str) throws MalformedException {
    return IpFilter.newMask(str);
  }

  IpFilter.Addr newAddr(String str) throws MalformedException {
    return IpFilter.newAddr(str);
  }

  // Create a mask and ensure that a Mask4 got created (else throw
  // ClassCastException)
  IpFilter.Mask newMask4(String str) throws MalformedException {
    return (IpFilter.Mask4)newMask(str);
  }

  // Create a mask and ensure that a Mask6 got created (else throw
  // ClassCastException)
  IpFilter.Mask newMask6(String str) throws MalformedException {
    return (IpFilter.Mask6)newMask(str);
  }

  // Create an addr and ensure that an Addr4 got created (else throw
  // ClassCastException)
  IpFilter.Addr newAddr4(String str) throws MalformedException {
    return (IpFilter.Addr4)newAddr(str);
  }

  // Create an addr and ensure that an Addr6 got created (else throw
  // ClassCastException)
  IpFilter.Addr newAddr6(String str) throws MalformedException {
    return (IpFilter.Addr6)newAddr(str);
  }


  private void assertMaskNotOk(String s) {
    try {
      IpFilter.Mask ip = newMask(s);
      fail(s + " should have failed, didn't: " + ip);
    } catch (IpFilter.MalformedException e) {
    }
  }

  private void assertMalformedAddr(String s) {
    try {
      IpFilter.Addr ip = newAddr(s);
      fail(s + " should have failed, didn't: " + ip);
    } catch (IpFilter.MalformedException e) {
    }
  }

  private void assertMaskOk(String s) throws IpFilter.MalformedException {
    try {
      IpFilter.Mask ip = newMask(s);
    } catch (IpFilter.MalformedException e) {
      fail(s + " failed: " + e);
    }
  }

  private void assertAddrOk(String s) throws IpFilter.MalformedException {
    try {
      IpFilter.Addr ip = newAddr(s);
    } catch (IpFilter.MalformedException e) {
      fail(s + " failed: " + e);
    }
  }

  public void assertMatch(String maskstr, String addrstr)
      throws IpFilter.MalformedException {
    checkMatch(maskstr, addrstr, true);
  }

  public void assertNoMatch(String maskstr, String addrstr)
      throws IpFilter.MalformedException {
    checkMatch(maskstr, addrstr, false);
  }

  public void checkMatch(String maskstr, String addrstr, boolean shouldMatch)
      throws IpFilter.MalformedException {
    Mask m = newMask(maskstr);
    Addr ad = newAddr(addrstr);
    boolean match = m.match(ad);
    if (match != shouldMatch) {
      fail("expected: " + m + ".match(" + ad + ") to be " + shouldMatch +
	   ", but was " + match);
    }
//     // matching is commutative
//     boolean revmatch = ad.match(m);
//     if (revmatch != shouldMatch) {
//       fail("expected: " + ad + ".match(" + m + ") to be " + shouldMatch +
// 	   ", but was " + match);
//     }
  }

  public void assertAllowed(String s) throws IpFilter.MalformedException {
    checkFilter(s, true);
  }

  public void assertNotAllowed(String s) throws IpFilter.MalformedException {
    checkFilter(s, false);
  }

  public void checkFilter(String s, boolean shouldBeAllowed)
      throws IpFilter.MalformedException {
    IpFilter.Addr ip = IpFilter.newAddr(s);
    boolean match = filt.isIpAllowed(ip);
    if (match != shouldBeAllowed) {
      fail("isIpAllowed(" + ip + ") was " + match +
	   ", should have been " + shouldBeAllowed);
    }
  }

  public void testName() throws Exception {
    assertMaskNotOk("foo");
    assertMaskNotOk("example.com");
    assertMaskNotOk("foo/20");
    assertMaskNotOk("example.com/24");
    assertMalformedAddr("foo");
    assertMalformedAddr("example.com");
    assertMalformedAddr("foo/20");
    assertMalformedAddr("example.com/24");
  }

  public void testConstructor4() throws Exception {
    newMask4("127.1.0.0/16");
    try {
      newMask6("127.1.0.0/16");
      fail("newMask6(IPv4Addr) should throw");
    } catch (RuntimeException e) {
    }
    newAddr4("127.0.0.1");
    try {
      newAddr6("127.0.0.1");
      fail("newAddr6(IPv4Addr) should throw");
    } catch (RuntimeException e) {
    }
    assertAddrOk("127.0.0.1");
    assertAddrOk("0.0.0.0");
    assertAddrOk("255.255.255.255");
    assertMaskNotOk("");
    assertMaskNotOk("...");
    assertMaskNotOk("36.48.0");
    assertMaskNotOk("36.48.0.");
    assertMalformedAddr("36.48.0.2.3");
    assertMaskNotOk("36.48.0.a");

    // mask not allowed
    assertMalformedAddr("123.45.12.*");
    assertMalformedAddr("127.0.1.0/24");

    // legal mask
    assertMaskOk("127.0.1.0/24");
    assertMaskOk("127.0.1/24");
    assertMaskOk("127.0/16");
    assertMaskOk("127/16");
    assertMaskOk("123.45.12.*");
    assertMaskOk("123.45.*.*");
    // illegal mask
    assertMaskNotOk("36.48.*.0");
    assertMaskNotOk("36.48.0.23/33");
    assertMaskNotOk("36.48.0.4/29");
    assertMaskNotOk("36.48.0.2/");

    // probe edges of mask
    assertMaskNotOk("1.0.0.0/1");
    assertMaskNotOk("1.0.0.0/2");
    assertMaskNotOk("1.0.0.0/6");
    assertMaskNotOk("1.0.0.0/7");
    assertMaskOk("1.0.0.0/8");
    assertMaskNotOk("1.128.0.0/8");
    assertMaskOk("1.0.0.0/9");
    assertMaskOk("1.0.0.0/10");
    assertMaskOk("1.0.0.0/15");
    assertMaskOk("1.0.0.0/16");

    assertMaskNotOk("0.0.1.0/1");
    assertMaskNotOk("0.0.1.0/2");
    assertMaskNotOk("0.0.1.0/6");
    assertMaskNotOk("0.0.1.0/7");
    assertMaskNotOk("0.0.1.0/8");
    assertMaskNotOk("0.0.1.0/9");
    assertMaskNotOk("0.0.1.0/15");
    assertMaskNotOk("0.0.1.0/16");
    assertMaskNotOk("0.0.1.0/17");
    assertMaskNotOk("0.0.1.0/23");
    assertMaskOk("0.0.1.0/24");
    assertMaskOk("0.0.1.0/25");

  }

  public void testConstructor6() throws Exception {
    newMask6("1::0/16");
    newMask6("::/0");
    try {
    newMask4("1::0/16");
      fail("newMask4(IPv6Addr) should throw");
    } catch (RuntimeException e) {
    }
    newAddr6("::1");
    newAddr6("::");
    try {
    newAddr4("::1234");
      fail("newAddr4(IPv6Addr) should throw");
    } catch (RuntimeException e) {
    }

    assertAddrOk("::1");
    assertAddrOk("::1%0");
    assertAddrOk("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
    assertAddrOk("2001:0db8::0370:7334");
    assertAddrOk("::7334");
    assertAddrOk("7334::");
    assertMalformedAddr("::7334::");
    assertMalformedAddr("::733h");
    // too few groups
    assertMalformedAddr("0db8:85a3:0000:0000:8a2e:0370:7334");
    // too many groups
    assertMalformedAddr("2001:0db8:85a3:0000:0000:8a2e:0370:7334:0");

    // mask not allowed
    assertMalformedAddr("::1/24");
    assertMalformedAddr("2001:0db8:85a3:0000:0000:8a2e:0370:7334/8");
    assertMalformedAddr("2001:0db8::0370:7334/7");
    assertMalformedAddr("::7334/22");
    assertMalformedAddr("7334::/20");

    // mask allowed
    assertMaskOk("::/0");
    assertMaskOk("ffff::/16");
    assertMaskNotOk("ffff::/15");
    assertMaskOk("::1/128");
    assertMaskOk("2001:0db8:85a3:0000:0000:8a2e:0370:7334/126");
    assertMaskOk("2001:0db8::/32");
    assertMaskOk("::7334/126");
    assertMaskOk("7334::/14");

    // illegal mask
    assertMaskNotOk("::1/127");
    assertMaskNotOk("ffff::/15");
    assertMaskNotOk("::1/129");
    assertMaskNotOk("::1/");
    assertMaskNotOk("2001:0db8::0370:7334/-4");

    // probe edges of mask
    assertMaskNotOk("8000::0/0");
    assertMaskOk("8000::0/1");
    assertMaskOk("8000::0/2");
    assertMaskOk("8000::0/31");

    assertMaskOk("::1/128");
    assertMaskNotOk("::1/127");
    assertMaskNotOk("::1/1");
    assertMaskNotOk("::1/0");

    assertMaskNotOk("0:0:8000::/0");
    assertMaskNotOk("0:0:8000::/1");
    assertMaskNotOk("0:0:8000::/31");
    assertMaskNotOk("0:0:8000::/32");
    assertMaskOk("0:0:8000::/33");
    assertMaskOk("0:0:8000::/34");
    assertMaskOk("0:0:8000::/63");
    assertMaskOk("0:0:8000::/64");
  }

  public void testMaskBits4() throws Exception {
    Mask m;
    m = newMask4("1.2.3.4");
    assertEquals(32, m.getMaskBits());
    m = newMask4("1.2.3.*");
    assertEquals(24, m.getMaskBits());
    m = newMask4("1.*.*.*");
    assertEquals(8, m.getMaskBits());
    m = newMask4("*.*.*.*");
    assertEquals(0, m.getMaskBits());
    m = newMask4("1.2.3.4/32");
    assertEquals(32, m.getMaskBits());
    m = newMask4("1.2.3.4/31");
    assertEquals(31, m.getMaskBits());
    m = newMask4("1.2.3.0/24");
    assertEquals(24, m.getMaskBits());
  }

  public void testMaskBits6() throws Exception {
    Mask m;
    m = newMask6("::1");
    assertEquals(128, m.getMaskBits());
    m = newMask6("100::/8");
    assertEquals(8, m.getMaskBits());
    m = newMask6("1:2:3:4::/64");
    assertEquals(64, m.getMaskBits());
    m = newMask6("::/0");
    assertEquals(0, m.getMaskBits());
  }

  public void testMaskEquals4() throws Exception {
    Mask m1 = newMask("1.2.3.4");
    Mask m2 = newMask(new String("1.2.3.4"));
    Mask m3 = newMask("4.4.4.4");
    Mask m4 = newMask("1.2.3.4/30");
    assertEquals(m1, m2);
    assertNotEquals(m1, m3);
    assertNotEquals(m1, m4);
  }

  public void testMaskEquals6() throws Exception {
    Mask m1 = newMask("::2");
    Mask m2 = newMask(new String("::2"));
    Mask m3 = newMask("1:2:3:4::");
    Mask m4 = newMask("::2/127");
    Mask m5 = newMask("::2/128");
    assertEquals(m1, m2);
    assertNotEquals(m1, m3);
    assertNotEquals(m1, m4);
    assertEquals(m1, m5);
  }

  public void testMaskEqualsMixed() throws Exception {
    Mask m1 = newMask("0.0.0.0");
    Mask m2 = newMask("::");
    Mask m3 = newMask("0.0.0.1");
    Mask m4 = newMask("::1");
    assertNotEquals(m1, m2);
    assertNotEquals(m1, m3);
    assertNotEquals(m3, m4);
  }

  public void testHash4() throws Exception {
    Mask m1 = newMask("1.2.3.4");
    Mask m2 = newMask(new String("1.2.3.4"));
    Mask m3 = newMask("1.2.3.5");
    Mask m4 = newMask("1.2.3.4/31");
    assertEquals(m1.hashCode(), m2.hashCode());
    assertNotEquals(m1.hashCode(), m3.hashCode());
    assertNotEquals(m1.hashCode(), m4.hashCode());
  }

  public void testHash6() throws Exception {
    Mask m1 = newMask("fe80::");
    Mask m2 = newMask(new String("fe80::"));
    Mask m3 = newMask("fe81::");
    Mask m4 = newMask("fe80::/16");
    assertEquals(m1.hashCode(), m2.hashCode());
    assertNotEquals(m1.hashCode(), m3.hashCode());
    assertNotEquals(m1.hashCode(), m4.hashCode());
  }

  public void testMatch4() throws Exception {
    assertMatch("127.0.1.0/24", "127.0.1.0");
    assertMatch("127.0.1.0/24", "127.0.1.255");
    assertMatch("127.0.1/24", "127.0.1.255");
    assertNoMatch("127.0.1.0/24", "127.2.1.0");
    assertNoMatch("127.0.1/24", "127.0.2.0");
    assertNoMatch("127.0.1/25", "127.0.1.255");
    assertMatch("127.*.*.*", "127.0.1.255");
    assertMatch("0/1", "127.0.1.255");
    assertMatch("128/1", "255.0.1.255");
    assertNoMatch("0/1", "255.0.1.255");
    assertNoMatch("128/1", "127.0.1.255");
    assertMatch("0/0", "127.0.1.255");
    assertMatch("0/0", "255.0.1.255");
  }

  public void testMatch6() throws Exception {
    assertMatch("ffff::abab:0000/126", "ffff::abab:0000");
    assertNoMatch("ffff::abab:0000/126", "ffff::abab:0008");
    assertNoMatch("ffff::abab:0000/126", "ffff::abab:0004");
    assertMatch("ffff::abab:0000/126", "ffff::abab:0003");
    assertNoMatch("ffff::abab:0000/126", "ffff::abab:8000");

    assertMatch("ffff::abab:000c/126", "ffff::abab:000f");
    assertMatch("ffff::abab:000c/126", "ffff::abab:000e");
    assertMatch("ffff::abab:000c/126", "ffff::abab:000d");
    assertMatch("ffff::abab:000c/126", "ffff::abab:000c");
    assertNoMatch("ffff::abab:000c/126", "ffff::abab:000b");

    assertMatch("ffff::/16", "ffff::");
    assertMatch("ffff::/16", "ffff::1");
    assertMatch("ffff::/16", "ffff::ffff");
    assertMatch("ffff::/16", "ffff:ffff::ffff");

    assertMatch("ffff::/32", "ffff::");
    assertMatch("ffff::/32", "ffff::1");
    assertMatch("ffff::/32", "ffff::ffff");
    assertNoMatch("ffff::/32", "ffff:8000::");
    assertNoMatch("ffff::/32", "ffff:1::");
    assertMatch("ffff::/32", "ffff:0:ffff::");

    assertMatch("1234:5678::/33", "1234:5678::");
    assertMatch("1234:5678::/33", "1234:5678:7fff::");
    assertNoMatch("1234:5678::/33", "1234:5678:8000::");
    assertMatch("1234:5678::/34", "1234:5678:3fff::");
    assertNoMatch("1234:5678::/34", "1234:5678:4000::");
    assertMatch("1234:5678::/35", "1234:5678:1fff::");
    assertNoMatch("1234:5678::/35", "1234:5678:2000::");
    assertMatch("1234:5678::/36", "1234:5678:0fff::");
    assertNoMatch("1234:5678::/36", "1234:5678:1000::");
    assertMatch("1234:5678::/37", "1234:5678:07ff::");
    assertNoMatch("1234:5678::/37", "1234:5678:0800::");
    assertMatch("1234:5678::/38", "1234:5678:03ff::");
    assertNoMatch("1234:5678::/38", "1234:5678:0400::");
    assertMatch("1234:5678::/39", "1234:5678:01ff::");
    assertNoMatch("1234:5678::/39", "1234:5678:0200::");
    assertMatch("1234:5678::/40", "1234:5678:00ff::");
    assertNoMatch("1234:5678::/40", "1234:5678:0100::");
    assertMatch("1234:5678::/41", "1234:5678:007f::");
    assertNoMatch("1234:5678::/41", "1234:5678:0080::");

    assertMatch("ffee::abab", "ffee::abab");
    assertNoMatch("ffee::abab", "7fee::abab");
    assertNoMatch("ffee::abab", "ffee::abac");

    assertMatch("::/0", "::1");
    assertMatch("::/0", "::1%123");
    assertMatch("::/0", "::1%eth0");
    assertMatch("::/0", "::1%snkrnt17");
    assertMatch("::/0", "1::");
    assertMatch("::/0", "1::1");
    assertMatch("::/0", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
  }

  public void assertToString(String exp, String mask)
      throws IpFilter.MalformedException {
    assertEquals(exp, newMask(mask).toString());
  }

  public void assertToString(String exp)
      throws IpFilter.MalformedException {
    assertToString(exp, exp);
  }

  public void testToString() throws Exception {
    assertToString("127.0.1.0");
    assertToString("127.0.1.0/24");
    assertToString("1.2.3.4", "1.2.3.4/32");
    assertToString("0:0:0:0:0:0:0:1", "::1");
    assertToString("ffff:0:0:0:0:0:0:0/16", "ffff::0/16");
    assertToString("0:0:0:0:0:0:0:1", "::1/128");
  }

  public void testFilter() throws Exception {
    filt.setFilters("172.16.25.*;10.0.4.1", "172.16.25.128/25");
    assertNotAllowed("10.0.4.13");
    assertAllowed("10.0.4.1");
    assertAllowed("172.16.25.1");
    assertNotAllowed("172.16.25.131");

    filt.setFilters("172.16.25.* ; ::1:1234 ; ::2:0/112",
		    "172.16.25.128/25 ; ::2:1234");
    assertNotAllowed("10.0.4.13");
    assertAllowed("172.16.25.1");
    assertNotAllowed("172.16.25.131");
    assertAllowed("::1:1234");
    assertAllowed("::2:1111");
    assertAllowed("::2:2222");
    assertNotAllowed("::2:1234");
    assertNotAllowed("::3:0");
  }

  public void testComment() throws Exception {
    filt.setFilters("# a comment ; 172.16.25.* ; #foo ; ::1:1234 ; ::2:0/112",
		    "172.16.25.128/25 ; #bar ; ::2:1234");
    assertNotAllowed("10.0.4.13");
    assertAllowed("172.16.25.1");
    assertNotAllowed("172.16.25.131");
    assertAllowed("::1:1234");
    assertAllowed("::2:1111");
    assertAllowed("::2:2222");
    assertNotAllowed("::2:1234");
    assertNotAllowed("::3:0");
  }

  // this will generate a log error message
  public void testMalformedIncludeFilter() throws Exception {
    filt.setFilters("x;172.16.25.*;10.0.4.1", "172.16.25.128/25");
    assertNotAllowed("10.0.4.13");
    assertAllowed("10.0.4.1");
    assertAllowed("172.16.25.1");
    assertNotAllowed("172.16.25.131");
  }

  public void testMalformedExcludeFilter() throws Exception {
    try {
      filt.setFilters("172.16.25.*;10.0.4.1", "xx;172.16.25.128/25");
      fail("Malformed entry in exclude list didn't throw");
    } catch (IpFilter.MalformedException e) {
    }
  }

  public void testDefault() throws Exception {
    // default is to match nothing
    assertNotAllowed("1.1.1.1");
    filt.setFilters("", "");
    assertNotAllowed("1.1.1.1");
    filt.setFilters("# a comment", " #foo");
    assertNotAllowed("1.1.1.1");
  }

  public void testUnionFilters() throws Exception {
    assertEquals("", IpFilter.unionFilters(null, null));
    assertEquals("", IpFilter.unionFilters(null, ""));
    assertEquals("1.2.3.4", IpFilter.unionFilters(null, "1.2.3.4"));
    assertEquals("1.2.3.4", IpFilter.unionFilters("1.2.3.4", null));
    assertEquals("1.2.3.4;2.2.2.2/2",
		 IpFilter.unionFilters("1.2.3.4", "2.2.2.2/2"));
    assertEquals("2.2.2.2/2;5.4.3.2;44.33.22.11/55",
		 IpFilter.unionFilters("2.2.2.2/2;5.4.3.2",
				       "44.33.22.11/55"));
    assertEquals("1.2.3.4", IpFilter.unionFilters("1.2.3.4", "1.2.3.4"));
    assertEquals("1.2.3.4;1.1.1.0/24",
		 IpFilter.unionFilters("1.2.3.4;1.1.1.0/24",
				       "1.2.3.4;1.1.1.0/24"));
    assertEquals("1.2.3.4;1.2.3.5;1.1.1.0/24",
		 IpFilter.unionFilters("1.2.3.4;1.2.3.5",
				       "1.2.3.4;1.1.1.0/24"));
  }
}
