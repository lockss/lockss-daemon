/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.*;

import org.junit.jupiter.api.Test;

public class TestAu {

  public static final String NAME_VALUE = "AU Name";
  public static final String EDITION_VALUE = "Edition Value";
  public static final String EISBN_VALUE = "eISBN Value";
  public static final String ISBN_VALUE = "ISBN Value";
  public static final String PLUGIN_VALUE = "Plugin Value";
  public static final String PLUGIN_PREFIX_VALUE = "Plugin Prefix Value";
  public static final String PLUGIN_SUFFIX_VALUE = "Plugin Suffix Value";
  public static final String PROVIDER_VALUE = "Provider Value";
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
  
  public static final String FOO_KEY = "aufookey";
  public static final String FOO_VALUE = "aufooval";
  
  public static final String PLUGIN1 = "org.lockss.plugin.FakePlugin";
  public static final String AUID1 = String.format("%s&%s~%s&%s~%s",
                                                   PLUGIN1.replace('.', '|'),
                                                   PARAM1_KEY,
                                                   PARAM1_VALUE,
                                                   PARAM2_KEY,
                                                   PARAM2_VALUE);
  public static final String AUIDPLUS1 = String.format("%s@@@NONDEF@@@%s~%s&%s~%s",
                                                       AUID1,
                                                       NONDEFPARAM1_KEY,
                                                       NONDEFPARAM1_VALUE,
                                                       NONDEFPARAM2_KEY,
                                                       NONDEFPARAM2_VALUE);

  @Test
  public void testKeys() throws Exception {
    assertEquals("edition", Au.EDITION);
    assertEquals("eisbn", Au.EISBN);
    assertEquals("isbn", Au.ISBN);
    assertEquals("name", Au.NAME);
    assertEquals("plugin", Au.PLUGIN);
    assertEquals("pluginPrefix", Au.PLUGIN_PREFIX);
    assertEquals("pluginSuffix", Au.PLUGIN_SUFFIX);
    assertEquals("provider", Au.PROVIDER);
    assertEquals("proxy", Au.PROXY);
    assertEquals("rights", Au.RIGHTS);
    assertEquals("status", Au.STATUS);
    assertEquals("status1", Au.STATUS1);
    assertEquals("status2", Au.STATUS2);
    assertEquals("volume", Au.VOLUME);
    assertEquals("year", Au.YEAR);
  }

  @Test
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

  @Test
  public void testEmpty() throws Exception {
    Au au = new Au(null);
    assertNull(au.getTitle());
    assertNull(au.getName());
    assertNull(au.getAuid());
    assertNull(au.getAuidPlus());
    assertNull(au.getComputedPlugin());
    assertNull(au.getEdition());
    assertNull(au.getEisbn());
    assertNull(au.getIsbn());
    assertNull(au.getPlugin());
    assertNull(au.getPluginPrefix());
    assertNull(au.getPluginSuffix());
    assertNull(au.getProvider());
    assertNull(au.getProxy());
    assertNull(au.getRights());
    assertNull(au.getStatus());
    assertNull(au.getStatus1());
    assertNull(au.getStatus2());
    assertNull(au.getVolume());
    assertNull(au.getYear());
    assertEquals(0, au.getParams().size());
    assertEquals(0, au.getNondefParams().size());
    assertEquals(0, au.getAttrs().size());
    assertNull(au.getImplicit());
    assertNull(au.getArbitraryValue(FOO_KEY));
  }

