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

package org.lockss.plugin.scielo;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;

/*
 * UrlNormalizer removes 
 * 
 * 
 * 
 */

public class TestSciELOUrlNormalizer extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private DefinablePlugin plugin;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.scielo.SciELOPlugin");
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ISSN_KEY, "2013-2016");
    props.setProperty(YEAR_KEY, "2013");
    
    Configuration config = ConfigurationUtil.fromProps(props);
    plugin.configureAu(config, null);
    }
  
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new SciELOUrlNormalizer();
    
    assertEquals(
        "http://www.scielo.br/pdf/abcd/v26n4/en_v26n4a20.pdf", normalizer.normalizeUrl(
        "http://www.scielo.br/readcube/epdf.php?doi=10.1590/S0102-67202013000400020&pid=S0102-67202013000400020&pdf_path=abcd/v26n4/en_v26n4a20.pdf&lang=en", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_arttext&pid=S0102-67202013000600001&lng=pt&tlng=pt", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_arttext%26pid=S0102-67202013000600001%26lng=pt%26nrm=iso%26tlng=pt", null));
    assertEquals(
        "http://www.mendeley.com/import/?url=http://www.scielo.br/scielo.php?script=sci_arttext%26p\n" + 
        "id=S0102-67202013000600001%26lng=pt%26nrm=iso%26tlng=pt", normalizer.normalizeUrl(
        "http://www.mendeley.com/import/?url=http://www.scielo.br/scielo.php?script=sci_arttext%26p\n" + 
        "id=S0102-67202013000600001%26lng=pt%26nrm=iso%26tlng=pt", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202013000600001&lng=pt&tlng=pt", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202013000600001&lng=pt&nrm=iso&tlng=pt", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_issuetoc&pid=0102-672020130006&lng=pt", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_issuetoc&pid=0102-672020130006&lng=pt&nrm=iso", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_alphabetic&lng=pt", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_alphabetic&lng=pt&nrm=iso", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_alphabetic&lng=pt&nrm=utf8", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_alphabetic&lng=pt&nrm=utf8", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_issues&lng=en", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_issues", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_issuetoc&pid=0102-672020140003&lng=en", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_issuetoc&pid=0102-672020140003&lng=en&nrm=iso", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202014000300167&lng=en", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202014000300167&lng=en&nrm=iso&tlng=en", null));
    assertEquals(
        "http://www.scielo.br/scieloOrg/php/articleXML.php?pid=S0102-67202014000200092&lang=en", normalizer.normalizeUrl(
        "http://www.scielo.br/scieloOrg/php/articleXML.php?pid=S0102-67202014000200092&lang=pt", null));
    assertEquals(
        "http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202014000200091&lng=pt&tlng=pt", normalizer.normalizeUrl(
        "http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202014000200091&lng=pt&nrm=iso&tlng=pt", null));
    assertEquals(
        "foo", normalizer.normalizeUrl(
        "foo", null));
  }
  
}
