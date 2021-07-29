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

package org.lockss.plugin.michigan;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.plugin.pub2web.Pub2WebUrlNormalizer;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;

import java.util.Properties;

/*
 * UrlNormalizer lowercases some urls
 */

public class TestUMichUrlNormalizer extends LockssTestCase {

  
  public void testUrlNormalizer() throws Exception {

    DefinablePlugin plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
            "org.lockss.plugin.michigan.UMichFulcrumBooksPlugin");
    Properties props = new Properties();
    props.setProperty("base_url", "http://www.test.com/");
    props.setProperty("book_uri", "10.21435/sfh.26");
    DefinableArchivalUnit au = null;
    try {
      Configuration config = ConfigurationUtil.fromProps(props);
      au = (DefinableArchivalUnit) plugin.configureAu(config, null);
    } catch (ConfigurationException ex) {
      au = null;
    }

    UrlNormalizer normalizer = new UMichUrlNormalizer();
    // TOC
    assertEquals("https://www.fulcrum.org/concern/file_sets/zc77ss02p",
            normalizer.normalizeUrl("https://www.fulcrum.org/concern/file_sets/zc77ss02p", au));
    assertEquals("https://www.fulcrum.org/concern/file_sets/zc77ss02p",
            normalizer.normalizeUrl("https://www.fulcrum.org/concern/file_sets/zc77ss02p?locale=en", au));
    // TOC PDF
    assertEquals("https://www.fulcrum.org/concern/monographs/xg94hr617?page=2",
            normalizer.normalizeUrl("https://www.fulcrum.org/concern/monographs/xg94hr617?locale=en&page=2", au));
    assertEquals("https://www.fulcrum.org/concern/monographs/xg94hr617",
            normalizer.normalizeUrl("https://www.fulcrum.org/concern/monographs/xg94hr617?locale=en?utf8=%E2%9C%93&locale=en", au));

  }
}