  @Test
  public void testAu() throws Exception {
    Publisher publisher = new Publisher();
    Title title = new Title(publisher);
    Au au = new Au(null, title);
    assertSame(title, au.getTitle());
    assertSame(publisher, au.getTitle().getPublisher());
    au.put(Au.NAME, NAME_VALUE);
    assertEquals(NAME_VALUE, au.getName());
    au.put(Au.EDITION, EDITION_VALUE);
    assertEquals(EDITION_VALUE, au.getEdition());
    au.put(Au.EISBN, EISBN_VALUE);
    assertEquals(EISBN_VALUE, au.getEisbn());
    au.put(Au.ISBN, ISBN_VALUE);
    assertEquals(ISBN_VALUE, au.getIsbn());
    au.put(Au.PROVIDER, PROVIDER_VALUE);
    assertEquals(PROVIDER_VALUE, au.getProvider());
    au.put(Au.PROXY, PROXY_VALUE);
    assertEquals(PROXY_VALUE, au.getProxy());
    au.put(Au.RIGHTS, RIGHTS_VALUE);
    assertEquals(RIGHTS_VALUE, au.getRights());
    au.put(Au.STATUS, STATUS_VALUE);
    assertEquals(STATUS_VALUE, au.getStatus());
    au.put(Au.STATUS1, STATUS1_VALUE);
    assertEquals(STATUS1_VALUE, au.getStatus1());
    au.put(Au.STATUS2, STATUS2_VALUE);
    assertEquals(STATUS2_VALUE, au.getStatus2());
    au.put(Au.VOLUME, VOLUME_VALUE);
    assertEquals(VOLUME_VALUE, au.getVolume());
    au.put(Au.YEAR, YEAR_VALUE);
    assertEquals(YEAR_VALUE, au.getYear());
    au.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    assertEquals(PARAM1_VALUE, au.getParams().get(PARAM1_KEY));
    assertNull(au.getParams().get("X" + PARAM1_KEY));
    au.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    assertEquals(NONDEFPARAM1_VALUE, au.getNondefParams().get(NONDEFPARAM1_KEY));
    assertNull(au.getNondefParams().get("X" + NONDEFPARAM1_KEY));
    au.put(String.format("attr[%s]", ATTR1_KEY), ATTR1_VALUE);
    assertEquals(ATTR1_VALUE, au.getAttrs().get(ATTR1_KEY));
    assertNull(au.getAttrs().get("X" + ATTR1_KEY));
    au.setImplicit(IMPLICIT_VALUE);
    assertThat(au.getImplicit(), hasSize(3));
    assertThat(au.getImplicit(), hasItems("implicit1", "implicit2", "implicit3"));
    au.put(FOO_KEY, FOO_VALUE);
    assertEquals(FOO_VALUE, au.getArbitraryValue(FOO_KEY));
    assertNull(au.getArbitraryValue("X" + FOO_KEY));
  }

  @Test
  public void testPlugin() throws Exception {
    Au au1 = new Au(null);
    au1.put(Au.PLUGIN, PLUGIN_VALUE);
    assertEquals(PLUGIN_VALUE, au1.getPlugin());
    assertNull(au1.getPluginPrefix());
    assertNull(au1.getPluginSuffix());
    assertEquals(PLUGIN_VALUE, au1.getComputedPlugin());

    Au au2 = new Au(null);
    au2.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    au2.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertNull(au2.getPlugin());
    assertEquals(PLUGIN_PREFIX_VALUE, au2.getPluginPrefix());
    assertEquals(PLUGIN_SUFFIX_VALUE, au2.getPluginSuffix());
    assertEquals(PLUGIN_PREFIX_VALUE + PLUGIN_SUFFIX_VALUE, au2.getComputedPlugin());
    
    // Other combinations are illegal but have the following behavior:

    Au au3 = new Au(null);
    au3.put(Au.PLUGIN, PLUGIN_VALUE);
    au3.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    assertEquals(PLUGIN_VALUE, au3.getPlugin());
    assertEquals(PLUGIN_PREFIX_VALUE, au3.getPluginPrefix());
    assertNull(au3.getPluginSuffix());
    assertEquals(PLUGIN_VALUE, au3.getComputedPlugin());

    Au au4 = new Au(null);
    au4.put(Au.PLUGIN, PLUGIN_VALUE);
    au4.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertEquals(PLUGIN_VALUE, au4.getPlugin());
    assertNull(au4.getPluginPrefix());
    assertEquals(PLUGIN_SUFFIX_VALUE, au4.getPluginSuffix());
    assertEquals(PLUGIN_VALUE, au4.getComputedPlugin());

    Au au5 = new Au(null);
    au5.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    assertNull(au5.getPlugin());
    assertEquals(PLUGIN_PREFIX_VALUE, au5.getPluginPrefix());
    assertNull(au5.getPluginSuffix());
    assertNull(au5.getComputedPlugin());

    Au au6 = new Au(null);
    au6.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertNull(au6.getPlugin());
    assertNull(au6.getPluginPrefix());
    assertEquals(PLUGIN_SUFFIX_VALUE, au6.getPluginSuffix());
    assertNull(au6.getComputedPlugin());
  }

