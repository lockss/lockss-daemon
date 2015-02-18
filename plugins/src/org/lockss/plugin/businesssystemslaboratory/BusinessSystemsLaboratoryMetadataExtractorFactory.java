/* $Id$ */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.businesssystemslaboratory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * This publisher provides inconsistent url structure, so not possible for 
 * the article iterator builder to guess the abstract where some metadata can
 * be found, from the full text pdf url.  The abstract html also has syntax 
 * error near where some metadata found. Hence, this extractor pulls metadata
 * from the tdb file and gets issue number of pdf url.
 */
public class BusinessSystemsLaboratoryMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(
      BusinessSystemsLaboratoryMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new BusinessSystemsLaboratoryMetadataExtractor();
  }

  // Gets metadata from tdb: date, eissn, journal.title, publisher, volume
  // and issue number from pdf url
  public static class BusinessSystemsLaboratoryMetadataExtractor 
    implements FileMetadataExtractor {
    
    // http://www.business-systems-review.org/
    //            BSR.Vol.2-Iss.1-Massaro.et.al.Organising.Innovation.pdf
    private Pattern ISSUE_PATTERN = 
        Pattern.compile("/BSR[.-]Vol[.-][0-9]+[.-](Iss?|n)[.-]([0-9]+)[^/]+");
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
      
      log.debug3("Metadata - cachedurl cu:" + cu.getUrl());
            
      ArticleMetadata am = new ArticleMetadata();
      Matcher mat = ISSUE_PATTERN.matcher(cu.getUrl());
      if (mat.find()) {
        String issue = mat.group(2);
        am.put(MetadataField.FIELD_ISSUE, issue);
      }
      emitter.emitMetadata(cu, am);
    }
    
  }
  
}
 
