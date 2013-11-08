/*
 * $Id: WileyArticleIteratorFactory.java,v 1.7 2013-11-08 19:21:12 pgust Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.wiley;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Iterates article files.  Archived source content zip files include files
 * with mime-type pdf and xml. The xml file contains the metadata and refers
 * to the name of the PDF file.
 * <p>
 * There's no way to consistently get the name
 * of the PDF file from the name of the XML file, so it's necessary to
 * iterate on the XML files and capture the name of the PDF files in the 
 * metadata extractor. Example XML file names include:
 * <pre>
 * 1/117966453266.3.zip!/ j.1365-2796.2009.02095.x.wml.xml
 * A/ADMA23.12.zip!/1427_ftp.wml.xml
 * A/ADMA23.12.zip!/1419_hdp.wml.xml 
 * C/CEAT34.10.zip!/1728_hrp.wml.xml
 * </pre>
 */
public class WileyArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
                          Logger.getLogger(WileyArticleIteratorFactory.class);
  
  // no need to set ROOT_TEMPLATE since all content is under <base_url>/<year>
  protected static final String PATTERN_TEMPLATE = 
      "\"%s%d/[A-Z0-9]/[^/]+\\.zip!/.*\\.wml\\.xml$\",base_url,year";
     
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    return new WileyArticleIterator(au, 
                                    new SubTreeArticleIterator.Spec()
                                        .setTarget(target)
                                        .setVisitArchiveMembers(true)
                                        .setPatternTemplate(PATTERN_TEMPLATE, 
                                                      Pattern.CASE_INSENSITIVE)
                                    );
  }
  
  protected static class WileyArticleIterator extends SubTreeArticleIterator {
	 
    protected final static Pattern XML_PATTERN = 
      Pattern.compile("/[^/]+\\.zip!/.*\\.wml\\.xml$",Pattern.CASE_INSENSITIVE);
    
    
    protected WileyArticleIterator(ArchivalUnit au,
                                   SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher matXml = XML_PATTERN.matcher(url);
      if (matXml.find()) {
        ArticleFiles af = new ArticleFiles();
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
        // set access URL to PDF file in metadata extractor
        // set fullTextCu here to make list of XML files visible
        // to non-metadata iterator clients.
        af.setFullTextCu(cu);
        return af;
      }
      log.warning("Url does not match XML_PATTERN: " + url);
      return null;
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                         MetadataTarget target)
        throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