  @Test
  public void testAuid() throws Exception {
    Au au1 = new Au(null);
    au1.put(Au.PLUGIN, PLUGIN1);
    au1.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au1.put(String.format("param[%s]", PARAM2_KEY), PARAM2_VALUE);
    assertEquals(AUID1, au1.getAuid());
    assertEquals(AUID1, au1.getAuidPlus());

    Au au2 = new Au(null);
    au2.put(Au.PLUGIN, PLUGIN1);
    au2.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au2.put(String.format("param[%s]", PARAM2_KEY), PARAM2_VALUE);
    au2.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    au2.put(String.format("nondefparam[%s]", NONDEFPARAM2_KEY), NONDEFPARAM2_VALUE);
    assertEquals(AUID1, au2.getAuid());
    assertEquals(AUIDPLUS1, au2.getAuidPlus());
  }

  @Test
  public void testNesting() throws Exception {
    Au au1 = new Au(null);
    au1.put(Au.EISBN, EISBN_VALUE);
    au1.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    au1.put(FOO_KEY, FOO_VALUE);
    au1.setImplicit(IMPLICIT_VALUE);
    Au au2 = new Au(null, au1);
    au2.put(Au.ISBN, ISBN_VALUE);
    au2.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    Au au3 = new Au(null, au2);
    au3.put(Au.PLUGIN, PLUGIN_VALUE);
    au3.put(String.format("attr[%s]", ATTR1_KEY), ATTR1_VALUE);
    assertEquals(EISBN_VALUE, au3.getEisbn());
    assertEquals(ISBN_VALUE, au3.getIsbn());
    assertEquals(PLUGIN_VALUE, au3.getPlugin());
    assertEquals(PLUGIN_VALUE, au3.getComputedPlugin());
    assertEquals(PARAM1_VALUE, au3.getParams().get(PARAM1_KEY));
    assertEquals(NONDEFPARAM1_VALUE, au3.getNondefParams().get(NONDEFPARAM1_KEY));
    assertEquals(ATTR1_VALUE, au3.getAttrs().get(ATTR1_KEY));
    assertThat(au3.getImplicit(), hasSize(3));
    assertThat(au3.getImplicit(), hasItems("implicit1", "implicit2", "implicit3"));
    assertEquals(FOO_VALUE, au3.getArbitraryValue(FOO_KEY));
    assertNull(au3.getArbitraryValue("X" + FOO_KEY));
  }

