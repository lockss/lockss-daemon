/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.util.*;

import org.apache.oro.text.regex.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.plugin.*;

import static org.lockss.state.SubstanceChecker.State;
import static org.lockss.state.SubstanceChecker.NoSubstanceRedirectUrl;
import static org.lockss.state.SubstanceChecker.NoSubstanceRedirectUrl.*;

public class TestSubstanceChecker extends LockssTestCase {

  List PERMS = ListUtil.list("http://perm/");
  List STARTS = ListUtil.list("http://start/");

  MockArchivalUnit mau;
  SubstanceChecker checker;


  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setStartUrls(STARTS);
    mau.setPermissionUrls(PERMS);
  }

  public void testConfig() throws Exception {
    ConfigurationUtil.addFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_MODE,
				  "None");
    checker = new SubstanceChecker(mau);
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_VOTE));

    ConfigurationUtil.addFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_MODE,
				  "Crawl");
    checker = new SubstanceChecker(mau);
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_VOTE));

    mau.setSubstanceUrlPatterns(RegexpUtil.compileRegexps(ListUtil.list("x")));
    checker = new SubstanceChecker(mau);
    assertTrue(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_VOTE));

    mau.setSubstanceUrlPatterns(null);
    checker = new SubstanceChecker(mau);
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));

    mau.setNonSubstanceUrlPatterns(RegexpUtil.compileRegexps(ListUtil.list("x")));
    checker = new SubstanceChecker(mau);
    assertTrue(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));

    ConfigurationUtil.addFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_MODE,
				  "Crawl");
    checker = new SubstanceChecker(mau);
    assertTrue(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_VOTE));

    ConfigurationUtil.addFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_MODE,
				  "Vote");
    checker = new SubstanceChecker(mau);
    assertFalse(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));
    assertTrue(checker.isEnabledFor(SubstanceChecker.CONTEXT_VOTE));

    ConfigurationUtil.addFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_MODE,
				  "All");
    checker = new SubstanceChecker(mau);
    assertTrue(checker.isEnabledFor(SubstanceChecker.CONTEXT_CRAWL));
    assertTrue(checker.isEnabledFor(SubstanceChecker.CONTEXT_VOTE));
  }

  public void testNoPatterns() {
    checker = new SubstanceChecker(mau);
    assertEquals(State.Unknown, checker.hasSubstance());
  }

  public void testSubst() throws Exception {
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "two" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/");
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testNonSubst() throws Exception {
    mau.setNonSubstanceUrlPatterns(compileRegexps(ListUtil.list("one",
								"two" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://start/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://perm/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://other/");
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testFindSubstNo() throws Exception {
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "two" )));
    List<MockCachedUrl> mcus = new ArrayList<MockCachedUrl>();

    mcus.add(mau.addUrl("http://four/", false, true));
    mcus.add(mau.addUrl("http://three/", false, true));
    mau.populateAuCachedUrlSet();
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.findSubstance());
    assertEquals(State.No, checker.hasSubstance());

    for (MockCachedUrl mcu : mcus) {
      assertFalse("CU left open: " + mcu, mcu.isOpen());
    }
  }

  public void testFindSubstYes() throws Exception {
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "two" )));
    List<MockCachedUrl> mcus = new ArrayList<MockCachedUrl>();
    mcus.add(mau.addUrl("http://four/", false, true));
    mcus.add(mau.addUrl("http://two/", false, true));
    mcus.add(mau.addUrl("http://three/", false, true));
    mau.populateAuCachedUrlSet();
    checker = new SubstanceChecker(mau);
    assertEquals(State.Yes, checker.findSubstance());
    assertEquals(State.Yes, checker.hasSubstance());

    for (MockCachedUrl mcu : mcus) {
      assertFalse("CU left open: " + mcu, mcu.isOpen());
    }
  }

  public void testCountSubst() throws Exception {
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "two" )));
    checker = new SubstanceChecker(mau);
    checker.setSubstanceMin(3);
    assertEquals(State.No, checker.hasSubstance());
    assertEquals(0, checker.getSubstanceCnt());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    assertEquals(0, checker.getSubstanceCnt());
    check("http://two/");
    assertEquals(State.Yes, checker.hasSubstance());
    assertEquals(1, checker.getSubstanceCnt());
    check("http://one/");
    assertEquals(State.Yes, checker.hasSubstance());
    assertEquals(2, checker.getSubstanceCnt());
    check("http://one/");
    assertEquals(State.Yes, checker.hasSubstance());
    assertEquals(3, checker.getSubstanceCnt());
    // should stop testing URL once reaches 3
    check("http://one/");
    assertEquals(State.Yes, checker.hasSubstance());
    assertEquals(3, checker.getSubstanceCnt());
  }

  public void testPluginPredicate() throws Exception {
    mau.setSubstancePredicate(new SubstancePredicate() {
	public boolean isSubstanceUrl(String url) {
	  return url.indexOf("yes") >= 0;
	}});
    checker = new SubstanceChecker(mau);
    assertTrue(checker.isSubstanceUrl("xxyesxx"));
    assertFalse(checker.isSubstanceUrl("xxnoxx"));

    assertEquals(State.No, checker.hasSubstance());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://yes/");
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirSubstLast() throws Exception {
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "redd" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/", "http://reddd/", Last);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirSubstFirst() throws Exception {
    ConfigurationUtil.setFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL, "First");
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "redd" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/", "http://reddd/", First);
    assertEquals(State.No, checker.hasSubstance());
    check("http://reddd/", "http://three/", First);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirSubstAll() throws Exception {
    ConfigurationUtil.setFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL, "All");
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "redd" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/", "http://xxx/", All);
    assertEquals(State.No, checker.hasSubstance());
    check(ListUtil.list("http://frob/", "http://reddd/", "http://three/"), All);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirSubstAll2() throws Exception {
    ConfigurationUtil.setFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL, "All");
    mau.setSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "redd" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://four/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://one/", "http://xxx/", All);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirNonSubstLast() throws Exception {
    mau.setNonSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "redd" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://one/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/", "http://one/", Last);
    assertEquals(State.No, checker.hasSubstance());
    check("http://two/", "http://six/", Last);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirNonSubstFirst() throws Exception {
    ConfigurationUtil.setFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL, "First");
    mau.setNonSubstanceUrlPatterns(compileRegexps(ListUtil.list("one", "redd" )));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://one/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://redd/", "http://two/", First);
    assertEquals(State.No, checker.hasSubstance());
    check("http://three/", "http://one/", First);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  public void testRedirNonSubstAll() throws Exception {
    ConfigurationUtil.setFromArgs(SubstanceChecker.PARAM_DETECT_NO_SUBSTANCE_REDIRECT_URL, "All");
    mau.setNonSubstanceUrlPatterns(compileRegexps(ListUtil.list("one",
								"redd",
								"green")));
    checker = new SubstanceChecker(mau);
    assertEquals(State.No, checker.hasSubstance());
    check("http://one/");
    assertEquals(State.No, checker.hasSubstance());
    check("http://redd/", "http://green/", All);
    assertEquals(State.No, checker.hasSubstance());
    check("http://one/", "http://redddd/", All);
    assertEquals(State.No, checker.hasSubstance());
    check(ListUtil.list("http://one/", "http://redd/", "http://green/"), All);
    assertEquals(State.No, checker.hasSubstance());
    check(ListUtil.list("http://twu/", "http://splortch/", "http://green/"),
	  All);
    assertEquals(State.Yes, checker.hasSubstance());
  }

  void check(String url) {
    MockCachedUrl mcu = new MockCachedUrl(url);
    assertEquals(ListUtil.list(url), checker.getUrlsToCheck(mcu));
    checker.checkSubstance(mcu);
  }

  void check(String url, String redirTo, NoSubstanceRedirectUrl mode) {
    MockCachedUrl first = mau.addUrl(url);
    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_REDIRECTED_TO, redirTo);
    props.put(CachedUrl.PROPERTY_CONTENT_URL, redirTo);
    first.setProperties(props);
    switch (mode) {
    case First:
      assertEquals(ListUtil.list(url), checker.getUrlsToCheck(first));
      break;
    case Last:
      assertEquals(ListUtil.list(redirTo), checker.getUrlsToCheck(first));
      break;
    case All:
      assertEquals(ListUtil.list(url, redirTo), checker.getUrlsToCheck(first));
      break;
    }
    checker.checkSubstance(first);
  }

  void check(List<String> urls, NoSubstanceRedirectUrl mode) {
    List<String> remUrls = new ArrayList(urls);
    String first = remUrls.remove(0);
    String last = urls.get(urls.size() - 1);
    MockCachedUrl firstCu = mau.addUrl(first, "content: " + first);
    List<MockCachedUrl> mcus = new ArrayList<MockCachedUrl>();
    mcus.add(firstCu);
    MockCachedUrl mcu = firstCu;

    for (String url : remUrls) {
      CIProperties props = new CIProperties();
      props.put(CachedUrl.PROPERTY_REDIRECTED_TO, url);
      props.put(CachedUrl.PROPERTY_CONTENT_URL, last);
      mcu.setProperties(props);
      props = new CIProperties();
      mcu = mau.addUrl(url, "content: " + url);
      mcus.add(mcu);
    }
    switch (mode) {
    case First:
      assertEquals(ListUtil.list(first), checker.getUrlsToCheck(firstCu));
      break;
    case Last:
      assertEquals(ListUtil.list(last), checker.getUrlsToCheck(firstCu));
      break;
    case All:
      assertEquals(urls, checker.getUrlsToCheck(firstCu));
      break;
    }
    checker.checkSubstance(firstCu);
    for (MockCachedUrl amcu : mcus) {
      if (amcu != firstCu) {
	assertFalse("CU left open: " + amcu, amcu.isOpen());
      };
    }      
  }

  List<Pattern> compileRegexps(List<String> regexps)
      throws MalformedPatternException {
    return RegexpUtil.compileRegexps(regexps);
  }

}
