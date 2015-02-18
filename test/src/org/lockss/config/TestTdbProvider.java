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
 * Test class for <code>org.lockss.config.TdbProvider</code>
 *
 * @author  Philip Gust
 * @version $Id$
 */

public class TestTdbProvider extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestTdbProvider");

  /**
   * Test creating valid TdbProvider.
   */
  public void testValidProvider() {
    TdbProvider provider = null;
    try {
      provider = new TdbProvider("Test Provider");
    } catch (IllegalArgumentException ex) {
    }
    assertEquals("Test Provider", provider.getName());
  }

  /**
   * Test creating TdbProvider with null name.
   */
  public void testNullProviderName() {
    TdbProvider provider = null;
    try {
      provider = new TdbProvider(null);
      fail("TdbProvider did not throw IllegalArgumentException for null constructor argument.");
    } catch (IllegalArgumentException ex) {
      
    }
    assertNull(provider);
  }
  
  /**
   * Test equals() method for provider.
   * @throws TdbException for invalid Tdb operations
   */
  public void testEquals() throws TdbException {
    TdbProvider provider1 = new TdbProvider("Test Provider");
    TdbAu au1 = new TdbAu("Test Au 1", "plugin1");
    provider1.addTdbAu(au1);
    assertEquals(provider1, provider1);
    
    // same as publisher1
    TdbProvider provider2 = new TdbProvider("Test Provider");
    TdbAu au2 = new TdbAu("Test Au 1", "plugin2");
    provider2.addTdbAu(au2);
    assertEquals(provider2, provider2);

    // differs from provider1 by au name
    TdbProvider provider3 = new TdbProvider("Test Provider");
    TdbAu au3 = new TdbAu("Test Au 3", "0000-0003");
    provider3.addTdbAu(au3);
    assertNotEquals(provider1, provider3);

    // differs from provider3 by provider name
    TdbProvider provider4 = new TdbProvider("Test Provider 4");
    TdbAu au4 = new TdbAu("Test Au 3", "0000-0004");
    provider4.addTdbAu(au4);
    assertNotEquals(provider3, provider4);
  }
  
  /**
   * Test addTdbAu() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testAddTdbAu() throws TdbException {
    TdbProvider provider = new TdbProvider("Test Provider");
    Collection<TdbAu> aus = provider.getTdbAus();
    assertEmpty(aus);
    
    // add title
    TdbAu au = new TdbAu("Test Au 1", "plugin1");
    provider.addTdbAu(au);
    aus = provider.getTdbAus();
    assertNotNull(aus);
    assertEquals(1, aus.size());

    // can't add null title
    try {
      provider.addTdbAu(null);
      fail("TdbProvider did not throw IllegalArgumentException adding null title.");
    } catch (IllegalArgumentException ex) {
    }
    aus = provider.getTdbAus();
    assertEquals(1, aus.size());

    TdbAu au2 = new TdbAu("Test Au 2", au.getPluginId());
    try {
      provider.addTdbAu(au2);
      fail("TdbProvider did not throw TdbException adding duplicate au");
    } catch (TdbException ex) {
    }
  }

  /**
   * Test various title and publisher methods.
   * @throws TdbException for invalid Tdb operations
   */
  public void testGetTitleAndPublisher() throws TdbException {
    TdbProvider provider = new TdbProvider("Test Provider");

    Collection<TdbTitle> titles = provider.getTdbTitles();
    assertEmpty(titles);
    
    Collection<TdbPublisher> publishers = provider.getTdbPublishers();
    assertEmpty(publishers);
    
    // add two titles
    TdbTitle title1 = new TdbTitle("Journal Title", "1234-5678");
    TdbPublisher publisher1 = new TdbPublisher("Publisher 1");
    publisher1.addTdbTitle(title1);
    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    title1.addTdbAu(au1);
    provider.addTdbAu(au1);

    TdbTitle title2 = new TdbTitle("Book Title", "978-0521807678");
    TdbPublisher publisher2 = new TdbPublisher("Publisher 2");
    publisher2.addTdbTitle(title2);
    TdbAu au2 = new TdbAu("Test AU2", "pluginB");
    title2.addTdbAu(au2);
    provider.addTdbAu(au2);

    TdbTitle title3 = new TdbTitle("Book Title", "978-0345303066");
    TdbPublisher publisher3 = new TdbPublisher("Publisher 3");
    publisher3.addTdbTitle(title3);
    TdbAu au3 = new TdbAu("Test AU3", "pluginC");
    title3.addTdbAu(au3);
    provider.addTdbAu(au3);

    titles = provider.getTdbTitles();
    assertEquals(3, titles.size());
    assertSameElements(ListUtil.list(title1, title2, title3), titles);

    publishers = provider.getTdbPublishers();
    assertEquals(3, publishers.size());
    assertSameElements(ListUtil.list(publisher1, publisher2, publisher3),
                       publishers);
    
    // get two titles by name
    Collection<TdbTitle> getTitle1 = provider.getTdbTitlesByName("Journal Title");
    assertEquals(1, getTitle1.size());
    assertEquals(title1, getTitle1.iterator().next());
  
    Collection<TdbTitle> getTitle2 = provider.getTdbTitlesByName("Book Title");
    assertEquals(2, getTitle2.size());
    assertTrue(getTitle2.contains(title2));
    assertTrue(getTitle2.contains(title3));

    assertEquals(3, provider.getTdbTitleCount());

    // get two publishers by name
    TdbPublisher getPublisher1 = provider.getTdbPublisherByName("Publisher 1");
    assertEquals(publisher1, getPublisher1);
  
    TdbPublisher noPublisher = provider.getTdbPublisherByName("unknown");
    assertNull(noPublisher);
    
    assertEquals(3, provider.getTdbPublisherCount());
    
    // get title by ID
    TdbTitle getTitleId2 = provider.getTdbTitleById("978-0521807678");
    assertEquals(title2, getTitleId2);
    
    // get unknown title by name
    Collection<TdbTitle> getTitleUnknown = provider.getTdbTitlesByName("unknown");
    assertEmpty(getTitleUnknown);

    // get unknown title by ID
    TdbTitle getTitleIdUnknown = provider.getTdbTitleById("unknown");
    assertNull(getTitleIdUnknown);
  }
  
  /**
   * Test TdbAu.addPluginIdsForDifferences() method.
   * @throws TdbException for invalid Tdb operations
   */
  public void testAddPluginIdsForDifferences() throws TdbException {
    TdbProvider provider1 = new TdbProvider("Test Provider");
    TdbTitle title1 = new TdbTitle("Test Title", "0000-0001");
    
    TdbAu au1 = new TdbAu("Test AU1", "pluginA");
    au1.setAttr("a", "A");
    au1.setAttr("b", "A");
    au1.setParam("x", "X");
    au1.setPluginVersion("3");
    title1.addTdbAu(au1);
    provider1.addTdbAu(au1);
    
    // duplicate of pub1
    TdbProvider provider2 = new TdbProvider("Test Provider");
    TdbTitle title2 = new TdbTitle("Test Title", "0000-0001");
    
    TdbAu au2 = new TdbAu("Test AU1", "pluginA");
    au2.setAttr("a", "A");
    au2.setAttr("b", "A");
    au2.setParam("x", "X");
    au2.setPluginVersion("3");
    title2.addTdbAu(au2);
    provider2.addTdbAu(au2);

    TdbProvider provider3 = new TdbProvider("Test Provider");
    TdbTitle title3 = new TdbTitle("Test Title", "0000-0003");
    
    TdbAu au3 = new TdbAu("Test AU1", "pluginB");
    au3.setAttr("a", "A");
    au3.setAttr("b", "A");
    au3.setParam("x", "X");
    au3.setPluginVersion("3");
    title3.addTdbAu(au3);
    provider3.addTdbAu(au3);

    // no differences becuase provider1 and provider2 are duplicates
    Tdb.Differences diffs12 = new Tdb.Differences();
    provider1.addDifferences(diffs12,provider2);
    assertEquals(0, diffs12.getPluginIdsForDifferences().size());
    
    // differences are 'pluginA' and 'pluginB'
    Tdb.Differences diffs13 = new Tdb.Differences();
    provider1.addDifferences(diffs13, provider3);
    assertEquals(2, diffs13.getPluginIdsForDifferences().size());
  }
}