  @Test
  public void testTraitFunctors() throws Exception {
    // Make a publisher
    Map<String, String> publisherMap = new LinkedHashMap<String, String>();
    Publisher publisher = new Publisher(publisherMap);

    // Make a title with that publisher
    Map<String, String> titleMap = new LinkedHashMap<String, String>();
    Title title = new Title(publisher, titleMap);

    // Make an AU with that title
    Au au = new Au(null, title);
    
    // Test AU traits
    au.put(Au.EDITION, EDITION_VALUE);
    assertEquals(EDITION_VALUE, Au.traitFunctor("au:edition").apply(au));
    assertSame(Au.traitFunctor("au:edition"), Au.traitFunctor("edition"));
    au.put(Au.EISBN, EISBN_VALUE);
    assertEquals(EISBN_VALUE, Au.traitFunctor("au:eisbn").apply(au));
    assertSame(Au.traitFunctor("au:eisbn"), Au.traitFunctor("eisbn"));
    au.put(Au.ISBN, ISBN_VALUE);
    assertEquals(ISBN_VALUE, Au.traitFunctor("au:isbn").apply(au));
    assertSame(Au.traitFunctor("au:isbn"), Au.traitFunctor("isbn"));
    au.put(Au.NAME, NAME_VALUE);
    assertEquals(NAME_VALUE, Au.traitFunctor("au:name").apply(au));
    assertSame(Au.traitFunctor("au:name"), Au.traitFunctor("name"));
    au.put(Au.PROVIDER, PROVIDER_VALUE);
    assertEquals(PROVIDER_VALUE, Au.traitFunctor("au:provider").apply(au));
    assertSame(Au.traitFunctor("au:provider"), Au.traitFunctor("provider"));
    au.put(Au.PROXY, PROXY_VALUE);
    assertEquals(PROXY_VALUE, Au.traitFunctor("au:proxy").apply(au));
    assertSame(Au.traitFunctor("au:proxy"), Au.traitFunctor("proxy"));
    au.put(Au.RIGHTS, RIGHTS_VALUE);
    assertEquals(RIGHTS_VALUE, Au.traitFunctor("au:rights").apply(au));
    assertSame(Au.traitFunctor("au:rights"), Au.traitFunctor("rights"));
    au.put(Au.STATUS, STATUS_VALUE);
    assertEquals(STATUS_VALUE, Au.traitFunctor("au:status").apply(au));
    assertSame(Au.traitFunctor("au:status"), Au.traitFunctor("status"));
    au.put(Au.STATUS1, STATUS1_VALUE);
    assertEquals(STATUS1_VALUE, Au.traitFunctor("au:status1").apply(au));
    assertSame(Au.traitFunctor("au:status1"), Au.traitFunctor("status1"));
    au.put(Au.STATUS2, STATUS2_VALUE);
    assertEquals(STATUS2_VALUE, Au.traitFunctor("au:status2").apply(au));
    assertSame(Au.traitFunctor("au:status2"), Au.traitFunctor("status2"));
    au.put(Au.VOLUME, VOLUME_VALUE);
    assertEquals(VOLUME_VALUE, Au.traitFunctor("au:volume").apply(au));
    assertSame(Au.traitFunctor("au:volume"), Au.traitFunctor("volume"));
    au.put(Au.YEAR, YEAR_VALUE);
    assertEquals(YEAR_VALUE, Au.traitFunctor("au:year").apply(au));
    assertSame(Au.traitFunctor("au:year"), Au.traitFunctor("year"));
    au.put(String.format("param[%s]", PARAM1_KEY), PARAM1_VALUE);
    assertEquals(PARAM1_VALUE, Au.traitFunctor(String.format("au:param[%s]", PARAM1_KEY)).apply(au));
    assertEquals(PARAM1_VALUE, Au.traitFunctor(String.format("param[%s]", PARAM1_KEY)).apply(au));
    au.put(String.format("nondefparam[%s]", NONDEFPARAM1_KEY), NONDEFPARAM1_VALUE);
    assertEquals(NONDEFPARAM1_VALUE, Au.traitFunctor(String.format("au:nondefparam[%s]", NONDEFPARAM1_KEY)).apply(au));
    assertEquals(NONDEFPARAM1_VALUE, Au.traitFunctor(String.format("nondefparam[%s]", NONDEFPARAM1_KEY)).apply(au));
    au.put(String.format("attr[%s]", ATTR1_KEY), ATTR1_VALUE);
    assertEquals(ATTR1_VALUE, Au.traitFunctor(String.format("au:attr[%s]", ATTR1_KEY)).apply(au));
    assertEquals(ATTR1_VALUE, Au.traitFunctor(String.format("attr[%s]", ATTR1_KEY)).apply(au));
    au.put(FOO_KEY, FOO_VALUE);
    assertEquals(FOO_VALUE, Au.traitFunctor(String.format("au:%s", FOO_KEY)).apply(au));
    assertNull(Au.traitFunctor(FOO_KEY));
    
    au.put(Au.PLUGIN, PLUGIN1);
    assertEquals(PLUGIN1, Au.traitFunctor("au:plugin").apply(au));
    assertSame(Au.traitFunctor("au:plugin"), Au.traitFunctor("plugin"));
    au.put(String.format("param[%s]", PARAM2_KEY), PARAM2_VALUE);
    au.put(String.format("nondefparam[%s]", NONDEFPARAM2_KEY), NONDEFPARAM2_VALUE);
    assertEquals(AUID1, Au.traitFunctor("au:auid").apply(au));
    assertSame(Au.traitFunctor("au:auid"), Au.traitFunctor("auid"));
    assertEquals(AUIDPLUS1, Au.traitFunctor("au:auidplus").apply(au));
    assertSame(Au.traitFunctor("au:auidplus"), Au.traitFunctor("auidplus"));
    
    au.put(Au.PLUGIN_PREFIX, PLUGIN_PREFIX_VALUE);
    assertEquals(PLUGIN_PREFIX_VALUE, Au.traitFunctor("au:pluginPrefix").apply(au));
    assertSame(Au.traitFunctor("au:pluginPrefix"), Au.traitFunctor("pluginPrefix"));
    au.put(Au.PLUGIN_SUFFIX, PLUGIN_SUFFIX_VALUE);
    assertEquals(PLUGIN_SUFFIX_VALUE, Au.traitFunctor("au:pluginSuffix").apply(au));
    assertSame(Au.traitFunctor("au:pluginSuffix"), Au.traitFunctor("pluginSuffix"));
    
    // Test title traits
    titleMap.put(Title.NAME, TestTitle.NAME_VALUE);
    assertEquals(TestTitle.NAME_VALUE, Au.traitFunctor("title:name").apply(au));
    assertSame(Au.traitFunctor("title:name"), Au.traitFunctor("title"));
    titleMap.put(Title.DOI, TestTitle.DOI_VALUE);
    assertEquals(TestTitle.DOI_VALUE, Au.traitFunctor("title:doi").apply(au));
    assertSame(Au.traitFunctor("title:doi"), Au.traitFunctor("doi"));
    titleMap.put(Title.EISSN, TestTitle.EISSN_VALUE);
    assertEquals(TestTitle.EISSN_VALUE, Au.traitFunctor("title:eissn").apply(au));
    assertSame(Au.traitFunctor("title:eissn"), Au.traitFunctor("eissn"));
    titleMap.put(Title.ISSN, TestTitle.ISSN_VALUE);
    assertEquals(TestTitle.ISSN_VALUE, Au.traitFunctor("title:issn").apply(au));
    assertSame(Au.traitFunctor("title:issn"), Au.traitFunctor("issn"));
    titleMap.put(Title.ISSNL, TestTitle.ISSNL_VALUE);
    assertEquals(TestTitle.ISSNL_VALUE, Au.traitFunctor("title:issnl").apply(au));
    assertSame(Au.traitFunctor("title:issnl"), Au.traitFunctor("issnl"));
    titleMap.put(Title.TYPE, TestTitle.TYPE_VALUE);
    assertEquals(TestTitle.TYPE_VALUE, Au.traitFunctor("title:type").apply(au));
    assertSame(Au.traitFunctor("title:type"), Au.traitFunctor("type"));
    titleMap.put(TestTitle.FOO_KEY, TestTitle.FOO_VALUE);
    assertEquals(TestTitle.FOO_VALUE, Au.traitFunctor(String.format("title:%s", TestTitle.FOO_KEY)).apply(au));
    assertNull(Au.traitFunctor(String.format("title:X%s", TestTitle.FOO_KEY)).apply(au));

    // Test publisher traits
    publisherMap.put(Publisher.NAME, TestPublisher.NAME_VALUE);
    assertEquals(TestPublisher.NAME_VALUE, Au.traitFunctor("publisher:name").apply(au));
    assertSame(Au.traitFunctor("publisher:name"), Au.traitFunctor("publisher"));
    publisherMap.put(TestPublisher.FOO_KEY, TestPublisher.FOO_VALUE);
    assertEquals(TestPublisher.FOO_VALUE, Au.traitFunctor(String.format("publisher:%s", TestPublisher.FOO_KEY)).apply(au));
    assertNull(Au.traitFunctor(String.format("publisher:X%s", TestPublisher.FOO_KEY)).apply(au));
  }
  
}
