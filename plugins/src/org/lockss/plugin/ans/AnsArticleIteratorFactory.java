/*
 * $Id: DryadArticleIteratorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.ans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AnsArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(AnsArticleIteratorFactory.class);

  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE_BASE = "\"%s\", base_url";
  protected static final String ROOT_TEMPLATE_DOWNLOAD = "\"%s\", download_url";

  protected static final String PATTERN_TEMPLATE =
      "\"^(%spubs/journals/%s|%s)\", base_url, journal_id, download_url";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    //http://www.ans.org/pubs/journals/nse/a_37021
    //http://epubs.ans.org/?a=37469
    
    final Pattern ABSTRACT_PATTERN = Pattern.compile(
       "pubs/journals/([^/]+)/a_([0-9]+)$",
       Pattern.CASE_INSENSITIVE);
    
    
    final String ABSTRACT_REPLACEMENT = "pubs/journals/$1/a_$2";
    final String CONTENT_REPLACEMENT = "?a=$2";
    ArrayList<String >rootList = new ArrayList<String>();
    rootList.add(ROOT_TEMPLATE_BASE);
    rootList.add(ROOT_TEMPLATE_DOWNLOAD);
    
    builder.setSpec(target,
        rootList, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    builder.addAspect(
    		ABSTRACT_PATTERN,
            ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, 
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(
            CONTENT_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
