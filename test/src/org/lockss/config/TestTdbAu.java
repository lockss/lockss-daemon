/*
 * $Id: TestTdbAu.java,v 1.1 2010-04-02 23:38:11 pgust Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.test.*;

import java.util.*;

/**
 * Test class for <code>org.lockss.config.TdbAu</code>
 *
 * @author  Philip Gust
 * @version $Id: TestTdbAu.java,v 1.1 2010-04-02 23:38:11 pgust Exp $
 */

public class TestTdbAu extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.config.TdbAu.class
  };

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
      au = new TdbAu("Test Au");
    } catch (IllegalArgumentException ex) {
      fail("Unexpected xception creating TdbAu", ex);
    }
    assertNotNull(au);
    assertEquals("Test Au", au.getName());
  }

  /**
   * Test creating TdbPublisher with null name.
   */
  public void testNullTitleName() {
    TdbPublisher publisher = null;
    try {
      publisher = new TdbPublisher(null);
      fail("TdbPublisher did not throw IllegalArgumentException for null argument.");
    } catch (IllegalArgumentException ex) {
      
    }
    assertNull(publisher);
  }
  
  /**
   * Test addAu() method.
   */
  public void testGetTitle() {
    TdbTitle title = new TdbTitle("Test Title");

    TdbAu au = new TdbAu("Test AU");
    title.addTdbAu(au);
    Collection<TdbAu> aus = title.getTdbAus();
    assertEquals(1, aus.size());
    assertTrue(aus.contains(au));
    
    // get title
    TdbTitle getTitle = au.getTdbTitle();
    assertEquals(title, getTitle);
  }
  
  /**
   * Test getPublisher() method.
   */
  public void testGetPublisher() {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    Collection titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add title
    TdbTitle title = new TdbTitle("Test Title");
    publisher.addTdbTitle(title);
    
    // add au
    TdbAu au = new TdbAu("Test AU");
    title.addTdbAu(au);
    
    // ensure same as publisher for AU's title
    TdbPublisher getPublisher = au.getTdbPublisher();
    assertEquals(publisher, getPublisher);
  }

  /**
   * Test convenience methods
   */
  public void testConvenienceMethods() {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    Collection titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add title
    TdbTitle title = new TdbTitle("Test Title");
    title.setId("1234-5678");
    publisher.addTdbTitle(title);

    // add au
    TdbAu au = new TdbAu("Test AU");
    au.setPluginId("org.lockss.TestPlugin");
    au.setPluginVersion("7");
    au.setPropertyByName("estSize", "32.5MB");
    au.setParam("p1", "v1");
    au.setParam("p2", "v2");
    title.addTdbAu(au);
    assertEquals("7", au.getPropertyByName("pluginVersion"));
    assertEquals("Test Title", au.getJournalTitle()); 
    assertEquals(32500000, au.getEstimatedSize());
    Properties props = au.toProperties();
    assertEquals("v1", props.getProperty("param.1.value"));
    /*
    assertEquals("32.5MB", props.getProperty("estSize"));
    */
  }
  /**
   * Test param methods
   */
  public void testParams() {
    // set two params
    TdbAu au = new TdbAu("Test AU");
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
      fail("TdbAu did not throw IllegalStateException resetting param.");
    } catch (IllegalStateException ex) {
    }
    assertNotNull(getParams);
    assertEquals(3, getParams.size());
    getParams = au.getParams();
    assertEquals("val2", getParams.get("name2"));
  }

  /**
   * Test attr methods
   */
  public void testAttrs() {
    // set two attrs
    TdbAu au = new TdbAu("Test AU");
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
      fail("TdbAu did not throw IllegalStateException resetting attr.");
    } catch (IllegalStateException ex) {
    }
    assertNotNull(getAttrs);
    assertEquals(2, getAttrs.size());
    getAttrs = au.getAttrs();
    assertEquals("val2", getAttrs.get("name2"));
  }
  
  /**
   * Test TdbAu.Id
   */
  public void testTdbAuId() {
    TdbAu au1 = new TdbAu("Test AU1");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginId("pluginA");
    au1.setPluginVersion("3");
    
    TdbAu au2 = new TdbAu("Test AU1");
    au2.setAttr("a", "A");
    au2.setAttr("b", "A");
    au2.setParam("x", "X");
    au2.setPluginId("pluginA");
    au2.setPluginVersion("3");

    TdbAu au3 = new TdbAu("Test AU1");
    au3.setAttr("a", "A");
    au3.setAttr("b", "A");
    au3.setParam("x", "X");
    au3.setPluginId("pluginB");
    au3.setPluginVersion("3");

    assertEquals(au1.getId(), au2.getId());
    assertNotEquals(au1.getId(), au3.getId());
  }
  
  /**
   * Test copyForTdbTitle method.
   */
  public void testCopyForTdbTitle() {
    TdbAu au1 = new TdbAu("Test AU1");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginId("pluginA");
    au1.setPluginVersion("3");

    TdbTitle title1 = new TdbTitle("Test Title1");
    title1.addTdbAu(au1);
    
    TdbTitle title2 = new TdbTitle("Test Title2");
    TdbAu au2 = au1.copyForTdbTitle(title2);
    assertEquals(title2, au2.getTdbTitle());
    assertEquals(au1.getName(), au2.getName());
    assertEquals(au1.getAttr("a"), au2.getAttr("a"));
    assertEquals(au1.getParam("x"), au2.getParam("x"));
    assertEquals(au1.getPluginId(), au2.getPluginId());
    assertEquals(au1.getPluginVersion(), au2.getPluginVersion());
    
    
  }
}
