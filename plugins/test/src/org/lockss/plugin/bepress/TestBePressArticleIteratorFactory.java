/*
 * $Id: TestBePressArticleIteratorFactory.java,v 1.5 2011-05-18 04:12:38 tlipkis Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.plugin.bepress.BePressArticleIteratorFactory.BePressArticleIterator;
import org.lockss.test.*;
import org.lockss.util.Constants.RegexpContext;

public class TestBePressArticleIteratorFactory extends LockssTestCase {

  private ArchivalUnit bau;

  public void setUp() throws Exception {
    super.setUp();

    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.getPluginManager().setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    daemon.getPluginManager().startService();

    bau =
      PluginTestUtil.createAu("org.lockss.plugin.bepress.BePressPlugin",
			      ConfigurationUtil.fromArgs("base_url",
							 "http://www.example.com/",
							 "journal_abbr", "jour",
							 "volume", "123"));
  }

  Pattern makePattern(ArchivalUnit au) {
    String pat = BePressArticleIteratorFactory.PATTERN_TEMPLATE;

    PrintfConverter.MatchPattern mp =
      PrintfConverter.newRegexpConverter(au, RegexpContext.Url).getMatchPattern(pat);
    return Pattern.compile(mp.getRegexp(), Pattern.CASE_INSENSITIVE);
  }

  public void testUrlsWithPrefixes() throws Exception {
    Pattern pat = makePattern(bau);
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/vol123/iss4/art5");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/default/vol123/iss4/art5");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/vol123/iss4/editorial5");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/default/vol123/iss4/editorial5");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/vol123/iss4/art5");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/default/vol123/iss4/art5");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/vol123/iss4/editorial5");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/default/vol123/iss4/editorial5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol999/iss4/art5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/vol999/iss4/art5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol999/iss4/editorial5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/vol999/iss4/editorial5");
    urlShouldMatch(pat, "http://www.example.com/jour/vol123/iss4/art5");
    urlShouldMatch(pat, "http://www.example.com/jour/default/vol123/iss4/art5");
    urlShouldMatch(pat, "http://www.example.com/jour/vol123/iss4/editorial5");
    urlShouldMatch(pat, "http://www.example.com/jour/default/vol123/iss4/editorial5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/vol123");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/iss4");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/vol123/iss4");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/iss4/art5/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/vol123/iss4/art5/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/iss4/editorial5/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/vol123/iss4/editorial5/wrong");
  }
  
  public void testUrlsWithoutPrefixes() throws Exception {
    Pattern pat = makePattern(bau);
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/123/4/5");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/default/123/4/5");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/123/4/5");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/default/123/4/5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/999/4/5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/999/4/5");
    urlShouldMatch(pat, "http://www.example.com/jour/123/4/5");
    urlShouldMatch(pat, "http://www.example.com/jour/default/123/4/5");
    urlShouldNotMatch(pat, "http://www.example.com/jour/123");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/123");
    urlShouldNotMatch(pat, "http://www.example.com/jour/123/4");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/123/4");
    urlShouldNotMatch(pat, "http://www.example.com/jour/123/4/5/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/default/123/4/5/wrong");
  }
  
  public void testShortArticleUrls() throws Exception {
    Pattern pat = makePattern(bau);
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/vol123/A456");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/vol123/P456");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/vol123/R456");
    urlShouldNotMatch(pat, "http://www.wrong.com/jour/vol123/S456");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/vol123/A456");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/vol123/P456");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/vol123/R456");
    urlShouldNotMatch(pat, "http://www.example.com/wrong/vol123/S456");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol999/A456");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol999/P456");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol999/R456");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol999/S456");
    urlShouldMatch(pat, "http://www.example.com/jour/vol123/A456");
    urlShouldMatch(pat, "http://www.example.com/jour/vol123/P456");
    urlShouldMatch(pat, "http://www.example.com/jour/vol123/R456");
    urlShouldMatch(pat, "http://www.example.com/jour/vol123/S456");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/A456/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/P456/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/R456/wrong");
    urlShouldNotMatch(pat, "http://www.example.com/jour/vol123/S456/wrong");
  }
  
  protected static void urlShouldMatch(Pattern pat, String url)
      throws Exception {
    assertTrue(url + " does not match " + pat.pattern(),
	       pat.matcher(url).find());
  }

  protected static void urlShouldNotMatch(Pattern pat, String url)
      throws Exception {
    assertTrue(url + " matches " + pat.pattern(), !pat.matcher(url).find());
  }
}
