/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.anu;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.w3c.rdf.vocabulary.dublin_core_19990702.DC;


/*
 * Metadata on an abstract page https://press.anu.edu.au/publications/agenda-journal-policy-analysis-and-reform-volume-15-2008-number-3
 * in the form of:
 * <meta scheme="stats-collector" name="DC.identifier" content="1447-4735" />
 * <meta scheme="stats-collector" name="DC.publisher" content="ANU Press" />
 * <meta scheme="stats-collector" name="DC.relation.isPartOf" content="Agenda - A Journal of Policy Analysis and Reform" />
 * <meta name="viewport" content="width=device-width, initial-scale=1" />
 * <meta scheme="stats-collector" name="DC.type" content="journal" />
 * <meta scheme="stats-collector" name="DC.title" content="Agenda - A Journal of Policy Analysis and Reform: Volume 15, Number 3, 2008" />
 * <meta name="description" content="Agenda is the journal of the College of Business and Economics, ANU. Launched in 1994, Agenda provides a forum for debate on public policy, mainly (but not exclusively) in Australia and New Zealand. It deals largely with economic issues but gives space to social and legal policy and also to the moral and philosophical foundations and implications of policy. Agenda Alerting service (Subscribe to this alerting service if you wish to be advised on forthcoming or new issues)" />
 * <meta name="generator" content="Drupal 7 (http://drupal.org)" />
 * <meta property="og:image" content="https://press.anu.edu.au/files/press-publication/b-thumb-a1503.jpg" />
 * <meta property="og:image:url" content="https://press.anu.edu.au/files/press-publication/b-thumb-a1503.jpg" />
 */

public class AnuHtmlMetadataExtractorFactory 
    extends JsoupTagExtractorFactory {
  static Logger log = Logger.getLogger(AnuHtmlMetadataExtractorFactory.class);

  static final String CHP_TITLE = "h1.Chapter-Title";
  static final String CHP_AUTHOR = "p.Chapter-Author";
  static final String REV_TITLE = "h1.Review-Title";
  static final String REV_AUTHOR = "p.Review-Author";

  //h1 class="Chapter-Title"
  //p class="Chapter-Author"

  //h1  class="Review-Title"
  //p class="Book-Details"
  //p class="Review-Author"

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new AnuHtmlMetadataExtractor(contentType);
  }
  
  public static class AnuHtmlMetadataExtractor extends JsoupTagExtractor {
    
    public AnuHtmlMetadataExtractor(String contentType) {
      super("text/html");
    }
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put(CHP_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put(CHP_AUTHOR, MetadataField.FIELD_AUTHOR);
      tagMap.put(REV_AUTHOR, MetadataField.FIELD_AUTHOR);
      tagMap.put(REV_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException, PluginException {
      // set our css selectors, this causes super.extract() to extract using css selector and not meta tags
      // i do not think both are possible. 'easily' perhaps calling super.extract twice, once with setSelectors set
      // and once without.
      setSelectors(tagMap);
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      
      return am;
    }
    
  }
  
}
