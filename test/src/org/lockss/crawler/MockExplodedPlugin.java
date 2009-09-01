/*
 * $Id: MockExplodedPlugin.java,v 1.1.30.1 2009-09-01 22:17:47 dshr Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import org.lockss.test.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.exploded.*;
import org.lockss.extractor.*;

/** MockExplodedPlugin extends ExplodedPlugin to register new AUs with
 * MockLockssDaemon */

public class MockExplodedPlugin extends ExplodedPlugin {

  private ArticleIteratorFactory articleIteratorFactory = null;
  private MetadataExtractorFactory metadataExtractorFactory = null;
  private String defaultArticleMimeType = null;

  public MockExplodedPlugin() {
    super();
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = super.createAu0(auConfig);
    createMockAuManagers(au);
    return au;
  }

  public String getPluginName() {
    return "MockExplodedPlugin";
  }

  void createMockAuManagers(ArchivalUnit au) {
    MockLockssDaemon daemon = (MockLockssDaemon)getDaemon();
    daemon.getLockssRepository(au).startService();
    daemon.setNodeManager(new MockNodeManager(), au);
  }

  /**
   * Return a {@link MetadataExtractor} that knows how to extract URLs from
   * content of the given MIME type
   * @param contentType content type to get a content parser for
   * @param au the AU in question
   * @return A MetadataExtractor or null
   */
  public MetadataExtractor getMetadataExtractor(String contentType,
						ArchivalUnit au) {
    MetadataExtractor ret = null;
    if (metadataExtractorFactory != null) {
      try {
	ret = metadataExtractorFactory.createMetadataExtractor(contentType);
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return ret;
  }
  public void setMetadataExtractorFactory(MetadataExtractorFactory mef) {
    metadataExtractorFactory = mef;
  }

  /**
   * Returns the article iterator factory for the mime type, if any
   * @param contentType the content type
   * @return the ArticleIteratorFactory
   */
  public ArticleIteratorFactory getArticleIteratorFactory(String contentType) {
    return articleIteratorFactory;
  }
  public void setArticleIteratorFactory(ArticleIteratorFactory aif) {
    articleIteratorFactory = aif;
  }

  /**
   * Returns the default mime type of articles in this AU
   * @return the default MimeType
   */
  public String getDefaultArticleMimeType() {
    return defaultArticleMimeType;
  }
  public void setDefaultArticleMimeType(String damt) {
    defaultArticleMimeType = damt;
  }
}
