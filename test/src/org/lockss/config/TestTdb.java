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
import org.lockss.config.Tdb;
import org.lockss.config.Tdb.TdbException;
import org.lockss.test.*;

import java.util.*;

/**
 * Test class for <code>org.lockss.config.Tdb</code>
 *
 * @author  Philip Gust
 * @version $Id$
 */

public class TestTdb extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestTdb");

  TdbProvider pv1;
  TdbProvider pv2;
  TdbPublisher p1;
  TdbTitle t1p1;
  TdbTitle t2p1;
  TdbTitle t3p1;
  TdbAu a1t1p1;
  TdbAu a2t1p1;
  TdbAu a1t2p1;
  TdbAu a2t2p1;
  TdbAu a1t3p1;


  /**
   * Add test publisher1 to this Tdb. 
   * 
   * @param tdb the Tdb
   * @throws TdbException for invalid Tdb operations
   */
  private void addTestPublisher1(Tdb tdb) throws TdbException {
    pv1 = new TdbProvider("pv1");
    p1 = new TdbPublisher("p1");
  
    // create title with 2 aus with different plugins
    t1p1 = new TdbTitle("t1p1", "0001-0001");
    p1.addTdbTitle(t1p1);
    a1t1p1 = new TdbAu("a1t1p1", "plugin_t1p1");
    a1t1p1.setParam("param", "1");
    t1p1.addTdbAu(a1t1p1);
    a2t1p1 = new TdbAu("a2t1p1", "plugin_t1p1");
    a2t1p1.setParam("param", "2");
    t1p1.addTdbAu(a2t1p1);
    pv1.addTdbAu(a1t1p1);
    pv1.addTdbAu(a2t1p1);

    // create title with 2 aus with the same plugin
    t2p1 = new TdbTitle("t2p1", "0010-0010");
    p1.addTdbTitle(t2p1);
    a1t2p1 = new TdbAu("a1t2p1", "plugin_t2p1");
    a1t2p1.setParam("param", "1");
    t2p1.addTdbAu(a1t2p1);
    a2t2p1 = new TdbAu("a2t2p1", "plugin_t2p1");
    a2t2p1.setParam("param", "2");
    t2p1.addTdbAu(a2t2p1);
    // provided by pv2
    pv1.addTdbAu(a1t2p1);
    pv1.addTdbAu(a2t2p1);

    // create title with 1 au
    t3p1 = new TdbTitle("t3p1", "0010-1001");
    p1.addTdbTitle(t3p1);
    a1t3p1 = new TdbAu("a1t3p1", "plugin_t3p1");
    a1t3p1.setParam("param", "1");
    a1t3p1.setAttr("attr", "x");
    t3p1.addTdbAu(a1t3p1);
    // provided by pv1
    pv1.addTdbAu(a1t3p1);
    
    // add AUs for publisher p1
    tdb.addTdbAu(a1t1p1);
    tdb.addTdbAu(a2t1p1);
    tdb.addTdbAu(a1t2p1);
    tdb.addTdbAu(a2t2p1);
    tdb.addTdbAu(a1t3p1);
  }

  TdbProvider pv1_changed;
  TdbProvider pv2_changed;
  TdbPublisher p1_changed;
  TdbTitle t1p1_changed;
  TdbTitle t2p1_changed;
  TdbTitle t3p1_changed;
  TdbAu a1t1p1_changed;
  TdbAu a2t1p1_changed;
  TdbAu a1t2p1_changed;
  TdbAu a2t2p1_changed;
  TdbAu a1t3p1_changed;


  /**
   * Add changed test publisher1 to this Tdb. This pubisher is different
   * than test publisher 1 in that the TdbTitle name is "xyzzy" instead
   * of "t2p1", and TdbAu "a1t1p1 has a pluign version property set.
   * 
   * @param tdb the Tdb
   * @throws TdbException for invalid Tdb operations
   */
  private void addTestPublisher1_Changed(Tdb tdb) throws TdbException {
    pv1_changed = new TdbProvider("pv1");
    p1_changed = new TdbPublisher("p1");
  
    // create title with 2 aus with different plugins
    t1p1_changed = new TdbTitle("t1p1", "0001-0001");
    p1_changed.addTdbTitle(t1p1_changed);
    a1t1p1_changed = new TdbAu("a1t1p1c", "plugin_t1p1");
    a1t1p1_changed.setParam("param", "1");
    a1t1p1_changed.setPluginVersion("3");
    t1p1_changed.addTdbAu(a1t1p1_changed);
    a2t1p1_changed = new TdbAu("a2t1p1c", "plugin_t1p1");
    a2t1p1_changed.setParam("param", "2");
    t1p1_changed.addTdbAu(a2t1p1_changed);
    pv1_changed.addTdbAu(a1t1p1_changed);
    pv1_changed.addTdbAu(a2t1p1_changed);

    // create title with 2 aus with the same plugin
    t2p1_changed = new TdbTitle("xyzzy", "0010-0010");
    p1_changed.addTdbTitle(t2p1_changed);
    a1t2p1_changed = new TdbAu("a1t2p1c", "plugin_t2p1");
    a1t2p1_changed.setParam("param", "1");
    t2p1_changed.addTdbAu(a1t2p1_changed);
    a2t2p1_changed = new TdbAu("a2t2p1c", "plugin_t2p1");
    a2t2p1_changed.setParam("param", "2");
    t2p1_changed.addTdbAu(a2t2p1_changed);
    pv1_changed.addTdbAu(a1t2p1_changed);
    pv1_changed.addTdbAu(a2t2p1_changed);


    // create title with 1 au
    t3p1_changed = new TdbTitle("t3p1", "0010-1001");
    p1_changed.addTdbTitle(t3p1_changed);
    a1t3p1_changed = new TdbAu("a1t3p1c", "plugin_t3p1");
    a1t3p1_changed.setParam("param", "1");
    a1t3p1_changed.setAttr("attr", "y");
    t3p1_changed.addTdbAu(a1t3p1_changed);
    pv1_changed.addTdbAu(a1t3p1_changed);


    // add AUs for publisher p1
    tdb.addTdbAu(a1t1p1_changed);
    tdb.addTdbAu(a2t1p1_changed);
    tdb.addTdbAu(a1t2p1_changed);
    tdb.addTdbAu(a2t2p1_changed);
    tdb.addTdbAu(a1t3p1_changed);
  }

  TdbPublisher p2;
  TdbTitle t1p2;
  TdbTitle t2p2;
  TdbAu a1t1p2;
  TdbAu a2t1p2;
  TdbAu a1t2p2;
  TdbAu a2t2p2;


  /**
   * Add publisher2 to this Tdb.
   * 
   * @param tdb the Tdb
   * @throws TdbException for invalid Tdb operations
   */
  private void addTestPublisher2(Tdb tdb) throws TdbException {
    pv2 = new TdbProvider("pv2");
    p2 = new TdbPublisher("p2");
    
    // add two title to p2
    t1p2 = new TdbTitle("t1p2", "0000-0001");
    p2.addTdbTitle(t1p2);
    a1t1p2 = new TdbAu("a1t1p2", "plugin_p2");
    a1t1p2.setParam("param", "1");
    t1p2.addTdbAu(a1t1p2);
    a2t1p2 = new TdbAu("a2t1p2", "plugin_p2");
    a2t1p2.setParam("param", "2");
    t1p2.addTdbAu(a2t1p2);
    pv2.addTdbAu(a1t1p2);
    pv2.addTdbAu(a2t1p2);

    t2p2 = new TdbTitle("t2p2", "0000-0002");
    p2.addTdbTitle(t2p2);
    a1t2p2 = new TdbAu("a1t2p2", "plugin_p2");
    a1t2p2.setParam("param", "3");
    t2p2.addTdbAu(a1t2p2);
    a2t2p2 = new TdbAu("a2t2p2", "plugin_p2");
    a2t2p2.setParam("param", "4");
    t2p2.addTdbAu(a2t2p2);
    pv2.addTdbAu(a1t2p2);
    pv2.addTdbAu(a2t2p2);

    // add AUs for publisher p2
    tdb.addTdbAu(a1t1p2);
    tdb.addTdbAu(a2t1p2);
    tdb.addTdbAu(a1t2p2);
    tdb.addTdbAu(a2t2p2);
  }
  
  TdbPublisher p2_changed;
  TdbTitle t1p2_changed;
  TdbTitle t2p2_changed;
  TdbAu a1t1p2_changed;
  TdbAu a2t1p2_changed;
  TdbAu a1t2p2_changed;
  TdbAu a2t2p2_changed;

  /**
   * Add changed test publisher2 to this Tdb. This pubisher is different
   * than test publisher in that TdbTitle t1p2 has a "continuedBy" link
   * set, and TdbAu a2t2p2 has a param set.
   * 
   * @param tdb the Tdb
   * @throws TdbException for invalid Tdb operations
   */
  private void addTestPublisher2_Changed(Tdb tdb) throws TdbException {
    pv2_changed = new TdbProvider("pv2");
    p2_changed = new TdbPublisher("p2");
    
    // add two title to p2_changed
    t1p2_changed = new TdbTitle("t1p2", "0000-0001");
    t1p2_changed.addLinkToTdbTitleId(TdbTitle.LinkType.continuedBy, "0001-0001");
    p2_changed.addTdbTitle(t1p2_changed);
    a1t1p2_changed = new TdbAu("a1t1p2c", "plugin_p2");
    a1t1p2_changed.setParam("param", "1");
    t1p2_changed.addTdbAu(a1t1p2_changed);
    a2t1p2_changed = new TdbAu("a2t1p2c", "plugin_p2");
    a2t1p2_changed.setParam("param", "2");
    t1p2_changed.addTdbAu(a2t1p2_changed);
    pv2_changed.addTdbAu(a1t1p2_changed);
    pv2_changed.addTdbAu(a2t1p2_changed);
    
    t2p2_changed = new TdbTitle("t2p2", "0000-0002");
    p2_changed.addTdbTitle(t2p2_changed);
    a1t2p2_changed = new TdbAu("a1t2p2c", "plugin_p2");
    a1t2p2_changed.setParam("param", "3");
    t2p2_changed.addTdbAu(a1t2p2_changed);
    a2t2p2_changed = new TdbAu("a2t2p2c", "plugin_p2");
    a2t2p2_changed.setParam("param1", "value1");
    a2t2p2_changed.setParam("param", "3");
    t2p2_changed.addTdbAu(a2t2p2_changed);
    pv2_changed.addTdbAu(a1t2p2_changed);
    pv2_changed.addTdbAu(a2t2p2_changed);

    // add AUs for publisher p2_changed
    tdb.addTdbAu(a1t1p2_changed);
    tdb.addTdbAu(a2t1p2_changed);
    tdb.addTdbAu(a1t2p2_changed);
    tdb.addTdbAu(a2t2p2_changed);
  }
  
  /**
   * Test creating valid TdbTitle.
   * @throws TdbException for invalid Tdb operations
   */
  public void testAddAu() throws TdbException {
    Tdb tdb = new Tdb();
    // add aus to tdb
    assertTrue(tdb.isEmpty());
    assertEquals(0, tdb.getAllTdbPublishers().size());

    addTestPublisher1(tdb);
    assertEquals(1, tdb.getAllTdbPublishers().size());
    assertEquals(5, tdb.getTdbAuCount());
    assertEquals(1, tdb.getTdbProviderCount());

    addTestPublisher2(tdb);
    assertEquals(2, tdb.getTdbPublisherCount());
    assertEquals(5, tdb.getTdbTitleCount());
    assertEquals(9, tdb.getTdbAuCount());
    assertEquals(2, tdb.getTdbProviderCount());

    assertEquals(4, tdb.getTdbAuIds("plugin_p2").size());
    assertEquals(2, tdb.getTdbAuIds("plugin_t1p1").size());
    assertEquals(2, tdb.getTdbAuIds("plugin_t2p1").size());
    assertEquals(1, tdb.getTdbAuIds("plugin_t3p1").size());
    
    assertEquals("t1p1", tdb.getTdbTitleById("0001-0001").getName());
  }
  
  /**
   * Test adding an AU from a properties that define the AU.
   * The property names are of the form "name", "param.1.name",
   * or "attributes.attr0.name".
   * @throws TdbException for invalid Tdb operations
   */
  public void testAddAuFromProperties() throws TdbException {
    Tdb tdb = new Tdb();

    // set title database properties
    Properties p0 = new Properties();
    p0.put("title", "Not me");
    p0.put("plugin", "org.lockss.NotThisClass");
    TdbAu tdbAu0 = tdb.addTdbAuFromProperties(p0);
    assertEquals("Publisher of [Title of [Not me]]", tdbAu0.getPublisherName());
    assertTrue(tdbAu0.getTdbPublisher().isUnknownPublisher());
    assertEquals(tdbAu0.getPublisherName(), tdbAu0.getProviderName());

    Map<String, TdbPublisher> pubsMap = tdb.getAllTdbPublishers();
    assertEquals(1, pubsMap.size());
    Map<String, TdbProvider> providerMap = tdb.getAllTdbProviders();
    assertEquals(1, providerMap.size());
    
    TdbPublisher pub0 = pubsMap.values().iterator().next();
    Collection<TdbTitle> titles0 = pub0.getTdbTitles();
    assertEquals(1, titles0.size());
    TdbTitle title0 = titles0.iterator().next();
    assertNotNull(title0);
    assertFalse(title0.getTdbAusByName("Not me").isEmpty());
    assertEquals(tdbAu0, title0.getTdbAuById(tdbAu0.getId()));
    Properties p1 = new Properties();
    p1.put("title", "Air & Space Volume 3");
    p1.put("plugin", "org.lockss.testplugin1");

    p1.put("pluginVersion", "4");
    p1.put("issn", "0003-0031");
    p1.put("eissn", "0033-0331");
    p1.put("issnl", "0333-3331");
    p1.put("journalId", "0333-3331");  // use issnl as journal title
   
    p1.put("journal.link.1.type", TdbTitle.LinkType.continuedBy.toString());
    p1.put("journal.link.1.journalId", "0333-3331");  // link to self
    p1.put("param.1.key", "volume");
    p1.put("param.1.value", "3");
    p1.put("param.2.key", "year");
    p1.put("param.2.value", "1999");
    p1.put("attributes.publisher", "The Smithsonian Institution");
    p1.put("attributes.provider", "Smithsonian Publications");

    TdbAu tdbAu1 = tdb.addTdbAuFromProperties(p1);
    assertFalse(tdbAu1.getTdbPublisher().isUnknownPublisher());
    assertFalse(tdbAu1.getTdbProvider().isUnknownProvider());
    assertNotEquals(tdbAu1.getTdbProvider().getName(), 
                    tdbAu1.getTdbPublisher().getName());

    providerMap = tdb.getAllTdbProviders();
    assertEquals(2, providerMap.size());
    TdbProvider provider1 = providerMap.get("Smithsonian Publications");
    assertNotNull(provider1);

    pubsMap = tdb.getAllTdbPublishers();
    assertEquals(2, pubsMap.size());
    TdbPublisher pub1 = pubsMap.get("The Smithsonian Institution");
    assertNotNull(pub1);

    Collection<TdbTitle> titles1 = pub1.getTdbTitles();
    assertNotNull(titles1);
    assertEquals(1, titles1.size());
    TdbTitle title1 = titles1.iterator().next();
    assertEquals("Air & Space", title1.getName());

    // find title by ID
    assertEquals(title1, tdb.getTdbTitleById("0333-3331"));

    // validate linked titles 
    Collection<TdbTitle> titles = tdb.getLinkedTdbTitlesForType(TdbTitle.LinkType.continuedBy, title1);
    assertNotNull(titles);
    assertEquals(1, titles.size());
    assertEquals(title1, titles.iterator().next());

    
    Collection<TdbAu> aus1 = title1.getTdbAus();
    assertEquals(1, aus1.size());
    
    TdbAu au1  = aus1.iterator().next();
    assertNotNull(au1);
    assertEquals("Air & Space Volume 3", au1.getName());
    assertEquals("1999", au1.getParam("year"));
    assertEquals("1999", au1.getYear());
    assertEquals("Air & Space", au1.getPublicationTitle());
    assertEquals("Smithsonian Publications", au1.getProviderName());
    assertEquals("0003-0031", au1.getPropertyByName("issn"));
    assertEquals("0003-0031", au1.getPrintIssn());
    assertEquals("0033-0331", au1.getEissn());
    assertEquals("0333-3331", au1.getIssnL());
    assertNotNull(au1.getIssn());
    assertEquals("4", au1.getPluginVersion());
    

    try {
      // try to add from the properties again
      tdb.addTdbAuFromProperties(p1);
      fail("TdbException not thrown when adding duplicate TdbAu 1 from properties");
    } catch (TdbException ex) {
    }

    Properties p2 = new Properties();
    p2.put("title", "Air & Space Volume 4");
    p2.put("plugin", "org.lockss.testplugin1");

    p2.put("pluginVersion", "4");
    p2.put("issn", "0032-0032");
   
    p2.put("journal.link.1.type", TdbTitle.LinkType.continuedBy.toString());
    p2.put("journal.link.1.journalId", "0032-0032");  // link to self
    p2.put("param.1.key", "volume");
    p2.put("param.1.value", "3");
    p2.put("param.2.key", "year");
    p2.put("param.2.value", "1999");
    p2.put("attributes.publisher", "The Smithsonian Institution");

    try {
      tdb.addTdbAuFromProperties(p2);
      fail("TdbException not thrown when adding duplicate TdbAu 2 from properties");
    } catch (TdbException ex) {
    }
  }

  /**
   * Test adding duplicate AU to TDB.
   * @throws TdbException for invalid Tdb operations
   */
  public void testDuplicateAu() throws TdbException {
    Tdb tdb = new Tdb();

    // create an AU
    TdbProvider pv1 = new TdbProvider("pv1");
    TdbPublisher p1 = new TdbPublisher("p1");
    TdbTitle t1p1 = new TdbTitle("t1p1", "0000-0001");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1", "plugin1");
    t1p1.addTdbAu(a1t1p1);
    pv1.addTdbAu(a1t1p1);
    assertTrue(tdb.addTdbAu(a1t1p1)); // au new so returns true
    assertFalse(tdb.isEmpty());
    assertEquals(1, tdb.getTdbAuIds("plugin1").size());
    assertFalse(tdb.addTdbAu(a1t1p1)); // au already added so returns false
    assertFalse(tdb.isEmpty());
    assertEquals(1, tdb.getTdbAuIds("plugin1").size());
  }

  /**
   * Test equals function for Tdbs
   * @throws TdbException for invalid Tdb operations
   */
  public void testEquals() throws TdbException {
    Tdb tdb1 = new Tdb();
    addTestPublisher1(tdb1);
    addTestPublisher2(tdb1);
    assertEquals(tdb1, tdb1);
    
    // same as tdb1
    Tdb tdb2 = new Tdb();
    addTestPublisher1(tdb2);
    addTestPublisher2(tdb2);
    assertEquals(tdb1, tdb2);
    
    // different than tdb1 by publisher1
    Tdb tdb3 = new Tdb();
    addTestPublisher1_Changed(tdb3);
    addTestPublisher2(tdb3);
    assertNotEquals(tdb1, tdb3);

    // different than tdb1 by publisher2
    Tdb tdb4 = new Tdb();
    addTestPublisher1(tdb4);
    addTestPublisher2_Changed(tdb4);
    assertNotEquals(tdb1, tdb4);

    // different than tdb1 by publisher1 and publisher2
    Tdb tdb5 = new Tdb();
    addTestPublisher1_Changed(tdb5);
    addTestPublisher2_Changed(tdb5);
    assertNotEquals(tdb1, tdb5);
}
  
  List fromIter(Iterator iter) {
    return ListUtil.fromIterator(iter);
  }

  /**
   * Test iterators
   */
  public void testIterators() throws TdbException {
    Tdb tdb1 = new Tdb();
    addTestPublisher1(tdb1);
    assertSameElements(ListUtil.list(p1),
		       fromIter(tdb1.tdbPublisherIterator()));
    assertSameElements(ListUtil.list(t1p1, t2p1, t3p1),
		       fromIter(tdb1.tdbTitleIterator()));
    assertSameElements(ListUtil.list(a1t1p1, a2t1p1, a1t2p1, a2t2p1, a1t3p1),
		       fromIter(tdb1.tdbAuIterator()));
    
    assertSameElements(fromIter(tdb1.tdbTitleIterator()),
		       fromIter(p1.tdbTitleIterator()));
    assertSameElements(fromIter(tdb1.tdbAuIterator()),
		       fromIter(p1.tdbAuIterator()));
    assertSameElements(ListUtil.list(a1t1p1, a2t1p1),
		       fromIter(t1p1.tdbAuIterator()));
    


    addTestPublisher2(tdb1);
    assertSameElements(ListUtil.list(p1, p2),
		       fromIter(tdb1.tdbPublisherIterator()));
    assertSameElements(ListUtil.list(t1p1, t2p1, t3p1, t1p2, t2p2),
		       fromIter(tdb1.tdbTitleIterator()));
    assertSameElements(ListUtil.list(a1t1p1, a2t1p1, a1t2p1, a2t2p1, a1t3p1,
				     a1t1p2, a2t1p2, a1t2p2, a2t2p2),
		       fromIter(tdb1.tdbAuIterator()));
  }    

  /**
   * Test Tdb.Difference mechanism
   * @throws TdbException for invalid Tdb operations
   */
  public void testDifferences() throws TdbException {
    Tdb tdb1 = new Tdb();
    addTestPublisher1(tdb1);
    addTestPublisher2(tdb1);
    
    Tdb tdb2 = new Tdb();
    addTestPublisher1(tdb2);
    addTestPublisher2(tdb2);

    // Differences between a Tdb and null should be equal to Tdb
    Tdb.Differences diffs0 = Tdb.computeDifferences(tdb1, null);
    assertEquals(4, diffs0.getPluginIdsForDifferences().size());
    assertSameElements(ListUtil.list(p1, p2),
		       diffs0.rawNewTdbPublishers());
    assertSameElements(fromIter(tdb1.tdbPublisherIterator()),
		       fromIter(diffs0.newTdbPublisherIterator()));
    assertEmpty(diffs0.rawNewTdbTitles());
    assertSameElements(fromIter(tdb1.tdbTitleIterator()),
		       fromIter(diffs0.newTdbTitleIterator()));
    assertEmpty(diffs0.rawNewTdbAus());
    assertSameElements(fromIter(tdb1.tdbAuIterator()),
		       fromIter(diffs0.newTdbAuIterator()));

    // verify no differences between a Tdb and itself
    Tdb.Differences diffs = tdb1.computeDifferences(tdb1);
    assertEquals(0, diffs.getPluginIdsForDifferences().size());
    assertEmpty(diffs.rawNewTdbPublishers());
    assertEmpty(diffs.rawNewTdbTitles());
    assertEmpty(diffs.rawNewTdbAus());
    assertEmpty(fromIter(diffs.newTdbTitleIterator()));
    assertEmpty(fromIter(diffs.newTdbAuIterator()));
    assertEmpty(fromIter(diffs.newTdbPublisherIterator()));
    
    Tdb.Differences diffs12 = tdb1.computeDifferences(tdb2);
    assertEquals(0, diffs12.getPluginIdsForDifferences().size());
    assertEmpty(diffs12.rawNewTdbPublishers());
    assertEmpty(diffs12.rawNewTdbTitles());
    assertEmpty(diffs12.rawNewTdbAus());
    assertEmpty(fromIter(diffs12.newTdbTitleIterator()));
    assertEmpty(fromIter(diffs12.newTdbAuIterator()));
    assertEmpty(fromIter(diffs12.newTdbPublisherIterator()));

    Tdb tdb3 = new Tdb();
    addTestPublisher1_Changed(tdb3);
    addTestPublisher2(tdb3);
    
    Tdb.Differences diffs13 = tdb1.computeDifferences(tdb3);
    Collection<String> changedPluginIds13 =
      diffs13.getPluginIdsForDifferences();
    assertEquals(3, changedPluginIds13.size());
    assertSameElements(ListUtil.list("plugin_t3p1", "plugin_t2p1", "plugin_t1p1"),
		       changedPluginIds13);
    assertEmpty(diffs13.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs13.newTdbPublisherIterator()));
    assertSameElements(ListUtil.list(t2p1), diffs13.rawNewTdbTitles());
    assertSameElements(ListUtil.list(t2p1),
		       fromIter(diffs13.newTdbTitleIterator()));
    assertSameElements(ListUtil.list(a1t1p1, a1t3p1), diffs13.rawNewTdbAus());
    assertSameElements(ListUtil.list(a1t1p1, a1t2p1, a2t2p1, a1t3p1),
		       fromIter(diffs13.newTdbAuIterator()));

    Tdb.Differences diffs31 = tdb3.computeDifferences(tdb1);
    Collection<String> changedPluginIds31 =
      diffs31.getPluginIdsForDifferences();
    assertEquals(3, changedPluginIds31.size());
    assertSameElements(ListUtil.list("plugin_t3p1", "plugin_t2p1", "plugin_t1p1"),
		       changedPluginIds31);
    assertEmpty(diffs31.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs31.newTdbPublisherIterator()));
    assertSameElements(ListUtil.list(t2p1_changed), diffs31.rawNewTdbTitles());
    assertSameElements(ListUtil.list(t2p1_changed),
		       fromIter(diffs31.newTdbTitleIterator()));
    assertSameElements(ListUtil.list(a1t3p1_changed, a1t1p1_changed),
		       diffs31.rawNewTdbAus());
    assertSameElements(ListUtil.list(a1t3p1_changed, a1t1p1_changed,
				     a1t2p1_changed, a2t2p1_changed),
		       fromIter(diffs31.newTdbAuIterator()));
    
    Tdb tdb4 = new Tdb();
    addTestPublisher1(tdb4);
    addTestPublisher2_Changed(tdb4);
    
    Tdb.Differences diffs14 = tdb1.computeDifferences(tdb4);
    Collection<String> changedPluginIds14 =
      diffs14.getPluginIdsForDifferences();
    assertEquals(1, changedPluginIds14.size());
    assertTrue(changedPluginIds14.contains("plugin_p2"));
    assertSameElements(ListUtil.list("plugin_p2"), changedPluginIds14);
    assertEmpty(diffs14.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs14.newTdbPublisherIterator()));
    assertSameElements(ListUtil.list(t1p2), diffs14.rawNewTdbTitles());
    assertSameElements(ListUtil.list(t1p2),
		       fromIter(diffs14.newTdbTitleIterator()));
    assertSameElements(ListUtil.list(a2t2p2), diffs14.rawNewTdbAus());
    assertSameElements(ListUtil.list(a1t1p2, a2t1p2, a2t2p2),
		       fromIter(diffs14.newTdbAuIterator()));


    Tdb.Differences diffs41 = tdb4.computeDifferences(tdb1);
    Collection<String> changedPluginIds41 =
      diffs41.getPluginIdsForDifferences();
    assertEquals(1, changedPluginIds41.size());
    assertSameElements(ListUtil.list("plugin_p2"), changedPluginIds41);
    assertEmpty(diffs41.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs41.newTdbPublisherIterator()));
    assertSameElements(ListUtil.list(t1p2_changed), diffs41.rawNewTdbTitles());
    assertSameElements(ListUtil.list(t1p2_changed),
		       fromIter(diffs41.newTdbTitleIterator()));
    assertSameElements(ListUtil.list(a2t2p2_changed), diffs41.rawNewTdbAus());
    assertSameElements(ListUtil.list(a1t1p2_changed, a2t1p2_changed,
				     a2t2p2_changed),
		       fromIter(diffs41.newTdbAuIterator()));


    Tdb tdb5 = new Tdb();
    addTestPublisher1_Changed(tdb5);
    addTestPublisher2_Changed(tdb5);
    
    Tdb.Differences diffs15 = tdb1.computeDifferences(tdb5);
    Collection<String> changedPluginIds15 =
      diffs15.getPluginIdsForDifferences();
    assertEquals(4, changedPluginIds15.size());
    assertSameElements(ListUtil.list("plugin_p2", "plugin_t1p1",
				     "plugin_t2p1", "plugin_t3p1"),
		       changedPluginIds15);
    assertEmpty(diffs15.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs15.newTdbPublisherIterator()));
    assertSameElements(ListUtil.list(t2p1, t1p2), diffs15.rawNewTdbTitles());
    assertSameElements(ListUtil.list(t1p2, t2p1),
		       fromIter(diffs15.newTdbTitleIterator()));
    assertSameElements(ListUtil.list(a1t1p1, a2t2p2, a1t3p1),
		       diffs15.rawNewTdbAus());
    assertSameElements(ListUtil.list(a1t1p1, a1t2p1, a2t2p1, a1t3p1,
				     a1t1p2, a2t1p2, a2t2p2),
		       fromIter(diffs15.newTdbAuIterator()));
    
    Tdb.Differences diffs51 = tdb5.computeDifferences(tdb1);
    Collection<String> changedPluginIds51 =
      diffs51.getPluginIdsForDifferences();
    assertEquals(4, changedPluginIds51.size());
    assertSameElements(ListUtil.list("plugin_p2", "plugin_t1p1",
				     "plugin_t2p1", "plugin_t3p1"),
		       changedPluginIds51);
    assertEmpty(diffs51.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs51.newTdbPublisherIterator()));
    assertSameElements(ListUtil.list(t1p2_changed, t2p1_changed),
		 diffs51.rawNewTdbTitles());
    assertSameElements(ListUtil.list(t1p2_changed, t2p1_changed),
		       fromIter(diffs51.newTdbTitleIterator()));
    assertSameElements(ListUtil.list(a1t1p1_changed, a2t2p2_changed,
				     a1t3p1_changed),
		       diffs51.rawNewTdbAus());
    assertSameElements(ListUtil.list(a1t1p1_changed, a1t2p1_changed,
				     a2t2p1_changed, a1t3p1_changed,
				     a1t1p2_changed, a2t1p2_changed,
				     a2t2p2_changed),
		       fromIter(diffs51.newTdbAuIterator()));

    Tdb tdb6 = new Tdb();
    addTestPublisher2(tdb6);
    
    Tdb.Differences diffs16 = tdb1.computeDifferences(tdb6);
    Collection<String> changedPluginIds16 =
      diffs16.getPluginIdsForDifferences();
    assertEquals(3, changedPluginIds16.size());
    assertSameElements(ListUtil.list("plugin_t1p1", "plugin_t2p1",
				     "plugin_t3p1"),
		       changedPluginIds16);
    assertSameElements(ListUtil.list(p1), diffs16.rawNewTdbPublishers());
    assertSameElements(ListUtil.list(p1),
		       fromIter(diffs16.newTdbPublisherIterator()));
    assertEmpty(diffs16.rawNewTdbTitles());
    assertEmpty(diffs16.rawNewTdbAus());
    
    Tdb.Differences diffs61 = tdb6.computeDifferences(tdb1);
    Collection<String> changedPluginIds61 =
      diffs61.getPluginIdsForDifferences();
    assertEquals(3, changedPluginIds61.size());
    assertSameElements(ListUtil.list("plugin_t1p1", "plugin_t2p1",
				     "plugin_t3p1"),
		       changedPluginIds61);
    assertEmpty(diffs61.rawNewTdbPublishers());
    assertEmpty(fromIter(diffs51.newTdbPublisherIterator()));
    assertEmpty(diffs61.rawNewTdbTitles());
    assertEmpty(fromIter(diffs61.newTdbTitleIterator()));
    assertEmpty(diffs61.rawNewTdbAus());
    assertEmpty(fromIter(diffs61.newTdbAuIterator()));
  }
  
  /**
   * Test sealing a TdbTitle.
   * @throws TdbException for invalid Tdb operations
   */
  public void testSealTDB() throws TdbException {
    Tdb tdb = new Tdb();
    assertFalse(tdb.isSealed());

    tdb.seal();
    assertTrue(tdb.isSealed());

    // create an AU
    TdbPublisher p1 = new TdbPublisher("p1");
    TdbTitle t1p1 = new TdbTitle("t1p1", "0000-0001");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1", "a1t1p1");
    t1p1.addTdbAu(a1t1p1);

    // try adding AU to sealed TDB
    try {
      tdb.addTdbAu(a1t1p1);
      fail("Tdb did not throw TdbException adding AU to sealed TDB.");
    } catch (TdbException ex) {
    }
    
    assertTrue(tdb.isEmpty());
  }

  /**
   * Test copyFrom() method
   * @throws TdbException for invalid Tdb operations
   */
  public void testCopyTDB() throws TdbException {
    Tdb tdb = new Tdb();

    // create an AU
    TdbProvider pv1 = new TdbProvider("pv1");
    TdbPublisher p1 = new TdbPublisher("p1");
    TdbTitle t1p1 = new TdbTitle("t1p1", "0000-0001");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1", "plugin1");
    t1p1.addTdbAu(a1t1p1);
    pv1.addTdbAu(a1t1p1);
    tdb.addTdbAu(a1t1p1);
    
    // copy into new Tdb
    Tdb copyTdb = new Tdb();
    copyTdb.copyFrom(tdb);
    assertFalse(copyTdb.isEmpty());
    assertEquals(tdb.getTdbAuCount(),copyTdb.getTdbAuCount());
    assertEquals(1, copyTdb.getTdbAuCount());
    assertEquals(1, copyTdb.getTdbPublisherCount());
    assertEquals(1, copyTdb.getTdbProviderCount());

    // copy all duplicates -- should be unchanged
    copyTdb.copyFrom(tdb);
    assertEquals(tdb.getTdbAuCount(),copyTdb.getTdbAuCount());
    assertEquals(1, copyTdb.getAllTdbAuIds().size());
    assertEquals(1, copyTdb.getTdbPublisherCount());
    assertEquals(1, copyTdb.getTdbProviderCount());

    Tdb tdb3 = new Tdb();

    // create a duplicate AU for a different publisher
    TdbProvider pv3 = new TdbProvider("pv3");
    TdbPublisher p3 = new TdbPublisher("p3");
    TdbTitle t3p3 = new TdbTitle("t3p3", "0000-0003");
    p3.addTdbTitle(t3p3);
    TdbAu a3t3p3 = new TdbAu("a1t3p3", "plugin1");
    t3p3.addTdbAu(a3t3p3);
    pv3.addTdbAu(a3t3p3);
    tdb3.addTdbAu(a3t3p3);
    
    // copy all duplicates -- should be unchanged
    tdb.copyFrom(tdb3);
    assertEquals(tdb.getTdbAuCount(),copyTdb.getTdbAuCount());
    assertEquals(1, copyTdb.getAllTdbAuIds().size());
    assertEquals(1, copyTdb.getTdbPublisherCount());
    assertEquals(1, copyTdb.getTdbProviderCount());
  }
}
