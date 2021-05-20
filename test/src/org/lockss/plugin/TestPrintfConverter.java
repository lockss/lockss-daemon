/*
 * $Id$
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

package org.lockss.plugin;

import java.util.*;

import org.lockss.test.*;
import org.lockss.config.Configuration;
import static org.lockss.daemon.ConfigParamDescr.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.Constants.RegexpContext;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.base.*;

public class TestPrintfConverter extends LockssTestCase {

  private static final String BASE = "http://foo.bar:8080/";

  List configProps;
  MockPlugin mplug;
  MockArchivalUnit mau;
  Configuration auconf;

  private static final ConfigParamDescr PAREN_PARAM =
    new ConfigParamDescr()
    .setKey("paren_param")
    .setDisplayName("Paren param")
    .setType(TYPE_STRING)
    .setSize(20);

  protected void setUp() throws Exception {
    super.setUp();

    // Use a descr of every type
    configProps = ListUtil.list(BASE_URL, // url
                                VOLUME_NUMBER, // pos int
                                VOLUME_NAME,   // string
				ISSUE_RANGE,   // string range
				NUM_ISSUE_RANGE, // int range
				ISSUE_SET,	 // set
				YEAR,		 // year
				PUB_DOWN,	 // boolean
				PAREN_PARAM	 // string
				);
    mplug = new MockPlugin();
    mau = new MockArchivalUnit(mplug);
  }

  public MockArchivalUnit setupAu(Configuration auconf) 
      throws ConfigurationException {
    mplug.setAuConfigDescrs(configProps);
    mau.setConfiguration(auconf);
    loadAuConfigDescrs(auconf);
    return mau;
  }

  PrintfConverter.MatchPattern
    convertVariableRegexpString(String printfString) {
    return PrintfConverter.newRegexpConverter(mau, RegexpContext.String).getMatchPattern(printfString);
  }

  PrintfConverter.MatchPattern
    convertVariableUrlRegexpString(String printfString) {
    return PrintfConverter.newRegexpConverter(mau, RegexpContext.Url).getMatchPattern(printfString);
  }

  List<String> convertUrlList(String printfString) {
    return PrintfConverter.newUrlListConverter(mau).getUrlList(printfString);
  }

  String convertNameString(String printfString) {
    return PrintfConverter.newNameConverter(mau).getName(printfString);
  }

  // copied from BasePlugin.  printf processing retrieves param values from
  // typed paramMap
  protected void loadAuConfigDescrs(Configuration config) throws
      ConfigurationException {
    TypedEntryMap paramMap = mau.getProperties();
    for (Iterator it = mplug.getAuConfigDescrs().iterator(); it.hasNext() ;) {
      ConfigParamDescr descr = (ConfigParamDescr) it.next();
      String key = descr.getKey();
      if (config.containsKey(key)) {
	try {
	  Object val = descr.getValueOfType(config.get(key));
	  paramMap.setMapElement(key, val);
	} catch (Exception ex) {
	  throw new ConfigurationException("Error configuring: " + key, ex);
	}
      }
    }
  }

  Configuration conf1() {
    Configuration conf =
      ConfigurationUtil.fromArgs("base_url", BASE,
				 "volume", "123"); // 
    conf.put("volume_name", "vol.name");
    conf.put("issue_range", "a-d");
    conf.put("num_issue_range", "9-12");
    conf.put("issue_set", "Jan,Feb,Mar,Apr");
    conf.put("year", "2018");
    conf.put("pub_down", "true");
    conf.put("paren_param", "aa(bb)cc");
    return conf;
  }

  public void testConvertUrlList() throws Exception {
    setupAu(conf1());

    assertEquals(ListUtil.list("http://no.args/blah"),
		 convertUrlList("http://no.args/blah"));
    assertEquals(ListUtil.list(BASE+"blah"),
		 convertUrlList("\"%sblah\", base_url"));
    assertEquals(ListUtil.list(BASE+"blah/v_123/y2018"),
		 convertUrlList("\"%sblah/v_%d/y%d\", base_url, volume, year"));
    assertEquals(ListUtil.list(BASE+"blah/v_vol.name/y2018"),
		 convertUrlList("\"%sblah/v_%s/y%d\", base_url, volume_name, year"));

    assertEquals(ListUtil.list("http://foo.bar:8080/blah/i_9/foo",
				  "http://foo.bar:8080/blah/i_10/foo",
				  "http://foo.bar:8080/blah/i_11/foo",
				  "http://foo.bar:8080/blah/i_12/foo"),
		 convertUrlList("\"%sblah/i_%d/foo\", base_url, num_issue_range"));


    assertEquals(ListUtil.list("http://foo.bar:8080/blah/i_9/foo9",
			       "http://foo.bar:8080/blah/i_10/foo9",
			       "http://foo.bar:8080/blah/i_11/foo9",
			       "http://foo.bar:8080/blah/i_12/foo9",
			       "http://foo.bar:8080/blah/i_9/foo10",
			       "http://foo.bar:8080/blah/i_10/foo10",
			       "http://foo.bar:8080/blah/i_11/foo10",
			       "http://foo.bar:8080/blah/i_12/foo10",
			       "http://foo.bar:8080/blah/i_9/foo11",
			       "http://foo.bar:8080/blah/i_10/foo11",
			       "http://foo.bar:8080/blah/i_11/foo11",
			       "http://foo.bar:8080/blah/i_12/foo11",
			       "http://foo.bar:8080/blah/i_9/foo12",
			       "http://foo.bar:8080/blah/i_10/foo12",
			       "http://foo.bar:8080/blah/i_11/foo12",
			       "http://foo.bar:8080/blah/i_12/foo12"),
		 convertUrlList("\"%sblah/i_%d/foo%d\", base_url, num_issue_range, num_issue_range"));


    assertEquals(ListUtil.list("http://foo.bar:8080/blah/i_Jan/foo",
				  "http://foo.bar:8080/blah/i_Feb/foo",
				  "http://foo.bar:8080/blah/i_Mar/foo",
				  "http://foo.bar:8080/blah/i_Apr/foo"),
		 convertUrlList("\"%sblah/i_%s/foo\", base_url, issue_set"));

    assertEquals(ListUtil.list("http://foo.bar:8080/blah/i_Jan/foo9",
			       "http://foo.bar:8080/blah/i_Feb/foo9",
			       "http://foo.bar:8080/blah/i_Mar/foo9",
			       "http://foo.bar:8080/blah/i_Apr/foo9",
			       "http://foo.bar:8080/blah/i_Jan/foo10",
			       "http://foo.bar:8080/blah/i_Feb/foo10",
			       "http://foo.bar:8080/blah/i_Mar/foo10",
			       "http://foo.bar:8080/blah/i_Apr/foo10",
			       "http://foo.bar:8080/blah/i_Jan/foo11",
			       "http://foo.bar:8080/blah/i_Feb/foo11",
			       "http://foo.bar:8080/blah/i_Mar/foo11",
			       "http://foo.bar:8080/blah/i_Apr/foo11",
			       "http://foo.bar:8080/blah/i_Jan/foo12",
			       "http://foo.bar:8080/blah/i_Feb/foo12",
			       "http://foo.bar:8080/blah/i_Mar/foo12",
			       "http://foo.bar:8080/blah/i_Apr/foo12"),
		 convertUrlList("\"%sblah/i_%s/foo%d\", base_url, issue_set, num_issue_range"));


    try {
      convertUrlList("\"%sblah/i_%d/foo\", base_url, issue_range");
      fail("issue range in url string is illegal");
    } catch (PluginException.InvalidDefinition e) {
    }
  }

  public void testConvertName() throws Exception {
    setupAu(conf1());

    assertEquals("no args",
		 convertNameString("no args"));
    assertEquals("Base is "+BASE+"blah",
		 convertNameString("\"Base is %sblah\", base_url"));
    assertEquals(BASE+"blah/v_123/y2018",
		 convertNameString("\"%sblah/v_%d/y%d\", base_url, volume, year"));
    assertEquals("Vol: vol.name Year: 2018",
		 convertNameString("\"Vol: %s Year: %d\", volume_name, year"));

    assertEquals("2018, Issues 9-12",
		 convertNameString("\"%i, Issues %s\", year, num_issue_range"));
    assertEquals("2018, Issues Jan, Feb, Mar, Apr",
		 convertNameString("\"%i, Issues %s\", year, issue_set"));
    try {
      convertNameString("\"%sblah/i_%d/foo\", base_url, issue_range");
      fail("issue range in url string is illegal");
    } catch (PluginException.InvalidDefinition e) {
    }
  }

  public void testConvertRegexp() throws Exception {
    setupAu(conf1());

    String s1 = "http\\:\\/\\/foo\\.bar\\:8080\\/";
    PrintfConverter.MatchPattern mp;

    mp = convertVariableRegexpString("blah/.*");
    assertEquals("blah/.*", mp.getRegexp());
    assertEmpty(mp.getMatchArgs());
    assertEmpty(mp.getMatchArgTypes());

    mp = convertVariableRegexpString("\"%sblah\", base_url");
    assertEquals(s1+"blah", mp.getRegexp());
    assertEmpty(mp.getMatchArgs());
    assertEmpty(mp.getMatchArgTypes());

    mp = convertVariableRegexpString("\"%svol_%s\\?bool=%s\", base_url, volume_name, pub_down");
    assertEquals(s1+"vol_vol\\.name\\?bool=true", mp.getRegexp());
    assertEmpty(mp.getMatchArgs());
    assertEmpty(mp.getMatchArgTypes());

    mp = convertVariableRegexpString("\"%svol_%s\", base_url, paren_param");
    assertEquals(s1+"vol_aa\\(bb\\)cc", mp.getRegexp());
    assertEmpty(mp.getMatchArgs());
    assertEmpty(mp.getMatchArgTypes());

    mp = convertVariableRegexpString("\"%sissue%s/foo\", base_url, issue_range");
    assertEquals(s1+"issue(.*)/foo", mp.getRegexp());
    assertEquals(ListUtil.list(ListUtil.list("a", "d")), mp.getMatchArgs());
    assertEquals(ListUtil.list(AuParamType.Range), mp.getMatchArgTypes());

    mp = convertVariableRegexpString("\"%sissue%s/foo\", base_url, num_issue_range");
    assertEquals(s1+"issue(\\d+)/foo", mp.getRegexp());

    assertEquals(ListUtil.list(ListUtil.list(9L, 12L)), mp.getMatchArgs());
    assertEquals(ListUtil.list(AuParamType.NumRange), mp.getMatchArgTypes());

    mp = convertVariableRegexpString("\"%sissue_%s/foo\", base_url, issue_set");
    assertEquals(s1+"issue_(?:Jan|Feb|Mar|Apr)/foo", mp.getRegexp());
    assertEmpty(mp.getMatchArgs());
    assertEmpty(mp.getMatchArgTypes());
  }

  public void testConvertRegexpWithBlanks() throws Exception {
    Configuration c = conf1();
    String base = BASE + "embedded space";
    c.put("base_url", base);
    setupAu(c);
    Configuration conf =
      ConfigurationUtil.fromArgs("base_url", base,
				 "volume", "123"); // 

    String s1 = "http\\:\\/\\/foo\\.bar\\:8080\\/embedded\\ space";
    PrintfConverter.MatchPattern mp;

    mp = convertVariableRegexpString("\"%s/blah\", base_url");
    assertEquals(s1+"/blah", mp.getRegexp());
    assertMatchesRE(mp.getRegexp(),
		    "http://foo.bar:8080/embedded space/blah/A");
    assertNotMatchesRE(mp.getRegexp(),
		    "http://foo.bar:8080/embedded+space/blah/B");
    assertNotMatchesRE(mp.getRegexp(),
		    "http://foo.bar:8080/embedded%20space/blah/C");
  }

  public void testConvertUrlRegexpWithBlanks() throws Exception {
    Configuration c = conf1();
    String base = BASE + "embedded space";
    c.put("base_url", base);
    setupAu(c);
    Configuration conf =
      ConfigurationUtil.fromArgs("base_url", base,
				 "volume", "123"); // 

    String s1 = "http\\:\\/\\/foo\\.bar\\:8080\\/embedded( |\\+|%20)space";
    PrintfConverter.MatchPattern mp;

    mp = convertVariableUrlRegexpString("\"%s/blah\", base_url");
    assertEquals(s1+"/blah", mp.getRegexp());
    assertMatchesRE(mp.getRegexp(),
		    "http://foo.bar:8080/embedded space/blah/A");
    assertMatchesRE(mp.getRegexp(),
		    "http://foo.bar:8080/embedded+space/blah/B");
    assertMatchesRE(mp.getRegexp(),
		    "http://foo.bar:8080/embedded%20space/blah/C");
  }

  public void testDefaultFunctor() throws Exception {
    setupAu(conf1());

    assertEquals("Base is http://www.foo.bar:8080/blah",
		 convertNameString("\"Base is %sblah\", add_www(base_url)"));
    assertEquals("Base is http://foo.bar:8080/blah",
		 convertNameString("\"Base is %sblah\", del_www(base_url)"));
    try {
      assertEquals("", convertNameString("\"a %s blah\", un_def(base_url)"));
      fail("Undefined function should throw");
    } catch (PluginException.InvalidDefinition e) {
      assertEquals("Undefined function: un_def", e.getMessage());
    }
  }

  public void testNonDefaultFunctor() throws Exception {
    mplug.setAuParamFunctor(new TestFunctor());
    setupAu(conf1());

    assertIsomorphic(ListUtil.list("http://foo.bar:8080/blah/i_Feb/foo",
				   "http://foo.bar:8080/blah/i_Apr/foo"),
		 convertUrlList("\"%sblah/i_%s/foo\", base_url, filter_set(issue_set)"));

    // Ensure delegates to BaseAuParamFunctor
    assertEquals("Base is http://www.foo.bar:8080/blah",
		 convertNameString("\"Base is %sblah\", add_www(base_url)"));
  }

  public static class TestFunctor extends BaseAuParamFunctor {
    @Override
    public Object apply(AuParamFunctor.FunctorData fd, String fn,
			Object arg, AuParamType type)
	throws PluginException {
      if (fn.equals("filter_set")) {
	List<String> lst = (List<String>)arg;
	boolean flg = false;
	for (ListIterator iter = lst.listIterator(); iter.hasNext();) {
	  iter.next();
	  if (flg = !flg) {
	    iter.remove();
	  }
	}
	return lst;
      }
      return super.apply(fd, fn, arg, type);
    }

    @Override
    public AuParamType type(AuParamFunctor.FunctorData fd, String fn) {
      if (fn.equals("filter_set")) {
	return AuParamType.Set;
      }
      return super.type(fd, fn);
    }
  }


}
