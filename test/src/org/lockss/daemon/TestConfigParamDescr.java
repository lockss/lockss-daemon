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

package org.lockss.daemon;

import java.io.*;
import java.util.*;

import org.lockss.app.LockssApp;
import org.lockss.daemon.ConfigParamDescr.InvalidFormatException;
import org.lockss.config.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.ConfigParamDescr
 */
public class TestConfigParamDescr extends LockssTestCase {
  ConfigParamDescr d1;
  ConfigParamDescr d2;

  public void setUp() {
    d1 = new ConfigParamDescr("key1");
    d2 = new ConfigParamDescr("key2");
  }

  public void testAccessors() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    assertEquals("k1", d1.getKey());
    assertSame(d1, d1.setKey("k2"));
    assertEquals("k2", d1.getKey());

    assertEquals("k2", d1.getDisplayName());
    assertSame(d1, d1.setKey("foob"));
    assertEquals("foob", d1.getDisplayName());

    assertNull(d1.getDescription());
    assertSame(d1, d1.setDescription("ddd"));
    assertEquals("ddd", d1.getDescription());

    assertSame(d1, d1.setType(3));
    assertEquals(3, d1.getType());

    assertSame(d1, d1.setSize(47));
    assertEquals(47, d1.getSize());

    assertTrue(d1.isDefinitional());
    assertSame(d1, d1.setDefinitional(false));
    assertFalse(d1.isDefinitional());

