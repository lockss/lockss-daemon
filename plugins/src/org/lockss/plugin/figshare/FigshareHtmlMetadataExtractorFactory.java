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

package org.lockss.plugin.figshare;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.config.TdbAu;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.extractor.MetadataField.Validator;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page http://api.figshare.com/v1/articles/23?format=html
            <meta name="citation_title" content="&lt;p&gt;Barchart of the prevalence of intestinal schistosomiasis by each diagnostic technique (as well as pooling techniques, DEDM* representing results obtained from Kato-Katz, Percoll and FLOTAC methods) for the examined children.&lt;/p&gt;">
            <meta name="citation_author" content="Martha Betson">
            <meta name="citation_author" content="Narcis B. Kabatereine">
            <meta name="citation_author" content="Moses Arinaitwe">
            <meta name="citation_author" content="J. Russell Stothard">
            <meta name="citation_author" content="Moses Adriko">
            <meta name="citation_author" content="Candia Rowell">
            <meta name="citation_author" content="Fred Besiyge">
            <meta name="citation_author" content="Jose C. Sousa-Figuereido">
            <meta name="citation_doi" content="doi:http://dx.doi.org/10.1371/journal.pntd.0000938.g001" />
            <meta name="citation_publication_date" content="02:16, Jan 04, 2011">

 */

public class FigshareHtmlMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("FigShareHtmlMetadataExtractorFactory");

  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new FigshareHtmlMetadataExtractor();
  }

  public static class FigshareHtmlMetadataExtractor
    implements FileMetadataExtractor {
    
    //supplementing Metadatafield.FIELD_DOI with locally defined
    //PLOS_DOI, which is similar to Metadatafield.FIELD_DOI, but can
    // have multiple values
    // Also, we supply a validator to normalise the "http://dx.doi.org/"
    // prefixed to the rest of the figshare/PLOS doi
    //http://dx.doi.org/10.1371/journal.pgen.1003809.t003  multi PLOS doi
    //http://dx.doi.org/10.1371/journal.pone.0029374.s001  single PLOS doi
    //http://dx.doi.org/10.6084/m9.figshare.809559  figshare doi (valid doi)^doi:http://dx\.doi\.org/10\.[\w]+/[^/]+$
    //private static Pattern PLOS_DOI_PAT = Pattern.compile("(^doi:http://dx\\.doi\\.org/)(10\\.[\\d]{4,}/[^/]+$)", Pattern.CASE_INSENSITIVE);
    // (^doi:)((http://dx\.doi\.org/)?10\.[\d]{4,}/[^/]+$)
    private static Pattern FIG_DOI_PAT = Pattern.compile("(^doi:)?((http://dx\\.doi\\.org/)?10\\.[\\d]{4,}/[^/]+$)", Pattern.CASE_INSENSITIVE);
    private static Pattern PLOS_DOI_PAT = Pattern.compile("(^http://dx\\.doi\\.org/)(10\\.[\\d]{4,}/[^/]+$)", Pattern.CASE_INSENSITIVE);
    private static final String DOI_REPL = "$2";
    private static boolean isPlosDoi(String doi) {
      if (doi == null) {
        return false;
      }
      Matcher m = FIG_DOI_PAT.matcher(doi);
      if(!m.matches()){
        return false;
      }
      return true;
    }
    private static String normalisePlosDoi(String doi) {
      String nVal = null;
      // this will strip "doi:" if it's there
      Matcher figM = FIG_DOI_PAT.matcher(doi);
      if (figM.matches()) {
        nVal = figM.replaceFirst(DOI_REPL);
      } else {
        nVal = doi;
      }
      // this will strip the "http://dx.doi.org/", if there
      Matcher plosM = PLOS_DOI_PAT.matcher(nVal);
      if (plosM.matches()) {
        nVal = plosM.replaceFirst(DOI_REPL);
        return nVal;
      }
      return nVal;
    }
    private static  Validator doiValid = new Validator() {
      public String validate(ArticleMetadata am,MetadataField field,String val)
      throws MetadataException.ValidationException {
        if (MetadataUtil.isDoi(val)){
          return val;
        } else if (isPlosDoi(val)) {
          String newVal = normalisePlosDoi(val);
          // check our normalised doi - is it valid?
          if (MetadataUtil.isDoi(newVal))
            return newVal;
        }
        throw new MetadataException.ValidationException("Illegal DOI: " 
              + val);
        }
    };
   
    public static final String PLOS_DOI_KEY = "PLOS_doi";
    public static final MetadataField FIGSHARE_DOI = new MetadataField(
        MetadataField.KEY_DOI, Cardinality.Single, doiValid);
    public static final MetadataField FIGSHARE_PLOS_DOI = new MetadataField(
        PLOS_DOI_KEY, Cardinality.Multi, doiValid, MetadataField.splitAt("; "));   
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_author",new MetadataField(MetadataField.FIELD_AUTHOR));
      //tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_doi", FIGSHARE_DOI);
      tagMap.put("PLOS_citation_doi", FIGSHARE_PLOS_DOI);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
    }
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {

      if (cu == null) {
        throw new IllegalArgumentException("extract() called with null CachedUrl");
      }
      log.debug3("extract()");      
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      log.debug3(" article metadata=["+am+"]");      
      
      // If the publisher doesn't appear in the meta tags, set it from the TitleConfig value (if there)
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        
        // We can try to get the publisher from the tdb file.  This would be the most accurate
        TdbAu tdbau = cu.getArchivalUnit().getTdbAu(); // returns null if titleConfig is null 
        String publisher = (tdbau == null) ? null : tdbau.getPublisherName();
        
        if (publisher == null) { 
          // Last chance, we can try to get the publishing platform off the plugin
          Plugin pg = cu.getArchivalUnit().getPlugin();
          publisher = (pg == null) ? null : pg.getPublishingPlatform();
         }
        if (publisher != null) {
          am.put(MetadataField.FIELD_PUBLISHER, publisher);
        }
      }
      emitter.emitMetadata(cu, am);
    }
  }
}
