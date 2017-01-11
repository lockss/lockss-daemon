/*
 * $Id$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.ers;

import java.io.IOException;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * The following are the default tags:
 *     T1, AU, JF,DO, PB, VL, IS, SP,EP, DA
 * Should the use of RIS become more common, we can generalize this
 *  NOTE: for European Respiratory Society I have found the following
 * TY  - BOOK
 * AU  - 
 * A2  - Bankier, A.
 * Gevenois, P.A.
 * PY  - 2004
 * T1  - Imaging (out of print)
 * PB  - European Respiratory Society
 * SP  - 363
 * DA  - 2004-12-01 00:00:00
 * SN  - 9781904097907
 * M3  - 10.1183/1025448x.erm3004
 * N2  - This line can be quite long... 
 * UR  - http://erspublications.com/content/9781904097907/9781904097907
 * ER  - 
 */
public class ERSRisMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ERSRisMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside ERS Metadata extractor factory for RIS files");

    ERSRisMetadataExtractor ers_ris = new ERSRisMetadataExtractor();

    ers_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ers_ris.addRisTag("A2", MetadataField.FIELD_AUTHOR);
    ers_ris.addRisTag("SN", MetadataField.FIELD_ISBN);
    ers_ris.addRisTag("M3", MetadataField.FIELD_DOI);
    // Do not use UR listed in the ris file! It will get set to full text CU by daemon
    return ers_ris;
  }

  public static class ERSRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      
      // this extracts from th file and cooks the data according to the map
      ArticleMetadata am = extract(target, cu); 

      // post-cook processing...
      // check for existence of content file - return without emitting if not there
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      ArchivalUnit au = cu.getArchivalUnit();
      CachedUrl fileCu = au.makeCachedUrl(pdfName);
      log.debug3("Check for existence of " + pdfName);
      if(fileCu == null || !(fileCu.hasContent())) {
        log.debug3(pdfName + " was not in cu");
        return; // do not emit, just return - no content
      }

      // correction or fallback values in to cooked map here
      
      // These are books, we know that. So set the publication title to the title
      am.put(MetadataField.FIELD_PUBLICATION_TITLE,  am.getRaw("T1"));
      am.put(MetadataField.FIELD_ACCESS_URL, pdfName);
 
      emitter.emitMetadata(cu, am);
    }
    
  }

}
