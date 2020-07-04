/*
 * $Id$
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

package org.lockss.util;

import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.daemon.*;

public class TestUrlUtil extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestUrlUtil.class);

  // For testing against the behavior of code extracted from Java 1.4 URI class
  static String normalizePath(String path) {
    switch (1) {
    case 1:
      try {
	return UrlUtil.normalizePath(path,
				     UrlUtil.PATH_TRAVERSAL_ACTION_ALLOW);
      } catch (MalformedURLException e) {
	throw new RuntimeException(e.toString());
      }
    case 2:
      try {
	Object urin =
	  PrivilegedAccessor.invokeConstructor("JavaUriNormalizer");
	return (String)PrivilegedAccessor.invokeMethod(urin,
						       "normalizePath",
						       ListUtil.list(path).toArray());
      } catch (Exception e) {
	log.warning("Couldn't invoke JavaUriNormalizer", e);
	return null;
      }
    }
    throw new RuntimeException();
  }

  public void assertSameNormalizePath(String s) {
    assertSame(s, normalizePath(s));
  }

  public void testNormalizeUrlEncodingCase() throws Exception {
    assertSame("", UrlUtil.normalizeUrlEncodingCase(""));
    assertSame("a", UrlUtil.normalizeUrlEncodingCase("a"));
    assertSame("aB", UrlUtil.normalizeUrlEncodingCase("aB"));
    assertEquals("%33", UrlUtil.normalizeUrlEncodingCase("%33"));
    assertEquals("%AB%CD", UrlUtil.normalizeUrlEncodingCase("%ab%cd"));
    assertEquals("foobar%d", UrlUtil.normalizeUrlEncodingCase("foobar%d"));
    assertEquals("foobar%3D", UrlUtil.normalizeUrlEncodingCase("foobar%3d"));
    assertEquals("%3Dfoobar", UrlUtil.normalizeUrlEncodingCase("%3dfoobar"));
    assertEquals("%3Dfoobar%3D",
		 UrlUtil.normalizeUrlEncodingCase("%3dfoobar%3d"));
    assertEquals("foo%3Dbar", UrlUtil.normalizeUrlEncodingCase("foo%3Dbar"));
    assertEquals("foo%3Dbar", UrlUtil.normalizeUrlEncodingCase("foo%3dbar"));
  }

  public void testNormalizePath() throws Exception {
    assertSameNormalizePath("");
    assertSameNormalizePath("a");
    assertSameNormalizePath("a/");
    assertSameNormalizePath("/a");
    assertSameNormalizePath("/");
    assertSameNormalizePath("/a/b/");
    assertSameNormalizePath("/a/.b/");
    assertSameNormalizePath("/a/b./");
    assertSameNormalizePath("/a/..b/");
    assertSameNormalizePath("/a/b../");
    assertSameNormalizePath("/a/.b");
    assertSameNormalizePath("/a/b.");
    assertSameNormalizePath("/a/..b");
    assertSameNormalizePath("/a/b..");
    assertSameNormalizePath("/a/b/c/");

    assertEquals("", normalizePath("."));
    assertEquals("..", normalizePath(".."));
    assertEquals("/", normalizePath("/."));
    assertEquals("", normalizePath("./"));
    assertEquals("../", normalizePath("../"));
    assertEquals("/..", normalizePath("/.."));
    assertEquals("../a", normalizePath("../a"));
    assertEquals("/..", normalizePath("/a/../.."));

    assertEquals("/a/b", normalizePath("/a/./b"));
    assertEquals("/a/b", normalizePath("/a/c/../b"));
    assertEquals("/a/b", normalizePath("//a/b"));
    assertEquals("/a/b", normalizePath("//a//b"));
    assertEquals("/a/b", normalizePath("//a///b"));

    assertEquals("/a/b/", normalizePath("/a/./b/"));
    assertEquals("/a/b/", normalizePath("/a/c/../b/"));
    assertEquals("/a/b/", normalizePath("//a/b/"));
    assertEquals("/a/b/", normalizePath("//a//b/"));
    assertEquals("/a/b/", normalizePath("//a///b/"));

    assertEquals("a/b", normalizePath("a/./b"));
    assertEquals("a/b", normalizePath("a/c/../b"));
    assertEquals("a/b", normalizePath("a/c/./../b"));
    assertEquals("a/b", normalizePath("a/c/./.././b"));
    assertEquals("a/b", normalizePath("a//b"));
    assertEquals("a/b", normalizePath("a///b"));

    assertEquals("/", normalizePath("/a/.."));
    assertEquals("/a/", normalizePath("/a/b/.."));
    assertEquals("/a/", normalizePath("/a/b/../"));
    assertEquals("/", normalizePath("/a/b/../.."));
    assertEquals("/", normalizePath("/a/b/../../"));

    assertEquals("/a/b", normalizePath("/a/c/d/../../b"));
    assertEquals("/a/", normalizePath("/a/c/d/../../b/../"));
    assertEquals("/a/", normalizePath("/a/c/d/../../b/.."));
    assertEquals("/..", normalizePath("/a/c/../../.."));
    assertEquals("/../..", normalizePath("/a/c/../../../.."));
    assertEquals("/../", normalizePath("/a/c/../../../"));
    assertEquals("/../../", normalizePath("/a/c/../../../../"));
    assertEquals("/", normalizePath("/a/.."));

    assertEquals("/../x", normalizePath("/a/c/../../../x"));
  }

  public void testNormalizePathTraversalAccept() throws Exception {
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				  "1");
    assertEquals("..", UrlUtil.normalizePath(".."));
    assertEquals("../", UrlUtil.normalizePath("../"));
    assertEquals("/..", UrlUtil.normalizePath("/.."));
    assertEquals("../a", UrlUtil.normalizePath("../a"));
    assertEquals("/..", UrlUtil.normalizePath("/a/../.."));
    assertEquals("/a/b", UrlUtil.normalizePath("/a/c/../b"));
    assertEquals("/a/b/", UrlUtil.normalizePath("/a/c/../b/"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/../b"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/./../b"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/./.././b"));
    assertEquals("/", UrlUtil.normalizePath("/a/.."));
    assertEquals("/a/", UrlUtil.normalizePath("/a/b/.."));
    assertEquals("/a/", UrlUtil.normalizePath("/a/b/../"));
    assertEquals("/", UrlUtil.normalizePath("/a/b/../.."));
    assertEquals("/", UrlUtil.normalizePath("/a/b/../../"));
    assertEquals("/a/b", UrlUtil.normalizePath("/a/c/d/../../b"));
    assertEquals("/a/", UrlUtil.normalizePath("/a/c/d/../../b/../"));
    assertEquals("/a/", UrlUtil.normalizePath("/a/c/d/../../b/.."));
    assertEquals("/..", UrlUtil.normalizePath("/a/c/../../.."));
    assertEquals("/../..", UrlUtil.normalizePath("/a/c/../../../.."));
    assertEquals("/../", UrlUtil.normalizePath("/a/c/../../../"));
    assertEquals("/../../", UrlUtil.normalizePath("/a/c/../../../../"));
    assertEquals("/", UrlUtil.normalizePath("/a/.."));
    assertEquals("/../x", UrlUtil.normalizePath("/a/c/../../../x"));
  }

  public void testNormalizePathTraversalRemove() throws Exception {
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				  "2");
    assertEquals("", UrlUtil.normalizePath(".."));
    assertEquals("/", UrlUtil.normalizePath("../"));
    assertEquals("/", UrlUtil.normalizePath("/.."));
    assertEquals("a", UrlUtil.normalizePath("../a"));
    assertEquals("/", UrlUtil.normalizePath("/a/../.."));
    assertEquals("/a/b", UrlUtil.normalizePath("/a/c/../b"));
    assertEquals("/a/b/", UrlUtil.normalizePath("/a/c/../b/"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/../b"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/./../b"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/./.././b"));
    assertEquals("/", UrlUtil.normalizePath("/a/.."));
    assertEquals("/a/", UrlUtil.normalizePath("/a/b/.."));
    assertEquals("/a/", UrlUtil.normalizePath("/a/b/../"));
    assertEquals("/", UrlUtil.normalizePath("/a/b/../.."));
    assertEquals("/", UrlUtil.normalizePath("/a/b/../../"));
    assertEquals("/a/b", UrlUtil.normalizePath("/a/c/d/../../b"));
    assertEquals("/a/", UrlUtil.normalizePath("/a/c/d/../../b/../"));
    assertEquals("/a/", UrlUtil.normalizePath("/a/c/d/../../b/.."));
    assertEquals("/", UrlUtil.normalizePath("/a/c/../../.."));
    assertEquals("/", UrlUtil.normalizePath("/a/c/../../../.."));
    assertEquals("/", UrlUtil.normalizePath("/a/c/../../../"));
    assertEquals("/", UrlUtil.normalizePath("/a/c/../../../../"));
    assertEquals("/", UrlUtil.normalizePath("/a/.."));
    assertEquals("/x", UrlUtil.normalizePath("/a/c/../../../x"));
  }

  void assertNormalizePathThrows(String path) {
    try {
      UrlUtil.normalizePath(path);
      fail("normalizePath("+path+") should throw, returned " +
	   UrlUtil.normalizePath(path));
    } catch (MalformedURLException e) {
    }
  }

  public void testNormalizePathTraversalThrow() throws Exception {
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				  "3");
    assertNormalizePathThrows("..");
    assertNormalizePathThrows("../");
    assertNormalizePathThrows("/..");
    assertNormalizePathThrows("../a");
    assertNormalizePathThrows("/a/../..");
    assertEquals("/a/b", UrlUtil.normalizePath("/a/c/../b"));
    assertEquals("/a/b/", UrlUtil.normalizePath("/a/c/../b/"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/../b"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/./../b"));
    assertEquals("a/b", UrlUtil.normalizePath("a/c/./.././b"));
    assertEquals("/", UrlUtil.normalizePath("/a/.."));
    assertEquals("/a/", UrlUtil.normalizePath("/a/b/.."));
    assertEquals("/a/", UrlUtil.normalizePath("/a/b/../"));
    assertEquals("/", UrlUtil.normalizePath("/a/b/../.."));
    assertEquals("/", UrlUtil.normalizePath("/a/b/../../"));
    assertEquals("/a/b", UrlUtil.normalizePath("/a/c/d/../../b"));
    assertEquals("/a/", UrlUtil.normalizePath("/a/c/d/../../b/../"));
    assertEquals("/a/", UrlUtil.normalizePath("/a/c/d/../../b/.."));
    assertNormalizePathThrows("/a/c/../../..");
    assertNormalizePathThrows("/a/c/../../../..");
    assertNormalizePathThrows("/a/c/../../../");
    assertNormalizePathThrows("/a/c/../../../../");
    assertEquals("/", UrlUtil.normalizePath("/a/.."));
    assertNormalizePathThrows("/a/c/../../../x");
  }

  public void assertSameNormalizeUrl(String s) throws MalformedURLException {
    assertSame(s, UrlUtil.normalizeUrl(s));
  }

  public void testNormalizeUrl() throws MalformedURLException {
    assertSameNormalizeUrl("http://a/b");

    assertEquals("http://a.com/b", UrlUtil.normalizeUrl("HTTP://A.COM/b"));
    assertEquals("http://a.com/B", UrlUtil.normalizeUrl("HTTP://A.COM/B"));

    assertEquals("http://a.com/", UrlUtil.normalizeUrl("http://a.com/"));
    assertEquals("http://a.com/", UrlUtil.normalizeUrl("http://a.com"));
    assertEquals("http://a.com/xy", UrlUtil.normalizeUrl("http://a.com/xy"));
    assertEquals("http://a.com/xy/", UrlUtil.normalizeUrl("http://a.com/xy/"));
    assertEquals("http://a.com/xy/",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/.."));
    assertEquals("http://a.com/xy/",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../"));
    assertEquals("http://a.com/",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../"));
    // port
    assertEquals("http://a.com/b",
		 UrlUtil.normalizeUrl("HTTP://A.COM:80/b"));
    assertEquals("http://a.com:8000/b",
		 UrlUtil.normalizeUrl("HTTP://A.COM:8000/b"));
    assertEquals("ftp://example.com/",
		 UrlUtil.normalizeUrl("ftp://example.com:21/"));
    // query not removed
    assertEquals("http://a.com/dd?foo=bar",
		 UrlUtil.normalizeUrl("http://a.com/dd?foo=bar"));

    // no path normalization of query string
    assertEquals("http://a.b/foo/bar?foo//bar",
		 UrlUtil.normalizeUrl("http://a.b/foo//bar?foo//bar"));
    assertEquals("http://a.b/bar?foo/../bar",
		 UrlUtil.normalizeUrl("http://a.b/foo/../bar?foo/../bar"));

    // remove newlines and leading whitespace
    assertEquals("http://a.b/foo/bar?foo//bar",
		 UrlUtil.normalizeUrl("ht\ntp://a.b/foo//bar?foo//bar"));
    assertEquals("http://a .b/bar?foo/../bar",
		 UrlUtil.normalizeUrl("  ht\n   tp://a .b/foo/../bar?foo/../bar"));

    assertEquals("http://a.b/bar%4Ffoo",
		 UrlUtil.normalizeUrl("http://a.b/bar%4ffoo"));
    assertEquals("http://a.b/bar%4Ffoo",
		 UrlUtil.normalizeUrl("http://a.b/bar%4Ffoo"));
    assertEquals("http://a.b/bar%4Ffoo?x=y%5Bz%5D",
		 UrlUtil.normalizeUrl("http://a.b/bar%4ffoo?x=y%5bz%5d"));
    assertEquals("http://a.b/bar%4Ffoo?x=y%5Bz%5D",
		 UrlUtil.normalizeUrl("http://a.b/bar%4ffoo?x=y%5Bz%5D"));

    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				  "1");
    assertMode1();
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				  "2");
    assertMode2();
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_PATH_TRAVERSAL_ACTION,
				  "3");
    assertMode3();

    // Empty query removal

    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_NORMALIZE_EMPTY_QUERY,
				  "false");
    assertEquals("http://a.b/bar?", UrlUtil.normalizeUrl("http://a.b/bar?"));
    assertEquals("http://a.b/ba?r", UrlUtil.normalizeUrl("http://a.b/ba?r"));
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_NORMALIZE_EMPTY_QUERY,
				  "true");
    assertEquals("http://a.b/bar", UrlUtil.normalizeUrl("http://a.b/bar?"));
    assertEquals("http://a.b/ba?r", UrlUtil.normalizeUrl("http://a.b/ba?r"));
  }

  // mode 1 leaves extra ".."s alone.
  void assertMode1() throws MalformedURLException {
    assertEquals("http://a.com/../",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../"));
    assertEquals("http://a.com/../../xxx",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../../xxx"));
    assertEquals("http://a.com/../a/b/c/d",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../a/b/c/d"));
    assertEquals("http://a.com/../a/b/c/d/",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../a/b/c/d/"));
  }

  // mode 2 removes extra ".."s.
  void assertMode2() throws MalformedURLException {
    assertEquals("http://a.com/",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../"));
    assertEquals("http://a.com/a/b/c/d",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../a/b/c/d"));
    assertEquals("http://a.com/a/b/c/d/",
		 UrlUtil.normalizeUrl("http://a.com/xy/ab/../../../a/b/c/d/"));

  }

  // mode 3 throws on extra ".."s.
  void assertMode3() throws MalformedURLException {
    try {
      String s = "http://a.com/xy/ab/../../../";
      UrlUtil.normalizeUrl(s);
      fail(s);
    } catch (MalformedURLException e) {
    }
    try {
      String s = "http://a.com/xy/ab/../../../a/b/c/d";
      UrlUtil.normalizeUrl(s);
      fail(s);
    } catch (MalformedURLException e) {
    }
  }

  public void testNormalizeUrlQueryString() throws MalformedURLException {
    assertEquals("http://a.com/dd?foo=bar",
		 UrlUtil.normalizeUrl("http://A.com/dd?foo=bar"));
  }

  public void testNormalizeUrlRemovesHash() throws MalformedURLException {
    assertEquals("http://a.com/xy",
		 UrlUtil.normalizeUrl("http://a.com/xy#blah"));

    assertEquals("http://www.bioone.org/perlserv/?request=archive-lockss&issn=0044-7447&volume=033",
		 UrlUtil.normalizeUrl("http://www.bioone.org/perlserv/?request=archive-lockss&issn=0044-7447&volume=033#content"));
  }

  void assertIllegalNormalization(String url, MockArchivalUnit au)
      throws MalformedURLException{
    try {
      String norm = UrlUtil.normalizeUrl(url, au);
      fail("siteNormalizeUrl(" + url + ") should throw, but was " + norm);
    } catch (PluginBehaviorException e) {
    }
  }

  public void testNormalizeSite()
      throws MalformedURLException, PluginBehaviorException {
    MockArchivalUnit mau = new MockArchivalUnit();

    Map normMap = org.apache.commons.lang3.ArrayUtils.toMap(new String[][] {
	{"http://a.com/spurious/file", "http://a.com/file"},
	{"http://a.com:80/path/file", "http://a.com/path/file"},
	{"http://a.com/path/file", "http://a.com:80/path/file"},
	{"https://a.com:443/path/file", "https://a.com/path/file"},
	{"https://a.com/path3/file", "https://a.com:443/path/file"},

	// Stem changes, illegal unless allowSiteNormalizeChangeStem and
	// new stem in au.getUrlStems()
	{"http://host1.com/path/file", "http://host22.com/path/file"},
	{"http://host2.org:8080/path/file", "http://host2.org/path/file"},
	{"http://host2.com/path2/file", "http://host2.com:8080/path2/file"},
	{"http://host3.com/path2/file", "http://host3.com:8080/path2/file"},
	{"http://scheme.com/path/file", "https://scheme.com/path/file"},
	{"http://scheme.host/path/file", "https://host.scheme/path/file"},
      });

    mau.setUrlNormalizeMap(normMap);

    String s = "http://a.com/b";
    assertSame(s, UrlUtil.normalizeUrl(s, mau));
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));
    assertEquals("http://a.com/spUrious/file",
		 UrlUtil.normalizeUrl("http://a.com/spUrious/file", mau));
    assertEquals("http://a.com/path/file",
		 UrlUtil.normalizeUrl("http://a.com:80/path/file", mau));
    assertEquals("http://a.com/path/file",
		 UrlUtil.normalizeUrl("http://a.com/path/file", mau));
    assertEquals("https://a.com/path/file",
		 UrlUtil.normalizeUrl("https://a.com:443/path/file", mau));
    assertEquals("https://a.com/path/file",
		 UrlUtil.normalizeUrl("https://a.com/path3/file", mau));
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));


    // allowSiteNormalizeChangeStem is true by default but the AU has no
    // stmes so these are all illegal
    assertEmpty(mau.getUrlStems());

    assertIllegalNormalization("http://host1.com/path/file", mau);
    assertIllegalNormalization("http://host2.com/path2/file", mau);
    assertIllegalNormalization("http://host3.com/path2/file", mau);
    assertIllegalNormalization("http://host2.org:8080/path/file", mau);
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));

    // Add just the original stems, still all illegal
    mau.setUrlStems(ListUtil.list("http://host1.com/",
				  "http://host2.com/",
				  "http://host3.com/",
				  "http://host2.org:8080/"));

    assertIllegalNormalization("http://host1.com/path/file", mau);
    assertIllegalNormalization("http://host2.com/path2/file", mau);
    assertIllegalNormalization("http://host3.com/path2/file", mau);
    assertIllegalNormalization("http://host2.org:8080/path/file", mau);
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));

    // Replace with just the normalized stems, still all illegal
    mau.setUrlStems(ListUtil.list("http://host22.com/",
				  "http://host2.org:8080/"));
    assertIllegalNormalization("http://host1.com/path/file", mau);
    assertIllegalNormalization("http://host2.com/path2/file", mau);
    assertIllegalNormalization("http://host3.com/path2/file", mau);
    assertIllegalNormalization("http://host2.org:8080/path/file", mau);
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));

    // With the original and normalized  stems, most are legal
    mau.setUrlStems(ListUtil.list("http://host1.com/",
				  "http://host2.com/",
				  "http://host22.com/",
				  "http://host3.com/",
				  "http://host2.com:8080/",
				  "http://host2.org/",
				  "http://host2.org:8080/"));

    assertEquals("http://host22.com/path/file",
		 UrlUtil.normalizeUrl("http://host1.com/path/file", mau));
    assertEquals("http://host2.com:8080/path2/file",
		 UrlUtil.normalizeUrl("http://host2.com/path2/file", mau));
    // result not in stems
    assertIllegalNormalization("http://host3.com/path2/file", mau);
    assertEquals("http://host2.org/path/file",
		 UrlUtil.normalizeUrl("http://host2.org:8080/path/file", mau));
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));

    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_ALLOW_SITE_NORMALIZE_CHANGE_STEM,
				  "false");

    assertIllegalNormalization("http://host1.com/path/file", mau);
    assertIllegalNormalization("http://host2.com/path2/file", mau);
    assertIllegalNormalization("http://host3.com/path2/file", mau);
    assertIllegalNormalization("http://host2.org:8080/path/file", mau);
    assertEquals("http://a.com/file",
		 UrlUtil.normalizeUrl("http://a.com/spurious/file", mau));
  }

  public void testNormalizeAkamai() throws MalformedURLException {
    String a1 =
      "http://a123.g.akamai.net/f/123/4567/1d/www.pubsite.com/images/blip.ico";
    String u1 = "http://www.pubsite.com/images/blip.ico";
    String a2 =
      "http://a123.akamai.net/f/123/4567/1d/www.pubsite.com/images/blip.ico";
    String u2 = u1;
    String a3 =
      "http://a123.g.akamai.net/f/123/odd/4/1d/www.pubsite.com/images/blip.ico";
    String u3 = "http://1d/www.pubsite.com/images/blip.ico";
    String a4 =
      "http://a123.g.akamai.net/f/123/4/1d/www.PUBSITE.com/foo/../images/blip.ico";
    String u4 = u2;
    String a5 = "http://a.com/xy";

    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_NORMALIZE_AKAMAI_URL, "false");
    assertSame(a1, UrlUtil.normalizeUrl(a1));
    assertSame(a2, UrlUtil.normalizeUrl(a2));
    assertSame(a3, UrlUtil.normalizeUrl(a3));
    assertNotEquals(a4, UrlUtil.normalizeUrl(a4));
    assertSame(a5, UrlUtil.normalizeUrl(a5));

    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_NORMALIZE_AKAMAI_URL, "true");
    assertEquals(u1, UrlUtil.normalizeUrl(a1));
    assertEquals(u2, UrlUtil.normalizeUrl(a2));
    assertEquals(u3, UrlUtil.normalizeUrl(a3));
    assertEquals(u4, UrlUtil.normalizeUrl(a4));
    assertSame(a5, UrlUtil.normalizeUrl(a5));
  }

  public void testEqualUrls() throws MalformedURLException {
    assertTrue(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				 new URL("http://foo.bar/xyz#tag")));
    assertTrue(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				 new URL("HTTP://FOO.bar/xyz#tag")));
    assertFalse(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				  new URL("ftp://foo.bar/xyz#tag")));
    assertFalse(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				  new URL("http://foo.baz/xyz#tag")));
    assertFalse(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				  new URL("http://foo.bar/xyzz#tag")));
    assertFalse(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				  new URL("http://foo.bar/xYz#tag")));
    assertFalse(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				  new URL("http://foo.bar/xyz#tag2")));
    assertFalse(UrlUtil.equalUrls(new URL("http://foo.bar/xyz#tag"),
				  new URL("http://foo.bar/xyz#Tag")));
    assertFalse(UrlUtil.equalUrls(new URL("http:80//foo.bar/xyz#tag"),
				  new URL("http:81//foo.bar/xyz#tag")));
  }

  public void testIsHttpUrl() {
    assertTrue(UrlUtil.isHttpUrl("http://foo"));
    assertFalse(UrlUtil.isHttpUrl("https://foo"));
    assertTrue(UrlUtil.isHttpUrl("HTTP://foo"));
    assertFalse(UrlUtil.isHttpUrl("HTTPS://foo"));
    assertFalse(UrlUtil.isHttpUrl("ftp://foo"));
    assertFalse(UrlUtil.isHttpUrl("file://foo"));
  }

  public void testIsHttpsUrl() {
    assertFalse(UrlUtil.isHttpsUrl("http://foo"));
    assertTrue(UrlUtil.isHttpsUrl("https://foo"));
    assertFalse(UrlUtil.isHttpsUrl("HTTP://foo"));
    assertTrue(UrlUtil.isHttpsUrl("HTTPS://foo"));
    assertFalse(UrlUtil.isHttpsUrl("ftp://foo"));
    assertFalse(UrlUtil.isHttpsUrl("file://foo"));
  }

  public void testIsHttpOrHttpsUrl() {
    assertTrue(UrlUtil.isHttpOrHttpsUrl("http://foo"));
    assertTrue(UrlUtil.isHttpOrHttpsUrl("https://foo"));
    assertTrue(UrlUtil.isHttpOrHttpsUrl("HTTP://foo"));
    assertTrue(UrlUtil.isHttpOrHttpsUrl("HTTPS://foo"));
    assertFalse(UrlUtil.isHttpOrHttpsUrl("ftp://foo"));
    assertFalse(UrlUtil.isHttpOrHttpsUrl("file://foo"));
  }

  public void testGetUrlPrefixNullString(){
    try{
      UrlUtil.getUrlPrefix((String)null);
      fail("Should have thrown MalformedURLException");
    }
    catch(MalformedURLException mue){
    }
  }

  public void testGetUrlPrefixNullUrl() throws Exception {
    try{
      UrlUtil.getUrlPrefix((URL)null);
      fail("Should have thrown NullPointerException");
    }
    catch(NullPointerException e){
    }
  }

  public void testGetUrlPrefixNotHttpUrl(){
    try{
      UrlUtil.getUrlPrefix("bad test string");
      fail("Should have thrown MalformedURLException");
    }
    catch(MalformedURLException mue){
    }
  }

  public void testGetUrlPrefixRootHighWireUrl() throws MalformedURLException{
    String root = "http://shadow8.stanford.edu/";
    String url = root + "lockss-volume327.shtml";
    assertEquals(root, UrlUtil.getUrlPrefix(url));
  }

  public void testGetUrlPrefixRootHighWireUrlWithOddPort()
      throws MalformedURLException{
    String root = "http://shadow8.stanford.edu:8080/";
    String url = root + "lockss-volume327.shtml";
    assertEquals(root, UrlUtil.getUrlPrefix(url));
  }

  public void testGetUrlPrefixPrefixUrl() throws MalformedURLException{
    String root0 = "http://shadow8.stanford.edu";
    String root = root0 + "/";
    assertEquals(root, UrlUtil.getUrlPrefix(root));
    assertEquals(root, UrlUtil.getUrlPrefix(root0));
  }

  public void testGetUrlPrefixSame() throws MalformedURLException{
    String root = "http://shadow8.stanford.edu:8080/";
    assertSame(root, UrlUtil.getUrlPrefix(root));
  }

  public void testGetUrlPrefixes() throws MalformedURLException{
    assertEquals(ListUtil.list("http://foo/", "https://bar.bar:23/"),
		 UrlUtil.getUrlPrefixes(ListUtil.list("http://foo/xys/s",
						      "https://bar.bar:23/1")));
  }

  public void testGetHost() throws Exception {
    assertEquals("xx.foo.bar", UrlUtil.getHost("http://xx.foo.bar/123"));
    assertEquals("foo", UrlUtil.getHost("http://foo/123"));
    assertEquals("foo.", UrlUtil.getHost("http://foo./123"));
    try{
      UrlUtil.getHost("garbage://xx.foo.bar/123");
      fail("Should have thrown MalformedURLException");
    }
    catch (MalformedURLException mue) {
    }
  }

  public void testGetDomain() throws Exception {
    assertEquals("foo.bar", UrlUtil.getDomain("http://xx.foo.bar/123"));
    assertEquals("foo", UrlUtil.getDomain("http://foo/123"));
    assertEquals("foo.", UrlUtil.getDomain("http://foo./123"));
    try{
      UrlUtil.getDomain("garbage://xx.foo.bar/123");
      fail("Should have thrown MalformedURLException");
    }
    catch (MalformedURLException mue) {
    }
  }

  public void testMinimallyEncode() throws Exception {
    try {
      assertEquals(null, UrlUtil.minimallyEncodeUrl(null));
      fail("minimallyEncodeUrl(null) didn't throw");
    } catch (NullPointerException e) {}
    assertEquals("", UrlUtil.minimallyEncodeUrl(""));
    assertEquals("foo", UrlUtil.minimallyEncodeUrl("foo"));
    assertEquals("foo%20", UrlUtil.minimallyEncodeUrl("foo "));
    assertEquals("f%22oo%20", UrlUtil.minimallyEncodeUrl("f\"oo "));
    assertEquals("%20foo%7C", UrlUtil.minimallyEncodeUrl(" foo|"));
    assertEquals("%5Bfoo%5D", UrlUtil.minimallyEncodeUrl("[foo]"));

    // ensure doesn't encode member name after !/
    assertEquals("http://foo.bar/ba%20z.zip!/a b",
		 UrlUtil.minimallyEncodeUrl("http://foo.bar/ba z.zip!/a b"));
  }

  public void testEncodeQueryArg() throws Exception {
    try {
      assertEquals(null, UrlUtil.encodeQueryArg(null));
      fail("encodeQueryArg(null) didn't throw");
    } catch (NullPointerException e) {}
    assertEquals("", UrlUtil.encodeQueryArg(""));
    assertEquals("foo", UrlUtil.encodeQueryArg("foo"));
    assertEquals("foo%3Dbar%3Fa%3Db%26c%3Db",
		 UrlUtil.encodeQueryArg("foo=bar?a=b&c=b"));
  }

  public void testEncodeUrl() throws Exception {
    assertEquals("", UrlUtil.encodeUrl(""));
    assertEquals("foo", UrlUtil.encodeUrl("foo"));
    assertEquals("http%3A%2F%2Fhost%2Ffoo+bar",
		 UrlUtil.encodeUrl("http://host/foo bar"));
    assertEquals("f%22oo+", UrlUtil.encodeUrl("f\"oo "));
    assertEquals("+foo%7C", UrlUtil.encodeUrl(" foo|"));
    assertEquals("%5Bfoo%5D", UrlUtil.encodeUrl("[foo]"));
    // "smart" left/right double quotes
    assertEquals("http%3A%2F%2Fhost%2FINT-%E2%80%9CToll%E2%80%9D-Extending",
		 UrlUtil.encodeUrl("http://host/INT-\u201cToll\u201d-Extending"));
  }

  public void testDecodeUrl() throws Exception {
    assertEquals("", UrlUtil.decodeUrl(""));
    assertEquals("foo", UrlUtil.decodeUrl("foo"));
    assertEquals("http://host/foo bar",
		 UrlUtil.decodeUrl("http%3A%2F%2Fhost%2Ffoo+bar"));
    assertEquals("f\"oo ", UrlUtil.decodeUrl("f%22oo+"));
    assertEquals(" foo|", UrlUtil.decodeUrl("+foo%7C"));
    assertEquals("[foo]", UrlUtil.decodeUrl("%5Bfoo%5D"));
    // "smart" left/right double quotes
    assertEquals("http://host/INT-\u201cToll\u201d-Extending",
		 UrlUtil.decodeUrl("http%3A%2F%2Fhost%2FINT-%E2%80%9CToll%E2%80%9D-Extending"));
  }

  boolean uri=false;

  public void testResolveUrl() throws Exception {
    // base ends with filename
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("ftp://gorp.org/xxx.jpg",
				    "http://test.com/foo/bar/a.html"));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/xxx.html",
				    "a.html"));
    assertEquals("http://test.com/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/xxx.html",
				    "/a.html"));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/baz/xxx.html",
				    "../a.html"));
    assertEquals("http://test.com/foo/bar/baz/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/baz/xxx.html",
				    "./a.html"));

    assertEquals("http://test.com/",
		 UrlUtil.resolveUri("http://test.com/foo/bar/baz/xxx.html",
				    "/"));

    // According to RFC 1808, Firefox, IE, Opera, last component of base
    // path (following final slash) is *not* removed if relative URL has
    // null path.  RFC 2396 disagrees, but we follow the browsers
    assertEquals("http://test.com/foo/bar/xxx.html?a=b",
		 UrlUtil.resolveUri("http://test.com/foo/bar/xxx.html",
				    "?a=b"));

    // base ends with slash
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("ftp://gorp.org/",
				    "http://test.com/foo/bar/a.html"));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "a.html"));
    assertEquals("http://test.com/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "/a.html"));
    assertEquals("http://test.com/foo/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "../a.html"));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "./a.html"));

    assertEquals("http://test.com/foo/bar/?a=b",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "?a=b"));
    assertEquals("http://test.com/",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "/"));


    // truncated base (no slash after hostname)

    // First, note how resolution relative to base with no path differs
    // between java.net.URL:
    assertEquals("http://test.com/a.html",
		 new URL(new URL("http://test.com"), "a.html").toString());
    // and java.net.URI:
    URI u1 = new URI("http://test.com");
    URI u2 = u1.resolve("a.html");
    assertEquals("http://test.coma.html", u2.toString());

    // make sure we add the missing slash
    assertEquals("http://test.com/a.html",
		 UrlUtil.resolveUri("http://test.com",
				    "a.html"));
    // ensure query string preserved
    assertEquals("http://test.com/foo/bar/a.html?foo=bar",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "a.html?foo=bar"));
    // relative query string
    assertEquals("http://test.com/prog.php?foo=bar",
		 UrlUtil.resolveUri("http://test.com/prog.php",
				    "?foo=bar"));
    assertEquals("http://test.com/prog.php?foo=bar",
		 UrlUtil.resolveUri("http://test.com/prog.php?fff=xxx",
				    "?foo=bar"));
    assertEquals("http://test.com/",
		 UrlUtil.resolveUri("http://test.com",
				    "/"));
    // With URL implementation this threw, URI version doesn't object.
    // Don't think anyone should count on this behavior.
    if (uri) {
      assertEquals("bar", UrlUtil.resolveUri("foo", "bar"));
    } else {
      try {
	UrlUtil.resolveUri("foo", "bar");
	fail("Should throw MalformedURLException");
      } catch (MalformedURLException e) {}
    }
  }

  public void testResolveProtocolNeutralUrl() throws Exception {
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://gorp.org/xxx.jpg",
				    "//test.com/foo/bar/a.html"));
    assertEquals("https://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("https://gorp.org/xxx.jpg",
				    "//test.com/foo/bar/a.html"));
  }

  //should trip leading and trailing whitespace from the second arg
  public void testResolveUrlTrimsLeadingAndTrailingWhiteSpace()
      throws MalformedURLException {
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/", " a.html"));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/", "a.html "));
    assertEquals("http://test.com/foo/bar/a.html",
 		 UrlUtil.resolveUri("http://test.com/foo/bar/", "\ta.html "));
    assertEquals("http://test.com/foo/bar/a.html",
 		 UrlUtil.resolveUri("http://test.com/foo/bar/", "\na.html "));
    assertEquals("http://test.com/foo/bar/a.html",
 		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "\n\t\ta.html "));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "a.h\n\t\ttml "));
    assertEquals("http://test.com/foo/bar/a.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "a.h\n\n\n\t\t\t\t\ttml "));
    assertEquals("http://test.com/foo/bar/a.html",
 		 UrlUtil.resolveUri("http://test.com/foo/bar/", "a.html\n "));
    assertEquals("http://test.com/foo/bar/a.html",
 		 UrlUtil.resolveUri("http://test.com/foo/bar/", "a.html\r "));
  }

  String enc(int i, boolean upper) {
    StringBuffer sb = new StringBuffer();
    sb.append("%");
    sb.append(Character.forDigit((i >> 4) & 0xF, 16));
    sb.append(Character.forDigit(i & 0xF, 16));
    return upper ? sb.toString().toUpperCase() : sb.toString().toLowerCase();
  }

  public void testEnc() {
    assertEquals("%01", enc(1, true));
    assertEquals("%20", enc(32, true));
    assertEquals("%FF", enc(255, true));
    assertEquals("%01", enc(1, false));
    assertEquals("%20", enc(32, false));
    assertEquals("%ff", enc(255, false));
  }

  public void testResolveUrlEncodingOld() throws MalformedURLException {
    // Embedded space should be escaped
    assertEquals("http://test.com/foo/bar/a%20test.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "a test.html"));
    // Percents should not be escaped, or risk double escapement
    assertEquals("http://test.com/foo/bar/a%20.html",
		 UrlUtil.resolveUri("http://test.com/foo/bar/",
				    "a%20.html"));
  }

  public void assertResolveUrl(String exp, String base, String rel)
      throws MalformedURLException {
    assertEquals(exp, exp, UrlUtil.resolveUri(base, rel));
  }

  public void testResolveUrlEncodingPat(int substChar, String expPat,
					String basePat, String relPat)
      throws MalformedURLException {
    testResolveUrlEncodingPat(enc(substChar, false), expPat, basePat, relPat);
    testResolveUrlEncodingPat(enc(substChar, true), expPat, basePat, relPat);
  }

  public void testResolveUrlEncodingPat(String subst, String expPat,
					String basePat, String relPat)
      throws MalformedURLException {
    String pat = "##";
    String exp = StringUtil.replaceString(expPat, pat, subst);
    String base = StringUtil.replaceString(basePat, pat, subst);
    String rel = StringUtil.replaceString(relPat, pat, subst);
    assertResolveUrl(exp, base, rel);
  }

  public void testResolveUrlEncoding() throws MalformedURLException {
//     assertResolveUrl("http://carcin.oxfordjournals.org/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F26%2F1%2F11",
// 		     "http://carcin.oxfordjournals.org/",
// 		     "/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F26%2F1%2F11");

    for (int ix = 1; ix <= 254; ix++) {
      testResolveUrlEncodingPat(ix,
				"http://test.com/foo/bar/a##.html",
				"http://test.com/foo/bar/",
				"a##.html");
      testResolveUrlEncodingPat(ix,
				"http://test.com/foo/xx?a##b=c",
				"http://test.com/foo/bar",
				"xx?a##b=c");
      testResolveUrlEncodingPat(ix,
				"http://test.com/foo/xx?ab=##c",
				"http://test.com/foo/bar",
				"xx?ab=##c");
      testResolveUrlEncodingPat(ix,
				"http://test.com/cgi/xx?ab=##c",
				"http://test.com/foo/bar",
				"/cgi/xx?ab=##c");
    }
  }

  public void testIsDirectoryRedirection() {
    assertTrue(UrlUtil.isDirectoryRedirection("http://xx.com/foo",
					      "http://xx.com/foo/"));
    assertTrue(UrlUtil.isDirectoryRedirection("http://xx.com/foo",
					      "Http://xx.com/foo/"));
    assertTrue(UrlUtil.isDirectoryRedirection("http://xx.com/foo",
					      "Http://Xx.COM/foo/"));
    assertFalse(UrlUtil.isDirectoryRedirection("http://xx.com/foo",
					       "http://xx.com/FOO/"));
    assertFalse(UrlUtil.isDirectoryRedirection("http://xx.com/foo",
					       "http://xx.com/foo"));
    assertFalse(UrlUtil.isDirectoryRedirection("http://xx.com/foo",
					       "http://zz.com/foo/"));

    assertTrue(UrlUtil.isDirectoryRedirection("http://xx.com/foo?a=b",
					      "http://xx.com/foo/?a=b"));
    // slash appended to query string isn't
    assertFalse(UrlUtil.isDirectoryRedirection("http://xx.com/foo?a=b",
					      "http://xx.com/foo?a=b/"));
    // ensure doesn't totally ignore query
    assertFalse(UrlUtil.isDirectoryRedirection("http://xx.com/foo?a=b",
					      "http://xx.com/foo/"));
    // not legal URLs, so returns false
    assertFalse(UrlUtil.isDirectoryRedirection("foo", "foo/"));
  }

  public void testGetHeadersNullConnection() {
    try {
      UrlUtil.getHeaders(null);
      fail("Calling getHeaderFields with a null argument should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetHeadersOneHeader() throws MalformedURLException {
    URL url = new URL("http://www.example.com");
    MockURLConnection conn = new MockURLConnection(url);
    conn.setHeaderFieldKeys(ListUtil.list("key1"));
    conn.setHeaderFields(ListUtil.list("field1"));
    assertEquals(ListUtil.list("key1;field1"), UrlUtil.getHeaders(conn));
  }

  public void testGetHeadersMultiHeaders() throws MalformedURLException {
    URL url = new URL("http://www.example.com");
    MockURLConnection conn = new MockURLConnection(url);
    conn.setHeaderFieldKeys(ListUtil.list("key1", "key2"));
    conn.setHeaderFields(ListUtil.list("field1", "field2"));

    assertEquals(ListUtil.list("key1;field1", "key2;field2"),
		 UrlUtil.getHeaders(conn));
  }

  public void testGetHeadersNullHeaders() throws MalformedURLException {
    URL url = new URL("http://www.example.com");
    MockURLConnection conn = new MockURLConnection(url);
    conn.setHeaderFieldKeys(ListUtil.list(null, "key2"));
    conn.setHeaderFields(ListUtil.list("field1", null));

    assertEquals(ListUtil.list("null;field1", "key2;null"),
		 UrlUtil.getHeaders(conn));
  }

  public void testIsAbsoluteUrl() {
    assertFalse(UrlUtil.isAbsoluteUrl(null));
    assertFalse(UrlUtil.isAbsoluteUrl(""));
    assertTrue(UrlUtil.isAbsoluteUrl("http://www.example.com/"));
    assertTrue(UrlUtil.isAbsoluteUrl("http://www.example.com/blah/"));
    assertFalse(UrlUtil.isAbsoluteUrl("www.example.com/"));
    assertFalse(UrlUtil.isAbsoluteUrl("blah/blah"));
    assertTrue(UrlUtil.isAbsoluteUrl("https://www.example.com/"));
  }

  public void testIsSameHost() {
    assertFalse(UrlUtil.isSameHost(null, null));
    assertTrue(UrlUtil.isSameHost("http://www.example.com/foo/bar",
				  "http://www.example.com/bar/bar/bar"));
    assertFalse(UrlUtil.isSameHost("http://www.example.com/foo/bar",
				   "http://www2.example.com/foo/bar"));
  }

  public void testStripsParams() throws MalformedURLException {
    assertNull(UrlUtil.stripQuery(null));
    assertEquals(null, UrlUtil.stripQuery(""));
    assertEquals("http://www.example.com/",
		 UrlUtil.stripQuery("http://www.example.com/"));
    assertEquals("http://www.example.com/blah",
		 UrlUtil.stripQuery("http://www.example.com/blah?param1=blah"));
    assertEquals("rtsp://www.example.com/blah",
		 UrlUtil.stripQuery("rtsp://www.example.com/blah?param1=blah"));
  }

  public void testStripProtocol() {
    assertNull(UrlUtil.stripProtocol(null));
    assertEquals("", UrlUtil.stripProtocol(""));
    assertEquals("www.example.com/",
		 UrlUtil.stripProtocol("http://www.example.com/"));
    assertEquals("www.example.com/",
		 UrlUtil.stripProtocol("http://www.example.com/"));
    assertEquals("www.example.com:8080/",
		 UrlUtil.stripProtocol("rtsp://www.example.com:8080/"));
  }

  public void testGetFileExtension() throws MalformedURLException {
    try {
      UrlUtil.getFileExtension(null);
      fail("getFileExtension(null should throw MalformedURLException");
    } catch (MalformedURLException e) {
    }
    try {
      UrlUtil.getFileExtension("");
      fail("getFileExtension(null should throw MalformedURLException");
    } catch (MalformedURLException e) {
    }
    assertEquals("zip",
		 UrlUtil.getFileExtension("http://a.b/file.zip"));
    assertEquals("zip",
		 UrlUtil.getFileExtension("http://a.b/file.bar.zip"));
    assertEquals("zip",
		 UrlUtil.getFileExtension("http://a.b/file.zip?foo=bar"));
    assertEquals("zip",
		 UrlUtil.getFileExtension("http://a.b/file.zip#ref"));
  }

  public void testAddSubDomain() {
    assertEquals("http://www.foo.bar",
		 UrlUtil.addSubDomain("http://foo.bar", "www"));
    assertEquals("http://web.foo.bar/",
		 UrlUtil.addSubDomain("http://foo.bar/", "web"));
    assertEquals("http://www.foo.bar/path",
		 UrlUtil.addSubDomain("http://foo.bar/path", "www"));
    assertEquals("http://www.foo.bar:8080/path",
		 UrlUtil.addSubDomain("http://foo.bar:8080/path", "www"));
    // Doesn't add if already there
    assertEquals("http://www.foo.bar/path",
		 UrlUtil.addSubDomain("http://www.foo.bar/path", "www"));
  }

  public void testDelSubDomain() {
    assertEquals("http://foo.bar/",
		 UrlUtil.delSubDomain("http://www.foo.bar/", "www"));
    assertEquals("http://foo.bar/",
		 UrlUtil.delSubDomain("http://WWW.foo.bar/", "www"));
    assertEquals("http://foo.bar/path",
		 UrlUtil.delSubDomain("http://web.foo.bar/path", "web"));
    assertEquals("http://foo.bar:80/path",
		 UrlUtil.delSubDomain("http://www.foo.bar:80/path", "www"));
    assertSame("http://foo.bar/path",
		 UrlUtil.delSubDomain("http://foo.bar/path", "www"));
    assertSame("http://wwwfoo.bar/path",
		 UrlUtil.delSubDomain("http://wwwfoo.bar/path", "www"));
  }

  public void testReplaceScheme() {
    assertEquals("http://foo.bar/",
		 UrlUtil.replaceScheme("http://foo.bar/", "https", "foo"));
    assertEquals("foo://foo.bar/",
		 UrlUtil.replaceScheme("https://foo.bar/", "https", "foo"));
    assertEquals("https://foo.bar/",
		 UrlUtil.replaceScheme("http://foo.bar/", "http", "https"));
    assertEquals("https://foo.bar/",
		 UrlUtil.replaceScheme("https://foo.bar/", "http", "https"));
    // ensure too-short string doesn't throw
    assertEquals("htt",
		 UrlUtil.replaceScheme("htt", "http", "https"));
  }

  public void testResolveJavascriptUrl() {
    assertEquals("http://www.example.com/link2.html",
		 UrlUtil.parseJavascriptUrl("javascript:popup(http://www.example.com/link2.html)"));

    assertEquals("http://www.example.com/link2.html",
		 UrlUtil.parseJavascriptUrl("javascript:newWindow(http://www.example.com/link2.html)"));

    assertEquals("http://www.example.com/link2.html",
		 UrlUtil.parseJavascriptUrl("javascript:popup('http://www.example.com/link2.html')"));

    assertEquals("http://www.example.com/link2.html",
		 UrlUtil.parseJavascriptUrl("javascript:newWindow('http://www.example.com/link2.html')"));

    assertEquals("link2.html",
		 UrlUtil.parseJavascriptUrl("javascript:popup(link2.html)"));

    assertEquals("link2.html",
		 UrlUtil.parseJavascriptUrl("javascript:newWindow(link2.html)"));

  }

  public void testIsMalformedUrl() {
    assertFalse(UrlUtil.isMalformedUrl("http://www.example.com"));
    assertTrue(UrlUtil.isMalformedUrl("blah blah blah"));
    assertTrue(UrlUtil.isMalformedUrl("javascript:popup(blah)"));
  }

  public void testIsFileUrl() {
    assertTrue(UrlUtil.isFileUrl("file:foo.bar"));
    assertTrue(UrlUtil.isFileUrl("file:///foo.bar"));
    assertFalse(UrlUtil.isFileUrl("http://foo.bar/"));
    assertFalse(UrlUtil.isFileUrl("jar:/foo.bar"));
    assertFalse(UrlUtil.isFileUrl("jar:file:/foo.bar!x.y"));
  }

  public void testIsUrl() {
    assertTrue(UrlUtil.isUrl("http://foo.bar/"));
    assertTrue(UrlUtil.isUrl("file:/foo.bar"));
    assertFalse(UrlUtil.isUrl("/path/foo.bar"));
    assertFalse(UrlUtil.isUrl("path/foo.bar"));
    assertFalse(UrlUtil.isUrl("jar:file:/foo.bar!x.y"));
  }

  public void testIsJarUrl() {
    assertTrue(UrlUtil.isJarUrl("jar:foo.bar"));
    assertTrue(UrlUtil.isJarUrl("jar:///foo.bar"));
    assertFalse(UrlUtil.isJarUrl("http://foo.bar/"));
    assertFalse(UrlUtil.isJarUrl("file:/foo.bar"));
  }

  public void testMakeJarFileUrl() throws MalformedURLException {
    assertMatchesRE("jar:file:.*/dir/2!/file.txt",    // ".*" skips over DOS drive spec
 		    UrlUtil.makeJarFileUrl("/dir/2", "file.txt"));
  }
}
