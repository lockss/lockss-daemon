/*
 * $Id: TestTdb.java,v 1.1 2010-04-02 23:38:11 pgust Exp $
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
 * Test class for <code>org.lockss.config.Tdb</code>
 *
 * @author  Philip Gust
 * @version $Id: TestTdb.java,v 1.1 2010-04-02 23:38:11 pgust Exp $
 */

public class TestTdb extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.config.Tdb.class
  };

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestTdb");

  /**
   * Add test publisher1 to this Tdb. 
   * 
   * @param tdb the Tdb
   */
  private void addTestPublisher1(Tdb tdb) {
    TdbPublisher p1 = new TdbPublisher("p1");
  
    // create title with 2 aus with different plugins
    TdbTitle t1p1 = new TdbTitle("t1p1");
    t1p1.setId("0001-0001");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1");
    a1t1p1.setPluginId("plugin_t1p1");
    t1p1.addTdbAu(a1t1p1);
    TdbAu a2t1p1 = new TdbAu("a2t1p1");
    a2t1p1.setPluginId("plugin_t1p1");
    t1p1.addTdbAu(a2t1p1);

    // create title with 2 aus with the same plugin
    TdbTitle t2p1 = new TdbTitle("t2p1");
    t2p1.setId("0010-0010");
    p1.addTdbTitle(t2p1);
    TdbAu a1t2p1 = new TdbAu("a1t2p1");
    a1t2p1.setPluginId("plugin_t2p1");
    t2p1.addTdbAu(a1t2p1);
    TdbAu a2t2p1 = new TdbAu("a2t2p1");
    a2t2p1.setPluginId("plugin_t2p1");
    t2p1.addTdbAu(a2t2p1);

    // add AUs for publisher p1
    tdb.addTdbAu(a1t1p1);
    tdb.addTdbAu(a2t1p1);
    tdb.addTdbAu(a1t2p1);
    tdb.addTdbAu(a2t2p1);
  }

  /**
   * Add changed test publisher1 to this Tdb. This pubisher is different
   * than test publisher 1 in that the TdbTitle name is "xyzzy" instead
   * of "t2p1", and TdbAu "a1t1p1 has a pluign version property set.
   * 
   * @param tdb the Tdb
   */
  private void addTestPublisher1_Changed(Tdb tdb) {
    TdbPublisher p1 = new TdbPublisher("p1");
  
    // create title with 2 aus with different plugins
    TdbTitle t1p1 = new TdbTitle("t1p1");
    t1p1.setId("0001-0001");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1");
    a1t1p1.setPluginId("plugin_t1p1");
    a1t1p1.setPluginVersion("3");
    t1p1.addTdbAu(a1t1p1);
    TdbAu a2t1p1 = new TdbAu("a2t1p1");
    a2t1p1.setPluginId("plugin_t1p1");
    t1p1.addTdbAu(a2t1p1);

    // create title with 2 aus with the same plugin
    TdbTitle t2p1 = new TdbTitle("xyzzy");
    t2p1.setId("0010-0010");
    p1.addTdbTitle(t2p1);
    TdbAu a1t2p1 = new TdbAu("a1t2p1");
    a1t2p1.setPluginId("plugin_t2p1");
    t2p1.addTdbAu(a1t2p1);
    TdbAu a2t2p1 = new TdbAu("a2t2p1");
    a2t2p1.setPluginId("plugin_t2p1");
    t2p1.addTdbAu(a2t2p1);

    // add AUs for publisher p1
    tdb.addTdbAu(a1t1p1);
    tdb.addTdbAu(a2t1p1);
    tdb.addTdbAu(a1t2p1);
    tdb.addTdbAu(a2t2p1);
  }

  /**
   * Add publisher2 to this Tdb.
   * 
   * @param tdb the Tdb
   */
  private void addTestPublisher2(Tdb tdb) {
    TdbPublisher p2 = new TdbPublisher("p2");
    // add two title to p2
    TdbTitle t1p2 = new TdbTitle("t1p2");
    p2.addTdbTitle(t1p2);
    TdbAu a1t1p2 = new TdbAu("a1t1p2");
    a1t1p2.setPluginId("plugin_p2");
    t1p2.addTdbAu(a1t1p2);
    TdbAu a2t1p2 = new TdbAu("a2t1p2");
    a2t1p2.setPluginId("plugin_p2");
    t1p2.addTdbAu(a2t1p2);

    TdbTitle t2p2 = new TdbTitle("t2p2");
    p2.addTdbTitle(t2p2);
    TdbAu a1t2p2 = new TdbAu("a1t2p2");
    a1t2p2.setPluginId("plugin_p2");
    t2p2.addTdbAu(a1t2p2);
    TdbAu a2t2p2 = new TdbAu("a2t2p2");
    a2t2p2.setPluginId("plugin_p2");
    t2p2.addTdbAu(a2t2p2);

    // add AUs for publisher p2
    tdb.addTdbAu(a1t1p2);
    tdb.addTdbAu(a2t1p2);
    tdb.addTdbAu(a1t2p2);
    tdb.addTdbAu(a2t2p2);
  }
  
  /**
   * Add changed test publisher2 to this Tdb. This pubisher is different
   * than test publisher in that TdbTitle t1p2 has a "continuedBy" link
   * set, and TdbAu a2t2p2 has a param set.
   * 
   * @param tdb the Tdb
   */
  private void addTestPublisher2_Changed(Tdb tdb) {
    TdbPublisher p2 = new TdbPublisher("p2");
    // add two title to p2
    TdbTitle t1p2 = new TdbTitle("t1p2");
    t1p2.addLinkToTdbTitleId(TdbTitle.LinkType.continuedBy, "0001-0001");
    p2.addTdbTitle(t1p2);
    TdbAu a1t1p2 = new TdbAu("a1t1p2");
    a1t1p2.setPluginId("plugin_p2");
    t1p2.addTdbAu(a1t1p2);
    TdbAu a2t1p2 = new TdbAu("a2t1p2");
    a2t1p2.setPluginId("plugin_p2");
    t1p2.addTdbAu(a2t1p2);

    TdbTitle t2p2 = new TdbTitle("t2p2");
    p2.addTdbTitle(t2p2);
    TdbAu a1t2p2 = new TdbAu("a1t2p2");
    a1t2p2.setPluginId("plugin_p2");
    t2p2.addTdbAu(a1t2p2);
    TdbAu a2t2p2 = new TdbAu("a2t2p2");
    a2t2p2.setPluginId("plugin_p2");
    a2t2p2.setParam("param1", "value1");
    t2p2.addTdbAu(a2t2p2);

    // add AUs for publisher p2
    tdb.addTdbAu(a1t1p2);
    tdb.addTdbAu(a2t1p2);
    tdb.addTdbAu(a1t2p2);
    tdb.addTdbAu(a2t2p2);
  }
  
  /**
   * Test creating valid TdbTitle.
   */
  public void testAddAu() {
    Tdb tdb = new Tdb();
    // add aus to tdb
    assertTrue(tdb.isEmpty());
    assertEquals(0, tdb.getAllTdbPublishers().size());

    addTestPublisher1(tdb);
    assertEquals(1, tdb.getAllTdbPublishers().size());
    assertEquals(4, tdb.getTdbAuCount());

    addTestPublisher2(tdb);
    assertEquals(2, tdb.getAllTdbPublishers().size());
    assertEquals(8, tdb.getTdbAuCount());

    assertEquals(4, tdb.getTdbAus("plugin_p2").size());
    assertEquals(2, tdb.getTdbAus("plugin_t1p1").size());
    assertEquals(2, tdb.getTdbAus("plugin_t2p1").size());
    
    assertEquals("t1p1", tdb.getTdbTitleForId("0001-0001").getName());
  }
  
  /**
   * Test adding an AU from a properties that define the AU.
   * The property names are of the form "name", "param.1.name",
   * or "attributes.attr0.name".
   */
  public void testAddAuFromProperties() {
    Tdb tdb = new Tdb();

    // set title database properties
    Properties p0 = new Properties();
    p0.put("title", "Not me");
    p0.put("plugin", "org.lockss.NotThisClass");
    TdbAu tdbAu0 = tdb.addTdbAuFromProperties(p0);

    Map<String, TdbPublisher> pubsMap = tdb.getAllTdbPublishers();
    assertEquals(1, pubsMap.size());
    
    TdbPublisher pub0 = pubsMap.values().iterator().next();
    Collection<TdbTitle> titles0 = pub0.getTdbTitles();
    assertEquals(1, titles0.size());
    TdbTitle title0 = titles0.iterator().next();
    assertNotNull(title0);
    assertFalse(title0.getTdbAusByName("Not me").isEmpty());
    assertEquals(tdbAu0, title0.getTdbAuById(tdbAu0.getId()));
    Properties p1 = new Properties();
    p1.put("title", "Air & Space volume 3");
    p1.put("plugin", "org.lockss.testplugin1");

    p1.put("pluginVersion", "4");
    p1.put("issn", "0003-0031");
   
    p1.put("journal.link.1.type", TdbTitle.LinkType.continuedBy.toString());
    p1.put("journal.link.1.journalId", "0003-0031");  // link to self
    p1.put("param.1.key", "volume");
    p1.put("param.1.value", "3");
    p1.put("param.2.key", "year");
    p1.put("param.2.value", "1999");
    p1.put("attributes.publisher", "The Smithsonian Institution");
    TdbAu tdbAu1 = tdb.addTdbAuFromProperties(p1);

    pubsMap = tdb.getAllTdbPublishers();
    assertEquals(2, pubsMap.size());
    TdbPublisher pub1 = pubsMap.get("The Smithsonian Institution");
    assertNotNull(pub1);

    Collection<TdbTitle> titles1 = pub1.getTdbTitles();
    assertNotNull(titles1);
    assertEquals(1, titles1.size());
    TdbTitle title1 = titles1.iterator().next();
    assertEquals("Air & Space", title1.getName());

    // validate linked titles 
    Collection<TdbTitle> titles = tdb.getLinkedTdbTitlesForType(TdbTitle.LinkType.continuedBy, title1);
    assertNotNull(titles);
    assertEquals(1, titles.size());
    assertEquals(title1, titles.iterator().next());

    
    Collection<TdbAu> aus1 = title1.getTdbAus();
    assertEquals(1, aus1.size());
    
    TdbAu au1  = aus1.iterator().next();
    assertNotNull(au1);
    assertEquals("Air & Space volume 3", au1.getName());
    assertEquals("1999", au1.getParam("year"));
    assertEquals("Air & Space", au1.getJournalTitle());
    assertEquals("0003-0031", au1.getPropertyByName("issn"));
    assertEquals("4", au1.getPluginVersion());
    
    assertEquals(title1, tdb.getTdbTitleForId("0003-0031"));
  }

  /**
   * Test adding duplicate AU to TDB.
   */
  public void testDuplicateAu() {
    Tdb tdb = new Tdb();

    // create an AU
    TdbPublisher p1 = new TdbPublisher("p1");
    TdbTitle t1p1 = new TdbTitle("t1p1");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1");
    a1t1p1.setPluginId("plugin1");
    t1p1.addTdbAu(a1t1p1);
    tdb.addTdbAu(a1t1p1);
    assertFalse(tdb.isEmpty());
    assertEquals(1, tdb.getTdbAus("plugin1").size());

    tdb.addTdbAu(a1t1p1);
    assertFalse(tdb.isEmpty());
    assertEquals(1, tdb.getTdbAus("plugin1").size());
  }

  /**
   * Test creating valid TdbTitle.
   */
  public void testGetPluginIdsForDifferences() {
    Tdb tdb1 = new Tdb();
    addTestPublisher1(tdb1);
    addTestPublisher2(tdb1);
    
    Tdb tdb2 = new Tdb();
    addTestPublisher1(tdb2);
    addTestPublisher2(tdb2);

    // verify no differences between a Tdb and itself
    assertEquals(0, tdb1.getPluginIdsForDifferences(tdb1).size());
    
    Collection<String> changedPluginIds12 = tdb1.getPluginIdsForDifferences(tdb2);
    assertEquals(0, changedPluginIds12.size());

    Tdb tdb3 = new Tdb();
    addTestPublisher1_Changed(tdb3);
    addTestPublisher2(tdb3);
    
    Collection<String> changedPluginIds13 = tdb1.getPluginIdsForDifferences(tdb3);
    assertEquals(1, changedPluginIds13.size());
    assertTrue(changedPluginIds13.contains("plugin_t2p1"));

    Tdb tdb4 = new Tdb();
    addTestPublisher1(tdb4);
    addTestPublisher2_Changed(tdb4);
    
    Collection<String> changedPluginIds14 = tdb1.getPluginIdsForDifferences(tdb4);
    assertEquals(1, changedPluginIds14.size());
    assertTrue(changedPluginIds14.contains("plugin_p2"));

    Tdb tdb5 = new Tdb();
    addTestPublisher1_Changed(tdb5);
    addTestPublisher2_Changed(tdb5);
    
    Collection<String> changedPluginIds15 = tdb1.getPluginIdsForDifferences(tdb5);
    assertEquals(2, changedPluginIds15.size());
    assertTrue(changedPluginIds15.contains("plugin_t2p1"));
    assertTrue(changedPluginIds15.contains("plugin_p2"));
  }
  
  /**
   * Test sealing a TdbTitle.
   */
  public void testSealTDB() {
    Tdb tdb = new Tdb();
    assertFalse(tdb.isSealed());

    tdb.seal();
    assertTrue(tdb.isSealed());

    // create an AU
    TdbPublisher p1 = new TdbPublisher("p1");
    TdbTitle t1p1 = new TdbTitle("t1p1");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1");
    t1p1.addTdbAu(a1t1p1);

    // try adding AU to sealed TDB
    try {
      tdb.addTdbAu(a1t1p1);
      fail("Tdb did not throw IllegalStateException adding AU to sealed TDB.");
    } catch (IllegalStateException ex) {
    }
    
    assertTrue(tdb.isEmpty());
  }

  /**
   * Test copyFrom() method
   */
  public void testCopyTDB() {
    Tdb tdb = new Tdb();

    // create an AU
    TdbPublisher p1 = new TdbPublisher("p1");
    TdbTitle t1p1 = new TdbTitle("t1p1");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu("a1t1p1");
    a1t1p1.setPluginId("plugin1");
    t1p1.addTdbAu(a1t1p1);
    tdb.addTdbAu(a1t1p1);
    
    Tdb copyTdb = new Tdb();
    copyTdb.copyFrom(tdb);
    assertFalse(copyTdb.isEmpty());
    assertEquals(1, copyTdb.getAllTdbAus().size());
    assertEquals(1, copyTdb.getAllTdbPublishers().size());
  }
}
