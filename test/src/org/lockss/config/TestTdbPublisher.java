/*
 * $Id$
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
import org.lockss.test.*;
import org.lockss.config.Tdb.TdbException;

import java.util.*;

/**
 * Test class for <code>org.lockss.config.TdbPublisher</code>
 *
 * @author  Philip Gust
 * @version $Id$
 */

public class TestTdbPublisher extends LockssTestCase {

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
   * Test equals() method for publisher.
   * @throws TdbException for invalid Tdb operations
   */
  public void testEquals() throws TdbException {
    TdbPublisher publisher1 = new TdbPublisher("Test Publisher");
    TdbTitle title1 = new TdbTitle("Test Title 1", "0000-0001");
    publisher1.addTdbTitle(title1);
    assertEquals(publisher1, publisher1);
    
    // same as publisher1
    TdbPublisher publisher2 = new TdbPublisher("Test Publisher");
    TdbTitle title2 = new TdbTitle("Test Title 1", "0000-0002");
    publisher2.addTdbTitle(title2);
    assertEquals(publisher2, publisher2);

    // differs from publisher1 by title name
    TdbPublisher publisher3 = new TdbPublisher("Test Publisher");
    TdbTitle title3 = new TdbTitle("Test Title 3", "0000-0003");
    publisher3.addTdbTitle(title3);
    assertNotEquals(publisher1, publisher3);

    // differs from publisher3 by publisher name
    TdbPublisher publisher4 = new TdbPublisher("Test Publisher 4");
    TdbTitle title4 = new TdbTitle("Test Title 3", "0000-0004");
    publisher4.addTdbTitle(title4);
    assertNotEquals(publisher3, publisher4);
  }
  
