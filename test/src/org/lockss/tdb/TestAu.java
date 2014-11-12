/*
 * $Id: TestAu.java,v 1.1 2014-11-12 00:16:04 thib_gc Exp $
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

package org.lockss.tdb;

import java.util.*;

import org.lockss.test.LockssTestCase;

public class TestAu extends LockssTestCase {

  public static final String NAME_VALUE = "AU Name";
  public static final String EDITION_VALUE = "Edition Value";
  public static final String EISBN_VALUE = "eISBN Value";
  public static final String ISBN_VALUE = "ISBN Value";
  public static final String PLUGIN_VALUE = "Plugin Value";
  public static final String PLUGIN_PREFIX_VALUE = "Plugin Prefix Value";
  public static final String PLUGIN_SUFFIX_VALUE = "Plugin Suffix Value";
  public static final String PROXY_VALUE = "Proxy Value";
  public static final String RIGHTS_VALUE = "Rights Value";
  public static final String STATUS_VALUE = "Status Value";
  public static final String STATUS1_VALUE = "Status1 Value";
  public static final String STATUS2_VALUE = "Status2 Value";
  public static final String VOLUME_VALUE = "Volume Value";
  public static final String YEAR_VALUE = "Year Value";
  public static final List<String> IMPLICIT_VALUE =
      AppUtil.ul("implicit1", "implicit2", "implicit3");
  
  public static final String PARAM1_KEY = "p1";
  public static final String PARAM1_VALUE = "v1";
  public static final String PARAM2_KEY = "p2";
  public static final String PARAM2_VALUE = "v2";
  public static final String NONDEFPARAM1_KEY = "ndp1";
  public static final String NONDEFPARAM1_VALUE = "ndv1";
  public static final String NONDEFPARAM2_KEY = "ndp2";
  public static final String NONDEFPARAM2_VALUE = "ndv2";
  public static final String ATTR1_KEY = "attr1";
  public static final String ATTR1_VALUE = "attr1v";
  
  public static final String FOO_KEY = "fookey";
  public static final String FOO_VALUE = "fooval";
  
  public void testKeys() throws Exception {
    assertEquals("edition", Au.EDITION);
    assertEquals("eisbn", Au.EISBN);
    assertEquals("isbn", Au.ISBN);
    assertEquals("name", Au.NAME);
    assertEquals("plugin", Au.PLUGIN);
    assertEquals("pluginPrefix", Au.PLUGIN_PREFIX);
    assertEquals("pluginSuffix", Au.PLUGIN_SUFFIX);
    assertEquals("proxy", Au.PROXY);
    assertEquals("rights", Au.RIGHTS);
    assertEquals("status", Au.STATUS);
    assertEquals("status1", Au.STATUS1);
    assertEquals("status2", Au.STATUS2);
    assertEquals("volume", Au.VOLUME);
    assertEquals("year", Au.YEAR);
  }
  
  public void testStatus() throws Exception {
    assertEquals("crawling", Au.STATUS_CRAWLING);
    assertEquals("deepCrawl", Au.STATUS_DEEP_CRAWL);
    assertEquals("doNotProcess", Au.STATUS_DO_NOT_PROCESS);
    assertEquals("doesNotExist", Au.STATUS_DOES_NOT_EXIST);
    assertEquals("down", Au.STATUS_DOWN);
    assertEquals("exists", Au.STATUS_EXISTS);
    assertEquals("expected", Au.STATUS_EXPECTED);
    assertEquals("finished", Au.STATUS_FINISHED);
    assertEquals("frozen", Au.STATUS_FROZEN);
    assertEquals("ingNotReady", Au.STATUS_ING_NOT_READY);
    assertEquals("manifest", Au.STATUS_MANIFEST);
    assertEquals("notReady", Au.STATUS_NOT_READY);
    assertEquals("ready", Au.STATUS_READY);
    assertEquals("readySource", Au.STATUS_READY_SOURCE);
    assertEquals("released", Au.STATUS_RELEASED);
    assertEquals("releasing", Au.STATUS_RELEASING);
    assertEquals("superseded", Au.STATUS_SUPERSEDED);
    assertEquals("testing", Au.STATUS_TESTING);
    assertEquals("wanted", Au.STATUS_WANTED);
    assertEquals("zapped", Au.STATUS_ZAPPED);
  }
  
  public void testEmpty() throws Exception {
    Au au = new Au();
    assertNull(au.getTitle());
    assertNull(au.getName());
    assertNull(au.getAuid());
    assertNull(au.getAuidPlus());
    assertNull(au.getEdition());
    assertNull(au.getEisbn());
    assertNull(au.getIsbn());
    assertNull(au.getPlugin());
    assertNull(au.getPluginPrefix());
    assertNull(au.getPluginSuffix());
    assertNull(au.getProxy());
    assertNull(au.getRights());
    assertNull(au.getStatus());
    assertNull(au.getStatus1());
    assertNull(au.getStatus2());
    assertNull(au.getVolume());
    assertNull(au.getYear());
    assertEmpty(au.getParams());
    assertEmpty(au.getNondefParams());
    assertEmpty(au.getAttrs());
    assertNull(au.getImplicit());
    assertNull(au.getArbitraryValue(FOO_KEY));
  }
  
  public void testAu() throws Exception {
    Publisher publisher = new Publisher();
    Title title = new Title(publisher);
    Au au = new Au(title);
    au.put(Au.NAME, NAME_VALUE);
    au.put(Au.EDITION, EDITION_VALUE);
    au.put(Au.EISBN, EISBN_VALUE);
    au.put(Au.ISBN, ISBN_VALUE);
    au.put(Au.PROXY, PROXY_VALUE);
    au.put(Au.RIGHTS, RIGHTS_VALUE);
    au.put(Au.STATUS, STATUS_VALUE);
    au.put(Au.STATUS1, STATUS1_VALUE);
    au.put(Au.STATUS2, STATUS2_VALUE);
    au.put(Au.VOLUME, VOLUME_VALUE);
    au.put(Au.YEAR, YEAR_VALUE);
    au.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    au.put(String.format("attr[%s]", ATTR1_KEY), ATTR1_VALUE);
    au.setImplicit(IMPLICIT_VALUE);
    au.put(FOO_KEY, FOO_VALUE);
    assertSame(title, au.getTitle());
    assertSame(publisher, au.getTitle().getPublisher());
    assertEquals(NAME_VALUE, au.getName());
    assertEquals(EDITION_VALUE, au.getEdition());
    assertEquals(EISBN_VALUE, au.getEisbn());
    assertEquals(ISBN_VALUE, au.getIsbn());
    assertEquals(PROXY_VALUE, au.getProxy());
    assertEquals(RIGHTS_VALUE, au.getRights());
    assertEquals(STATUS_VALUE, au.getStatus());
    assertEquals(STATUS1_VALUE, au.getStatus1());
    assertEquals(STATUS2_VALUE, au.getStatus2());
    assertEquals(VOLUME_VALUE, au.getVolume());
    assertEquals(YEAR_VALUE, au.getYear());
    assertEquals(PARAM1_VALUE, au.getParams().get(PARAM1_KEY));
    assertNull(au.getParams().get("X" + PARAM1_KEY));
    assertEquals(NONDEFPARAM1_VALUE, au.getNondefParams().get(NONDEFPARAM1_KEY));
    assertNull(au.getNondefParams().get("X" + NONDEFPARAM1_KEY));
    assertEquals(ATTR1_VALUE, au.getAttrs().get(ATTR1_KEY));
    assertNull(au.getAttrs().get("X" + ATTR1_KEY));
    assertIsomorphic(Arrays.asList("implicit1", "implicit2", "implicit3"), au.getImplicit());
    assertEquals(FOO_VALUE, au.getArbitraryValue(FOO_KEY));
    assertNull(au.getArbitraryValue("X" + FOO_KEY));
  }

  public void testPlugin() throws Exception {
    Au au1 = new Au();
    au1.put(Au.PLUGIN, PLUGIN_VALUE);
    assertEquals(PLUGIN_VALUE, au1.getPlugin());
    assertNull(au1.getPluginPrefix());
    assertNull(au1.getPluginSuffix());

    Au au2 = new Au();
    au2.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    au2.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertEquals(PLUGIN_PREFIX_VALUE + PLUGIN_SUFFIX_VALUE, au2.getPlugin());
    assertEquals(PLUGIN_PREFIX_VALUE, au2.getPluginPrefix());
    assertEquals(PLUGIN_SUFFIX_VALUE, au2.getPluginSuffix());
    
    // Other combinations are illegal but have the following behavior:

    Au au3 = new Au();
    au3.put(Au.PLUGIN, PLUGIN_VALUE);
    au3.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    assertEquals(PLUGIN_VALUE, au3.getPlugin());
    assertEquals(PLUGIN_PREFIX_VALUE, au3.getPluginPrefix());
    assertNull(au3.getPluginSuffix());

    Au au4 = new Au();
    au4.put(Au.PLUGIN, PLUGIN_VALUE);
    au4.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertEquals(PLUGIN_VALUE, au4.getPlugin());
    assertNull(au4.getPluginPrefix());
    assertEquals(PLUGIN_SUFFIX_VALUE, au4.getPluginSuffix());

    Au au5 = new Au();
    au5.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    assertNull(au5.getPlugin());
    assertEquals(PLUGIN_PREFIX_VALUE, au5.getPluginPrefix());
    assertNull(au5.getPluginSuffix());

    Au au6 = new Au();
    au6.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertNull(au6.getPlugin());
    assertNull(au6.getPluginPrefix());
    assertEquals(PLUGIN_SUFFIX_VALUE, au6.getPluginSuffix());
  }

  public void testAuid() throws Exception {
    final String expectedPlugin = "org.lockss.plugin.FakePlugin";
    final String expectedAuid1 =
        String.format("%s&%s~%s&%s~%s",
                      expectedPlugin.replace(".", "|"),
                      PARAM1_KEY,
                      PARAM1_VALUE,
                      PARAM2_KEY,
                      PARAM2_VALUE);
    
    Au au1 = new Au();
    au1.put(Au.PLUGIN, expectedPlugin);
    au1.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au1.put(String.format("param[%s]", PARAM2_KEY), PARAM2_VALUE);
    assertEquals(expectedAuid1, au1.getAuid());
    assertEquals(expectedAuid1, au1.getAuidPlus());

    final String expectedAuidPlus2 =
        String.format("%s@@@NONDEF@@@%s~%s&%s~%s",
                      expectedAuid1,
                      NONDEFPARAM1_KEY,
                      NONDEFPARAM1_VALUE,
                      NONDEFPARAM2_KEY,
                      NONDEFPARAM2_VALUE);
    
    Au au2 = new Au();
    au2.put(Au.PLUGIN, expectedPlugin);
    au2.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au2.put(String.format("param[%s]", PARAM2_KEY), PARAM2_VALUE);
    au2.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    au2.put(String.format("nondefparam[%s]", NONDEFPARAM2_KEY), NONDEFPARAM2_VALUE);
    assertEquals(expectedAuid1, au2.getAuid());
    assertEquals(expectedAuidPlus2, au2.getAuidPlus());
  }

  public void testNesting() throws Exception {
    Au au1 = new Au();
    au1.put(Au.EISBN, EISBN_VALUE);
    au1.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au1.put(FOO_KEY, FOO_VALUE);
    au1.setImplicit(IMPLICIT_VALUE);
    Au au2 = new Au(au1);
    au2.put(Au.ISBN, ISBN_VALUE);
    au2.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    Au au3 = new Au(au2);
    au3.put(Au.PLUGIN, PLUGIN_VALUE);
    au3.put(String.format("attr[%s]", ATTR1_KEY), ATTR1_VALUE);
    assertEquals(EISBN_VALUE, au3.getEisbn());
    assertEquals(ISBN_VALUE, au3.getIsbn());
    assertEquals(PLUGIN_VALUE, au3.getPlugin());
    assertEquals(PARAM1_VALUE, au3.getParams().get(PARAM1_KEY));
    assertEquals(NONDEFPARAM1_VALUE, au3.getNondefParams().get(NONDEFPARAM1_KEY));
    assertEquals(ATTR1_VALUE, au3.getAttrs().get(ATTR1_KEY));
    assertIsomorphic(Arrays.asList("implicit1", "implicit2", "implicit3"), au3.getImplicit());
    assertEquals(FOO_VALUE, au3.getArbitraryValue(FOO_KEY));
    assertNull(au3.getArbitraryValue("X" + FOO_KEY));
  }
  
}