    assertFalse(d1.isDefaultOnly());
    assertSame(d1, d1.setDefaultOnly(true));
    assertTrue(d1.isDefaultOnly());
  }

  public void testTypeIntToEnum() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    d1.setType(ConfigParamDescr.TYPE_STRING);
    assertEquals(AuParamType.String, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_INT);
    assertEquals(AuParamType.Int, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_URL);
    assertEquals(AuParamType.Url, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_YEAR);
    assertEquals(AuParamType.Year, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_BOOLEAN);
    assertEquals(AuParamType.Boolean, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_POS_INT);
    assertEquals(AuParamType.PosInt, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_RANGE);
    assertEquals(AuParamType.Range, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_NUM_RANGE);
    assertEquals(AuParamType.NumRange, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_SET);
    assertEquals(AuParamType.Set, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_USER_PASSWD);
    assertEquals(AuParamType.UserPasswd, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_LONG);
    assertEquals(AuParamType.Long, d1.getTypeEnum());
    d1.setType(ConfigParamDescr.TYPE_TIME_INTERVAL);
    assertEquals(AuParamType.TimeInterval, d1.getTypeEnum());
  }

  public void testTypeEnumToInt() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    d1.setType(AuParamType.String);
    assertEquals(ConfigParamDescr.TYPE_STRING, d1.getType());
    d1.setType(AuParamType.Int);
    assertEquals(ConfigParamDescr.TYPE_INT, d1.getType());
    d1.setType(AuParamType.Url);
    assertEquals(ConfigParamDescr.TYPE_URL, d1.getType());
    d1.setType(AuParamType.Year);
    assertEquals(ConfigParamDescr.TYPE_YEAR, d1.getType());
    d1.setType(AuParamType.Boolean);
    assertEquals(ConfigParamDescr.TYPE_BOOLEAN, d1.getType());
    d1.setType(AuParamType.PosInt);
    assertEquals(ConfigParamDescr.TYPE_POS_INT, d1.getType());
    d1.setType(AuParamType.Range);
    assertEquals(ConfigParamDescr.TYPE_RANGE, d1.getType());
    d1.setType(AuParamType.NumRange);
    assertEquals(ConfigParamDescr.TYPE_NUM_RANGE, d1.getType());
    d1.setType(AuParamType.Set);
    assertEquals(ConfigParamDescr.TYPE_SET, d1.getType());
    d1.setType(AuParamType.UserPasswd);
    assertEquals(ConfigParamDescr.TYPE_USER_PASSWD, d1.getType());
    d1.setType(AuParamType.Long);
    assertEquals(ConfigParamDescr.TYPE_LONG, d1.getType());
    d1.setType(AuParamType.TimeInterval);
    assertEquals(ConfigParamDescr.TYPE_TIME_INTERVAL, d1.getType());
  }

  public void testSizeDefault() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    assertEquals(0, d1.getSize());
    d1.setType(ConfigParamDescr.TYPE_BOOLEAN);
    assertEquals(4, d1.getSize());

    ConfigParamDescr d2 = new ConfigParamDescr("k1");
    d2.setType(ConfigParamDescr.TYPE_YEAR);
    assertEquals(4, d2.getSize());

    ConfigParamDescr d3 = new ConfigParamDescr("k1");
    d3.setType(ConfigParamDescr.TYPE_INT);
    assertEquals(10, d3.getSize());
    d3.setSize(12);
    assertEquals(12, d3.getSize());

    ConfigParamDescr d4 = new ConfigParamDescr("k1");
    d4.setSize(12);
    d4.setType(ConfigParamDescr.TYPE_INT);
    assertEquals(12, d4.getSize());

    ConfigParamDescr d5 = new ConfigParamDescr("k1");
    d5.setType(ConfigParamDescr.TYPE_LONG);
    assertEquals(10, d5.getSize());
    d5.setSize(18);
    assertEquals(18, d5.getSize());

    ConfigParamDescr d6 = new ConfigParamDescr("k1");
    d6.setType(ConfigParamDescr.TYPE_TIME_INTERVAL);
    assertEquals(10, d6.getSize());
    d6.setSize(18);
    assertEquals(18, d6.getSize());

  }

  public void testSizeDefault2() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    assertEquals(0, d1.getSize());
    d1.setType(AuParamType.Boolean);
    assertEquals(4, d1.getSize());

    ConfigParamDescr d2 = new ConfigParamDescr("k1");
    d2.setType(AuParamType.Year);
    assertEquals(4, d2.getSize());

    ConfigParamDescr d3 = new ConfigParamDescr("k1");
    d3.setType(AuParamType.Int);
    assertEquals(10, d3.getSize());
    d3.setSize(12);
    assertEquals(12, d3.getSize());

    ConfigParamDescr d4 = new ConfigParamDescr("k1");
    d4.setSize(12);
    d4.setType(AuParamType.Int);
    assertEquals(12, d4.getSize());

    ConfigParamDescr d5 = new ConfigParamDescr("k1");
    d5.setType(AuParamType.Long);
    assertEquals(10, d5.getSize());
    d5.setSize(18);
    assertEquals(18, d5.getSize());

    ConfigParamDescr d6 = new ConfigParamDescr("k1");
    d6.setType(AuParamType.TimeInterval);
    assertEquals(10, d6.getSize());
    d6.setSize(18);
    assertEquals(18, d6.getSize());

  }

  public void testCompareTo() {
    ConfigParamDescr d1 = new ConfigParamDescr("cc");
    ConfigParamDescr d2 = new ConfigParamDescr("dd");
    assertTrue(d1.compareTo(d2) < 0);
    d2.setDisplayName("bb");
    assertTrue(d1.compareTo(d2) > 0);
    d1.setKey("bb");
    assertEquals(0, d1.compareTo(d2));
  }

  public void testEquals() {
    ConfigParamDescr d1 = new ConfigParamDescr("k1");
    ConfigParamDescr d2 = new ConfigParamDescr(new String("k1"));
    ConfigParamDescr d3 = new ConfigParamDescr("k2");
    ConfigParamDescr d4 = new ConfigParamDescr("k2");

    assertEquals(d1, d1);
    assertEquals(d1, d2);
    assertNotEquals(d1, d3);
    d2.setType(3);
    assertNotEquals(d1, d2);
    d2 = new ConfigParamDescr(new String("k1"));
    assertEquals(d1, d2);
    d2.setSize(10);
    assertNotEquals(d1, d2);

    assertEquals(d3, d4);
    d4.setDefinitional(false);
    assertNotEquals(d3, d4);
  }

  public void testIsReserved() {
    assertTrue(ConfigParamDescr.isReservedParam("reserved.foo"));
    assertTrue(ConfigParamDescr.isReservedParam("reserved."));
    assertFalse(ConfigParamDescr.isReservedParam("year"));
    assertFalse(ConfigParamDescr.isReservedParam("reservedfoo"));
  }

  public void testHash() {
    ConfigParamDescr d1 = new ConfigParamDescr("foo");
    ConfigParamDescr d2 = new ConfigParamDescr("foo");
    assertEquals(d1.hashCode(), d2.hashCode());
  }

  public void testDerived() {
    ConfigParamDescr d = ConfigParamDescr.BASE_URL;
    ConfigParamDescr d1 = d.getDerivedDescr("base_url_host");
    assertFalse(d.isDerived());
    assertTrue(d1.isDerived());
    assertNotEquals(d1, d);
    assertEquals(d.getType(), d1.getType());
    assertFalse(d1.isDefinitional());
    assertEquals("base_url_host (derived from Base URL)", d1.getDisplayName());
    // Should always get same one back
    ConfigParamDescr d2 = d.getDerivedDescr("base_url_host");
    assertSame(d2, d1);
    // this one is different
    ConfigParamDescr d3 = d.getDerivedDescr("base_url2_host");
    assertNotSame(d2, d3);
    assertNotEquals(d2, d3);
  }
    

  /**
   * <p>Tests that {@link ConfigParamDescr#postUnmarshalResolve(LockssApp)}
   * works at least for the elements of
   * {@link ConfigParamDescr#DEFAULT_DESCR_ARRAY}.</p>
   * @throws Exception if an unexpected error occurs.
   */
  public void testPostUnnarshalResolve() throws Exception {
    XStreamSerializer serializer = new XStreamSerializer();
    for (int ix = 0 ; ix < ConfigParamDescr.DEFAULT_DESCR_ARRAY.length ; ++ix) {
      File file = File.createTempFile("testfile", ".xml");
      file.deleteOnExit();
      serializer.serialize(file, ConfigParamDescr.DEFAULT_DESCR_ARRAY[ix]);
      assertSame(ConfigParamDescr.DEFAULT_DESCR_ARRAY[ix],
                 serializer.deserialize(file));
    }
  }
  
  /**
   * <p>Tests {@link ConfigParamDescr#getValueOfType(String)} for
   * the {@link ConfigParamDescr} instance
   * {@link ConfigParamDescr#ISSUE_RANGE}.</p>
   * @throws Exception if any unexpected error occurs.
   */
  public void testGetValueOfTypeIssueRange() throws Exception {
    ConfigParamDescr range = ConfigParamDescr.ISSUE_RANGE;
    Object ret = null;
    Vector vec = null;
    
    // Range
    ret = range.getValueOfType("bar-foo");
    assertTrue(ret instanceof Vector);
    vec = (Vector)ret;
    assertEquals(2, vec.size());
    assertEquals("bar", vec.get(0));
    assertEquals("foo", vec.get(1));
    
    // Trivial range
    ret = range.getValueOfType("foo-foo");
    assertTrue(ret instanceof Vector);
    vec = (Vector)ret;
    assertEquals(2, vec.size());
    assertEquals("foo", vec.get(0));
    assertEquals("foo", vec.get(1));
    
    // Invalid range
    try {
      ret = range.getValueOfType("foo-bar");
      fail("Should have thrown InvalidFormatException");
    }
    catch (InvalidFormatException expected) {
      // all is well
    }
  }

  /**
   * <p>Tests {@link ConfigParamDescr#getValueOfType(String)} for
   * the {@link ConfigParamDescr} instance
   * {@link ConfigParamDescr#NUM_ISSUE_RANGE}.</p>
   * @throws Exception if any unexpected error occurs.
   */
  public void testGetValueOfTypeNumIssueRange() throws Exception {
    ConfigParamDescr range = ConfigParamDescr.NUM_ISSUE_RANGE;
    Object ret = null;
    Vector vec = null;
    
    // Range
    ret = range.getValueOfType("1-99");
    assertTrue(ret instanceof Vector);
    vec = (Vector)ret;
    assertEquals(2, vec.size());
    assertEquals(new Long(1), vec.get(0));
    assertEquals(new Long(99), vec.get(1));
    
    // Trivial range
    ret = range.getValueOfType("1-1");
    assertTrue(ret instanceof Vector);
    vec = (Vector)ret;
    assertEquals(2, vec.size());
    assertEquals(new Long(1), vec.get(0));
    assertEquals(new Long(1), vec.get(1));
    
    // Invalid range
    try {
      ret = range.getValueOfType("99-1");
      fail("Should have thrown InvalidFormatException");
    }
    catch (InvalidFormatException expected) {
      // all is well
    }
  }

  public void testGetValueOfTypeSet() throws Exception {
    ConfigParamDescr set = ConfigParamDescr.ISSUE_SET;
    
    assertEquals(ListUtil.list("1"), set.getValueOfType("1"));
    assertEquals(ListUtil.list("1", "apple", "bear"),
		 set.getValueOfType("1,apple , bear"));
    assertEquals(ListUtil.list("1", "1b", "2", "3", "4", "5", "6", "6A", "6B",
			       "7000", "7001", "7002"),
		 set.getValueOfType(" 1, 1b, {2-6} , 6A,6B ,{7000-7002}"));
    assertEquals(ListUtil.list("1", "2", "3", "5", "6", "8", "9", "11"),
		 set.getValueOfType(" { 1 - 3 }, {5-6} , {  8-9  } ,{11-11}"));
    assertEquals(ListUtil.list("1", "{-2}"), set.getValueOfType("1,{-2}"));
    assertEquals(ListUtil.list("1"), set.getValueOfType("1,"));

    Object largeSet = set.getValueOfType("1,{0-100000}");
    assertEquals(10000, ((Collection)largeSet).size());
  }

  /**
   * <p>Tests {@link ConfigParamDescr#getValueOfType(String)} for
   * the {@link ConfigParamDescr} instance
   * {@link ConfigParamDescr#ISSUE_RANGE}.</p>
   * @throws Exception if any unexpected error occurs.
   */
  public void testGetValueOfTypeUserPass() throws Exception {
    ConfigParamDescr pass = ConfigParamDescr.USER_CREDENTIALS;
    assertEquals("foo:bar", pass.getValueOfType("foo:bar"));
    // Invalid pass
    try {
      pass.getValueOfType("foobar");
      fail("Should have thrown InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      pass.getValueOfType("foo:");
      fail("Should have thrown InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
  }

  public void testGetValueOfType() throws Exception {
    ConfigParamDescr ncint = ConfigParamDescr.CRAWL_INTERVAL;
    assertEquals(4 * Constants.HOUR, ncint.getValueOfType("4h"));
    // Invalid time interval
    try {
      ncint.getValueOfType("abcd");
      fail("Should have thrown InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
  }

  // Ensure that either exception can be caught.  Remove after 1.67 when
  // ConfigParamDescr.InvalidFormatException is retired
  public void testException() throws Exception {
    ConfigParamDescr desc = ConfigParamDescr.YEAR;
    try {
      desc.getValueOfType("abcd");
      fail("Should have thrown ConfigParamDescr.InvalidFormatException");
    } catch (ConfigParamDescr.InvalidFormatException expected) {
    }
    try {
      desc.getValueOfType("abcd");
      fail("Should have thrown AuParamType.InvalidFormatException");
    } catch (AuParamType.InvalidFormatException expected) {
    }
  }

  public void testSampleValue() {
    assertEquals("42", ConfigParamDescr.VOLUME_NUMBER.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.VOLUME_NAME.getSampleValue());
    assertEquals("abc-def", ConfigParamDescr.ISSUE_RANGE.getSampleValue());
    assertEquals("52-63", ConfigParamDescr.NUM_ISSUE_RANGE.getSampleValue());
    assertEquals("winter,spring,summer,fall",
		 ConfigParamDescr.ISSUE_SET.getSampleValue());
    assertEquals("2038", ConfigParamDescr.YEAR.getSampleValue());
    assertEquals("http://example.com/path/file.ext",
		 ConfigParamDescr.BASE_URL.getSampleValue());
    assertEquals("http://example.com/path/file.ext",
		 ConfigParamDescr.BASE_URL2.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.JOURNAL_DIR.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.JOURNAL_ABBR.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.JOURNAL_ID.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.JOURNAL_ISSN.getSampleValue());
    assertEquals("SampleString",
		 ConfigParamDescr.PUBLISHER_NAME.getSampleValue());
    assertEquals("http://example.com/path/file.ext",
		 ConfigParamDescr.OAI_REQUEST_URL.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.OAI_SPEC.getSampleValue());
    assertEquals("username:passwd",
		 ConfigParamDescr.USER_CREDENTIALS.getSampleValue());
    assertEquals("true", ConfigParamDescr.AU_CLOSED.getSampleValue());
    assertEquals("true", ConfigParamDescr.PUB_DOWN.getSampleValue());
    assertEquals("true", ConfigParamDescr.PUB_NEVER.getSampleValue());
    assertEquals("42", ConfigParamDescr.PROTOCOL_VERSION.getSampleValue());
    assertEquals("SampleString", ConfigParamDescr.CRAWL_PROXY.getSampleValue());
    assertEquals("10d", ConfigParamDescr.CRAWL_INTERVAL.getSampleValue());
  }
  
  public void testYear() throws Exception {
    // Four-digit years are okay
    assertEquals(new Integer(1000), ConfigParamDescr.YEAR.getValueOfType("1000"));
    assertEquals(new Integer(9999), ConfigParamDescr.YEAR.getValueOfType("9999"));
    
    // Currently, the special value "0" is allowed
    assertEquals(new Integer(0), ConfigParamDescr.YEAR.getValueOfType("0"));

    // Other lengths are not allowed 
    try {
      ConfigParamDescr.YEAR.getValueOfType("999");
      fail("Should have thrown InvalidFormatException");
    }
    catch (InvalidFormatException expected) {
      // Expected
    }
    try {
      ConfigParamDescr.YEAR.getValueOfType("10000");
      fail("Should have thrown InvalidFormatException");
    }
    catch (InvalidFormatException expected) {
      // Expected
    }

    // Strings that parse to negative integers or that don't parse to integers are not allowed
    try {
      ConfigParamDescr.YEAR.getValueOfType("-123");
      fail("Should have thrown InvalidFormatException");
    }
    catch (InvalidFormatException expected) {
      // Expected
    }
    try {
      ConfigParamDescr.YEAR.getValueOfType("123X");
      fail("Should have thrown InvalidFormatException");
    }
    catch (InvalidFormatException expected) {
      // Expected
    }
  }
  
}
