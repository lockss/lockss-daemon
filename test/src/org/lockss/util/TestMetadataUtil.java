/*
 * $Id: TestMetadataUtil.java,v 1.3 2011-01-20 08:38:40 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.net.URLDecoder;
import org.apache.commons.lang.LocaleUtils;

import org.lockss.test.*;


/**
 * Created by IntelliJ IDEA.
 * User: dsferopo
 * Date: Dec 17, 2009
 * Time: 3:40:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestMetadataUtil extends LockssTestCase {

  void assertLocale(String lang, Locale l) {
    assertEquals("Language", lang, l.getLanguage());
  }

  void assertLocale(String lang, String country, Locale l) {
    assertEquals("Language", lang, l.getLanguage());
    assertEquals("Country", country, l.getCountry());
  }

  void assertLocale(String lang, String country, String variant, Locale l) {
    assertEquals("Language", lang, l.getLanguage());
    assertEquals("Country", country, l.getCountry());
    assertEquals("Variant", variant, l.getVariant());
  }

  static List<Locale> testLocales =
    ListUtil.list(new Locale("bb"),
		  new Locale("aa"),
		  new Locale("aa", "DD", "Var2"),
		  new Locale("aa", "DD"),
		  new Locale("aa", "DD", "Var1"));
		  

  String findClosestLocale(String str) {
    Locale l =  MetadataUtil.findClosestLocale(LocaleUtils.toLocale(str),
					       testLocales);
    return l != null ? l.toString() : null;
  }

  String findClosestAvailableLocale(String str) {
    Locale l =
      MetadataUtil.findClosestAvailableLocale(LocaleUtils.toLocale(str));
    return l != null ? l.toString() : null;
  }

  public void testFindClosestLocale() {
    assertEquals("aa_DD_Var2", findClosestLocale("aa_DD_Var2"));
    assertEquals("aa_DD", findClosestLocale("aa_DD"));
    assertEquals("aa", findClosestLocale("aa"));
    assertEquals(null, findClosestLocale("xx"));
    assertEquals(null, findClosestLocale("xx_DD"));
    assertEquals("bb", findClosestLocale("bb_DD"));
    assertEquals("bb", findClosestLocale("bb_DD_vvv"));
    assertEquals("aa_DD_Var2", findClosestLocale("aa_DD_Var3"));
    assertEquals("aa", findClosestLocale("aa_EE_Var3"));
  }

  // Java spec says Locale.US must exist; still not sure this will succeed
  // in all environments
  public void testFindClosestAvailableLocale() {
    assertEquals("en", findClosestAvailableLocale("en"));
    assertEquals("en", findClosestAvailableLocale("en_ZF"));
    assertEquals("en_US", findClosestAvailableLocale("en_US"));
  }

  static Locale DEF_LOC = MetadataUtil.DEFAULT_DEFAULT_LOCALE;

  public void testConfigDefaultLocale() {
    assertEquals(DEF_LOC, MetadataUtil.getDefaultLocale());
    ConfigurationUtil.setFromArgs(MetadataUtil.PARAM_DEFAULT_LOCALE, "fr_CA");
    assertLocale("fr", "CA", MetadataUtil.getDefaultLocale());
    ConfigurationUtil.setFromArgs(MetadataUtil.PARAM_DEFAULT_LOCALE, "");
    assertEquals(DEF_LOC, MetadataUtil.getDefaultLocale());
  }


  private String validISSNS [] = {
          "1144-875X",
          "1543-8120",
          "1508-1109",
          "1733-5329",
          "0740-2783",
          "0097-4463",
          "1402-2001",
          "1523-0430",
          "1938-4246",
          "0006-3363"
  };

  private String invalidISSNS [] = {
          "1144-175X",
          "1144-8753",
          "1543-8122",
          "1541-8120",
          "1508-1409",
          "2740-2783",
          "1938-42463",
          "140-42001",
          "15236-430",
          "1402-200",
          "1938/4246",
          "1402",
          "1402-",
          "-4246",
	  null
  };

  private String validDOIS [] = {
          "10.1095/biolreprod.106.054056",
          "10.2992/007.078.0301",
          "10.1206/606.1",
          "10.1640/0002-8444-99.2.61",
          "10.1663/0006-8101(2007)73[267:TPOSRI]2.0.CO;2",
          "10.1663/0006-8101(2007)73[267%3ATPOSRI]2.0.CO%3B2"
  };

  private String invalidDOIS [] = {
          "12.1095/biolreprod.106.054056",
          "10.2992-007.078.0301",
          "10.1206",
          "/0002-8444-99.2.61",
          "-0002-8444-99.2.61",
          "10.1640/0002-8444/99.2.61",
	  null
  };

  public void testISSN() {
    for(int i=0; i<validISSNS.length;i++){
      assertTrue(MetadataUtil.isISSN(validISSNS[i]));
    }

    for(int j=0; j<invalidISSNS.length;j++){
      assertFalse(MetadataUtil.isISSN(invalidISSNS[j]));
    }
  }

  public void testDOI() {
    for(int i=0; i<validDOIS.length;i++){
      assertTrue(MetadataUtil.isDOI(URLDecoder.decode(validDOIS[i])));
    }

    for(int j=0; j<invalidDOIS.length;j++){
      assertFalse(MetadataUtil.isDOI(invalidDOIS[j]));
    }
  }

  public void testFoo() {
    for (Locale l : Locale.getAvailableLocales()) {
      log.info("l: " + l);
    }
  }

}
