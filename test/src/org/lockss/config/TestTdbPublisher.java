/*
 * $Id: TestTdbPublisher.java,v 1.3 2010-04-05 17:37:50 pgust Exp $
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
 * Test class for <code>org.lockss.config.TdbPublisher</code>
 *
 * @author  Philip Gust
 * @version $Id: TestTdbPublisher.java,v 1.3 2010-04-05 17:37:50 pgust Exp $
 */

public class TestTdbPublisher extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.config.TdbPublisher.class
  };

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestTdbPublisher");

  /**
   * Test creating valid TdbPublisher.
   */
  public void testValidPublisher() {
    TdbPublisher publisher = null;
    try {
      publisher = new TdbPublisher("Test Publisher");
    } catch (IllegalArgumentException ex) {
    }
    assertEquals("Test Publisher", publisher.getName());
  }

  /**
   * Test creating TdbPublisher with null name.
   */
  public void testNullPublisherName() {
    TdbPublisher publisher = null;
    try {
      publisher = new TdbPublisher(null);
      fail("TdbPublisher did not throw IllegalArgumentException for null constructor argument.");
    } catch (IllegalArgumentException ex) {
      
    }
    assertNull(publisher);
  }
  
  /**
   * Test addTitle() method.
   */
  public void testAddTitle() {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    Collection titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add title
    TdbTitle title = new TdbTitle("Test Title 1");
    publisher.addTdbTitle(title);
    titles = publisher.getTdbTitles();
    assertNotNull(titles);
    assertEquals(1, titles.size());

    // can't add same title twice
    try {
      publisher.addTdbTitle(title);
      fail("TdbPublisher did not throw IllegalStateException adding duplicate title.");
    } catch (IllegalStateException ex) {
    }
    titles = publisher.getTdbTitles();
    assertEquals(1, titles.size());

    // can't add null title
    try {
      publisher.addTdbTitle(null);
      fail("TdbPublisher did not throw IllegalArgumentException adding null title.");
    } catch (IllegalArgumentException ex) {
    }
    titles = publisher.getTdbTitles();
    assertEquals(1, titles.size());

    TdbTitle title2 = new TdbTitle("Test Title 2");
    title2.setId(title.getId());
    try {
      publisher.addTdbTitle(title2);
      fail("TdbPublisher did not throw IllegalStateException adding duplicate title");
    } catch (IllegalStateException ex) {
    }
  }

  /**
   * Test getTitlesByName() and getTitleById() methods.
   */
  public void testGetTitle() {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");

    Collection titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add two titles
    TdbTitle title1 = new TdbTitle("Journal Title");
    title1.setId("1234-5678");
    publisher.addTdbTitle(title1);

    TdbTitle title2 = new TdbTitle("Book Title");
    title2.setId("978-0521807678");
    publisher.addTdbTitle(title2);

    TdbTitle title3 = new TdbTitle("Book Title");
    title3.setId("978-0345303066");
    publisher.addTdbTitle(title3);

    // get two titles by name
    Collection<TdbTitle> getTitle1 = publisher.getTdbTitlesByName("Journal Title");
    assertEquals(1, getTitle1.size());
    assertEquals(title1, getTitle1.iterator().next());
  
    Collection<TdbTitle> getTitle2 = publisher.getTdbTitlesByName("Book Title");
    assertEquals(2, getTitle2.size());
    assertTrue(getTitle2.contains(title2));
    assertTrue(getTitle2.contains(title3));

    // get title by ID
    TdbTitle getTitleId2 = publisher.getTdbTitleById("978-0521807678");
    assertEquals(title2, getTitleId2);
    
    // get unknown title by name
    Collection<TdbTitle> getTitleUnknown = publisher.getTdbTitlesByName("unknown");
    assertEmpty(getTitleUnknown);

    // get unknown title by ID
    TdbTitle getTitleIdUnknown = publisher.getTdbTitleById("unknown");
    assertNull(getTitleIdUnknown);
  }
  
  /**
   * Test TdbAu.addPluginIdsForDifferences() method.
   */
  public void testAddPluginIdsForDifferences() {
    TdbPublisher pub1 = new TdbPublisher("Test Publisher");
    TdbTitle title1 = new TdbTitle("Test Title");
    
    TdbAu au1 = new TdbAu("Test AU1");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginId("pluginA");
    au1.setPluginVersion("3");
    title1.addTdbAu(au1);
    pub1.addTdbTitle(title1);
    
    TdbPublisher pub2 = new TdbPublisher("Test Publisher");
    TdbTitle title2 = new TdbTitle("Test Title");
    
    TdbAu au2 = new TdbAu("Test AU1");
    au2.setAttr("a", "A");
    au2.setAttr("b", "A");
    au2.setParam("x", "X");
    au2.setPluginId("pluginA");
    au2.setPluginVersion("3");
    title2.addTdbAu(au2);
    pub2.addTdbTitle(title2);

    TdbPublisher pub3 = new TdbPublisher("Test Publisher");
    TdbTitle title3 = new TdbTitle("Test Title");
    
    TdbAu au3 = new TdbAu("Test AU1");
    au3.setAttr("a", "A");
    au3.setAttr("b", "A");
    au3.setParam("x", "X");
    au3.setPluginId("pluginB");
    au3.setPluginVersion("3");
    title3.addTdbAu(au3);
    pub3.addTdbTitle(title3);

    Set<String> diff12 = new HashSet();
    pub1.addPluginIdsForDifferences(diff12, pub2);
    assertEquals(0, diff12.size());
    
    Set<String> diff13 = new HashSet();
    pub1.addPluginIdsForDifferences(diff13, pub3);
    assertEquals(2, diff13.size());
  }
}
