/*
 * $Id: TestDefinableArchivalUnit.java,v 1.30 2007-05-01 23:34:02 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;

import org.lockss.plugin.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;

/**
 * TestConfigurableArchivalUnit: test case for the ConfigurableArchivalUnit
 * @author Claire Griffin
 * @version 1.0
 */

public class TestDefinableArchivalUnit extends LockssTestCase {
  private DefinableArchivalUnit cau = null;
  private TypedEntryMap configMap;
  private ExternalizableMap defMap;
  private List configProps;
  private List crawlRules = ListUtil.list("1,\"%s\", base_url",
                                          "1,\".*\\.gif\"");

  private static String PLUGIN_NAME = "Test Plugin";
  private static String CURRENT_VERSION = "Version 1.0";

  DefinablePlugin cp;


  protected void setUp() throws Exception {
    super.setUp();

    configProps = ListUtil.list(ConfigParamDescr.BASE_URL,
                                ConfigParamDescr.VOLUME_NUMBER);

    cp = new DefinablePlugin();
    defMap = new ExternalizableMap();
    cp.initPlugin(getMockLockssDaemon(), defMap);
    cau = new DefinableArchivalUnit(cp, defMap);
    configMap = cau.getProperties();
    configMap.putString(DefinablePlugin.KEY_PLUGIN_NAME, PLUGIN_NAME);
    configMap.putString(DefinablePlugin.KEY_PLUGIN_VERSION, CURRENT_VERSION);
    configMap.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS, configProps);
  }

  protected void tearDown() throws Exception {
    cau = null;
    super.tearDown();
  }

  public void testConverVariableStringWithNumRange() {
    Vector vec = new Vector(2);
    String key = ConfigParamDescr.NUM_ISSUE_RANGE.getKey();
    vec.add(0, new Long(10));
    vec.add(1, new Long(20));
    configMap.setMapElement(key, vec);
    String substr = "\"My Test Range = %s\", " + key;
    String expectedReturn = "My Test Range = (\\d+)";
    String actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConverVariableStringWithRange() {
    Vector vec = new Vector(2);
    String key = ConfigParamDescr.ISSUE_RANGE.getKey();
    vec.add(0, "aaa");
    vec.add(1, "zzz");
    configMap.setMapElement(key, vec);
    String substr = "\"My Test Range = %s\", " + key;
    String expectedReturn = "My Test Range = (.*)";
    String actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConvertVariableStringWithSet() {
    Vector vec = new Vector();
    String key = ConfigParamDescr.ISSUE_SET.getKey();
    vec.add("apple");
    vec.add("bananna");
    vec.add("grape");
    configMap.setMapElement(key, vec);
    String substr = "\"My Test Range = %s\", " + key;
    String expectedReturn = "My Test Range = (.*)";
    String actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConvertVariableString() {
    configMap.putInt("INTEGER", 10);
    configMap.putBoolean("BOOLEAN", true);
    //  ensure dot in substitution string doesn't get regexp quoted
    configMap.putString("STRING", "Yo.Mama!");
    configMap.putInt(ConfigParamDescr.YEAR.getKey(), 2003);
    configMap.putInt(DefinableArchivalUnit.PREFIX_AU_SHORT_YEAR +
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
    expectedReturn = "My Test String = Yo.Mama!";
    actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test Short Year = %02d\", au_short_year";
    expectedReturn = "My Test Short Year = 03";
    actualReturn = cau.convertVariableString(substr);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConvertRegexpString() {
    configMap.putInt("INTEGER", 10);
    configMap.putBoolean("BOOLEAN", true);
    //  ensure meta chars in substitution string't get regexp quoted
    configMap.putString("STRING", "Yo.M[am]a?foo=bar!");
    configMap.putInt(ConfigParamDescr.YEAR.getKey(), 2003);
    configMap.putInt(DefinableArchivalUnit.PREFIX_AU_SHORT_YEAR +
               ConfigParamDescr.YEAR.getKey(),3);

    String substr = "\"My Test Integer = %d\", INTEGER";
    String expectedReturn = "My Test Integer = 10";
    String actualReturn = cau.convertVariableRegexpString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test Boolean = %s\", BOOLEAN";
    expectedReturn = "My Test Boolean = true";
    actualReturn = cau.convertVariableRegexpString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test String = %s\", STRING";
    expectedReturn = "My Test String = Yo\\.M\\[am\\]a\\?foo\\=bar\\!";
    actualReturn = cau.convertVariableRegexpString(substr);
    assertEquals("return value", expectedReturn, actualReturn);

    substr = "\"My Test Short Year = %02d\", au_short_year";
    expectedReturn = "My Test Short Year = 03";
    actualReturn = cau.convertVariableRegexpString(substr);
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testConvertRule() throws LockssRegexpException {
    configMap.putString("URL", "http://www.example.com/");
    String rule1 = "1,\".*\\.gif\"";
    String rule2 = "1,\"%s\",URL";

    CrawlRule rule = cau.convertRule(rule1);
    assertEquals(CrawlRule.INCLUDE,
                 rule.match("http://www.example.com/mygif.gif"));

    rule = cau.convertRule(rule2);
    assertEquals(CrawlRule.INCLUDE,
                 rule.match("http://www.example.com/"));

    // shouldn't match if dot properly quoted
    assertEquals(CrawlRule.IGNORE,
                 rule.match("http://www1example.com/"));
  }

  public void testConvertRangeRule() throws LockssRegexpException {
    Vector vec = new Vector(2);
    String key = ConfigParamDescr.ISSUE_RANGE.getKey();
    vec.add(0, "aaa");
    vec.add(1, "hhh");
    configMap.setMapElement(key, vec);

    configProps.add(ConfigParamDescr.ISSUE_RANGE);
    defMap.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS, configProps);

    String rule = "1,\"http://www.example.com/%sissue.html\", " + key;
    CrawlRule crule = cau.convertRule(rule);
    assertEquals(CrawlRule.INCLUDE,
                 crule.match("http://www.example.com/abxissue.html"));
    assertEquals(CrawlRule.IGNORE,
                 crule.match("http://www.example.com/zylophoneissue.html"));
  }
  public void testConvertNumRangeRule() throws LockssRegexpException {
    Vector vec = new Vector(2);
    String key = ConfigParamDescr.NUM_ISSUE_RANGE.getKey();
    vec.add(0, new Long(10));
    vec.add(1, new Long(20));
    configMap.setMapElement(key, vec);

    configProps.add(ConfigParamDescr.NUM_ISSUE_RANGE);
    defMap.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS, configProps);

    String rule = "1,\"http://www.example.com/issue%s.html\", " + key;
    CrawlRule crule = cau.convertRule(rule);
    assertEquals(CrawlRule.INCLUDE,
                 crule.match("http://www.example.com/issue13.html"));
    assertEquals(CrawlRule.IGNORE,
                 crule.match("http://www.example.com/issue44.html"));
  }

  public void testConvertSetRule() throws LockssRegexpException {
    Vector vec = new Vector();
    String key = ConfigParamDescr.ISSUE_SET.getKey();
    vec.add("apple");
    vec.add("banana");
    vec.add("grape");
    vec.add("fig");
    configMap.setMapElement(key, vec);

    configProps.add(ConfigParamDescr.ISSUE_SET);
    defMap.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS, configProps);

    String rule = "1,\"http://www.example.com/%sissue.html\", " + key;
    CrawlRule crule = cau.convertRule(rule);
    assertEquals(CrawlRule.INCLUDE,
                 crule.match("http://www.example.com/appleissue.html"));
    assertEquals(CrawlRule.IGNORE,
                 crule.match("http://www.example.com/orangeissue.html"));
  }

  public void testMakeName() {
    configMap.putString("JOURNAL_NAME", "MyJournal");
    configMap.putInt("VOLUME", 43);
    defMap.putString(DefinableArchivalUnit.KEY_AU_NAME,
                  "\"%s Vol %d\",JOURNAL_NAME,VOLUME");
    String expectedReturn = "MyJournal Vol 43";
    String actualReturn = cau.makeName();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testMakeRules() throws LockssRegexpException {
    configMap.putString("base_url", "http://www.example.com/");
    defMap.putCollection(DefinableArchivalUnit.KEY_AU_CRAWL_RULES, crawlRules);

    CrawlRule rules = cau.makeRules();
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/mygif.gif"));
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/"));
  }

  public void testMakeStartUrl() {
    configMap.putInt("VOLUME", 43);
    configMap.putString("URL", "http://www.example.com/");
    defMap.putString(ArchivalUnit.KEY_AU_START_URL,
                  "\"%slockss-volume/%d.html\", URL, VOLUME");

    String expectedReturn = "http://www.example.com/lockss-volume/43.html";
    String actualReturn = cau.makeStartUrl();
    assertEquals("return value", expectedReturn, actualReturn);
  }

  public void testUserMessage() {
    String str = "test user msg";
    Collection configProps = ListUtil.list(ConfigParamDescr.BASE_URL,
					   ConfigParamDescr.VOLUME_NUMBER);
    defMap.putCollection(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS,
			 configProps);
    defMap.putString(DefinablePlugin.KEY_PLUGIN_AU_CONFIG_USER_MSG, str);
    cau.addImpliedConfigParams();
    assertEquals(str, cau.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));
  }

  public void testGetManifestPage() {

    configMap.putString("HOST", "www.example.com");
    configMap.putInt("YEAR", 2003);
    defMap.putString(DefinableArchivalUnit.KEY_AU_MANIFEST,
            "\"http://%s/contents-by-date.%d.shtml\", HOST, YEAR");
    String expectedReturn = "http://www.example.com/contents-by-date.2003.shtml";
    String actualReturn = (String)cau.getPermissionPages().get(0);
    assertEquals("return valuse", expectedReturn, actualReturn);
  }

  public void testGetLinkExtractor() {
    // test we find the default
    LinkExtractor extractor;
    extractor = cau.getLinkExtractor("text/html");
    assertTrue(extractor instanceof GoslingHtmlLinkExtractor);

    // test we don't find one that doesn't exist
    extractor = cau.getLinkExtractor("text/ram");
    assertNull(extractor);

    // test we find one we've added
    defMap.putString("text/ram_link_extractor_factory",
		     "org.lockss.test.MockLinkExtractorFactory");
    cp.initMimeMap();
    // hard to test wrapper here because getLinkExtractorFactory isn't exposed

    extractor = cau.getLinkExtractor("text/ram");
    assertTrue("extractor is " + extractor,
	       extractor instanceof MockLinkExtractor);
    extractor = cau.getLinkExtractor("text/ram;charset=oddly");
    assertTrue("extractor is " + extractor,
	       extractor instanceof MockLinkExtractor);
  }

  public void testGetCrawlRule() throws LockssRegexpException {
    defMap.putString(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,
 		  "org.lockss.plugin.definable.TestDefinableArchivalUnit$NegativeCrawlRuleFactory");

    CrawlRule rules = cau.makeRules();
    assertEquals(CrawlRule.EXCLUDE,
                 rules.match("http://www.example.com/mygif.gif"));
    assertEquals(CrawlRule.EXCLUDE,
                 rules.match("http://www.example.com/"));

    defMap.putString(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,
		  "org.lockss.plugin.definable.TestDefinableArchivalUnit$PositiveCrawlRuleFactory");

    rules = cau.makeRules();
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/mygif.gif"));
    assertEquals(CrawlRule.INCLUDE,
                 rules.match("http://www.example.com/"));
  }


  public static class NegativeCrawlRuleFactory
    implements CrawlRuleFromAuFactory {

    public CrawlRule createCrawlRule(ArchivalUnit au) {
      return new NegativeCrawlRule();
    }
  }

  public void testGetCrawlRuleThrowsOnBadClass() throws LockssRegexpException {
    defMap.putString(DefinableArchivalUnit.KEY_AU_CRAWL_RULES,
		  "org.lockss.bogus.FakeClass");

    try {
      CrawlRule rules = cau.makeRules();
      fail("Should have thrown on a non-existant class");
    } catch (PluginException.InvalidDefinition e){
    }
  }


  public void testMakePermissionCheckersNone() {
    PermissionChecker permissionChecker = cau.makePermissionChecker();
    assertNull(permissionChecker);
  }

  /**
   * Verify that the permission checker returned by makePermissionChecker
   * is the same class that the factory would make
   */
  public void testMakePermissionCheckers() {
    defMap.putString(DefinableArchivalUnit.KEY_AU_PERMISSION_CHECKER_FACTORY,
 		  "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyPermissionCheckerFactory");
    PermissionChecker permissionChecker = cau.makePermissionChecker();
    assertTrue(permissionChecker instanceof MockPermissionChecker);
  }

  public static class MyPermissionCheckerFactory
    implements PermissionCheckerFactory {
    public List createPermissionCheckers(ArchivalUnit au) {
      List checkers = new ArrayList();
      checkers.add(new MockPermissionChecker());
      return checkers;
    }
  }

  private static class MockPermissionChecker implements PermissionChecker {
    public boolean checkPermission(Crawler.PermissionHelper pHelper,
				   Reader inputReader, String url) {
      throw new UnsupportedOperationException("not implemented");
    }
  }

  public void testMakeLoginPageCheckers() {
    assertNull(cau.makeLoginPageChecker());
  }

  public void testMakeLoginPageChecker() {
    defMap.putString(DefinableArchivalUnit.KEY_AU_LOGIN_PAGE_CHECKER,
 		  "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyLoginPageChecker");
    LoginPageChecker lpc = cau.makeLoginPageChecker();
    assertTrue(lpc instanceof LoginPageCheckerWrapper);
    assertTrue(WrapperUtil.unwrap(lpc) instanceof MyLoginPageChecker);
  }

  public static class MyLoginPageChecker implements LoginPageChecker {
    public boolean isLoginPage(Properties props, Reader reader) {
      throw new UnsupportedOperationException("not implemented");
    }
  }


  /*
  public void testMakeCrawlSpec() throws Exception {
    Properties props = new Properties();
//      props.setProperty(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
//      props.setProperty(ConfigParamDescr.VOLUME_NUMBER.getKey(), "10");
//      props.setProperty(BaseArchivalUnit.PAUSE_TIME_KEY, "10000");
//      props.setProperty(BaseArchivalUnit.USE_CRAWL_WINDOW, "true");
//      props.setProperty(BaseArchivalUnit.NEW_CONTENT_CRAWL_KEY, "10000");
//      Configuration config = ConfigurationUtil.fromProps(props);

     cau.setConfiguration(ConfigurationUtil.fromProps(props));

     defMap.putString("plugin_crawl_type", "HTML Links");
     defMap.putString("au_manifest", "http://www.example.com");
//      defMap.putString("au_start_url", "http://www.example.com");
//     cau = new DefinableArchivalUnit(cp, defMap);

    CrawlSpec spec = cau.makeCrawlSpec();
    assertNotNull("makeCrawlSpec returned a null crawl spec", spec);
    assertTrue("makeCrawlSpec should have returned a SpiderCrawlSpec",
	       spec instanceof SpiderCrawlSpec);
  }
  */
  public void testGetFilterRule() {
    assertNull(cau.getFilterRule(null));
  }

  public void testGetFilterRuleMimeType() {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_RULE,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterRule");
    FilterRule rule = cau.getFilterRule("text/html");
    assertTrue(rule instanceof FilterRuleWrapper);
    assertTrue(WrapperUtil.unwrap(rule) instanceof MyMockFilterRule);
  }

  public void testGetFilterRuleMimeTypeSpace() {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_RULE,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterRule");
    FilterRule rule = cau.getFilterRule(" text/html ");
    assertTrue(rule instanceof FilterRuleWrapper);
    assertTrue(WrapperUtil.unwrap(rule) instanceof MyMockFilterRule);
  }

  public void testGetFilterRuleContentType() {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_RULE,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterRule");
    FilterRule rule = cau.getFilterRule("text/html ; random-char-set");
    assertTrue(rule instanceof FilterRuleWrapper);
    assertTrue(WrapperUtil.unwrap(rule) instanceof MyMockFilterRule);
  }

  public void testGetFilterRuleContentTypeSpace() {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_RULE,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterRule");
    FilterRule rule = cau.getFilterRule(" text/html ; random-char-set");
    assertTrue(rule instanceof FilterRuleWrapper);
    assertTrue(WrapperUtil.unwrap(rule) instanceof MyMockFilterRule);
  }

  public void testGetFilterFactoryMimeType() throws Exception {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_FACTORY,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterFactory");
    cp.initPlugin(getMockLockssDaemon(), defMap);
    FilterFactory fact = cau.getFilterFactory("text/html");
    System.err.println("fact: " + fact);
    assertTrue(fact instanceof FilterFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(fact) instanceof MyMockFilterFactory);
  }

  public void testGetFilterFactoryMimeTypeSpace() throws Exception {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_FACTORY,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterFactory");
    cp.initPlugin(getMockLockssDaemon(), defMap);
    FilterFactory fact = cau.getFilterFactory(" text/html ");
    assertTrue(fact instanceof FilterFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(fact) instanceof MyMockFilterFactory);
  }

  public void testGetFilterFactoryContentType() throws Exception {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_FACTORY,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterFactory");
    cp.initPlugin(getMockLockssDaemon(), defMap);
    FilterFactory fact = cau.getFilterFactory("text/html ; random-char-set");
    assertTrue(fact instanceof FilterFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(fact) instanceof MyMockFilterFactory);
  }

  public void testGetFilterFactoryContentTypeSpace() throws Exception {
    defMap.putString("text/html"+DefinableArchivalUnit.SUFFIX_FILTER_FACTORY,
		     "org.lockss.plugin.definable.TestDefinableArchivalUnit$MyMockFilterFactory");
    cp.initPlugin(getMockLockssDaemon(), defMap);
    FilterFactory fact = cau.getFilterFactory(" text/html ; random-char-set");
    assertTrue(fact instanceof FilterFactoryWrapper);
    assertTrue(WrapperUtil.unwrap(fact) instanceof MyMockFilterFactory);
  }


  public static class PositiveCrawlRuleFactory
    implements CrawlRuleFromAuFactory {

    public CrawlRule createCrawlRule(ArchivalUnit au) {
      return new PositiveCrawlRule();
    }
  }

  public static class MyMockFilterRule implements FilterRule {
    public Reader createFilteredReader(Reader reader) {
      throw new UnsupportedOperationException("not implemented");
    }

  }

  public static class MyMockFilterFactory implements FilterFactory {
    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding) {
      throw new UnsupportedOperationException("not implemented");
    }
  }
}
