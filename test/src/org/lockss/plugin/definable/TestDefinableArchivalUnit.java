/*
 * $Id: TestDefinableArchivalUnit.java,v 1.9 2004-09-01 23:36:50 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.definable;

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.blackbird.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.crawler.*;

/**
 * TestConfigurableArchivalUnit: test case for the ConfigurableArchivalUnit
 * @author Claire Griffin
 * @version 1.0
 */

public class TestDefinableArchivalUnit extends LockssTestCase {
  private DefinableArchivalUnit cau = null;
  private ExternalizableMap map;
  private List configProps = ListUtil.list(ConfigParamDescr.BASE_URL,
                                           ConfigParamDescr.VOLUME_NUMBER);
  private List crawlRules = ListUtil.list("1,\"%s\", base_url",
                                          "1,\".*\\.gif\"");

  private static String PLUGIN_NAME = "Test Plugin";
  private static String CURRENT_VERSION = "Version 1.0";

  protected void setUp() throws Exception {
    super.setUp();

    DefinablePlugin cp = new DefinablePlugin();
    map = cp.getDefinitionMap();
    map.putString(DefinablePlugin.CM_NAME_KEY, PLUGIN_NAME);
    map.putString(DefinablePlugin.CM_VERSION_KEY, CURRENT_VERSION);
    map.putCollection(DefinablePlugin.CM_CONFIG_PROPS_KEY, configProps);
    cau = new DefinableArchivalUnit(cp, map);
  }

  protected void tearDown() throws Exception {
    cau = null;
    super.tearDown();
  }

  public void testConvertVariableString() {
    map.putInt("INTEGER", 10);
    map.putBoolean("BOOLEAN", true);
    map.putString("STRING", "Yo Mama!");
    map.putInt(ConfigParamDescr.YEAR.getKey(), 2003);
    map.putInt(DefinableArchivalUnit.AU_SHORT_YEAR_PREFIX +
               ConfigParamDescr.YEAR.getKey(),3);

    String substr = "\"My Test Integer = %d\", INTEGER";
    String expectedReturn = "My Test Integer = 10";
    String actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test Boolean = %s\", BOOLEAN";
    expectedReturn = "My Test Boolean = true";
    actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test String = %s\", STRING";
    expectedReturn = "My Test String = Yo Mama!";
    actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test Short Year = %02d\", au_short_year";
    expectedReturn = "My Test Short Year = 03";
    actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test Decade = %4.2d\", year";
    expectedReturn = "My Test Decade = 2000";
    actualReturn = cau.convertVariableString(substr);
//    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConvertRule() throws LockssRegexpException {
    map.putString("URL", "http://www.example.com/");
    String rule1 = "1,\".*\\.gif\"";
    String rule2 = "1,\"%s\",URL";

    CrawlRule actualReturn = cau.convertRule(rule1);
    assertEquals(CrawlRule.INCLUDE,
                 actualReturn.match("http://www.example.com/mygif.gif"));

    actualReturn = cau.convertRule(rule2);
    assertEquals(CrawlRule.INCLUDE,
                 actualReturn.match("http://www.example.com/"));
  }


  public void testMakeName() {
    map.putString("JOURNAL_NAME", "MyJournal");
    map.putInt("VOLUME", 43);
    map.putString(DefinableArchivalUnit.AU_NAME_KEY,
                  "\"%s Vol %d\",JOURNAL_NAME,VOLUME");
    String expectedReturn = "MyJournal Vol 43";
    String actualReturn = cau.makeName();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testMakeRules() throws LockssRegexpException {
    map.putString("base_url", "http://www.example.com/");
    map.putCollection(DefinableArchivalUnit.AU_RULES_KEY, crawlRules);

    CrawlRule rules = cau.makeRules();
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/mygif.gif"));
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/"));
  }

  public void testMakeStartUrl() {
    map.putInt("VOLUME", 43);
    map.putString("URL", "http://www.example.com/");
    map.putString(DefinableArchivalUnit.AU_START_URL_KEY,
                  "\"%slockss-volume/%d.html\", URL, VOLUME");

    String expectedReturn = "http://www.example.com/lockss-volume/43.html";
    String actualReturn = cau.makeStartUrl();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testGetManifestPage() {

    map.putString("HOST", "www.example.com");
    map.putInt("YEAR", 2003);
    map.putString(DefinableArchivalUnit.AU_MANIFEST_KEY,
            "\"http://%s/contents-by-date.%d.shtml\", HOST, YEAR");
    String expectedReturn = "http://www.example.com/contents-by-date.2003.shtml";
    String actualReturn = (String)cau.getPermissionPages().get(0);
    assertEquals("return valuse", expectedReturn, actualReturn);
  }

  public void testGetContentParser() {
    // test we find the default
    ContentParser parser = null;
    parser = cau.getContentParser("text/html");
    assertTrue(parser instanceof org.lockss.crawler.GoslingHtmlParser);

    // test we don't find one that doesn't exist
    parser = cau.getContentParser("text/ram");
    assertNull(parser);

    // test we find one we've added
    map.putString("text/ram_parser",
		  "org.lockss.plugin.blackbird.BlackbirdRamParser");
    parser = cau.getContentParser("text/ram");
    assertTrue(parser instanceof org.lockss.plugin.blackbird.BlackbirdRamParser);
  }

  public void testGetCrawlRule() throws LockssRegexpException {
    map.putString(DefinableArchivalUnit.AU_RULES_KEY,
		  "org.lockss.test.NegativeCrawlRule");

    CrawlRule rules = cau.makeRules();
    assertEquals(CrawlRule.EXCLUDE,
                 rules.match("http://www.example.com/mygif.gif"));
    assertEquals(CrawlRule.EXCLUDE,
                 rules.match("http://www.example.com/"));

    map.putString(DefinableArchivalUnit.AU_RULES_KEY,
		  "org.lockss.test.PositiveCrawlRule");

    rules = cau.makeRules();
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/mygif.gif"));
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/"));
  }

  public void testGetCrawlRuleThrowsOnBadClass() throws LockssRegexpException {
    map.putString(DefinableArchivalUnit.AU_RULES_KEY,
		  "org.lockss.bogus.FakeClass");

    try {
      CrawlRule rules = cau.makeRules();
      fail("Should have thrown on a non-existant class");
    } catch (DefinablePlugin.InvalidDefinitionException e){
    }
  }

}