  /**
   * Test ISBNs and ISBNs
   * @throws TdbException for invalid Tdb operations
   */
  public void testIssns() throws TdbException {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    TdbTitle title1 = new TdbTitle("Test Title 1", "TestTitle1");
    publisher.addTdbTitle(title1);
    TdbAu au1 = new TdbAu("Test AU 1", "pluginA");
    title1.addTdbAu(au1);
    au1.setPropertyByName("issn", "1234-5678");
    au1.setPropertyByName("eissn", "2468-1357");
    au1.setPropertyByName("issnl", "8765-4321");
    au1.setAttr("isbn", "1234567890");
    assertEquals(title1, publisher.getTdbTitleByIssn("1234-5678"));
    assertEquals(title1, publisher.getTdbTitleByIssn("2468-1357"));
    assertEquals(title1, publisher.getTdbTitleByIssn("8765-4321"));
    assertSameElements(new TdbAu[] {au1}, 
                       publisher.getTdbAusByIsbn("1234567890"));
    assertSameElements(new TdbTitle[] {title1}, 
                       publisher.getTdbTitlesByIssn("1234-5678"));
    
    // add second title with same elements
    TdbTitle title2 = new TdbTitle("Test Title 2", "TestTitle2");
    publisher.addTdbTitle(title2);
    TdbAu au2 = new TdbAu("Test AU 2", "pluginB");
    title2.addTdbAu(au2);
    au2.setPropertyByName("issn", "1234-5678");
    au2.setPropertyByName("eissn", "2468-1357");
    au2.setPropertyByName("issnl", "8765-4321");
    au2.setAttr("isbn", "1234567890");
    assertSameElements(new TdbAu[] {au1, au2},
                       publisher.getTdbAusByIsbn("1234567890"));
    assertSameElements(new TdbTitle[] {title1, title2},
                       publisher.getTdbTitlesByIssn("1234-5678"));
  }
  
  
  /**
   * Test addTitle() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testAddTitle() throws TdbException {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");
    Collection<TdbTitle> titles = publisher.getTdbTitles();
    assertEmpty(titles);
    
    // add title
    TdbTitle title = new TdbTitle("Test Title 1", "0000-0001");
    publisher.addTdbTitle(title);
    titles = publisher.getTdbTitles();
    assertNotNull(titles);
    assertEquals(1, titles.size());

    // can't add null title
    try {
      publisher.addTdbTitle(null);
      fail("TdbPublisher did not throw IllegalArgumentException adding null title.");
    } catch (IllegalArgumentException ex) {
    }
    titles = publisher.getTdbTitles();
    assertEquals(1, titles.size());

    TdbTitle title2 = new TdbTitle("Test Title 2", title.getId());
    try {
      publisher.addTdbTitle(title2);
      fail("TdbPublisher did not throw TdbException adding duplicate title");
    } catch (TdbException ex) {
    }
  }

  /**
   * Test getTitlesByName() and getTitleById() methods.
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetTitle() throws TdbException {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");

    Collection<TdbTitle> titles = publisher.getTdbTitles();
    assertEmpty(titles);
    assertFalse(publisher.tdbTitleIterator().hasNext());
    
    // add two titles
    TdbTitle title1 = new TdbTitle("Journal Title", "1234-5678");
    publisher.addTdbTitle(title1);

    TdbTitle title2 = new TdbTitle("Book Title", "978-0521807678");
    publisher.addTdbTitle(title2);

    TdbTitle title3 = new TdbTitle("Book Title", "978-0345303066");
    publisher.addTdbTitle(title3);

    titles = publisher.getTdbTitles();
    assertEquals(3, titles.size());
    assertSameElements(ListUtil.list(title1, title2, title3),
		       ListUtil.fromIterator(publisher.tdbTitleIterator()));
    assertEmpty(ListUtil.fromIterator(publisher.tdbAuIterator()));

    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    TdbAu au2 = new TdbAu("Test AU2", "pluginB");
    TdbAu au3 = new TdbAu("Test AU3", "pluginC");
    title1.addTdbAu(au1);
    title1.addTdbAu(au2);
    title3.addTdbAu(au3);
    assertSameElements(ListUtil.list(au1, au2, au3),
		       ListUtil.fromIterator(publisher.tdbAuIterator()));

    
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
   * Test getTdbProviderCount() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetProvider() throws TdbException {
    TdbPublisher publisher = new TdbPublisher("Test Publisher");

    Collection<TdbTitle> titles = publisher.getTdbTitles();
    assertEmpty(titles);
    assertFalse(publisher.tdbTitleIterator().hasNext());
    
    // add two titles
    TdbTitle title1 = new TdbTitle("Journal Title", "1234-5678");
    publisher.addTdbTitle(title1);

    TdbTitle title2 = new TdbTitle("Book Title", "978-0521807678");
    publisher.addTdbTitle(title2);

    TdbTitle title3 = new TdbTitle("Book Title", "978-0345303066");
    publisher.addTdbTitle(title3);

    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    new TdbProvider("provider1").addTdbAu(au1);
    TdbAu au2 = new TdbAu("Test AU2", "pluginB");
    new TdbProvider("provider2").addTdbAu(au2);
    TdbAu au3 = new TdbAu("Test AU3", "pluginC");
    new TdbProvider("provider3").addTdbAu(au3);
    title1.addTdbAu(au1);
    title1.addTdbAu(au2);
    title3.addTdbAu(au3);

    // ensure there are two providers: one that was
    // explicitly added  for au2, and one that was
    // implicitly added for au1 and au3
    assertEquals(3, publisher.getTdbProviderCount());
  }

  /**
   * Test TdbAu.addPluginIdsForDifferences() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testAddPluginIdsForDifferences() throws TdbException {
    TdbPublisher pub1 = new TdbPublisher("Test Publisher");
    TdbTitle title1 = new TdbTitle("Test Title", "0000-0001");
    
    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginVersion("3");
    title1.addTdbAu(au1);
    pub1.addTdbTitle(title1);
    
    // duplicate of pub1
    TdbPublisher pub2 = new TdbPublisher("Test Publisher");
    TdbTitle title2 = new TdbTitle("Test Title", "0000-0001");
    
    TdbAu au2 = new TdbAu("Test AU1", "pluginA");
    au2.setAttr("a", "A");
    au2.setAttr("b", "A");
    au2.setParam("x", "X");
    au2.setPluginVersion("3");
    title2.addTdbAu(au2);
    pub2.addTdbTitle(title2);

    TdbPublisher pub3 = new TdbPublisher("Test Publisher");
    TdbTitle title3 = new TdbTitle("Test Title", "0000-0003");
    
    TdbAu au3 = new TdbAu("Test AU1", "pluginB");
    au3.setAttr("a", "A");
    au3.setAttr("b", "A");
    au3.setParam("x", "X");
    au3.setPluginVersion("3");
    title3.addTdbAu(au3);
    pub3.addTdbTitle(title3);

    // no differences becuase pub1 and pub2 are duplicates
    Tdb.Differences diffs12 = new Tdb.Differences();
    pub1.addDifferences(diffs12, pub2);
    assertEquals(0, diffs12.getPluginIdsForDifferences().size());
    
    // differences are 'pluginA' and 'pluginB'
    Tdb.Differences diffs13 = new Tdb.Differences();
    pub1.addDifferences(diffs13, pub3);
    assertEquals(2, diffs13.getPluginIdsForDifferences().size());
  }
}
