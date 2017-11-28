/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;

/*
 * UrlNormalizer lowercases some urls
 */

public class TestPub2WebUrlNormalizer extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url
  static final String BOOK_ROOT_URL = "http://www.book.com/"; //this is not a real url
  
  public void testUrlNormalizer() throws Exception {
    
    DefinablePlugin plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.pub2web.Pub2WebJournalsPlugin");
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://jgv.microbiologyresearch.org/");
    props.setProperty(JID_KEY, "jgv");
    props.setProperty(VOL_KEY, "96");
    DefinableArchivalUnit au = null;
    try {
      Configuration config = ConfigurationUtil.fromProps(props);
      au = (DefinableArchivalUnit)plugin.configureAu(config, null);
    }
    catch (ConfigurationException ex) {
      au = null;
    }
    
    UrlNormalizer normalizer = new Pub2WebUrlNormalizer();
    // TOC
    assertEquals("http://jgv.microbiologyresearch.org/content/journal/jgv/96/12",
            normalizer.normalizeUrl("http://jgv.microbiologyresearch.org/content/journal/jgv/96/12", au));
    // TOC PDF
    assertEquals("http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/12/toc.pdf?itemId=/content/journal/jgv/96/12/tocpdf1&mimeType=pdf",
            normalizer.normalizeUrl("http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/12/toc.pdf?itemId=/content/journal/jgv/96/12/tocpdf1&mimeType=pdf", au));
    // TOC section render list
    assertEquals("http://jgv.microbiologyresearch.org/articles/renderlist.action?fmt=ahah&items=http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000294,http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000314",
            normalizer.normalizeUrl("http://jgv.microbiologyresearch.org/articles/renderlist.action?fmt=ahah&items=http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000294,http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000314", au));
    // abstract or landing page
    assertEquals("http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000298",
            normalizer.normalizeUrl("http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000298", au));
    // PDF link
    assertEquals("http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069625-0?crawler=true&mimetype=application/pdf",
        normalizer.normalizeUrl("http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/2/420_vir069625.pdf?itemId=/content/journal/jgv/10.1099/vir.0.069625-0&mimeType=pdf&isFastTrackArticle=", au));
    // HTML link
    assertEquals("http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069625-0?crawler=true&mimetype=html",
        normalizer.normalizeUrl("http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/2/420.html?itemId=/content/journal/jgv/10.1099/vir.0.069625-0&mimeType=html&fmt=ahah", au));
    
    assertNotEquals("", 
        normalizer.normalizeUrl("http://", au));
  }
  
}
