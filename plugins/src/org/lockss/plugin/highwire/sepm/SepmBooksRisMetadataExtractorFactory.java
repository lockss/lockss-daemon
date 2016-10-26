/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.highwire.sepm;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
//
//TY - CHAP
//A1 - RETALLACK, GREGORY J.
//Y1 - 2013/01/01
//PY - 2013/01/01
//T1 - A Short History and Long Future for Paleopedology (DEFAULT MAP)
//TI - New Frontiers in Paleopedology and Terrestrial Paleoclimatology
//SP - 5 (DEFAULT MAP)
//EP - 16 (DEFAULT MAP)
//VL - 104 (DEFAULT MAP)
//UR - http://sp.sepmonline.org/content/sepsp104/1/SEC2.abstract
//N2 - long line of text
//PB - SEPM (Society for Sedimentary Geology) (DEFAULT MAP)
//ER - 
// No DA for date;  no DO for doi; A1 instead of AU for author; TI is volume (book title) 
// get DOI from url, 
// no ISSN, ISBN here, get from tdb file
 


public class SepmBooksRisMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SepmBooksRisMetadataExtractorFactory.class);
  
  private static final Pattern DOI_PATTERN = Pattern.compile("gcadoi=(10\\.[0-9]+)%2F(.*)$");
  // cannot change group ordering group(1) must be the TAG and group(2) must be the value
  // (?: )? - allow for one or two spaces between the tag and the hyphen
  // 
  public static final Pattern EXTENDED_RIS_PATTERN = 
      Pattern.compile("^([A-Z][A-Z0-9]) (?: )?- (.*)$");
  

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside SepmBooks Metadata extractor factory for RIS files");

    SepmRisMetadataExtractor sepm_ris = new SepmRisMetadataExtractor();

    sepm_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    sepm_ris.addRisTag("TI", MetadataField.FIELD_PUBLICATION_TITLE);
    sepm_ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    return sepm_ris;
  }

  public static class SepmRisMetadataExtractor
  extends RisMetadataExtractor {

    // SEPM uses only one space before hyphen after RIS tag
     public SepmRisMetadataExtractor() {
      super(EXTENDED_RIS_PATTERN);
    }

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); 

      /* 
       * if we got to a page without valid ris, eg "this page not found" html page
       * don't emit empty metadata (because defaults would get put in
       * Must do this after cooking, because it checks size of cooked info
       */
      if (am.isEmpty()) {
        return;
      }

      /*
       * RIS data can be variable.  We don't have any way to add priority to
       * the cooking of data, so fallback to alternate values manually
       */
      if (am.get(MetadataField.FIELD_DATE) == null) {
        if (am.getRaw("Y1") != null) { // if DA wasn't there, use Y1
          am.put(MetadataField.FIELD_DATE, am.getRaw("Y1"));
        } else if (am.getRaw("PY") != null) { // if how about P1?
          am.put(MetadataField.FIELD_DATE, am.getRaw("PY"));
        }
      }  

     /*
      * Fill in DOI from the URL
      * issn, isbn and correction of a non-existent access_url happen in base
      */
      String cu_url = cu.getUrl();
      Matcher doi_mat = DOI_PATTERN.matcher(cu_url);
      if (doi_mat.find()) {
        String doi1 = doi_mat.group(1);
        String doi2 = doi_mat.group(2);
        am.put(MetadataField.FIELD_DOI,  doi1 + "/" + doi2);
      }
        
      emitter.emitMetadata(cu, am);
    }
    }

}
