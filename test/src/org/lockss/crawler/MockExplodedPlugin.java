/*
 * $Id$
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

  private ArticleIteratorFactory articleIteratorFactory;
  private ArticleMetadataExtractorFactory articleMetadataExtractorFactory;
  private FileMetadataExtractorFactory fileMetadataExtractorFactory;
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

  public ArticleMetadataExtractor
    getArticleMetadataExtractor(MetadataTarget target,
				ArchivalUnit au) {
    if (articleMetadataExtractorFactory != null) {
      try {
	return
	  articleMetadataExtractorFactory.createArticleMetadataExtractor(target);
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return null;
  }
  public void
    setArticleMetadataExtractorFactory(ArticleMetadataExtractorFactory mef) {
    articleMetadataExtractorFactory = mef;
  }

  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target,
							String contentType,
							ArchivalUnit au) {
    if (fileMetadataExtractorFactory != null) {
      try {
	return
	  fileMetadataExtractorFactory.createFileMetadataExtractor(target,
								   contentType);
      } catch (PluginException e) {
	throw new RuntimeException(e);
      }
    }
    return null;
  }
  public void
    setFileMetadataExtractorFactory(FileMetadataExtractorFactory mef) {
    fileMetadataExtractorFactory = mef;
  }

  /**
   * Returns the article iterator factory for the mime type, if any
   * @param contentType the content type
   * @return the ArticleIteratorFactory
   */
  public ArticleIteratorFactory getArticleIteratorFactory() {
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
