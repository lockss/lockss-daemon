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

package org.lockss.config;

import org.lockss.util.*;
import org.lockss.config.Tdb.TdbException;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

import java.util.*;

/**
 * Test class for <code>org.lockss.config.TdbAu</code>
 *
 * @author  Philip Gust
 * @version $Id$
 */

public class TestTdbAu extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestTdbAu");

  /**
   * Test creating valid TdbAu.
   */
  public void testValidAu() {
    TdbAu au = null;
    try {
      au = new TdbAu("Test Au", "Test Plugin");
    } catch (IllegalArgumentException ex) {
      fail("Unexpected xception creating TdbAu", ex);
    }
    assertNotNull(au);
    assertEquals("Test Au", au.getName());
  }

  /**
   * Test equals() method
   * @throws TdbException for invalid Tdb operations
   */
  public void testIsDown() throws TdbException{
    TdbAu au1 = new TdbAu("Test AU", "pluginA");
    assertFalse(au1.isDown());
    
    // same as title1
    TdbAu au2 = new TdbAu("Test AU", "pluginA");
    au2.setParam("pub_down", "true");
    assertTrue(au2.isDown());
  }
  
  /**
   * Test getProprietaryId() and getProprietarySeriesId() methods.
   * @throws TdbException for invalid Tdb operations
   */
  public void testProprietaryId() throws TdbException {
    TdbAu au1 = new TdbAu("Test AU", "pluginA");
    
    // use param if specified with au type "journal"
    au1.setParam("journal_id", "foo");
    assertEquals("foo", au1.getProprietaryIds()[0]);
    // use attr instead if also specified
    au1.setAttr("journal_id", "bar");
    assertEquals("bar", au1.getProprietaryIds()[0]);
    
    // verify no series series id for journal
    if (au1.getProprietarySeriesIds() != null
	&& au1.getProprietarySeriesIds().length > 0) {
      assertNull(au1.getProprietarySeriesIds()[0]);
    }
    
    // change au type to "book series" and verify no proprietary id
    au1.setPropertyByName("type", "bookSeries");
    if (au1.getProprietaryIds() != null && au1.getProprietaryIds().length > 0) {
      assertNull(au1.getProprietaryIds()[0]);
    }

    // verify "journal_id" is is now interpreted as series id
    assertEquals("bar", au1.getProprietarySeriesIds()[0]);
    
    // verify that book id is now intrpreted as proprietary id
    au1.setAttr("book_id", "baz");
    assertEquals("baz", au1.getProprietaryIds()[0]);
  }
  
  /**
   * Test equals() method
   * @throws TdbException for invalid Tdb operations
   */
  public void testEquals() throws TdbException{
    TdbAu au1 = new TdbAu("Test AU", "pluginA");
    au1.setParam("name1", "val1");
    au1.setParam("name2", "val2");
    assertEquals(au1, au1);
    
    // same as title1
    TdbAu au2 = new TdbAu("Test AU", "pluginA");
    au2.setParam("name1", "val1");
    au2.setParam("name2", "val2");
    assertEquals(au1, au2);

    // differs from title1 only by au param
    TdbAu au3 = new TdbAu("Test AU", "pluginA");
    au3.setParam("name1", "val1");
    au3.setParam("name2", "val3");
    assertNotEquals(au1, au3);
    
    // differs from title3 only by plugin id 
    TdbAu au4 = new TdbAu("Test AU", "pluginB");
    au4.setParam("name1", "val1");
    au4.setParam("name2", "val3");
    assertNotEquals(au3, au4);
    
    // au1 and au2 differ only by a property
    au1.setPropertyByName("issn", "1234-5678");
    assertNotEquals(au1, au2);
    
    au2.setPropertyByName("issn", "1234-5678");
    assertEquals(au1, au2);
    
    // au1 and au2 differ only by an attribute
    au1.setAttr("year", "1953");
    assertNotEquals(au1, au2);

    au2.setAttr("year", "1954");
    assertNotEquals(au1, au2);
  }
  
  /**
   * Test addAu() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetTitle() throws TdbException {
    TdbTitle title = new TdbTitle("Test Title", "0000-0000");
    
    TdbAu au = new TdbAu("Test AU", "pluginA");
    title.addTdbAu(au);
    Collection<TdbAu> aus = title.getTdbAus();
    assertEquals(1, aus.size());
    assertTrue(aus.contains(au));
    assertSameElements(ListUtil.list(au),
		       ListUtil.fromIterator(title.tdbAuIterator()));

    
    // get title
    TdbTitle getTitle = au.getTdbTitle();
    assertSame(title, getTitle);
  }
  
  /**
   * Test year operations
   * @throws TdbException for invalid Tdb operations
   */
  public void testYear() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertNull(au.getYear());
    assertNull(au.getStartYear());
    assertNull(au.getEndYear());
    
    au.setParam("year", "1971");
    assertEquals("1971", au.getYear());
    assertEquals("1971", au.getStartYear());
    assertEquals("1971", au.getEndYear());
    assertTrue(au.includesYear("1971"));
    assertFalse(au.includesYear("1970"));

    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("year", "1971-1977");
    assertEquals("1971-1977", au.getYear());
    assertEquals("1971", au.getStartYear());
    assertEquals("1977", au.getEndYear());
    assertTrue(au.includesYear("1971")); 
    assertTrue(au.includesYear("1975"));
    assertFalse(au.includesYear("1970"));
    assertFalse(au.includesYear("1979"));

    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("year", "MCMLXXI-1977");
    assertEquals("MCMLXXI-1977", au.getYear());
    assertEquals("MCMLXXI", au.getStartYear());
    assertEquals("1977", au.getEndYear());
    assertTrue(au.includesYear("MCMLXXVII"));
    assertTrue(au.includesYear("MCMLXXV"));
    assertFalse(au.includesYear("MCMLXX"));
    assertFalse(au.includesYear("1979"));

    // test year range as range list
    au = new TdbAu("TestAU", "pluginA");
    String issStr = "1960-1969, 1970, MCMLXXI-1977";
    au.setAttr("year", issStr);
    assertEquals(issStr, au.getYear());
    assertTrue(NumberUtil.isContiguousRange(au.getYear()));
    assertEquals("1960", au.getStartYear());
    assertEquals("1977", au.getEndYear());
    assertTrue(au.includesYear("MCMLXXVII"));
    assertTrue(au.includesYear("1972"));
    assertTrue(au.includesYear("1968"));
    assertTrue(au.includesYear("1970"));

    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("year", "1971,1973,1975-1977");
    assertEquals("1971,1973,1975-1977", au.getYear());
    assertEquals("1971", au.getStartYear());
    assertEquals("1977", au.getEndYear());
    assertTrue(au.includesYear("1971")); 
    assertTrue(au.includesYear("1975"));
    assertFalse(au.includesYear("1970"));
    assertFalse(au.includesYear("1974"));
    assertFalse(au.includesYear("1979"));
  }
  
  /**
   * Test volumes operations
   * @throws TdbException for invalid Tdb operations
   */
  public void testVolumes() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertNull(au.getVolume());
    assertNull(au.getStartVolume());
    assertNull(au.getEndVolume());
    
    // test single arabic  number
    au.setParam("volume", "1971");
    assertEquals("1971", au.getVolume());
    assertEquals("1971", au.getStartVolume());
    assertEquals("1971", au.getEndVolume());
    assertTrue(au.includesVolume("1971"));
    assertFalse(au.includesVolume("1970"));
    assertFalse(au.includesVolume("1979"));

    // test arabic number range
    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("volume", "1971-1977");
    assertEquals("1971-1977", au.getVolume());
    assertEquals("1971", au.getStartVolume());
    assertEquals("1977", au.getEndVolume());
    assertTrue(au.includesVolume("1971"));
    assertTrue(au.includesVolume("1975"));
    assertFalse(au.includesVolume("1970"));
    assertFalse(au.includesVolume("1979"));

    // test interchangeability of arabic and roman numbers
    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("volume", "MCMLXXI-1977");
    assertEquals("MCMLXXI-1977", au.getVolume());
    assertEquals("MCMLXXI", au.getStartVolume());
    assertEquals("1977", au.getEndVolume());
    assertTrue(au.includesVolume("MCMLXXVII"));
    assertTrue(au.includesVolume("MCMLXXV"));
    assertFalse(au.includesVolume("MCMLXX"));
    assertFalse(au.includesVolume("1979"));

    // test volume ranges as "topic ranges"
    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("volume", "Ge-Ma");
    assertEquals("Ge-Ma", au.getVolume());
    assertEquals("Ge", au.getStartVolume());
    assertEquals("Ma", au.getEndVolume());
    assertTrue(au.includesVolume("Ge"));
    assertTrue(au.includesVolume("Georgia"));
    assertTrue(au.includesVolume("M"));
    assertTrue(au.includesVolume("Ma"));
    assertTrue(au.includesVolume("Mathematics"));
    assertFalse(au.includesVolume("G"));
    assertFalse(au.includesVolume("Mbone"));

    // test volume range as range list
    au = new TdbAu("TestAU", "pluginA");
    String volStr = "Ge-Ma;MCMLXXI-1977;1960-1969";
    au.setAttr("volume", volStr);
    assertEquals(volStr, au.getVolume());
    assertFalse(NumberUtil.isContiguousRange(au.getVolume()));
    assertEquals("Ge", au.getStartVolume());
    assertEquals("1969", au.getEndVolume());
    assertTrue(au.includesVolume("Ge"));
    assertTrue(au.includesVolume("MCMLXXVII"));
    assertTrue(au.includesVolume("1972"));
    assertTrue(au.includesVolume("1968"));
    assertFalse(au.includesVolume("1970"));

    // Test that Roman tokens get interpreted as Roman tokens
    au = new TdbAu("TestAU", "pluginA");
    volStr = "s1v-s1x; s1i, s1ii";
    au.setAttr("volume", volStr);
    assertEquals(volStr, au.getVolume());
    // Include values from an alphabetic interpretation (topic ranges)
    // s1v-s1x to include s1ix and also s1w and
    assertTrue(au.includesVolume("s1vi"));
    assertTrue(au.includesVolume("s1ix"));
    assertTrue(au.includesVolume("s1w"));
    assertTrue(au.includesVolume("s1word"));
    // s1ii to not include s1iii
    assertFalse(au.includesVolume("s1iii"));

    au = new TdbAu("TestAU", "pluginA");
    volStr = "1971,1973,1975-1977";
    au.setAttr("volume", volStr);
    assertEquals(volStr, au.getVolume());
    assertEquals("1971", au.getStartVolume());
    assertEquals("1977", au.getEndVolume());
    assertTrue(au.includesVolume("1971")); 
    assertTrue(au.includesVolume("1975"));
    assertFalse(au.includesVolume("1970"));
    assertFalse(au.includesVolume("1974"));
    assertFalse(au.includesVolume("1979"));
  }
  
  /**
   * Test issue operations
   * @throws TdbException for invalid Tdb operations
   */
  public void testIssues() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertNull(au.getIssue());
    assertNull(au.getStartIssue());
    assertNull(au.getEndIssue());
    
    // test single arabic  number
    au.setParam("issue", "1971");
    assertEquals("1971", au.getIssue());
    assertEquals("1971", au.getStartIssue());
    assertEquals("1971", au.getEndIssue());
    assertTrue(au.includesIssue("1971"));
    assertFalse(au.includesIssue("1970"));
    assertFalse(au.includesIssue("1979"));

    // test arabic number range
    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("issue", "1971-1977");
    assertEquals("1971-1977", au.getIssue());
    assertEquals("1971", au.getStartIssue());
    assertEquals("1977", au.getEndIssue());
    assertTrue(au.includesIssue("1971"));
    assertTrue(au.includesIssue("1975"));
    assertFalse(au.includesIssue("1970"));
    assertFalse(au.includesIssue("1979"));

    // test interchangeability of arabic and roman numbers
    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("issue", "MCMLXXI-1977");
    assertEquals("MCMLXXI-1977", au.getIssue());
    assertEquals("MCMLXXI", au.getStartIssue());
    assertEquals("1977", au.getEndIssue());
    assertTrue(au.includesIssue("MCMLXXVII"));
    assertTrue(au.includesIssue("MCMLXXV"));
    assertFalse(au.includesIssue("MCMLXX"));
    assertFalse(au.includesIssue("1979"));

    // test issue ranges as "topic ranges"
    au = new TdbAu("TestAU", "pluginA");
    au.setAttr("issue", "Ge-Ma");
    assertEquals("Ge-Ma", au.getIssue());
    assertEquals("Ge", au.getStartIssue());
    assertEquals("Ma", au.getEndIssue());
    assertTrue(au.includesIssue("Ge"));
    assertTrue(au.includesIssue("Georgia"));
    assertTrue(au.includesIssue("M"));
    assertTrue(au.includesIssue("Ma"));
    assertTrue(au.includesIssue("Mathematics"));
    assertFalse(au.includesIssue("G"));
    assertFalse(au.includesIssue("Mbone"));

    // test issue range as range list
    au = new TdbAu("TestAU", "pluginA");
    String issStr = "Ge-Ma;MCMLXXI-1977;1960-1969";
    au.setAttr("issue", issStr);
    assertEquals(issStr, au.getIssue());
    assertFalse(NumberUtil.isContiguousRange(au.getIssue()));
    assertEquals("Ge", au.getStartIssue());
    assertEquals("1969", au.getEndIssue());
    assertTrue(au.includesIssue("Ge"));
    assertTrue(au.includesIssue("MCMLXXVII"));
    assertTrue(au.includesIssue("1972"));
    assertTrue(au.includesIssue("1968"));
    assertFalse(au.includesIssue("1970"));

    au = new TdbAu("TestAU", "pluginA");
    issStr = "1971,1973,1975-1977";
    au.setAttr("issue", issStr);
    assertEquals(issStr, au.getIssue());
    assertEquals("1971", au.getStartIssue());
    assertEquals("1977", au.getEndIssue());
    assertTrue(au.includesIssue("1971")); 
    assertTrue(au.includesIssue("1975"));
    assertFalse(au.includesIssue("1970"));
    assertFalse(au.includesIssue("1974"));
    assertFalse(au.includesIssue("1979"));
  }
  
  /**
   * Test ISBNs and ISBNs
   * @throws TdbException for invalid Tdb operations
   */
  public void testIssns() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    au.setPropertyByName("issn", "1234-5678");
    au.setPropertyByName("eissn", "2468-1357");
    au.setPropertyByName("issnl", "8765-4321");
    au.setAttr("isbn", "1234567890");
    assertEquals("1234-5678", au.getPrintIssn());
    assertEquals("2468-1357", au.getEissn());
    assertEquals("8765-4321", au.getIssnL());
    assertNotNull(au.getIssn());
    assertEquals("1234567890", au.getIsbn());
    assertEquals("1234567890", au.getPrintIsbn());
    assertNull(au.getEisbn());

    // Test behaviour with ill-formed ISSNs
    au.setPropertyByName("issn", "1234-5");
    au.setPropertyByName("eissn", "-2468");
    au.setPropertyByName("issnl", "string");
    // Individual getters should return the invalid string,
    // but getIssn() should return null
    assertEquals("1234-5", au.getPrintIssn());
    assertEquals("-2468", au.getEissn());
    assertEquals("string", au.getIssnL());
    assertNull(au.getIssn());
  }
  
  /**
   * Test publication title
   * @throws TdbException for invalid Tdb operations
   */
  public void testJournalTitle() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertNull(au.getPublicationTitle());  // because au has no TdbTitle
    au.setPropertyByName("issn", "1234-5678");
    assertNull(au.getPublicationTitle());  // because au has no TdbTitle
    au.setAttr("isbn", "978-3-540-33894-9");
    assertEquals("Test AU", au.getPublicationTitle());
    au.setPropertyByName("type", "book");
    assertEquals("Test AU", au.getPublicationTitle());
  }
  
  /**
   * Test publication type
   * @throws TdbException for invalid Tdb operations
   */
  public void testPublicationType() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertEquals("journal", au.getPublicationType());
    au.setPropertyByName("issn", "1234-5678");
    assertEquals("journal", au.getPublicationType());
    au.setAttr("isbn", "978-3-540-33894-9");
    assertEquals("bookSeries", au.getPublicationType());
    au.setPropertyByName("type", "book");
    assertEquals("book", au.getPublicationType());
  }
  
  /**
   * Test coverage depth
   * @throws TdbException for invalid Tdb operations
   */
  public void testCoverageDepth() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    // test default when no attributes present
    assertEquals("fulltext", au.getCoverageDepth());
    // test default when attributes present
    au.setAttr("attr_name", "attr_value");
    assertEquals("fulltext", au.getCoverageDepth());
    // test when au_coverage_depth attribute set
    au.setAttr("au_coverage_depth","abstracts");
    assertEquals("abstracts", au.getCoverageDepth());
  }
  
  /**
   * Test getPublisher() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetPublisher() throws TdbException {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    Collection<TdbTitle> titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add title
    TdbTitle title = new TdbTitle("Test Title", "0000-0000");
    publisher.addTdbTitle(title);
    
    // add au
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertNull(au.getPublicationTitle());
    title.addTdbAu(au);
    
    // ensure same as publisher for AU's title
    TdbPublisher getPublisher = au.getTdbPublisher();
    assertEquals(publisher, getPublisher);
    assertEquals("Test Publisher", au.getPublisherName());
  }

  /**
   * Test getProvider() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetProvider() throws TdbException {
    TdbProvider provider = new TdbProvider("Test Provider");
    Collection<TdbTitle> titles = provider.getTdbTitles();
    assertEmpty(titles);
    
    // add au
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertNull(au.getProviderName());
    provider.addTdbAu(au);
    
    // ensure same as publisher for AU's title
    TdbProvider getProvider = au.getTdbProvider();
    assertEquals(provider, getProvider);
    assertEquals("Test Provider", au.getProviderName());
  }

  /**
   * Test convenience methods.
   * @throws TdbException for invalid Tdb operations
   */
  public void testConvenienceMethods() throws TdbException {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    Collection<TdbTitle> titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add title
    TdbTitle title = new TdbTitle("Test Title", "1234-5678");
    publisher.addTdbTitle(title);

    // add au
    TdbAu au = new TdbAu("Test AU", "org.lockss.TestPlugin");
    au.setPluginVersion("7");
    au.setPropertyByName("estSize", "32.5MB");
    au.setParam("p1", "v1");
    au.setParam("p2", "v2");
    title.addTdbAu(au);
    assertEquals("7", au.getPropertyByName("pluginVersion"));
    assertEquals("Test Title", au.getPublicationTitle()); 
    assertEquals(32500000, au.getEstimatedSize());
    Properties props = au.toProperties();
    assertTrue(props.containsKey("param.1.value"));
    assertTrue(props.containsValue("v1"));
    assertTrue(props.containsKey("param.2.value"));
    assertTrue(props.containsValue("v2"));
    /*
    assertEquals("32.5MB", props.getProperty("estSize"));
    */
  }
  /**
   * Test param methods
   * @throws TdbException for invalid Tdb operations
   */
  public void testParams() throws TdbException {
    // set two params
    TdbAu au = new TdbAu("Test AU", "pluginA");
    au.setParam("name1", "val1");
    au.setParam("name2", "val2");
    au.setParam("name3", "val3");
    
    // set invalid params
    try {
      au.setParam("name4", null);
      fail("TdbAu did not throw IllegalArgumentException for null param argument.");
    } catch (IllegalArgumentException ex) {
    }
    
    // ensure params contain expected names and values
    Map<String,String> getParams = au.getParams();
    assertNotNull(getParams);
    assertEquals(3, getParams.size());
    assertEquals("val1", getParams.get("name1"));
    assertEquals("val2", getParams.get("name2"));
    assertEquals("val3", getParams.get("name3"));
  
    // ensure param cannot be reset
    try {
      au.setParam("name2", "newval2");
      fail("TdbAu did not throw TdbException resetting param.");
    } catch (TdbException ex) {
    }
    assertNotNull(getParams);
    assertEquals(3, getParams.size());
    getParams = au.getParams();
    assertEquals("val2", getParams.get("name2"));
    
    TdbTitle title = new TdbTitle("Test Title", "0000-0000");
    title.addTdbAu(au);
    try {
      au.setParam("name4", "newval4");
      fail("TdbAu did not throw TdbException setting param once added to title.");
    } catch (TdbException ex) {
    }
    assertNotNull(getParams);
    assertEquals(3, getParams.size());
    getParams = au.getParams();
    assertNull(getParams.get("name4"));
  }

  /**
   * Test attr methods
   */
  public void testAttrs() throws TdbException {
    // set two attrs
    TdbAu au = new TdbAu("Test AU", "Test ID");
    au.setAttr("name1", "val1");
    au.setAttr("name2", "val2");
    
    // set invalid params
    try {
      au.setAttr("name3", null);
      fail("TdbAu did not throw IllegalArgumentException for null attr argument.");
    } catch (IllegalArgumentException ex) {
    }
    
    // ensure params contain expected names and values
    Map<String,String> getAttrs = au.getAttrs();
    assertNotNull(getAttrs);
    assertEquals(2, getAttrs.size());
    assertEquals("val1", getAttrs.get("name1"));
    assertEquals("val2", getAttrs.get("name2"));
  
    // ensure attr cannot be reset
    try {
      au.setAttr("name2", "newval2");
      fail("TdbAu did not throw TdbException resetting attr.");
    } catch (TdbException ex) {
    }
    assertNotNull(getAttrs);
    assertEquals(2, getAttrs.size());
    getAttrs = au.getAttrs();
    assertEquals("val2", getAttrs.get("name2"));
  }
  
  /**
   * Test TdbAu.Id
   * @throws TdbException for invalid Tdb operations
   */
  public void testTdbAuId() throws TdbException {
    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginVersion("3");
    
    TdbAu au2 = new TdbAu("Test AU1", "pluginA");
    au2.setAttr("a", "A");
    au2.setAttr("b", "A");
    au2.setParam("x", "X");
    au2.setPluginVersion("3");

    TdbAu au3 = new TdbAu("Test AU1", "pluginB");
    au3.setAttr("a", "A");
    au3.setAttr("b", "A");
    au3.setParam("x", "X");
    au3.setPluginVersion("3");

    assertEquals(au1.getId(), au2.getId());
    assertNotEquals(au1.getId(), au3.getId());
  }
  
  /**
   * Test AuId
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetAuId() throws TdbException {
    PluginManager pmgr = getMockLockssDaemon().getPluginManager();

    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    au1.setAttr("a", "A");
    au1.setParam("x", "X");
    try {
      au1.getAuId(pmgr);
      fail("getAuId() with no plugin should throw");
    } catch (IllegalStateException e) {
    }
    
    MockPlugin plug = (MockPlugin)PluginTestUtil.findPlugin(MockPlugin.class);
    plug.setAuConfigDescrs(ListUtil.list(ConfigParamDescr.BASE_URL,
					 ConfigParamDescr.VOLUME_NUMBER,
					 ConfigParamDescr.CRAWL_PROXY));

    TdbAu au2 = new TdbAu("Test AU1", plug.getPluginId());
    au2.setAttr("a", "A");
    au2.setParam("base_url", "http://example.com/bbb/");
    au2.setParam("volume", "12");
    au2.setParam("crawl_proxy", "foo.bar:8080");
    assertEquals("org|lockss|test|MockPlugin&base_url~http%3A%2F%2Fexample%2Ecom%2Fbbb%2F&volume~12",
		 au2.getAuId(pmgr));

    TdbAu au3 = new TdbAu("Test AU1", plug.getPluginId());
    au3.setAttr("a", "A");
    au3.setParam("base_url", "http://example.com/bbb/");
    au3.setParam("crawl_proxy", "foo.bar:8080");
    try {
      au3.getAuId(pmgr);
      fail("getAuId() with missing config param should throw");
    } catch (NullPointerException e) {
    }
  }

  /**
   * Test copyForTdbTitle method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testCopyForTdbTitle() throws TdbException {
    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginVersion("3");

    TdbTitle title1 = new TdbTitle("Test Title1", "0000-0000");
    title1.addTdbAu(au1);
    
    TdbTitle title2 = new TdbTitle("Test Title2", "0000-0000");
    TdbAu au2 = au1.copyForTdbTitle(title2);
    assertSame(title2, au2.getTdbTitle());
    assertNotSame(title1, title2);
    assertEquals(au1.getName(), au2.getName());
    assertEquals(au1.getAttr("a"), au2.getAttr("a"));
    assertEquals(au1.getParam("x"), au2.getParam("x"));
    assertEquals(au1.getPluginId(), au2.getPluginId());
    assertEquals(au1.getPluginVersion(), au2.getPluginVersion());
  }
  
  /**
   * Test isSerial()
   * @throws TdbException for invalid Tdb operations
   */
  public void testIsSerial() throws TdbException {
    TdbAu au = new TdbAu("Test AU", "pluginA");
    assertTrue(au.isSerial());
    au.setPropertyByName("issn", "1234-5678");
    assertTrue(au.isSerial());
    au.setAttr("isbn", "978-3-540-33894-9");
    assertTrue(au.isSerial());
    au.setPropertyByName("type", "book");
    assertFalse(au.isSerial());
    au.setPropertyByName("type", "blog");
    assertFalse(au.isSerial());
  }
}
