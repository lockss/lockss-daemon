/*
 * $Id$
 */

/*

 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class IngentaHtmlMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(IngentaHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new IngentaHtmlMetadataExtractor();
  }

  public static class IngentaHtmlMetadataExtractor 
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    private static Pattern doiPattern = Pattern.compile("info:doi/(.*)");
    private static Pattern issnPattern = Pattern.compile("urn:ISSN:(.*)");
    private static MultiMap tagMap = new MultiValueMap();
    static {
      
      // not used at the moment
      // String splitMetaPattern = "(.*)[,](.*)[,](.*)[,]([^-]+)[-]([^-()]+)"; 
      
      //  <meta name="DC.creator" content="Karni, Nirit"/>
      //  <meta name="DC.creator" content="Reiter, Shunit"/>
      //  <meta name="DC.creator" content="Bryen, Diane Nelson"/>  
      tagMap.put("DC.creator", MetadataField.FIELD_AUTHOR);
      // <meta name="DC.type" scheme="DCMIType" content="Text"/>
      tagMap.put("Dc.type",MetadataField.DC_FIELD_TYPE);
      tagMap.put("DC.Date.issued", MetadataField.FIELD_DATE);
      // <meta name="DC.title" content="Israeli Arab Teachers&#039; Attitudes on 
      //Inclusion of Students with Disabilities"/>
      tagMap.put("DC.title", MetadataField.FIELD_ARTICLE_TITLE);
      //<meta name="DCTERMS.issued" content="July 2011"/>
      tagMap.put("DC.Issued", MetadataField.FIELD_DATE);
      //  <meta name="DC.identifier" scheme="URI"
      //content="info:doi/10.1179/096979511798967106"/>
      tagMap.put("DC.identifier", new MetadataField(
          MetadataField.FIELD_DOI, MetadataField.extract(doiPattern,1)));
      // <meta name="DCTERMS.isPartOf" scheme="URI" content="urn:ISSN:0969-7950"/>
      tagMap.put("DCTERMS.isPartOf", new MetadataField(
          MetadataField.FIELD_ISSN, MetadataField.extract(issnPattern,1)));
      // <meta name="DC.publisher" content="Manchester University Press">
      tagMap.put("DC.publisher", MetadataField.DC_FIELD_PUBLISHER);
      // will extract FIELD_PUBLISHER from tdb
      
      /* 
       * Currently the extract using pattern will put an actual "null" in to the 
       * value list for they stated key if the pattern doesn't match. 
       * This will mean that the value cannot be later overwritten.  
       * So for now, do this manually after cooking... 
       */
      /*
      // <meta name="DC.bibliographicCitation" content="Visual Culture in Britain">
      tagMap.put("DCTERMS.bibliographicCitation", new MetadataField(
           ING_FIELD_JOURNAL_TITLE,
           MetadataField.extract(splitMetaPattern,1)));
       //<meta name="DCTERMS.bibliographicCitation" 
       // content="The British Journal of Development
       //   Disabilities, 57, 113, 123-132(10)"/>
      tagMap.put("DCTERMS.bibliographicCitation", new MetadataField(
          MetadataField.FIELD_VOLUME, 
          MetadataField.extract(splitMetaPattern,2)));
      tagMap.put("DCTERMS.bibliographicCitation", new MetadataField(
          MetadataField.FIELD_ISSUE,
          MetadataField.extract(splitMetaPattern,3)));
      tagMap.put("DCTERMS.bibliographicCitation", new MetadataField(
          MetadataField.FIELD_START_PAGE, 
          MetadataField.extract(splitMetaPattern,4)));
      tagMap.put("DCTERMS.bibliographicCitation", new MetadataField(
          MetadataField.FIELD_END_PAGE, 
          MetadataField.extract(splitMetaPattern,5))); */
      tagMap.put("crawler.fulltextlink", MetadataField.FIELD_ACCESS_URL);
    }
    
    private static final Pattern fullPattern = Pattern.compile(
        "(.*)[,](.*)[,](.*)[,]([^-]+)[-]([^-()]+)", Pattern.CASE_INSENSITIVE);
    // go for the first 3 item - check for numbers because of possible , in title
    private static final Pattern noPagePattern = Pattern.compile(
        "(.*)[,](.*)[,](.*)[,]", Pattern.CASE_INSENSITIVE);
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
      
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      
      /* 
       * Handle the biobliographicCitation manually
       * While this isn't particularly pretty, it is equivalent to the tagMap with extract
       * but handles mismatches more gracefully
       */
      String raw_biblio = am.getRaw("DCTERMS.bibliographicCitation");
      if (raw_biblio != null && !(raw_biblio.isEmpty())) {
        Boolean hadAMatch = true;
        // <meta name=\"DCTERMS.bibliographicCitation\"
        Matcher m = fullPattern.matcher(raw_biblio);
        // eg. content="The British Journal of Development Disabilities, 57, 113, 123-132(10)"/>
        if (!(m.find())) { 
          // full pattern didn't match, see if we match shortened pattern
          m = noPagePattern.matcher(raw_biblio);
          // eg. content=\"Bronte Studies, 37, 4, iii"</meta>  
          hadAMatch = m.find();
        }
        if (hadAMatch) {
          // use what we did find with the matcher - we know we at least had 3 groups... 
          if (!(m.group(1)).isEmpty()) { am.put(MetadataField.FIELD_PUBLICATION_TITLE, m.group(1)); }
          if (!(m.group(2)).isEmpty()) { am.put(MetadataField.FIELD_VOLUME,  m.group(2));}
          if (!(m.group(3)).isEmpty()) { am.put(MetadataField.FIELD_ISSUE,  m.group(3));}
          if (m.groupCount() > 3) {
            // we matched the full pattern, so we have groups 4&5 as well
            if (!(m.group(4)).isEmpty()) { am.put(MetadataField.FIELD_START_PAGE,  m.group(4));}
            if (!(m.group(5)).isEmpty()) { am.put(MetadataField.FIELD_END_PAGE,  m.group(5));}
          }
        } else {
          // eg  content="Visual Culture in Britain"/>
          // we can't really guess - the title might have commas, just put the whole thing in....
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, raw_biblio);
        }
      }
      return am;
    }
  }
}

